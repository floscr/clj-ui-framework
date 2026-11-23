(ns ui.canvas
  "Canvas: a frame that pans and zooms a world of nodes and the edges
   between them, and that code can steer (fit, reveal, follow). For flows,
   pipelines and any graph too big to draw at one size.

   Renders data-attributed markup; the behavior (gestures, keys, the camera)
   lives in the shared JS runtime (ui.js.canvas, bundled in ui-runtime.js).
   Squint/Replicant SPAs that don't load ui-runtime.js need a side-effect
   require of the runtime module.

   Two layers. The frame and its world take any children:

     (canvas/canvas {:id \"board\" :label \"Board\"}
       (canvas/world {}
         (canvas/node {:id \"a\" :x 24 :y 24} [:div \"Plan the launch\"]))
       (canvas/controls {}))

   and `flow` draws a run from data, the way a workflow reads: every node a
   step card that says what it does, the path taken a solid rail, a spinner
   on the step that runs, and the camera on it:

     (canvas/flow {:id \"run\" :label \"Flow pr-1\"
                   :nodes [{:id \"push\" :title \"push\" :description \"Both branches\"
                            :icon :terminal :status :done :x 0 :y 0}
                           {:id \"ci\" :title \"ci\" :description \"Checks of both PRs\"
                            :icon :clock :status :current :x 0 :y 92}]
                   :edges [{:from \"push\" :to \"ci\" :taken true}]})

   Positions are in world units (pixels at zoom 1); the caller lays the
   graph out. `edge-path` and `bounds` are the geometry `flow` uses.

   Steering from code (browser targets): `fit!`, `reveal!`, `center!`,
   `follow!`, `zoom-by!`, `zoom-to!`, `pan-by!`, `viewport`, each taking the
   canvas id first. On every target the followed node can also be changed by
   rendering another :follow: the runtime moves the camera to it."
  (:require [clojure.string :as str]
            [ui.button :as button]
            [ui.icon :as icon]
            [ui.spinner :as spinner]
            [ui.toolbar :as toolbar]
            [ui.util :as util]))

(def step-width
  "Width of a step card in world units."
  228)

(def step-height
  "Height of a step card in world units."
  56)

(def ^:private corner 10)
(def ^:private rail-gap 30)
(def ^:private tip-length 5)

;; ── Helpers ─────────────────────────────────────────────────────────

(defn- style
  "Inline style from [name value] pairs, in the target's :style shape."
  [pairs]
  #?(:squint (reduce (fn [m [k v]] (assoc m k v)) {} pairs)
     :cljs   (reduce (fn [m [k v]] (assoc m (keyword k) v)) {} pairs)
     :clj    (str/join " " (map (fn [[k v]] (str k ": " v ";")) pairs))))

(defn- px [n] (str n "px"))

(defn- el
  "An element with `classes` (a vector) and `attrs`."
  [tag classes attrs & children]
  (into [tag (util/class-attrs classes nil attrs)] children))

;; ── Geometry (pure, shared, testable) ───────────────────────────────

(defn bounds
  "The rectangle around `rects` ({:x :y :w :h} maps) as {:left :top :right
   :bottom}, nil for none."
  [rects]
  (when (seq rects)
    {:left   (apply min (map (fn [r] (:x r)) rects))
     :top    (apply min (map (fn [r] (:y r)) rects))
     :right  (apply max (map (fn [r] (+ (:x r) (:w r))) rects))
     :bottom (apply max (map (fn [r] (+ (:y r) (:h r))) rects))}))

(defn edge-path
  "The line from rect `from` to rect `to` ({:x :y :w :h}, whole numbers) as
   {:d path-data :tip [x y degrees]}, the tip being where its arrowhead
   points. Without :rail it leaves the bottom of `from` and curves into the
   top of `to` (a step forward, down the page). With :rail :left or :right
   and :rail-x it leaves the side of `from`, runs along the vertical line at
   :rail-x and enters the same side of `to`: the way back up without
   crossing what lies between."
  [from to {:keys [rail rail-x]}]
  (let [side (when rail (util/kw-name rail))]
    (if side
      (let [left? (= side "left")
            s  (if left? 1 -1)
            ay (+ (:y from) (quot (:h from) 2))
            by (+ (:y to) (quot (:h to) 2))
            sx (if left? (:x from) (+ (:x from) (:w from)))
            tx (if left? (:x to) (+ (:x to) (:w to)))
            ;; the corner turns toward the target, up or down
            v  (if (< by ay) -1 1)]
        {:d (str "M" sx "," ay
                 " H" (+ rail-x (* s corner))
                 " Q" rail-x "," ay " " rail-x "," (+ ay (* v corner))
                 " V" (- by (* v corner))
                 " Q" rail-x "," by " " (+ rail-x (* s corner)) "," by
                 " H" (- tx (* s tip-length)))
         :tip [tx by (if left? 0 180)]})
      (let [ax (+ (:x from) (quot (:w from) 2))
            bx (+ (:x to) (quot (:w to) 2))
            ay (+ (:y from) (:h from))
            by (- (:y to) tip-length)
            m  (quot (+ ay by) 2)]
        {:d (str "M" ax "," ay " C" ax "," m " " bx "," m " " bx "," by)
         :tip [bx (:y to) 90]}))))

;; ── Frame ───────────────────────────────────────────────────────────

(defn canvas-attrs
  "Data attributes for the frame: what the runtime reads."
  [{:keys [id label follow initial min-zoom max-zoom padding gestures insets]}]
  (cond-> {:data-ui-canvas "true"
           :tabindex "0"
           :role "application"}
    id       (assoc :id id)
    label    (assoc :aria-label label)
    follow   (assoc :data-follow (util/kw-name follow))
    initial  (assoc :data-initial (util/kw-name initial))
    min-zoom (assoc :data-min-zoom (str min-zoom))
    max-zoom (assoc :data-max-zoom (str max-zoom))
    padding  (assoc :data-padding (str padding))
    gestures (assoc :data-gestures (util/kw-name gestures))
    (:top insets)    (assoc :data-inset-top (str (:top insets)))
    (:right insets)  (assoc :data-inset-right (str (:right insets)))
    (:bottom insets) (assoc :data-inset-bottom (str (:bottom insets)))
    (:left insets)   (assoc :data-inset-left (str (:left insets)))))

(defn canvas-class-list
  "Vector of CSS classes for the frame. :grid false leaves the dots out."
  [{:keys [grid]}]
  (cond-> ["canvas"]
    (not (false? grid)) (conj "canvas-grid")))

(defn canvas-classes
  "Space-joined class string for the frame."
  [opts]
  (str/join " " (canvas-class-list opts)))

(defn canvas
  "The frame. Children: one `world`, then what floats over it (`controls`,
   `tag`).

   Props:
     :id        - element id; what the steering fns take
     :label     - accessible name (aria-label)
     :follow    - id of the node the camera keeps centered
     :initial   - :fit (default without :follow), :follow (default with
                  it) or :none (the viewport at 0 0 1)
     :min-zoom  - smallest scale (default 0.25)
     :max-zoom  - largest scale (default 2.5)
     :padding   - pixels kept free around what fit and reveal show (24)
     :insets    - {:top :right :bottom :left} in pixels: how much of the
                  frame something covers (a panel floating over it). The
                  camera fits, reveals and follows in the part left free
     :gestures  - :canvas (default: it takes the wheel and every drag) or
                  :page (inside a scrolling page: the page keeps the wheel
                  and vertical swipes)
     :height    - frame height in pixels; without it, size it with CSS
     :grid      - false leaves the dot grid out
     :class     - additional CSS classes
     :attrs     - additional HTML attributes"
  [{:keys [id label follow initial min-zoom max-zoom padding insets gestures height grid class attrs]}
   & children]
  (into [:div (util/class-attrs
               (canvas-class-list {:grid grid})
               class
               (merge (canvas-attrs {:id id :label label :follow follow :initial initial
                                     :min-zoom min-zoom :max-zoom max-zoom
                                     :padding padding :insets insets
                                     :gestures gestures})
                      (when height {:style (style [["height" (px height)]])})
                      attrs))]
        (remove nil? children)))

(defn world
  "The layer the viewport moves: everything in it pans and zooms as one.
   Props: :width and :height (world units; needed when it holds `edges`),
   :class, :attrs."
  [{:keys [width height class attrs]} & children]
  (into [:div (util/class-attrs
               ["canvas-world"]
               class
               (merge {:data-ui-canvas-world "true"}
                      (when (and width height)
                        {:style (style [["width" (px width)] ["height" (px height)]])})
                      attrs))]
        children))

(defn node
  "A positioned box in the world that fit, reveal and follow can find.
   Props: :id, :x and :y (world units), :width and :height (optional),
   :class, :attrs."
  [{:keys [id x y width height class attrs]} & children]
  (into [:div (util/class-attrs
               ["canvas-node"]
               class
               (merge {:data-ui-canvas-node (util/kw-name id)
                       :style (style (cond-> [["left" (px x)] ["top" (px y)]]
                                       width  (conj ["width" (px width)])
                                       height (conj ["height" (px height)])))}
                      attrs))]
        children))

;; ── Edges ───────────────────────────────────────────────────────────

(defn edge-class-list
  "Vector of CSS classes for an edge: :taken draws it in full, :live moves."
  [{:keys [taken live]}]
  (cond-> ["canvas-edge"]
    taken (conj "canvas-edge-taken")
    live  (conj "canvas-edge-live")))

(defn edge
  "One edge between two rects, for inside `edges`.
   Props: :from and :to ({:x :y :w :h}), :rail (:left | :right) with
   :rail-x for a way back, :taken, :live, :class."
  [{:keys [from to rail rail-x taken live class]}]
  (let [{:keys [d tip]} (edge-path from to {:rail rail :rail-x rail-x})
        [tx ty deg] tip]
    [:g (util/class-attrs (edge-class-list {:taken taken :live live}) class nil)
     [:path {:d d}]
     (el :path ["canvas-edge-tip"]
         {:transform (str "translate(" tx "," ty ") rotate(" deg ")")
          :d "M-6,-3.5 L0,0 L-6,3.5 Z"})]))

(defn edges
  "The SVG layer under the nodes. Children: `edge`s.
   Props: :width and :height (the world's), :class, :attrs."
  [{:keys [width height class attrs]} & children]
  (into [:svg (util/class-attrs
               ["canvas-edges"]
               class
               (merge {:width (str width) :height (str height)
                       :viewBox (str "0 0 " width " " height)
                       :aria-hidden "true"}
                      attrs))]
        children))

;; ── Step card ───────────────────────────────────────────────────────

(defn step-class-list
  "Vector of CSS classes for a step card. :status is :done, :current or
   :idle (default); :selected adds the picked ring."
  [{:keys [status selected]}]
  (let [s (or (some-> status util/kw-name) "idle")]
    (cond-> ["canvas-node" "canvas-step" (str "canvas-step-" s)]
      selected (conj "canvas-step-selected"))))

(defn step-classes
  "Space-joined class string for a step card."
  [opts]
  (str/join " " (step-class-list opts)))

(defn step
  "A node drawn as a step of a run: an icon, what it is called and what it
   does, and its state at the end of the card (a check when done, a spinner
   while it runs, a ring before).

   Props:
     :id          - node id
     :x :y        - position in world units
     :title       - its name
     :description - one short line under it
     :icon        - ui.icon name
     :status      - :done, :current or :idle (default)
     :selected    - boolean
     :class       - additional CSS classes
     :attrs       - additional HTML attributes (a click handler goes here)"
  [{:keys [id x y title description icon status selected class attrs]}]
  (let [s (or (some-> status util/kw-name) "idle")]
    [:div (util/class-attrs
           (step-class-list {:status status :selected selected})
           class
           (merge {:data-ui-canvas-node (util/kw-name id)
                   :tabindex "0"
                   :style (style [["left" (px x)] ["top" (px y)]
                                  ["width" (px step-width)] ["height" (px step-height)]])}
                  attrs))
     (when icon
       (el :span ["canvas-step-icon"] nil (icon/icon {:icon-name icon :size :sm})))
     (el :span ["canvas-step-text"] nil
         (el :span ["canvas-step-title"] nil title)
         (when description
           (el :span ["canvas-step-description"] nil description)))
     (el :span ["canvas-step-status"] {:aria-label s}
         (cond
           (= s "done")    (icon/icon {:icon-name :check :size :sm})
           (= s "current") (spinner/spinner {:size :xs})
           :else           (el :span ["canvas-step-dot"] nil)))]))

;; ── What floats: in the world, over the frame ───────────────────────

(defn note
  "A small pill in the world, beside a node: what the running step is at.
   Props: :x :y (world units), :class, :attrs."
  [{:keys [x y class attrs]} & children]
  (into [:div (util/class-attrs
               ["canvas-note"]
               class
               (merge {:style (style [["left" (px x)] ["top" (px y)]])} attrs))]
        children))

(defn tag
  "A quiet label in the frame's top left corner: what this canvas shows.
   Props: :live (a dot that says it is running), :class, :attrs."
  [{:keys [live class attrs]} & children]
  (into [:div (util/class-attrs ["canvas-tag"] class attrs)
         (when live (el :span ["canvas-tag-dot"] {:aria-hidden "true"}))]
        children))

(defn controls
  "The camera's buttons, in the frame's top right corner: follow, zoom out,
   the zoom in percent (press it for 100%), zoom in, fit. The runtime binds
   them; they work on every target.

   Props:
     :follow - false leaves the follow button out (a canvas that follows
               nothing)
     :zoom   - false leaves the three zoom controls out (narrow frames)
     :fit    - false leaves the fit button out
     :class  - additional CSS classes
     :attrs  - additional HTML attributes"
  [{:keys [follow zoom fit class attrs]}]
  (let [act (fn [action label] {:data-ui-canvas-act action :aria-label label})]
    (into [:div (util/class-attrs ["canvas-controls"] class attrs)]
          [(into (toolbar/toolbar {})
                 (remove nil?
                         [(when-not (false? follow)
                            (button/button {:variant :ghost :size :sm :icon-left :map-pin
                                            :attrs (assoc (act "follow" "Follow the current step")
                                                          :aria-pressed "false")}
                                           "Follow"))
                          (when (and (not (false? follow)) (not (false? zoom)))
                            (toolbar/toolbar-separator {}))
                          (when-not (false? zoom)
                            (button/button {:variant :ghost :size :sm :icon :minus
                                            :attrs (act "zoom-out" "Zoom out")}))
                          (when-not (false? zoom)
                            (button/button {:variant :ghost :size :sm
                                            :class "canvas-zoom"
                                            :attrs (assoc (act "zoom-reset" "Zoom to 100%")
                                                          :data-ui-canvas-zoom "true")}
                                           "100%"))
                          (when-not (false? zoom)
                            (button/button {:variant :ghost :size :sm :icon :plus
                                            :attrs (act "zoom-in" "Zoom in")}))
                          (when (and (not (false? fit)) (or (not (false? zoom)) (not (false? follow))))
                            (toolbar/toolbar-separator {}))
                          (when-not (false? fit)
                            (button/button {:variant :ghost :size :sm :icon :maximize
                                            :attrs (act "fit" "Fit everything")}))]))])))

;; ── A run from data ─────────────────────────────────────────────────

(defn- step-rect [{:keys [x y]}]
  {:x x :y y :w step-width :h step-height})

;; `flow` takes :edges, :note, :tag and :controls as props, which hide the fns
(defn- edges-el [opts] (edges opts))
(defn- note-el [opts text] (note opts text))
(defn- tag-el [opts text] (tag opts text))
(defn- controls-el [opts] (controls opts))

(defn flow
  "A run drawn from data: step cards on a rail, the camera on the step that
   runs.

   Props:
     :id       - element id; what the steering fns take
     :label    - accessible name
     :nodes    - [{:id :title :description :icon :status :x :y}], positions
                 in world units; lay them out top to bottom, one row every
                 92 units or more
     :edges    - [{:from id :to id :taken bool :rail :left | :right}];
                 :rail sends a way back up the side
     :follow   - node id the camera follows (default: the :current node)
     :tag      - label for the top left corner
     :note     - text beside the current step (\"check 4, next in 1:12\")
     :controls - false leaves the buttons out; a map is passed to `controls`
     :height   - frame height in pixels
     :gestures - :canvas (default) or :page
     :min-zoom :max-zoom :padding :insets :grid :class :attrs - as for
                `canvas`"
  [{:keys [id label nodes edges follow tag note controls height gestures
           min-zoom max-zoom padding insets grid class attrs]}]
  (let [status   (fn [n] (some-> (:status n) util/kw-name))
        by-id    (reduce (fn [m n] (assoc m (util/kw-name (:id n)) n)) {} nodes)
        current  (first (filter (fn [n] (= "current" (status n))) nodes))
        followed (or (some-> follow util/kw-name) (some-> current :id util/kw-name))
        frame    (or (bounds (map step-rect nodes)) {:left 0 :top 0 :right 0 :bottom 0})
        rail-l   (- (:left frame) rail-gap)
        rail-r   (+ (:right frame) rail-gap)
        w        (+ (:right frame) rail-gap rail-gap)
        h        (+ (:bottom frame) rail-gap)
        ;; taken edges last, so they lie on top of the dim ones
        taken?   (fn [e] (boolean (:taken e)))
        ordered  (concat (remove taken? edges) (filter taken? edges))]
    (canvas {:id id :label label :follow followed :height height :gestures gestures
             :min-zoom min-zoom :max-zoom max-zoom :padding padding :insets insets :grid grid
             :class (if class (str "canvas-flow " class) "canvas-flow")
             :attrs attrs}
            (into (world {:width w :height h}
                         (into (edges-el {:width w :height h})
                               (keep (fn [{:keys [from to taken rail]}]
                                       (let [a (get by-id (util/kw-name from))
                                             b (get by-id (util/kw-name to))
                                             side (some-> rail util/kw-name)]
                                         (when (and a b)
                                           (edge {:from (step-rect a) :to (step-rect b)
                                                  :rail rail
                                                  :rail-x (when side (if (= side "left") rail-l rail-r))
                                                  :taken taken
                                                  :live (and taken current
                                                             (= (util/kw-name to) (util/kw-name (:id current))))}))))
                                     ordered)))
                  (concat
                   (map step nodes)
                   (when (and note current)
                     [(note-el {:x (+ (:x current) step-width 14)
                                :y (+ (:y current) (quot (- step-height 24) 2))}
                               note)])))
            (when tag (tag-el {:live (some? current)} tag))
            (when-not (false? controls)
              ;; nothing to follow: no follow button
              (controls-el (cond-> (if (map? controls) controls {})
                             (nil? followed) (assoc :follow false)))))))

;; ── Steering from code (browser targets) ────────────────────────────

(do
  #?@(:squint
      [(defn- call! [method args]
         (when-let [api (aget js/window "__uiCanvas")]
           (.apply (aget api method) api (into-array args))))
       (defn fit!
         "Show everything in canvas `id`. opts: {:padding :maxZoom}."
         ([id] (call! "fit" [id]))
         ([id opts] (call! "fit" [id opts])))
       (defn reveal!
         "Pan canvas `id` just far enough that node `node-id` is in view."
         ([id node-id] (call! "reveal" [id node-id]))
         ([id node-id opts] (call! "reveal" [id node-id opts])))
       (defn center!
         "Center node `node-id` in canvas `id`, at `zoom` when given."
         ([id node-id] (call! "center" [id node-id]))
         ([id node-id zoom] (call! "center" [id node-id zoom])))
       (defn follow!
         "Have the camera of canvas `id` follow node `node-id` (nil: none)."
         [id node-id] (call! "follow" [id node-id]))
       (defn zoom-by!
         "Scale canvas `id` by `factor` around its center."
         [id factor] (call! "zoomBy" [id factor]))
       (defn zoom-to!
         "Set the scale of canvas `id` (1 is its normal size)."
         [id zoom] (call! "zoomTo" [id zoom]))
       (defn pan-by!
         "Move canvas `id` by screen pixels."
         [id dx dy] (call! "panBy" [id dx dy]))
       (defn viewport
         "The viewport of canvas `id`: {x y zoom}."
         [id] (call! "viewport" [id]))]

      :cljs
      [(defn- call! [method args]
         (when-let [api (aget js/window "__uiCanvas")]
           (.apply (aget api method) api (into-array args))))
       (defn fit!
         "Show everything in canvas `id`. opts: {:padding :maxZoom}."
         ([id] (call! "fit" [id]))
         ([id opts] (call! "fit" [id (clj->js opts)])))
       (defn reveal!
         "Pan canvas `id` just far enough that node `node-id` is in view."
         ([id node-id] (call! "reveal" [id node-id]))
         ([id node-id opts] (call! "reveal" [id node-id (clj->js opts)])))
       (defn center!
         "Center node `node-id` in canvas `id`, at `zoom` when given."
         ([id node-id] (call! "center" [id node-id]))
         ([id node-id zoom] (call! "center" [id node-id zoom])))
       (defn follow!
         "Have the camera of canvas `id` follow node `node-id` (nil: none)."
         [id node-id] (call! "follow" [id node-id]))
       (defn zoom-by!
         "Scale canvas `id` by `factor` around its center."
         [id factor] (call! "zoomBy" [id factor]))
       (defn zoom-to!
         "Set the scale of canvas `id` (1 is its normal size)."
         [id zoom] (call! "zoomTo" [id zoom]))
       (defn pan-by!
         "Move canvas `id` by screen pixels."
         [id dx dy] (call! "panBy" [id dx dy]))
       (defn viewport
         "The viewport of canvas `id` as {:x :y :zoom}."
         [id] (some-> (call! "viewport" [id]) (js->clj :keywordize-keys true)))]))
