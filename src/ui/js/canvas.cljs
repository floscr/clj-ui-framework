(ns canvas
  "Canvas runtime: pans and zooms the world of a [data-ui-canvas] frame and
   moves it from code (fit, reveal, center, follow). After ui-sdk.dev's
   canvas behavior: the viewport is {x y zoom}, x and y placing the world's
   origin inside the frame in screen pixels, zoom 1 its normal size.

   Self-initializing: scans for [data-ui-canvas] at load and on DOM changes.
   The .cljc component (ui.canvas) only renders data-attributed markup.

   DOM contract (rendered by ui.canvas):
     [data-ui-canvas id data-min-zoom data-max-zoom data-padding
      data-initial=fit|follow|none data-follow=<node id> data-gestures=page
      data-inset-top|right|bottom|left=<px>]
       [data-ui-canvas-world]            one transform moves everything in it
         [data-ui-canvas-node=<id>] …    what fit, reveal and follow measure
       [data-ui-canvas-act=follow|fit|zoom-in|zoom-out|zoom-reset] buttons
       [data-ui-canvas-zoom]             text: the zoom in percent

   Engine-managed attributes (styling hooks):
     frame: data-panning, data-following=true|false
     world: data-animate (a move from code; CSS gives it its transition)
     follow button: aria-pressed

   Gestures: drag pans (a press that doesn't move stays a click on what is
   under it), wheel pans, Ctrl/Cmd+wheel and pinch zoom at the pointer. With
   data-gestures=page the page keeps the wheel and vertical swipes (a canvas
   inside a scrolling column): drags, sideways swipes, Ctrl/Cmd+wheel and
   pinch still move it. Keys on the focused frame: + − 0 and the arrows;
   Tab walks the nodes, and the one that takes the focus is revealed.

   Follow: data-follow names the node the camera keeps centered. It is
   re-centered when the attribute changes or the frame resizes; a gesture
   lets go (data-following=false) until the follow button is pressed.

   Insets: data-inset-* say how much of the frame something covers (a panel
   floating over it). Fit, reveal, center and follow work in the part left
   free, and the followed node is re-centered when an inset changes.

   Events (bubbling CustomEvents on the frame):
     ui-canvas-viewport  detail {x y zoom}
     ui-canvas-follow    detail {following}

   API: window.__uiCanvas.fit(id, opts) .reveal(id, nodeId, opts)
        .center(id, nodeId, zoom) .follow(id, nodeId) .zoomBy(id, factor)
        .zoomTo(id, zoom) .panBy(id, dx, dy) .setViewport(id, {x y zoom})
        .viewport(id) .scan()")

(def ^:private pan-threshold 4)
(def ^:private key-pan 60)

(defn- num-attr [el attr fallback]
  (let [v (js/parseFloat (.getAttribute el attr))]
    (if (js/isNaN v) fallback v)))

(defn- clamp [v lo hi]
  (js/Math.min (js/Math.max v lo) hi))

(defn- controller [frame]
  (aget frame "__uiCanvas"))

;; ── Viewport ────────────────────────────────────────────────────────

(defn- emit! [c event-name detail]
  (.dispatchEvent (.-frame c)
                  (js/CustomEvent. event-name #js {:bubbles true :detail detail})))

(defn- paint!
  "Write the viewport to the world. `animate?`: a move from code, which the
   stylesheet eases; a gesture tracks the pointer."
  [c animate?]
  (let [world (.-world c)
        v (.-view c)]
    (if animate?
      (.setAttribute world "data-animate" "true")
      (.removeAttribute world "data-animate"))
    (set! (.-transform (.-style world))
          (str "translate(" (.-x v) "px," (.-y v) "px) scale(" (.-zoom v) ")"))
    (when-let [readout (.querySelector (.-frame c) "[data-ui-canvas-zoom]")]
      (set! (.-textContent readout) (str (js/Math.round (* 100 (.-zoom v))) "%")))
    (emit! c "ui-canvas-viewport" #js {:x (.-x v) :y (.-y v) :zoom (.-zoom v)})))

(defn- set-view! [c x y zoom animate?]
  (aset c "view" #js {:x x :y y :zoom (clamp zoom (.-minZoom c) (.-maxZoom c))})
  (paint! c animate?))

(defn- zoom-at!
  "Zoom to `zoom` keeping the frame point (px, py) where it is."
  [c zoom px py animate?]
  (let [v (.-view c)
        z (clamp zoom (.-minZoom c) (.-maxZoom c))
        k (/ z (.-zoom v))]
    (set-view! c (- px (* (- px (.-x v)) k)) (- py (* (- py (.-y v)) k)) z animate?)))

(defn- frame-center
  "The part of the frame nothing covers (data-inset-*), in frame pixels: its
   center {x y}, size {w h} and corner {left top}."
  [c]
  (let [frame (.-frame c)
        r (.getBoundingClientRect frame)
        l (num-attr frame "data-inset-left" 0)
        t (num-attr frame "data-inset-top" 0)
        w (js/Math.max 1 (- (- (.-width r) l) (num-attr frame "data-inset-right" 0)))
        h (js/Math.max 1 (- (- (.-height r) t) (num-attr frame "data-inset-bottom" 0)))]
    #js {:x (+ l (/ w 2)) :y (+ t (/ h 2)) :w w :h h :left l :top t}))

;; ── Rectangles in world units ───────────────────────────────────────

(defn- rect-of [el]
  #js {:left (.-offsetLeft el) :top (.-offsetTop el)
       :right (+ (.-offsetLeft el) (.-offsetWidth el))
       :bottom (+ (.-offsetTop el) (.-offsetHeight el))})

(defn- node-el [c node-id]
  (when (some? node-id)
    (.querySelector (.-world c) (str "[data-ui-canvas-node=\"" node-id "\"]"))))

(defn- all-bounds
  "The rectangle around every node, nil when there is none."
  [c]
  (let [nodes (js/Array.from (.querySelectorAll (.-world c) "[data-ui-canvas-node]"))]
    (when (pos? (.-length nodes))
      (.reduce nodes
               (fn [acc el]
                 (let [r (rect-of el)]
                   #js {:left (js/Math.min (.-left acc) (.-left r))
                        :top (js/Math.min (.-top acc) (.-top r))
                        :right (js/Math.max (.-right acc) (.-right r))
                        :bottom (js/Math.max (.-bottom acc) (.-bottom r))}))
               (rect-of (aget nodes 0))))))

;; ── Moves from code ─────────────────────────────────────────────────

(defn- opt [opts k fallback]
  (let [v (when (some? opts) (aget opts k))]
    (if (some? v) v fallback)))

(defn- fit-rect!
  "Show `rect` in full: {padding maxZoom animate}."
  [c rect opts]
  (when (some? rect)
    (let [f (frame-center c)
          pad (opt opts "padding" (.-padding c))
          w (js/Math.max 1 (- (.-right rect) (.-left rect)))
          h (js/Math.max 1 (- (.-bottom rect) (.-top rect)))
          z (clamp (js/Math.min (/ (- (.-w f) (* 2 pad)) w)
                                (/ (- (.-h f) (* 2 pad)) h)
                                (opt opts "maxZoom" 1))
                   (.-minZoom c) (.-maxZoom c))]
      (set-view! c
                 (- (+ (.-left f) (/ (- (.-w f) (* w z)) 2)) (* (.-left rect) z))
                 (- (+ (.-top f) (/ (- (.-h f) (* h z)) 2)) (* (.-top rect) z))
                 z
                 (opt opts "animate" true)))))

(defn- reveal-rect!
  "Pan just far enough that `rect` is inside the frame; the zoom stays."
  [c rect opts]
  (when (some? rect)
    (let [f (frame-center c)
          v (.-view c)
          z (.-zoom v)
          pad (opt opts "padding" (.-padding c))
          l (+ (* (.-left rect) z) (.-x v))
          t (+ (* (.-top rect) z) (.-y v))
          r (+ (* (.-right rect) z) (.-x v))
          b (+ (* (.-bottom rect) z) (.-y v))
          lo-x (+ (.-left f) pad)
          hi-x (- (+ (.-left f) (.-w f)) pad)
          lo-y (+ (.-top f) pad)
          hi-y (- (+ (.-top f) (.-h f)) pad)
          dx (cond (< l lo-x) (- lo-x l)
                   (> r hi-x) (- hi-x r)
                   :else 0)
          dy (cond (< t lo-y) (- lo-y t)
                   (> b hi-y) (- hi-y b)
                   :else 0)]
      (set-view! c (+ (.-x v) dx) (+ (.-y v) dy) z (opt opts "animate" true)))))

(defn- center-rect! [c rect zoom animate?]
  (when (some? rect)
    (let [f (frame-center c)
          z (clamp (if (some? zoom) zoom (.-zoom (.-view c))) (.-minZoom c) (.-maxZoom c))]
      (set-view! c
                 (- (.-x f) (* (/ (+ (.-left rect) (.-right rect)) 2) z))
                 (- (.-y f) (* (/ (+ (.-top rect) (.-bottom rect)) 2) z))
                 z animate?))))

;; ── Follow ──────────────────────────────────────────────────────────

(defn- sync-follow-button! [c]
  (.forEach (js/Array.from (.querySelectorAll (.-frame c) "[data-ui-canvas-act=\"follow\"]"))
            (fn [b] (.setAttribute b "aria-pressed" (if (.-following c) "true" "false")))))

(defn- set-following! [c on?]
  (when (not= on? (.-following c))
    (aset c "following" on?)
    (.setAttribute (.-frame c) "data-following" (if on? "true" "false"))
    (sync-follow-button! c)
    (emit! c "ui-canvas-follow" #js {:following on?})))

(defn- follow-now!
  "Center the followed node, when there is one and the camera follows."
  [c animate?]
  (when (.-following c)
    (when-let [el (node-el c (.getAttribute (.-frame c) "data-follow"))]
      (center-rect! c (rect-of el) nil animate?))))

(defn- let-go! [c]
  (set-following! c false))

;; ── Gestures ────────────────────────────────────────────────────────

(defn- interactive? [target]
  (some? (.closest target "button, a, input, select, textarea, [data-ui-canvas-act]")))

(defn- bind-gestures! [c]
  (let [frame (.-frame c)
        points (js/Map.)
        state #js {:panning false :moved false :pinch nil :startX 0 :startY 0}]
    (.addEventListener
     frame "wheel"
     (fn [e]
       (let [v (.-view c)
             r (.getBoundingClientRect frame)
             zoom? (or (.-ctrlKey e) (.-metaKey e))]
         ;; in a scrolling column the plain wheel belongs to the page
         (when (or zoom? (not= "page" (.getAttribute frame "data-gestures")))
           (.preventDefault e)
           (let-go! c)
           (if zoom?
             (zoom-at! c (* (.-zoom v) (js/Math.exp (* -0.01 (.-deltaY e))))
                       (- (.-clientX e) (.-left r)) (- (.-clientY e) (.-top r)) false)
             (set-view! c (- (.-x v) (.-deltaX e)) (- (.-y v) (.-deltaY e)) (.-zoom v) false)))))
     #js {:passive false})
    (.addEventListener
     frame "pointerdown"
     (fn [e]
       (when-not (interactive? (.-target e))
         (.set points (.-pointerId e) #js {:x (.-clientX e) :y (.-clientY e)})
         (aset state "moved" false)
         (aset state "startX" (.-clientX e))
         (aset state "startY" (.-clientY e)))))
    (.addEventListener
     frame "pointermove"
     (fn [e]
       (when-let [p (.get points (.-pointerId e))]
         (let [v (.-view c)
               r (.getBoundingClientRect frame)
               far? (> (js/Math.hypot (- (.-clientX e) (.-startX state))
                                      (- (.-clientY e) (.-startY state)))
                       pan-threshold)]
           ;; a press that moves becomes a pan; until then it may be a click
           (when (and far? (not (.-panning state)))
             (aset state "panning" true)
             (aset state "moved" true)
             (.setPointerCapture frame (.-pointerId e))
             (.setAttribute frame "data-panning" "true")
             (let-go! c))
           (when (and (.-panning state) (= 1 (.-size points)))
             (set-view! c (+ (.-x v) (- (.-clientX e) (.-x p)))
                        (+ (.-y v) (- (.-clientY e) (.-y p))) (.-zoom v) false))
           (.set points (.-pointerId e) #js {:x (.-clientX e) :y (.-clientY e)})
           (when (= 2 (.-size points))
             (let [ps (js/Array.from (.values points))
                   a (aget ps 0)
                   b (aget ps 1)
                   d (js/Math.hypot (- (.-x a) (.-x b)) (- (.-y a) (.-y b)))]
               (when (some? (.-pinch state))
                 (let-go! c)
                 (aset state "moved" true)
                 (zoom-at! c (* (.-zoom (.-view c)) (/ d (.-pinch state)))
                           (- (/ (+ (.-x a) (.-x b)) 2) (.-left r))
                           (- (/ (+ (.-y a) (.-y b)) 2) (.-top r)) false))
               (aset state "pinch" d)))))))
    (let [release (fn [e]
                    (.delete points (.-pointerId e))
                    (aset state "pinch" nil)
                    (when (zero? (.-size points))
                      (aset state "panning" false)
                      (.removeAttribute frame "data-panning")))]
      (.addEventListener frame "pointerup" release)
      (.addEventListener frame "pointercancel" release))
    ;; the click that ends a pan is not a click on the node under it
    (.addEventListener
     frame "click"
     (fn [e]
       (when (.-moved state)
         (aset state "moved" false)
         (.stopPropagation e)
         (.preventDefault e)))
     true)
    (.addEventListener
     frame "keydown"
     (fn [e]
       (when (identical? (.-target e) frame)
         (let [k (.-key e)
               v (.-view c)
               f (frame-center c)
               pan (fn [dx dy] (let-go! c) (set-view! c (+ (.-x v) dx) (+ (.-y v) dy) (.-zoom v) true))
               handled (cond
                         (or (= k "+") (= k "=")) (do (zoom-at! c (* (.-zoom v) 1.25) (.-x f) (.-y f) true) true)
                         (= k "-") (do (zoom-at! c (* (.-zoom v) 0.8) (.-x f) (.-y f) true) true)
                         (= k "0") (do (zoom-at! c 1 (.-x f) (.-y f) true) true)
                         (= k "ArrowLeft") (do (pan key-pan 0) true)
                         (= k "ArrowRight") (do (pan (- key-pan) 0) true)
                         (= k "ArrowUp") (do (pan 0 key-pan) true)
                         (= k "ArrowDown") (do (pan 0 (- key-pan)) true)
                         :else false)]
           (when handled (.preventDefault e))))))))

;; ── Controls ────────────────────────────────────────────────────────

(defn- act! [c action]
  (let [v (.-view c)
        f (frame-center c)]
    (cond
      (= action "fit") (do (let-go! c) (fit-rect! c (all-bounds c) nil))
      (= action "zoom-in") (zoom-at! c (* (.-zoom v) 1.25) (.-x f) (.-y f) true)
      (= action "zoom-out") (zoom-at! c (* (.-zoom v) 0.8) (.-x f) (.-y f) true)
      (= action "zoom-reset") (zoom-at! c 1 (.-x f) (.-y f) true)
      (= action "follow") (do (set-following! c (not (.-following c)))
                              (follow-now! c true)))))

(defn- bind-controls! [c]
  (.addEventListener
   (.-frame c) "click"
   (fn [e]
     (when-let [b (.closest (.-target e) "[data-ui-canvas-act]")]
       (act! c (.getAttribute b "data-ui-canvas-act"))))))

;; ── Init ────────────────────────────────────────────────────────────

(defn- home!
  "Where a canvas starts: its followed node, else everything it holds."
  [c]
  (let [followed (node-el c (.getAttribute (.-frame c) "data-follow"))
        initial (or (.getAttribute (.-frame c) "data-initial")
                    (if (some? followed) "follow" "fit"))]
    (cond
      (= initial "none") (paint! c false)
      (and (some? followed) (= initial "follow")) (center-rect! c (rect-of followed) 1 false)
      :else (fit-rect! c (all-bounds c) #js {:animate false}))))

(defn- init! [frame]
  (when (nil? (controller frame))
    (when-let [world (.querySelector frame "[data-ui-canvas-world]")]
      (let [c #js {:frame frame
                   :world world
                   :view #js {:x 0 :y 0 :zoom 1}
                   :minZoom (num-attr frame "data-min-zoom" 0.25)
                   :maxZoom (num-attr frame "data-max-zoom" 2.5)
                   :padding (num-attr frame "data-padding" 24)
                   :following (some? (.getAttribute frame "data-follow"))}]
        (aset frame "__uiCanvas" c)
        (.setAttribute frame "data-following" (if (.-following c) "true" "false"))
        (sync-follow-button! c)
        (bind-gestures! c)
        (bind-controls! c)
        ;; Tab walks the nodes: the one that takes the focus comes into view
        (.addEventListener
         world "focusin"
         (fn [e]
           (when-let [el (.closest (.-target e) "[data-ui-canvas-node]")]
             (when (.matches el ":focus-visible")
               (let-go! c)
               (reveal-rect! c (rect-of el) nil)))))
        (home! c)
        ;; the followed node changed: the camera goes there
        (let [mo (js/MutationObserver.
                  (fn [_]
                    (when (some? (.getAttribute frame "data-follow"))
                      (aset c "following" true)
                      (.setAttribute frame "data-following" "true")
                      (sync-follow-button! c)
                      (follow-now! c true))))]
          (.observe mo frame #js {:attributes true :attributeFilter #js ["data-follow"]}))
        ;; something covers more or less of the frame: back into the free part
        (let [mo (js/MutationObserver. (fn [_] (follow-now! c true)))]
          (.observe mo frame #js {:attributes true
                                  :attributeFilter #js ["data-inset-top" "data-inset-right"
                                                        "data-inset-bottom" "data-inset-left"]}))
        (let [ro (js/ResizeObserver. (fn [_] (follow-now! c false)))]
          (.observe ro frame))
        c))))

(defn- scan! []
  (.forEach (js/Array.from (js/document.querySelectorAll "[data-ui-canvas]"))
            (fn [frame] (init! frame))))

(def ^:private scan-scheduled (atom nil))

(defn- schedule-scan! []
  (when (nil? @scan-scheduled)
    (reset! scan-scheduled
            (js/requestAnimationFrame
             (fn []
               (reset! scan-scheduled nil)
               (scan!))))))

;; ── API ─────────────────────────────────────────────────────────────

(defn- with-canvas
  "Run `f` on the controller of the canvas with element id `id`."
  [id f]
  (when-let [frame (js/document.getElementById id)]
    (when-let [c (or (controller frame) (init! frame))]
      (f c))))

(aset js/window "__uiCanvas"
      #js {:scan scan!
           :fit (fn [id opts] (with-canvas id (fn [c] (let-go! c) (fit-rect! c (all-bounds c) opts))))
           :reveal (fn [id node-id opts]
                     (with-canvas id (fn [c] (when-let [el (node-el c node-id)]
                                               (reveal-rect! c (rect-of el) opts)))))
           :center (fn [id node-id zoom]
                     (with-canvas id (fn [c] (when-let [el (node-el c node-id)]
                                               (let-go! c)
                                               (center-rect! c (rect-of el) zoom true)))))
           :follow (fn [id node-id]
                     (with-canvas id (fn [c]
                                       (if (some? node-id)
                                         (.setAttribute (.-frame c) "data-follow" node-id)
                                         (do (.removeAttribute (.-frame c) "data-follow")
                                             (set-following! c false))))))
           :zoomBy (fn [id factor]
                     (with-canvas id (fn [c] (let [f (frame-center c)]
                                               (zoom-at! c (* (.-zoom (.-view c)) factor) (.-x f) (.-y f) true)))))
           :zoomTo (fn [id zoom]
                     (with-canvas id (fn [c] (let [f (frame-center c)]
                                               (zoom-at! c zoom (.-x f) (.-y f) true)))))
           :panBy (fn [id dx dy]
                    (with-canvas id (fn [c] (let [v (.-view c)]
                                              (let-go! c)
                                              (set-view! c (+ (.-x v) dx) (+ (.-y v) dy) (.-zoom v) true)))))
           :setViewport (fn [id view]
                          (with-canvas id (fn [c] (let-go! c)
                                            (set-view! c (.-x view) (.-y view) (.-zoom view) true))))
           :viewport (fn [id]
                       (with-canvas id (fn [c] (let [v (.-view c)]
                                                 #js {:x (.-x v) :y (.-y v) :zoom (.-zoom v)}))))})

(defn- start! []
  (scan!)
  (let [mo (js/MutationObserver. (fn [_] (schedule-scan!)))]
    (.observe mo js/document.body #js {:childList true :subtree true})))

(if (= js/document.readyState "loading")
  (js/document.addEventListener "DOMContentLoaded" (fn [] (start!)))
  (start!))
