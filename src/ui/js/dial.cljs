(ns dial
  "DialKit runtime — tune interface values in real time from a floating panel.

   A vanilla port of https://www.dialkit.dev/ for the clj-ui JS runtime.
   Mount panels imperatively; bind their live values into the DOM (CSS vars,
   inline styles) or your app state via the `onChange`/`subscribe` callback.

   Public API (all attached to window):

     window.__uiDial(name, config, opts?) -> controller
       config : object mapping keys -> control definitions
                number / [def,min,max,step?]  -> slider
                boolean                        -> toggle
                \"#hex\" / rgb()/oklch()...      -> color
                other string                   -> text
                {type: \"text\"|\"select\"|\"color\"|\"image\"|\"pad\"
                       |\"spring\"|\"easing\"|\"action\", ...}
                nested object (no :type)       -> folder
       opts   : {id, persist, onChange, onAction, defaultCollapsed,
                 enabled, shortcuts,
                 position: \"top-right\"|\"top-left\"|\"bottom-right\"|\"bottom-left\",
                 icon: char shown when minimized (default \"⚙\")}
       controller: {values, getValues, setValue, setValues, resetValues,
                    setOpen, getOpen, subscribe, destroy, id}

     window.DialStore -> {setPanelOpen, togglePanelOpen, getPanelOpen,
                          savePreset, deletePreset, getPresets,
                          getActivePresetId, clearActivePreset}

   Panel fields are read with (:kw panel) and written with (aset panel \"kw\" v)
   — never `.-` interop, which munges hyphens to underscores in squint.

   See src/ui/dial.css for styling."
  (:require [clojure.string :as str]))

;; ── DOM helpers ─────────────────────────────────────────────────────

(defn- tof [v] (js* "typeof ~{}" v))

(defn- mk [tag class]
  (let [e (js/document.createElement tag)]
    (when class (set! (.-className e) class))
    e))

(defn- add! [parent & children]
  (doseq [c children] (when c (.appendChild parent c)))
  parent)

(defn- txt! [e s] (set! (.-textContent e) (str s)) e)
(defn- on! [e ev f] (.addEventListener e ev f) e)
(defn- attr! [e k v] (.setAttribute e k v) e)

(defn- clamp [v lo hi] (Math/max lo (Math/min v hi)))

(defn- path-str [path] (.join path "."))

;; ── Number formatting ───────────────────────────────────────────────

(defn- fmt-num [n]
  (if (not (js/isFinite n))
    "0"
    (str (/ (js/Math.round (* n 1000)) 1000))))

(defn- round-step [v step]
  (if (and step (> step 0))
    (* (js/Math.round (/ v step)) step)
    v))

;; ── Range / step inference ──────────────────────────────────────────

(defn- infer-range [v]
  (cond
    (< v 0)    #js {:min (* v 3) :max (* (- v) 3) :step 1}
    (<= v 1)   #js {:min 0 :max 1 :step 0.01}
    (<= v 10)  #js {:min 0 :max (* v 3) :step 0.1}
    (<= v 100) #js {:min 0 :max (* v 3) :step 1}
    :else      #js {:min 0 :max (* v 3) :step 10}))

(defn- infer-step [mx]
  (cond (<= mx 1) 0.01 (<= mx 10) 0.1 (<= mx 100) 1 :else 10))

;; ── Colour + label helpers ──────────────────────────────────────────

(def ^:private color-re
  (js/RegExp. "^\\s*(#([0-9a-fA-F]{3,8})|(rgb|rgba|hsl|hsla|oklch|oklab|color)\\()"))

(defn- color-str? [s]
  (and (= (tof s) "string") (.test color-re s)))

;; ── Colour maths (HSV ⇄ RGB ⇄ HSL, CSS parsing) ─────────────────────

(defn- hsv->rgb [h s v]
  (let [c  (* v s)
        h' (/ (mod h 360) 60)
        x  (* c (- 1 (js/Math.abs (- (mod h' 2) 1))))
        m  (- v c)
        rgb (cond
              (< h' 1) #js [c x 0]
              (< h' 2) #js [x c 0]
              (< h' 3) #js [0 c x]
              (< h' 4) #js [0 x c]
              (< h' 5) #js [x 0 c]
              :else    #js [c 0 x])]
    #js [(js/Math.round (* 255 (+ (aget rgb 0) m)))
         (js/Math.round (* 255 (+ (aget rgb 1) m)))
         (js/Math.round (* 255 (+ (aget rgb 2) m)))]))

(defn- rgb->hsv [r g b]
  (let [r  (/ r 255) g (/ g 255) b (/ b 255)
        mx (js/Math.max r g b) mn (js/Math.min r g b)
        d  (- mx mn)
        h  (cond
             (zero? d) 0
             (= mx r)  (* 60 (mod (/ (- g b) d) 6))
             (= mx g)  (* 60 (+ (/ (- b r) d) 2))
             :else     (* 60 (+ (/ (- r g) d) 4)))
        h  (if (< h 0) (+ h 360) h)
        s  (if (zero? mx) 0 (/ d mx))]
    #js [h s mx]))

(defn- rgb->hsl [r g b]
  (let [r  (/ r 255) g (/ g 255) b (/ b 255)
        mx (js/Math.max r g b) mn (js/Math.min r g b)
        d  (- mx mn)
        l  (/ (+ mx mn) 2)
        h  (cond
             (zero? d) 0
             (= mx r)  (* 60 (mod (/ (- g b) d) 6))
             (= mx g)  (* 60 (+ (/ (- b r) d) 2))
             :else     (* 60 (+ (/ (- r g) d) 4)))
        h  (if (< h 0) (+ h 360) h)
        s  (if (zero? d) 0 (/ d (- 1 (js/Math.abs (- (* 2 l) 1)))))]
    #js [h s l]))

(def ^:private color-probe nil)

(defn- parse-rgba [s]
  ;; Resolve ANY CSS colour string to #js [r g b a] via a hidden probe element.
  (let [el (or color-probe
               (let [e (mk "div" nil)]
                 (set! (.. e -style -display) "none")
                 (.appendChild js/document.body e)
                 (set! color-probe e)
                 e))]
    (set! (.. el -style -color) "")
    (set! (.. el -style -color) (str s))
    (when (not= "" (.. el -style -color))
      (let [cs (.-color (js/getComputedStyle el))
            m  (.match cs (js/RegExp. "rgba?\\(([^)]+)\\)"))]
        (when m
          (let [parts (.split (aget m 1) (js/RegExp. "[ ,/]+"))
                a (if (> (.-length parts) 3) (js/parseFloat (aget parts 3)) 1)]
            #js [(js/parseFloat (aget parts 0))
                 (js/parseFloat (aget parts 1))
                 (js/parseFloat (aget parts 2))
                 (if (js/isFinite a) a 1)]))))))

(defn- to-hex2 [n]
  (.padStart (.toString (clamp (js/Math.round n) 0 255) 16) 2 "0"))

(defn- compose-color [h s v a fmt]
  (let [rgb (hsv->rgb h s v)
        r (aget rgb 0) g (aget rgb 1) b (aget rgb 2)]
    (case fmt
      "rgb" (if (< a 0.999)
              (str "rgba(" r ", " g ", " b ", " (fmt-num a) ")")
              (str "rgb(" r ", " g ", " b ")"))
      "hsl" (let [hsl (rgb->hsl r g b)
                  hh (js/Math.round (aget hsl 0))
                  ss (js/Math.round (* (aget hsl 1) 100))
                  ll (js/Math.round (* (aget hsl 2) 100))]
              (if (< a 0.999)
                (str "hsla(" hh ", " ss "%, " ll "%, " (fmt-num a) ")")
                (str "hsl(" hh ", " ss "%, " ll "%)")))
      (str "#" (to-hex2 r) (to-hex2 g) (to-hex2 b)
           (if (< a 0.999) (to-hex2 (* a 255)) "")))))

(defn- humanize [k]
  (let [s (-> (str k)
              (.replace (js/RegExp. "([a-z0-9])([A-Z])" "g") "$1 $2")
              (.replace (js/RegExp. "[_\\-]" "g") " ")
              (.trim))]
    (if (zero? (.-length s))
      s
      (str (.toUpperCase (.charAt s 0)) (.slice s 1)))))

(defn- normalize-options [opts]
  (mapv (fn [o]
          (if (= (tof o) "object")
            #js {:value (aget o "value") :label (or (aget o "label") (aget o "value"))}
            #js {:value o :label o}))
        opts))

;; ── Config parsing → control descriptors ────────────────────────────

(declare parse-config)

(defn- parse-node [k v path]
  (cond
    (js/Array.isArray v)
    (let [step (if (> (.-length v) 3) (aget v 3) (infer-step (aget v 2)))]
      {:ctype "slider" :key k :path path :label (humanize k)
       :default (aget v 0) :min (aget v 1) :max (aget v 2) :step step})

    (= (tof v) "number")
    (let [r (infer-range v)]
      {:ctype "slider" :key k :path path :label (humanize k)
       :default v :min (aget r "min") :max (aget r "max") :step (aget r "step")})

    (= (tof v) "boolean")
    {:ctype "toggle" :key k :path path :label (humanize k) :default v}

    (= (tof v) "string")
    (if (color-str? v)
      {:ctype "color" :key k :path path :label (humanize k) :default v}
      {:ctype "text" :key k :path path :label (humanize k) :default v :placeholder ""})

    (= (tof v) "object")
    (let [t (aget v "type")]
      (cond
        (= t "text")
        {:ctype "text" :key k :path path :label (or (aget v "label") (humanize k))
         :default (or (aget v "default") "") :placeholder (or (aget v "placeholder") "")}

        (= t "select")
        (let [os (normalize-options (or (aget v "options") #js []))]
          {:ctype "select" :key k :path path :label (or (aget v "label") (humanize k))
           :options os
           :default (or (aget v "default")
                        (when (seq os) (aget (first os) "value"))
                        "")})

        (= t "color")
        {:ctype "color" :key k :path path :label (or (aget v "label") (humanize k))
         :default (or (aget v "default") "#000000")}

        (= t "image")
        (let [os (when (aget v "options") (normalize-options (aget v "options")))]
          {:ctype "image" :key k :path path :label (or (aget v "label") (humanize k))
           :options os
           :default (or (aget v "default")
                        (when (seq os) (aget (first os) "value"))
                        "")})

        (= t "pad")
        (let [ax (fn [a def-]
                   (let [tup (or (aget v a) def-)]
                     #js {:default (aget tup 0) :min (aget tup 1) :max (aget tup 2)
                          :step (or (aget tup 3) (/ (- (aget tup 2) (aget tup 1)) 200))}))
              default-axis #js [0 -1 1 0.01]
              xa (ax "x" default-axis)
              ya (ax "y" default-axis)
              labels (or (aget v "labels") #js {})]
          {:ctype "pad" :key k :path path :label (or (aget v "label") (humanize k))
           :x xa :y ya
           :x-label (or (aget labels "x") "X") :y-label (or (aget labels "y") "Y")
           :default #js {:x (aget xa "default") :y (aget ya "default")}})

        (or (= t "spring") (= t "easing"))
        {:ctype "transition" :key k :path path :label (or (aget v "label") (humanize k))
         :default (js/JSON.parse (js/JSON.stringify v))}

        (= t "action")
        {:ctype "action" :key k :path path :label (or (aget v "label") (humanize k))}

        :else
        {:ctype "folder" :key k :path path :label (humanize k)
         :collapsed (boolean (aget v "_collapsed"))
         :children (parse-config v path)}))

    :else nil))

(defn- parse-config [config path]
  (let [out #js []]
    (doseq [k (js/Object.keys config)]
      (when-not (= k "_collapsed")
        (let [node (parse-node k (aget config k) (conj path k))]
          (when node (.push out node)))))
    out))

;; ── Value store ─────────────────────────────────────────────────────

(defn- init-values! [store controls]
  (doseq [c controls]
    (cond
      (= (:ctype c) "folder") (init-values! store (:children c))
      (= (:ctype c) "action") nil
      :else (swap! store assoc-in (:path c) (:default c)))))

(declare persist-save!)

(defn- notify! [panel]
  (let [vs @(:store panel)]
    (when-let [cb (:onChange panel)] (cb (clj->js vs)))
    (doseq [s (js/Array.from (:subs panel))] (s (clj->js vs)))))

(defn- commit! [panel path v]
  (swap! (:store panel) assoc-in path v)
  (notify! panel)
  (persist-save! panel))

(defn- refresh-updaters! [panel]
  (let [vs @(:store panel)
        us (:updaters panel)]
    (doseq [k (js/Object.keys us)]
      ((aget us k) (get-in vs (.split k "."))))))

(defn- set-value! [panel path v]
  (swap! (:store panel) assoc-in path v)
  (when-let [u (aget (:updaters panel) (path-str path))] (u v))
  (notify! panel)
  (persist-save! panel))

;; ── Control renderers ───────────────────────────────────────────────

(defn- reg-updater! [panel path f]
  (aset (:updaters panel) (path-str path) f))

(defn- row [label]
  (let [r (mk "div" "dial-row")]
    (when label
      (add! r (txt! (mk "label" "dial-label") label)))
    r))

;; Slider ------------------------------------------------------------

(defn- render-slider [panel c]
  (let [{:keys [path label min max step]} c
        r      (mk "div" "dial-row dial-row--slider")
        field  (mk "div" "dial-slider")
        fill   (mk "div" "dial-slider-fill")
        lab    (mk "span" "dial-slider-label")
        num    (mk "input" "dial-num")
        cur    (fn [] (get-in @(:store panel) path))
        paint  (fn [v]
                 (let [pct (* 100 (clamp (/ (- v min) (- max min)) 0 1))]
                   (set! (.. fill -style -width) (str pct "%"))
                   (set! (.-value num) (fmt-num v))))
        set-at (fn [clientx]
                 (let [rect (.getBoundingClientRect field)
                       t    (clamp (/ (- clientx (.-left rect)) (.-width rect)) 0 1)
                       raw  (+ min (* t (- max min)))
                       v    (clamp (round-step raw step) min max)]
                   (commit! panel path v)
                   (paint v)))]
    (txt! lab label)
    (attr! lab "title" label)
    (set! (.-type num) "text")
    (attr! num "inputmode" "decimal")
    (attr! num "spellcheck" "false")
    (add! field fill lab num)
    (add! r field)
    (attr! field "tabindex" "0")
    (let [drag #js {:on false}]
      (on! field "pointerdown"
           (fn [e]
             ;; the number is click-to-edit only — pressing it never scrubs;
             ;; scrubbing happens on the rest of the track
             (when (not= (.-target e) num)
               (.preventDefault e)
               (.setPointerCapture field (.-pointerId e))
               (aset drag "on" true)
               (set-at (.-clientX e)))))
      (on! field "pointermove"
           (fn [e] (when (aget drag "on") (set-at (.-clientX e)))))
      (on! field "pointerup"
           (fn [e]
             (when (aget drag "on")
               (aset drag "on" false)
               (when (.hasPointerCapture field (.-pointerId e))
                 (.releasePointerCapture field (.-pointerId e)))
               (.focus field))))
      (on! field "pointercancel" (fn [_] (aset drag "on" false))))
    (on! num "focus" (fn [_] (.select num)))
    (on! num "change"
         (fn [_] (let [v (js/parseFloat (.-value num))]
                   (if (js/isFinite v)
                     (let [v2 (clamp (round-step v step) min max)]
                       (commit! panel path v2) (paint v2))
                     (paint (cur))))))
    (on! field "keydown"
         (fn [e]
           (when (not= (.-target e) num)
             (let [k (.-key e)
                   big (or (.-shiftKey e) (= k "PageUp") (= k "PageDown"))
                   d (* step (if big 10 1))]
               (cond
                 (or (= k "Enter") (= k " ")) (do (.preventDefault e) (.focus num) (.select num))
                 (or (= k "ArrowUp") (= k "ArrowRight") (= k "PageUp"))
                 (do (.preventDefault e) (let [v (clamp (+ (cur) d) min max)] (commit! panel path v) (paint v)))
                 (or (= k "ArrowDown") (= k "ArrowLeft") (= k "PageDown"))
                 (do (.preventDefault e) (let [v (clamp (- (cur) d) min max)] (commit! panel path v) (paint v)))
                 (= k "Home") (do (.preventDefault e) (commit! panel path min) (paint min))
                 (= k "End")  (do (.preventDefault e) (commit! panel path max) (paint max)))))))
    (paint (cur))
    (reg-updater! panel path paint)
    r))

;; Toggle ------------------------------------------------------------

(defn- render-toggle [panel c]
  (let [{:keys [path label]} c
        r     (row label)
        wrap  (mk "label" "switch dial-switch")
        input (mk "input" "switch-input")
        track (mk "span" "switch-track")
        thumb (mk "span" "switch-thumb")
        paint (fn [v]
                (set! (.-checked input) (boolean v))
                (if v (.add (.-classList track) "switch-track--checked")
                    (.remove (.-classList track) "switch-track--checked")))]
    (attr! input "type" "checkbox")
    (add! track thumb)
    (add! wrap input track)
    (on! input "change"
         (fn [_] (let [v (.-checked input)]
                   (commit! panel path v) (paint v))))
    (add! r wrap)
    (paint (get-in @(:store panel) path))
    (reg-updater! panel path paint)
    r))

;; Text --------------------------------------------------------------

(defn- render-text [panel c]
  (let [{:keys [path label placeholder]} c
        r  (row label)
        ta (mk "textarea" "dial-text")]
    (attr! ta "rows" "1")
    (when (seq placeholder) (attr! ta "placeholder" placeholder))
    (on! ta "input"
         (fn [_]
           (set! (.. ta -style -height) "auto")
           (set! (.. ta -style -height) (str (Math/min 120 (.-scrollHeight ta)) "px"))
           (commit! panel path (.-value ta))))
    (add! r ta)
    (set! (.-value ta) (or (get-in @(:store panel) path) ""))
    (reg-updater! panel path (fn [v] (set! (.-value ta) (or v ""))))
    r))

;; Select ------------------------------------------------------------

(defn- render-select [panel c]
  (let [{:keys [path label options]} c
        r       (row label)
        wrap    (mk "div" "select dial-select")
        trigger (mk "button" "select-trigger")
        valspan (mk "span" "select-value")
        opt-for (fn [v] (.find options (fn [o] (= (aget o "value") v))))
        set-lbl (fn [v] (let [o (opt-for v)]
                          (txt! valspan (if o (aget o "label") (or v "")))))]
    (attr! trigger "type" "button")
    (attr! trigger "role" "combobox")
    (attr! trigger "aria-haspopup" "listbox")
    (attr! trigger "aria-expanded" "false")
    (when (empty? options) (set! (.-disabled trigger) true))
    (add! trigger valspan)
    (add! wrap trigger)
    (add! r wrap)
    (let [cur (get-in @(:store panel) path)]
      (attr! trigger "data-select-value" (or cur "")) (set-lbl cur))
    (on! trigger "click"
         (fn [_]
           (when-let [f js/window.__uiSelect]
             (f trigger options
                (fn [v]
                  (attr! trigger "data-select-value" v)
                  (set-lbl v)
                  (commit! panel path v))))))
    (reg-updater! panel path
                  (fn [v] (attr! trigger "data-select-value" (or v "")) (set-lbl v)))
    r))

;; Color -------------------------------------------------------------

(defn- render-color [panel c]
  (let [{:keys [path label]} c
        r        (mk "div" "dial-row dial-row--color")
        wrap     (mk "div" "dial-color")
        formats  (mk "div" "dial-color-formats")
        plane    (mk "div" "dial-color-plane")
        marker   (mk "span" "dial-color-marker")
        tracks   (mk "div" "dial-color-tracks")
        huerow   (mk "label" "dial-color-track-row")
        hue      (mk "input" "dial-color-track dial-color-hue")
        oprow    (mk "label" "dial-color-track-row")
        op       (mk "input" "dial-color-track dial-color-opacity")
        txtf     (mk "input" "dial-color-input")
        st       #js {:h 265 :s 0.6 :v 0.9 :a 1 :fmt "hex"}
        fmt-btns #js {}
        cur      (fn [] (get-in @(:store panel) path))
        emit     (fn [] (compose-color (:h st) (:s st) (:v st) (:a st) (:fmt st)))
        detect-fmt (fn [s]
                     (let [s (.toLowerCase (.trim (str s)))]
                       (cond
                         (.startsWith s "hsl") "hsl"
                         (.startsWith s "rgb") "rgb"
                         (.startsWith s "#")   "hex"
                         :else (:fmt st))))
        adopt    (fn [s]
                   (let [rgba (parse-rgba s)]
                     (when rgba
                       (let [hsv (rgb->hsv (aget rgba 0) (aget rgba 1) (aget rgba 2))]
                         ;; keep prior hue for greys so the plane doesn't jump
                         (when (> (aget hsv 1) 0.0001) (aset st "h" (aget hsv 0)))
                         (aset st "s" (aget hsv 1))
                         (aset st "v" (aget hsv 2))
                         (aset st "a" (aget rgba 3))))))
        paint-ui (fn []
                   (let [h (:h st) s (:s st) v (:v st) a (:a st)
                         rgb (hsv->rgb h s v)
                         hue-col (str "hsl(" (js/Math.round h) ", 100%, 50%)")
                         solid (str "rgb(" (aget rgb 0) ", " (aget rgb 1) ", " (aget rgb 2) ")")]
                     (set! (.. plane -style -background)
                           (str "linear-gradient(to top, #000, rgba(0,0,0,0)),"
                                "linear-gradient(to right, #fff, " hue-col ")"))
                     (set! (.. marker -style -left) (str (* 100 s) "%"))
                     (set! (.. marker -style -top) (str (* 100 (- 1 v)) "%"))
                     (set! (.. marker -style -background) solid)
                     (set! (.-value hue) (str h))
                     (set! (.-value op) (str a))
                     (.setProperty (.-style op) "--dial-color-solid" solid)
                     (doseq [f #js ["hex" "rgb" "hsl"]]
                       (let [b (aget fmt-btns f)]
                         (when b (attr! b "data-active" (if (= f (:fmt st)) "true" "false")))))))
        set-fmt  (fn [f]
                   (aset st "fmt" f)
                   (let [v (emit)]
                     (commit! panel path v)
                     (set! (.-value txtf) v)
                     (paint-ui)))
        push     (fn []
                   (let [v (emit)]
                     (commit! panel path v)
                     (set! (.-value txtf) v)
                     (paint-ui)))
        plane-at (fn [e]
                   (let [rect (.getBoundingClientRect plane)
                         sx (clamp (/ (- (.-clientX e) (.-left rect)) (.-width rect)) 0 1)
                         sy (clamp (/ (- (.-clientY e) (.-top rect)) (.-height rect)) 0 1)]
                     (aset st "s" sx)
                     (aset st "v" (- 1 sy))
                     (push)))
        paint    (fn [v]
                   (let [v (or v "#000000")]
                     (adopt v)
                     (aset st "fmt" (detect-fmt v))
                     (set! (.-value txtf) v)
                     (paint-ui)))]
    ;; format segmented control
    (doseq [pair #js [#js ["hex" "Hex"] #js ["rgb" "RGB"] #js ["hsl" "HSL"]]]
      (let [f (aget pair 0)
            b (mk "button" "dial-color-format")]
        (set! (.-type b) "button")
        (txt! b (aget pair 1))
        (aset fmt-btns f b)
        (on! b "click" (fn [_] (set-fmt f)))
        (add! formats b)))
    ;; plane
    (attr! plane "tabindex" "0")
    (add! plane marker)
    (let [dragging #js {:on false}]
      (on! plane "pointerdown"
           (fn [e]
             (.preventDefault e)
             (.setPointerCapture plane (.-pointerId e))
             (aset dragging "on" true) (plane-at e)))
      (on! plane "pointermove" (fn [e] (when (aget dragging "on") (plane-at e))))
      (on! plane "pointerup" (fn [_] (aset dragging "on" false)))
      (on! plane "pointercancel" (fn [_] (aset dragging "on" false))))
    ;; hue + opacity tracks
    (set! (.-type hue) "range") (set! (.-min hue) "0") (set! (.-max hue) "360") (set! (.-step hue) "1")
    (set! (.-type op) "range") (set! (.-min op) "0") (set! (.-max op) "1") (set! (.-step op) "0.01")
    (add! huerow (txt! (mk "span" nil) "Hue") hue)
    (add! oprow (txt! (mk "span" nil) "Opacity") op)
    (on! hue "input" (fn [_] (aset st "h" (js/parseFloat (.-value hue))) (push)))
    (on! op "input" (fn [_] (aset st "a" (js/parseFloat (.-value op))) (push)))
    ;; css text input
    (set! (.-type txtf) "text")
    (attr! txtf "spellcheck" "false")
    (on! txtf "change"
         (fn [_]
           (let [v (.-value txtf)]
             (when (color-str? v)
               (adopt v)
               (aset st "fmt" (detect-fmt v)))
             (commit! panel path v)
             (paint-ui))))
    ;; assemble
    (add! tracks huerow oprow)
    (add! wrap formats plane tracks txtf)
    (when label (add! r (txt! (mk "label" "dial-label") label)))
    (add! r wrap)
    (paint (cur))
    (reg-updater! panel path paint)
    r))

;; Image -------------------------------------------------------------

(defn- render-image [panel c]
  (let [{:keys [path label options]} c
        r     (row label)
        wrap  (mk "div" "dial-image")
        grid  (mk "div" "dial-image-grid")
        drop  (mk "label" "dial-image-drop")
        file  (mk "input" nil)
        cur   (fn [] (get-in @(:store panel) path))
        mark  (fn [v]
                (doseq [ch (js/Array.from (.-children grid))]
                  (if (= (.getAttribute ch "data-value") v)
                    (.add (.-classList ch) "is-active")
                    (.remove (.-classList ch) "is-active"))))
        read! (fn [f]
                (when f
                  (let [rd (js/FileReader.)]
                    (set! (.-onload rd) (fn [_]
                                          (let [v (.-result rd)]
                                            (commit! panel path v) (mark v))))
                    (.readAsDataURL rd f))))]
    (set! (.-type file) "file") (attr! file "accept" "image/*")
    (set! (.. file -style -display) "none")
    (when (seq options)
      (doseq [o options]
        (let [b (mk "button" "dial-image-opt")]
          (attr! b "type" "button")
          (attr! b "data-value" (aget o "value"))
          (attr! b "title" (aget o "label"))
          (set! (.. b -style -backgroundImage) (str "url(" (js/JSON.stringify (aget o "value")) ")"))
          (on! b "click" (fn [_] (let [v (aget o "value")] (commit! panel path v) (mark v))))
          (add! grid b))))
    (txt! drop "Drop / upload")
    (add! drop file)
    (on! file "change" (fn [_] (read! (aget (.-files file) 0))))
    (on! drop "dragover" (fn [e] (.preventDefault e) (.add (.-classList drop) "is-over")))
    (on! drop "dragleave" (fn [_] (.remove (.-classList drop) "is-over")))
    (on! drop "drop" (fn [e] (.preventDefault e) (.remove (.-classList drop) "is-over")
                       (read! (aget (.. e -dataTransfer -files) 0))))
    (add! wrap grid drop)
    (add! r wrap)
    (mark (cur))
    (reg-updater! panel path (fn [v] (mark v)))
    r))

;; XY Pad ------------------------------------------------------------

(defn- render-pad [panel c]
  (let [{:keys [path x y x-label y-label]} c
        r      (row (:label c))
        area   (mk "div" "dial-pad")
        dot    (mk "div" "dial-pad-dot")
        meta   (mk "div" "dial-pad-meta")
        cur    (fn [] (get-in @(:store panel) path))
        to-pct (fn [v ax]
                 (clamp (/ (- v (aget ax "min")) (- (aget ax "max") (aget ax "min"))) 0 1))
        paint  (fn [val]
                 (let [px (* 100 (to-pct (aget val "x") x))
                       py (* 100 (- 1 (to-pct (aget val "y") y)))]
                   (set! (.. dot -style -left) (str px "%"))
                   (set! (.. dot -style -top) (str py "%"))
                   (txt! meta (str x-label " " (fmt-num (aget val "x")) "   "
                                   y-label " " (fmt-num (aget val "y"))))))
        set-at (fn [cx cy]
                 (let [rect (.getBoundingClientRect area)
                       tx (clamp (/ (- cx (.-left rect)) (.-width rect)) 0 1)
                       ty (clamp (/ (- cy (.-top rect)) (.-height rect)) 0 1)
                       vx (clamp (round-step (+ (aget x "min") (* tx (- (aget x "max") (aget x "min")))) (aget x "step")) (aget x "min") (aget x "max"))
                       vy (clamp (round-step (+ (aget y "min") (* (- 1 ty) (- (aget y "max") (aget y "min")))) (aget y "step")) (aget y "min") (aget y "max"))
                       v #js {:x vx :y vy}]
                   (commit! panel path v) (paint v)))]
    (add! area dot)
    (add! r area meta)
    (attr! area "tabindex" "0")
    (let [dragging #js {:on false}]
      (on! area "pointerdown" (fn [e] (.setPointerCapture area (.-pointerId e))
                                (aset dragging "on" true) (set-at (.-clientX e) (.-clientY e))))
      (on! area "pointermove" (fn [e] (when (aget dragging "on") (set-at (.-clientX e) (.-clientY e)))))
      (on! area "pointerup" (fn [_] (aset dragging "on" false)))
      (on! area "dblclick" (fn [_] (commit! panel path (:default c)) (paint (:default c)))))
    (paint (cur))
    (reg-updater! panel path paint)
    r))

;; Transition (spring / easing) -------------------------------------

(defn- cubic-bezier [p1x p1y p2x p2y t]
  (let [mt (- 1 t)
        y (+ (* 3 mt mt t p1y) (* 3 mt t t p2y) (* t t t))]
    #js {:x t :y y}))

(defn- sample-spring [visual-dur bounce t]
  (let [zeta  (clamp (- 1 bounce) 0.05 1)
        omega (/ (* 2 js/Math.PI) (Math/max 0.05 visual-dur))]
    (if (< zeta 1)
      (let [wd (* omega (js/Math.sqrt (- 1 (* zeta zeta))))]
        (- 1 (* (js/Math.exp (* (- zeta) omega t))
                (+ (js/Math.cos (* wd t))
                   (* (/ (* zeta omega) wd) (js/Math.sin (* wd t)))))))
      (- 1 (* (js/Math.exp (* (- omega) t)) (+ 1 (* omega t)))))))

(defn- render-transition [panel c]
  (let [{:keys [path]} c
        r      (row (:label c))
        wrap   (mk "div" "dial-transition")
        modes  (mk "div" "dial-seg")
        canvas (mk "canvas" "dial-curve")
        fields (mk "div" "dial-fields")
        cur    (fn [] (get-in @(:store panel) path))
        get-mode (fn [] (let [v (cur)]
                          (cond (= (aget v "type") "easing") "easing"
                                (aget v "stiffness") "physics"
                                :else "time")))
        draw   (fn []
                 (let [ctx (.getContext canvas "2d")
                       w 220 h 90
                       accent (.trim (.getPropertyValue (js/getComputedStyle (.-documentElement js/document)) "--accent"))]
                   (set! (.-width canvas) w) (set! (.-height canvas) h)
                   (.clearRect ctx 0 0 w h)
                   (set! (.-lineWidth ctx) 2)
                   (set! (.-strokeStyle ctx) (if (seq accent) accent "#7c5cfc"))
                   (.beginPath ctx)
                   (let [v (cur) mode (get-mode)]
                     (dotimes [i 61]
                       (let [t (/ i 60)
                             y (cond
                                 (= mode "easing")
                                 (let [e (or (aget v "ease") #js [0.25 0.1 0.25 1])]
                                   (aget (cubic-bezier (aget e 0) (aget e 1) (aget e 2) (aget e 3) t) "y"))
                                 (= mode "physics")
                                 (sample-spring 0.5 0.2 (* t 1))
                                 :else
                                 (let [vd (or (aget v "visualDuration") 0.4)]
                                   (sample-spring vd (or (aget v "bounce") 0.2) (* t (* vd 2)))))
                             px (* t w)
                             py (- h (* (clamp y -0.2 1.4) (* h 0.7)) (* h 0.1))]
                         (if (zero? i) (.moveTo ctx px py) (.lineTo ctx px py)))))
                   (.stroke ctx)))
        num-field (fn [key- lbl mn mx st]
                    (let [fw  (mk "label" "dial-field")
                          inp (mk "input" nil)]
                      (set! (.-type inp) "number")
                      (set! (.-min inp) (str mn)) (set! (.-max inp) (str mx)) (set! (.-step inp) (str st))
                      (set! (.-value inp) (str (or (aget (cur) key-) "")))
                      (add! fw (txt! (mk "span" nil) lbl) inp)
                      (on! inp "input"
                           (fn [_] (let [v (js/JSON.parse (js/JSON.stringify (cur)))]
                                     (aset v key- (js/parseFloat (.-value inp)))
                                     (commit! panel path v) (draw))))
                      #js {:el fw :inp inp}))
        rebuild (fn []
                  (set! (.-innerHTML fields) "")
                  (let [mode (get-mode) v (cur)]
                    (cond
                      (= mode "easing")
                      (add! fields (aget (num-field "duration" "Duration" 0 5 0.05) "el"))
                      (= mode "physics")
                      (doseq [f [(num-field "stiffness" "Stiffness" 1 500 1)
                                 (num-field "damping" "Damping" 1 60 1)
                                 (num-field "mass" "Mass" 0.1 5 0.1)]]
                        (add! fields (aget f "el")))
                      :else
                      (doseq [f [(num-field "visualDuration" "Duration" 0.05 2 0.01)
                                 (num-field "bounce" "Bounce" 0 1 0.01)]]
                        (add! fields (aget f "el")))))
                  (draw))
        set-mode (fn [m]
                   (let [v (js/JSON.parse (js/JSON.stringify (cur)))]
                     (cond
                       (= m "easing") (do (aset v "type" "easing")
                                          (when-not (aget v "ease") (aset v "ease" #js [0.25 0.1 0.25 1]))
                                          (when-not (aget v "duration") (aset v "duration" 0.3)))
                       (= m "physics") (do (aset v "type" "spring")
                                           (aset v "stiffness" (or (aget v "stiffness") 200))
                                           (aset v "damping" (or (aget v "damping") 25))
                                           (aset v "mass" (or (aget v "mass") 1)))
                       :else (do (aset v "type" "spring")
                                 (js-delete v "stiffness") (js-delete v "damping") (js-delete v "mass")
                                 (aset v "visualDuration" (or (aget v "visualDuration") 0.4))
                                 (aset v "bounce" (or (aget v "bounce") 0.2))))
                     (commit! panel path v) (rebuild)))]
    (doseq [pair [#js ["easing" "Easing"] #js ["time" "Time"] #js ["physics" "Physics"]]]
      (let [b (mk "button" "dial-seg-btn")]
        (attr! b "type" "button")
        (txt! b (aget pair 1))
        (on! b "click" (fn [_] (set-mode (aget pair 0))))
        (add! modes b)))
    (add! wrap modes canvas fields)
    (add! r wrap)
    (rebuild)
    (reg-updater! panel path (fn [_] (rebuild)))
    r))

;; Action ------------------------------------------------------------

(defn- render-action [panel c]
  (let [{:keys [path label]} c
        r   (mk "div" "dial-row dial-row--action")
        btn (mk "button" "dial-action")]
    (attr! btn "type" "button")
    (txt! btn label)
    (on! btn "click" (fn [_] (when-let [f (:onAction panel)] (f (path-str path)))))
    (add! r btn)
    r))

;; Folder ------------------------------------------------------------

(declare render-control)

(defn- render-folder [panel c]
  (let [wrap (mk "div" "dial-folder")
        head (mk "button" "dial-folder-head")
        body (mk "div" "dial-folder-body")
        chev (mk "span" "dial-chevron")
        open (atom (not (:collapsed c)))
        sync (fn [] (if @open
                      (.remove (.-classList wrap) "is-collapsed")
                      (.add (.-classList wrap) "is-collapsed")))]
    (attr! head "type" "button")
    (add! head chev (txt! (mk "span" nil) (:label c)))
    (on! head "click" (fn [_] (swap! open not) (sync)))
    (doseq [child (:children c)]
      (add! body (render-control panel child)))
    (add! wrap head body)
    (sync)
    wrap))

;; Dispatch ----------------------------------------------------------

(defn- render-control [panel c]
  (let [t (:ctype c)]
    (cond
      (= t "slider")     (render-slider panel c)
      (= t "toggle")     (render-toggle panel c)
      (= t "text")       (render-text panel c)
      (= t "select")     (render-select panel c)
      (= t "color")      (render-color panel c)
      (= t "image")      (render-image panel c)
      (= t "pad")        (render-pad panel c)
      (= t "transition") (render-transition panel c)
      (= t "action")     (render-action panel c)
      (= t "folder")     (render-folder panel c)
      :else (mk "div" nil))))

;; ── Persistence ─────────────────────────────────────────────────────

(defn- storage-for [panel]
  (let [p (:persist panel)]
    (if (and p (= (aget p "storage") "sessionStorage"))
      js/sessionStorage js/localStorage)))

(defn- storage-key [panel]
  (let [p (:persist panel)]
    (or (and p (aget p "key")) (str "dialkit:" (:id panel)))))

(defn- persist-save! [panel]
  (when (:persist panel)
    (try
      (let [payload #js {:values (clj->js @(:store panel))
                         :presets (:presets panel)
                         :active (:activePreset panel)}]
        (.setItem (storage-for panel) (storage-key panel) (js/JSON.stringify payload)))
      (catch :default _ nil))))

(defn- persist-load! [panel]
  (when (:persist panel)
    (try
      (let [raw (.getItem (storage-for panel) (storage-key panel))]
        (when raw
          (let [data (js/JSON.parse raw)]
            (when (aget data "values")
              (reset! (:store panel) (aget data "values")))
            (when (aget data "presets") (aset panel "presets" (aget data "presets")))
            (when (aget data "active") (aset panel "activePreset" (aget data "active"))))))
      (catch :default _ nil))))

;; ── Root container + dragging ───────────────────────────────────────

(def ^:private root-el (atom nil))
(def ^:private panels (atom #js {}))
(def ^:private drag-state #js {:on false :x 0 :y 0 :sx 0 :sy 0})

(defn- apply-root-position! [el pos]
  (let [s (.-style el)
        bottom? (or (= pos "bottom-right") (= pos "bottom-left"))
        left?   (or (= pos "top-left") (= pos "bottom-left"))]
    (set! (.-top s) "auto") (set! (.-bottom s) "auto")
    (set! (.-left s) "auto") (set! (.-right s) "auto")
    (if bottom? (set! (.-bottom s) "12px") (set! (.-top s) "12px"))
    (if left? (set! (.-left s) "12px") (set! (.-right s) "12px"))))

(defn- ensure-root! []
  (or @root-el
      (let [el (mk "div" "dialkit-root")]
        (attr! el "data-theme" "system")
        (apply-root-position! el "top-right")
        (.appendChild js/document.body el)
        (reset! root-el el)
        (js/window.addEventListener "pointermove"
          (fn [e] (when (aget drag-state "on")
                    (let [dx (- (.-clientX e) (aget drag-state "sx"))
                          dy (- (.-clientY e) (aget drag-state "sy"))]
                      (set! (.. el -style -right) "auto")
                      (set! (.. el -style -bottom) "auto")
                      (set! (.. el -style -left) (str (+ (aget drag-state "x") dx) "px"))
                      (set! (.. el -style -top) (str (+ (aget drag-state "y") dy) "px"))))))
        (js/window.addEventListener "pointerup" (fn [_] (aset drag-state "on" false)))
        el)))

(defn- start-drag! [e]
  (let [el @root-el
        rect (.getBoundingClientRect el)]
    (aset drag-state "on" true)
    (aset drag-state "sx" (.-clientX e))
    (aset drag-state "sy" (.-clientY e))
    (aset drag-state "x" (.-left rect))
    (aset drag-state "y" (.-top rect))))

;; ── Panel build / versions ──────────────────────────────────────────

(defn- build-body! [panel]
  (let [body (:bodyEl panel)]
    (set! (.-innerHTML body) "")
    (aset panel "updaters" #js {})
    (doseq [c (:controls panel)]
      (add! body (render-control panel c)))))

(declare render-versions!)

(defn- select-version! [panel id]
  (aset panel "activePreset" id)
  ;; The store natively holds a plain JS object (squint), and snapshots are
  ;; taken with clj->js, so reset directly — squint's core has no js->clj
  ;; (it compiles to an undefined ref and throws, silently killing the
  ;; reset / version handlers).
  (if id
    (let [p (.find (:presets panel) (fn [x] (= (aget x "id") id)))]
      (when p (reset! (:store panel) (aget p "values"))))
    (reset! (:store panel) (:baseValues panel)))
  (refresh-updaters! panel)
  (notify! panel)
  (persist-save! panel)
  (render-versions! panel))

(defn- save-version! [panel]
  (let [id (str "v" (.now js/Date))
        n  (str "Version " (+ 2 (.-length (:presets panel))))
        preset #js {:id id :name n :values (clj->js @(:store panel))}]
    (.push (:presets panel) preset)
    (select-version! panel id)))

(defn- render-versions! [panel]
  (let [sel (:versionSel panel)]
    (set! (.-innerHTML sel) "")
    (let [o0 (mk "option" nil)]
      (set! (.-value o0) "") (txt! o0 "Version 1") (add! sel o0))
    (doseq [p (:presets panel)]
      (let [o (mk "option" nil)]
        (set! (.-value o) (aget p "id")) (txt! o (aget p "name")) (add! sel o)))
    (set! (.-value sel) (or (:activePreset panel) ""))))

(defn- copy-config! [panel]
  (let [vals (js/JSON.stringify (clj->js @(:store panel)) nil 2)
        text (str "// DialKit values for \"" (:name panel) "\"\n"
                  "// Replace your config defaults with these tuned values:\n"
                  vals)]
    (when js/navigator.clipboard
      (.writeText js/navigator.clipboard text))
    (when-let [t js/window.__uiToast] (t "Copied values to clipboard" #js {:variant "success"}))))

;; ── Panel construction + mount ──────────────────────────────────────

(defn- make-panel [name config opts]
  (let [id (or (aget opts "id") (str "dial-" (.slice (.toString (js/Math.random) 36) 2 8)))
        controls (parse-config config #js [])
        store (atom {})
        panel #js {:id id :name name :controls controls :store store
                   :updaters #js {}
                   :subs #js []
                   :onChange (aget opts "onChange")
                   :onAction (aget opts "onAction")
                   :persist (let [p (aget opts "persist")]
                              (cond (= p true) #js {}
                                    (= (tof p) "object") p
                                    :else nil))
                   :presets #js []
                   :activePreset nil
                   :position (or (aget opts "position") "top-right")
                   :icon (or (aget opts "icon") "⚙")
                   :open (not (aget opts "defaultCollapsed"))}]
    (init-values! store controls)
    (aset panel "baseValues" (clj->js @store))
    (persist-load! panel)
    panel))

(defn- mount-panel! [panel opts]
  (let [root   (ensure-root!)
        pos    (:position panel)
        side   (if (or (= pos "top-left") (= pos "bottom-left")) "pos-left" "pos-right")
        card   (mk "div" (str "dial-panel " side))
        head   (mk "div" "dial-panel-head")
        title  (mk "div" "dial-panel-title")
        tools  (mk "div" "dial-panel-tools")
        vsel   (mk "select" "dial-version")
        addb   (mk "button" "dial-tool")
        copyb  (mk "button" "dial-tool")
        resetb (mk "button" "dial-tool")
        collb  (mk "button" "dial-tool")
        iconb  (mk "button" "dial-panel-icon")
        body   (mk "div" "dial-panel-body")]
    (apply-root-position! root pos)
    (txt! title (:name panel))
    (aset panel "bodyEl" body)
    (aset panel "versionSel" vsel)
    (aset panel "cardEl" card)
    (attr! iconb "type" "button")
    (attr! iconb "title" (str "Expand " (:name panel)))
    (txt! iconb (:icon panel))
    (doseq [spec [#js [addb "+" "Save version"] #js [copyb "⧉" "Copy values"]
                  #js [resetb "↺" "Reset"] #js [collb "–" "Minimize"]]]
      (let [b (aget spec 0)]
        (attr! b "type" "button") (attr! b "title" (aget spec 2)) (txt! b (aget spec 1))))
    (on! vsel "change" (fn [_] (select-version! panel (let [v (.-value vsel)] (when (seq v) v)))))
    (on! addb "click" (fn [_] (save-version! panel)))
    (on! copyb "click" (fn [_] (copy-config! panel)))
    (on! resetb "click" (fn [_] (select-version! panel nil)))
    (on! collb "click" (fn [_]
                         (.add (.-classList card) "is-iconified")
                         (aset panel "open" false)))
    (on! iconb "click" (fn [_]
                         (.remove (.-classList card) "is-iconified")
                         (aset panel "open" true)))
    (on! head "pointerdown" (fn [e]
                              (when (or (= (.-target e) head) (= (.-target e) title))
                                (start-drag! e))))
    (add! tools vsel addb copyb resetb collb)
    (add! head title tools)
    (add! card head body iconb)
    (add! root card)
    (build-body! panel)
    (render-versions! panel)
    (when (aget opts "defaultCollapsed")
      (.add (.-classList card) "is-iconified") (aset panel "open" false))
    card))

;; ── Controller / public API ─────────────────────────────────────────

(defn- setvals-walk! [panel prefix o]
  (doseq [k (js/Object.keys o)]
    (let [v (aget o k) path (conj prefix k)]
      (if (and (= (tof v) "object") (not (js/Array.isArray v))
               (not (aget (:updaters panel) (path-str path))))
        (setvals-walk! panel path v)
        (set-value! panel path v)))))

(defn- controller [panel]
  #js {:id (:id panel)
       :getValues (fn [] (clj->js @(:store panel)))
       :setValue (fn [p v] (set-value! panel (.split p ".") v) js/undefined)
       :setValues (fn [obj] (setvals-walk! panel #js [] obj) js/undefined)
       :resetValues (fn [] (select-version! panel nil) js/undefined)
       :setOpen (fn [o]
                  (aset panel "open" o)
                  (when-let [card (:cardEl panel)]
                    (if o (.remove (.-classList card) "is-iconified") (.add (.-classList card) "is-iconified")))
                  js/undefined)
       :getOpen (fn [] (:open panel))
       :subscribe (fn [cb immediate]
                    (.push (:subs panel) cb)
                    (when (not= immediate false) (cb (clj->js @(:store panel))))
                    (fn [] (let [i (.indexOf (:subs panel) cb)] (when (>= i 0) (.splice (:subs panel) i 1)))))
       :destroy (fn []
                  (when-let [card (:cardEl panel)] (.remove card))
                  (js-delete @panels (:id panel))
                  js/undefined)})

(defn use-dial [name config opts]
  (let [opts (or opts #js {})
        panel (make-panel name config opts)
        ctrl (controller panel)]
    (aset @panels (:id panel) panel)
    (aset panel "ctrl" ctrl)
    (js/Object.defineProperty ctrl "values"
      #js {:get (fn [] (clj->js @(:store panel)))})
    (when (not= (aget opts "enabled") false)
      (mount-panel! panel opts))
    ctrl))

;; ── DialStore ───────────────────────────────────────────────────────

(defn- panel-by-id [id] (aget @panels id))

(def dial-store
  #js {:setPanelOpen (fn [id o] (when-let [p (panel-by-id id)] ((aget (:ctrl p) "setOpen") o)))
       :togglePanelOpen (fn [id] (when-let [p (panel-by-id id)] ((aget (:ctrl p) "setOpen") (not (:open p)))))
       :getPanelOpen (fn [id] (when-let [p (panel-by-id id)] (:open p)))
       :getPresets (fn [id] (when-let [p (panel-by-id id)] (:presets p)))
       :getActivePresetId (fn [id] (when-let [p (panel-by-id id)] (:activePreset p)))
       :clearActivePreset (fn [id] (when-let [p (panel-by-id id)] (select-version! p nil)))
       :savePreset (fn [id] (when-let [p (panel-by-id id)] (save-version! p)))
       :deletePreset (fn [id pid]
                       (when-let [p (panel-by-id id)]
                         (aset p "presets" (.filter (:presets p) (fn [x] (not= (aget x "id") pid))))
                         (render-versions! p) (persist-save! p)))})

(aset js/window "__uiDial" use-dial)
(aset js/window "DialStore" dial-store)
