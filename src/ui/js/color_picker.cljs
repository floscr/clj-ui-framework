(ns ui.js.color-picker
  "Runtime for [data-ui-color-picker] elements (markup from ui.color-picker).

   Owns the picker state (hue, saturation, brightness, alpha, format) as a JS
   object on the element and paints everything derived from it. A
   document-level MutationObserver paints new pickers and re-adopts
   `data-value` when it changes to a value this runtime did not emit, so a
   controlled re-render keeps the hue of a grey or black pick.

   Emitting writes the hidden [data-ui-color-value] input and fires `input`
   on it for every change and `change` when a gesture ends.

   Imperative API (used by the dial; any plain-JS caller):
     window.__uiColorPicker.create({value, alpha, formats}) -> element
     window.__uiColorPicker.setValue(el, value)")

;; ── Color math (HSV ⇄ RGB ⇄ HSL) ────────────────────────────────────

(defn- clamp [v lo hi] (js/Math.max lo (js/Math.min v hi)))

(defn- fmt-num [n]
  (if (js/isFinite n) (str (/ (js/Math.round (* n 1000)) 1000)) "0"))

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

(defn- hue-of [r g b mx d]
  (let [h (cond
            (zero? d) 0
            (= mx r)  (* 60 (mod (/ (- g b) d) 6))
            (= mx g)  (* 60 (+ (/ (- b r) d) 2))
            :else     (* 60 (+ (/ (- r g) d) 4)))]
    (if (< h 0) (+ h 360) h)))

(defn- rgb->hsv [r g b]
  (let [r (/ r 255) g (/ g 255) b (/ b 255)
        mx (js/Math.max r g b) mn (js/Math.min r g b)
        d  (- mx mn)]
    #js [(hue-of r g b mx d) (if (zero? mx) 0 (/ d mx)) mx]))

(defn- linear [c]
  (let [c (/ c 255)]
    (if (<= c 0.04045) (/ c 12.92) (js/Math.pow (/ (+ c 0.055) 1.055) 2.4))))

(defn- rgb->oklch
  "sRGB bytes → #js [L C H] (OKLab in polar form, H in degrees)."
  [r g b]
  (let [r (linear r) g (linear g) b (linear b)
        l (js/Math.cbrt (+ (* 0.4122214708 r) (* 0.5363325363 g) (* 0.0514459929 b)))
        m (js/Math.cbrt (+ (* 0.2119034982 r) (* 0.6806995451 g) (* 0.1073969566 b)))
        s (js/Math.cbrt (+ (* 0.0883024619 r) (* 0.2817188376 g) (* 0.6299787005 b)))
        L  (+ (* 0.2104542553 l) (* 0.7936177850 m) (* -0.0040720468 s))
        oa (+ (* 1.9779984951 l) (* -2.4285922050 m) (* 0.4505937099 s))
        ob (+ (* 0.0259040371 l) (* 0.7827717662 m) (* -0.8086757660 s))
        h  (* (/ 180 js/Math.PI) (js/Math.atan2 ob oa))]
    #js [L (js/Math.sqrt (+ (* oa oa) (* ob ob))) (if (< h 0) (+ h 360) h)]))

(defn- rgb->hsl [r g b]
  (let [r (/ r 255) g (/ g 255) b (/ b 255)
        mx (js/Math.max r g b) mn (js/Math.min r g b)
        d  (- mx mn)
        l  (/ (+ mx mn) 2)]
    #js [(hue-of r g b mx d)
         (if (zero? d) 0 (/ d (- 1 (js/Math.abs (- (* 2 l) 1)))))
         l]))

;; Any CSS color (hex, named, rgb, hsl, oklch, color(), …) → #js [r g b a].
;; Painting a 1×1 canvas converts every color space to sRGB bytes, where
;; getComputedStyle would hand back oklch()/color() untouched.
(def ^:private probe (atom nil))

(defn- probe-ctx []
  (or @probe
      (let [c (js/document.createElement "canvas")]
        (set! (.-width c) 1)
        (set! (.-height c) 1)
        (let [ctx (.getContext c "2d" #js {:willReadFrequently true})]
          (reset! probe ctx)
          ctx))))

(defn parse-rgba [s]
  (let [s (.trim (str (or s "")))]
    (when (and (not= "" s) (js/CSS.supports "color" s))
      (let [ctx (probe-ctx)]
        (.clearRect ctx 0 0 1 1)
        (set! (.-fillStyle ctx) s)
        (.fillRect ctx 0 0 1 1)
        (let [d (.-data (.getImageData ctx 0 0 1 1))]
          #js [(aget d 0) (aget d 1) (aget d 2) (/ (aget d 3) 255)])))))

(defn- hex2 [n]
  (.padStart (.toString (clamp (js/Math.round n) 0 255) 16) 2 "0"))

(defn- compose [h s v a fmt]
  (let [rgb (hsv->rgb h s v)
        r (aget rgb 0) g (aget rgb 1) b (aget rgb 2)
        translucent? (< a 0.999)]
    (case fmt
      "oklch" (let [lch (rgb->oklch r g b)
                    c (aget lch 1)
                    ;; greys have no hue; print 0 instead of rounding noise
                    h (if (< c 0.0005) 0 (/ (js/Math.round (* 10 (aget lch 2))) 10))]
                (str "oklch(" (fmt-num (aget lch 0)) " " (fmt-num c) " " h
                     (if translucent? (str " / " (fmt-num a)) "") ")"))
      "rgb" (if translucent?
              (str "rgba(" r ", " g ", " b ", " (fmt-num a) ")")
              (str "rgb(" r ", " g ", " b ")"))
      "hsl" (let [hsl (rgb->hsl r g b)
                  hh (js/Math.round (aget hsl 0))
                  ss (js/Math.round (* 100 (aget hsl 1)))
                  ll (js/Math.round (* 100 (aget hsl 2)))]
              (if translucent?
                (str "hsla(" hh ", " ss "%, " ll "%, " (fmt-num a) ")")
                (str "hsl(" hh ", " ss "%, " ll "%)")))
      (str "#" (hex2 r) (hex2 g) (hex2 b) (if translucent? (hex2 (* a 255)) "")))))

(defn- detect-fmt [s]
  (let [s (.toLowerCase (.trim (str (or s ""))))]
    (cond
      (.startsWith s "#")   "hex"
      (.startsWith s "rgb") "rgb"
      (.startsWith s "hsl") "hsl"
      (.startsWith s "oklch") "oklch")))

;; ── Picker state ────────────────────────────────────────────────────

(defn- q [el sel] (.querySelector el sel))

(defn- state [el] (aget el "__uiColor"))

(defn- formats [el]
  (.split (or (.getAttribute el "data-formats") "oklch hex rgb hsl") " "))

(defn- adopt!
  "Take color string `value` into the picker state. Keeps the previous hue
   when the color is grey and the previous saturation when it is black."
  [el value]
  (let [st (or (state el)
               (let [o #js {:h 0 :s 0 :v 0 :a 1 :fmt nil :value nil}]
                 (aset el "__uiColor" o)
                 o))
        fs (formats el)
        f  (detect-fmt value)
        cur (aget st "fmt")]
    (when-let [rgba (parse-rgba value)]
      (let [hsv (rgb->hsv (aget rgba 0) (aget rgba 1) (aget rgba 2))]
        (when (> (aget hsv 1) 0.0001) (aset st "h" (aget hsv 0)))
        (when (> (aget hsv 2) 0.0001) (aset st "s" (aget hsv 1)))
        (aset st "v" (aget hsv 2))
        (aset st "a" (aget rgba 3))))
    (aset st "fmt" (cond
                     (and f (.includes fs f))     f
                     (and cur (.includes fs cur)) cur
                     :else                        (aget fs 0)))
    st))

(defn- paint! [el]
  (let [st (state el)
        h (aget st "h") s (aget st "s") v (aget st "v")
        rgb (hsv->rgb h s v)
        solid (str "rgb(" (aget rgb 0) ", " (aget rgb 1) ", " (aget rgb 2) ")")]
    (when-let [plane (q el ".color-picker-plane")]
      (set! (.. plane -style -background)
            (str "linear-gradient(to top, #000, rgba(0,0,0,0)),"
                 "linear-gradient(to right, #fff, hsl(" (js/Math.round h) ", 100%, 50%))"))
      (.setAttribute plane "aria-valuetext" (or (aget st "value") "")))
    (when-let [marker (q el ".color-picker-marker")]
      (set! (.. marker -style -left) (str (* 100 s) "%"))
      (set! (.. marker -style -top) (str (* 100 (- 1 v)) "%"))
      (set! (.. marker -style -background) solid))
    (when-let [hue (q el "[data-ui-color-hue]")]
      (set! (.-value hue) (str h)))
    (when-let [op (q el "[data-ui-color-alpha]")]
      (set! (.-value op) (str (aget st "a")))
      (.setProperty (.-style op) "--color-picker-solid" solid))
    (.forEach (.querySelectorAll el "[data-ui-color-format]")
              (fn [b]
                (.setAttribute b "data-active"
                               (if (= (.getAttribute b "data-ui-color-format") (aget st "fmt"))
                                 "true" "false"))))))

(defn- show-value! [el value]
  (aset (state el) "value" value)
  (when-let [t (q el "[data-ui-color-text]")] (set! (.-value t) value)))

(defn- sync!
  "Paint `el` from its data-value unless that is the value it already shows."
  [el]
  (let [v (or (.getAttribute el "data-value") "")
        st (state el)]
    (when (or (nil? st) (not= v (aget st "value")))
      (adopt! el v)
      (show-value! el v)
      (paint! el))))

(defn- fire! [input type]
  (.dispatchEvent input (js/Event. type #js {:bubbles true})))

(defn- emit!
  "Compose the state into a color, show it and fire `input` (plus `change`
   when `commit?`) on the hidden value input."
  [el commit?]
  (let [st (state el)
        v  (compose (aget st "h") (aget st "s") (aget st "v") (aget st "a") (aget st "fmt"))]
    (show-value! el v)
    (.setAttribute el "data-value" v)
    (paint! el)
    (when-let [input (q el "[data-ui-color-value]")]
      (set! (.-value input) v)
      (fire! input "input")
      (when commit? (fire! input "change")))))

(defn- commit! [el]
  (when-let [input (q el "[data-ui-color-value]")]
    (fire! input "change")))

;; ── Interaction (document-level delegation) ─────────────────────────

(defn- closest [target sel]
  (when (and target (.-closest target)) (.closest target sel)))

(defn- live-picker
  "The enabled picker around `node`, synced so its state exists."
  [node]
  (when-let [el (closest node "[data-ui-color-picker]")]
    (when-not (.hasAttribute el "data-ui-color-disabled")
      (sync! el)
      el)))

(def ^:private drag (atom nil))

(defn- plane-at! [el plane e]
  (let [rect (.getBoundingClientRect plane)
        st (state el)]
    (aset st "s" (clamp (/ (- (.-clientX e) (.-left rect)) (.-width rect)) 0 1))
    (aset st "v" (- 1 (clamp (/ (- (.-clientY e) (.-top rect)) (.-height rect)) 0 1)))
    (emit! el false)))

(defn- on-pointerdown [e]
  (when-let [plane (closest (.-target e) ".color-picker-plane")]
    (when-let [el (live-picker plane)]
      (.preventDefault e)
      (.focus plane)
      (.setPointerCapture plane (.-pointerId e))
      (reset! drag #js [el plane])
      (plane-at! el plane e))))

(defn- on-pointermove [e]
  (when-let [d @drag]
    (plane-at! (aget d 0) (aget d 1) e)))

(defn- on-pointerup [_]
  (when-let [d @drag]
    (reset! drag nil)
    (commit! (aget d 0))))

(defn- on-input [e]
  (let [t (.-target e)
        hue? (and (.-hasAttribute t) (.hasAttribute t "data-ui-color-hue"))
        alpha? (and (.-hasAttribute t) (.hasAttribute t "data-ui-color-alpha"))]
    (when (or hue? alpha?)
      (when-let [el (live-picker t)]
        (aset (state el) (if hue? "h" "a") (js/parseFloat (.-value t)))
        (emit! el false)))))

(defn- commit-text! [el t]
  (if (parse-rgba (.-value t))
    (do (adopt! el (.trim (.-value t)))
        (emit! el true))
    (set! (.-value t) (aget (state el) "value"))))

(defn- on-change [e]
  (let [t (.-target e)]
    (when (.-hasAttribute t)
      (cond
        (or (.hasAttribute t "data-ui-color-hue") (.hasAttribute t "data-ui-color-alpha"))
        (when-let [el (live-picker t)] (commit! el))

        (.hasAttribute t "data-ui-color-text")
        (when-let [el (live-picker t)] (commit-text! el t))))))

(defn- on-click [e]
  (when-let [b (closest (.-target e) "[data-ui-color-format]")]
    (when-let [el (live-picker b)]
      (aset (state el) "fmt" (.getAttribute b "data-ui-color-format"))
      (emit! el true))))

(def ^:private arrow-deltas
  #js {"ArrowLeft" #js ["s" -1] "ArrowRight" #js ["s" 1]
       "ArrowDown" #js ["v" -1] "ArrowUp"    #js ["v" 1]})

(defn- on-keydown [e]
  (when-let [delta (aget arrow-deltas (.-key e))]
    (when-let [plane (closest (.-target e) ".color-picker-plane")]
      (when-let [el (live-picker plane)]
        (.preventDefault e)
        (let [st (state el)
              k (aget delta 0)
              step (if (.-shiftKey e) 0.1 0.01)]
          (aset st k (clamp (+ (aget st k) (* step (aget delta 1))) 0 1))
          (emit! el true))))))

;; ── Imperative API ──────────────────────────────────────────────────

(defn- mk [tag cls]
  (let [e (js/document.createElement tag)]
    (when cls (set! (.-className e) cls))
    e))

(defn- track-row [label cls data-attr mx step]
  (let [row (mk "label" "color-picker-track-row")
        span (mk "span" nil)
        input (mk "input" (str "color-picker-track " cls))]
    (set! (.-textContent span) label)
    (set! (.-type input) "range")
    (set! (.-min input) "0")
    (set! (.-max input) (str mx))
    (set! (.-step input) (str step))
    (.setAttribute input data-attr "true")
    (.append row span input)
    row))

(def ^:private all-formats #js ["oklch" "hex" "rgb" "hsl"])

(def ^:private format-labels #js {"oklch" "OKLCH" "hex" "Hex" "rgb" "RGB" "hsl" "HSL"})

(defn create
  "Build a picker element (same markup as ui.color-picker) and paint it."
  [opts]
  (let [opts (or opts #js {})
        fs (.filter (or (aget opts "formats") all-formats)
                    (fn [f] (aget format-labels f)))
        fs (if (pos? (.-length fs)) fs all-formats)
        el (mk "div" "color-picker")
        plane (mk "div" "color-picker-plane")
        tracks (mk "div" "color-picker-tracks")
        text (mk "input" "color-picker-text")
        value (mk "input" nil)]
    (.setAttribute el "data-ui-color-picker" "true")
    (.setAttribute el "data-value" (or (aget opts "value") ""))
    (.setAttribute el "data-formats" (.join fs " "))
    (when (aget opts "alpha") (.setAttribute el "data-alpha" "true"))
    (when (> (.-length fs) 1)
      (let [group (mk "div" "color-picker-formats")]
        (.setAttribute group "role" "group")
        (.setAttribute group "aria-label" "Color format")
        (.forEach fs (fn [f]
                       (let [b (mk "button" "color-picker-format")]
                         (set! (.-type b) "button")
                         (set! (.-textContent b) (aget format-labels f))
                         (.setAttribute b "data-ui-color-format" f)
                         (.append group b))))
        (.append el group)))
    (.setAttribute plane "tabindex" "0")
    (.setAttribute plane "role" "slider")
    (.setAttribute plane "aria-label" "Saturation and brightness")
    (.append plane (mk "span" "color-picker-marker"))
    (.append tracks (track-row "Hue" "color-picker-hue" "data-ui-color-hue" 360 1))
    (when (aget opts "alpha")
      (.append tracks (track-row "Opacity" "color-picker-opacity" "data-ui-color-alpha" 1 0.01)))
    (set! (.-type text) "text")
    (.setAttribute text "spellcheck" "false")
    (.setAttribute text "autocomplete" "off")
    (.setAttribute text "aria-label" "Color value")
    (.setAttribute text "data-ui-color-text" "true")
    (set! (.-type value) "hidden")
    (.setAttribute value "data-ui-color-value" "true")
    (set! (.-value value) (or (aget opts "value") ""))
    (.append el plane tracks text value)
    (sync! el)
    el))

(defn set-value!
  "Set a picker's color from outside (no events fire)."
  [el value]
  (.setAttribute el "data-value" (or value ""))
  (when-let [input (q el "[data-ui-color-value]")] (set! (.-value input) (or value "")))
  (sync! el))

;; ── Bootstrap ───────────────────────────────────────────────────────

(defn- scan! []
  (.forEach (js/document.querySelectorAll "[data-ui-color-picker]") (fn [el] (sync! el))))

(defn- on-mutations [records]
  (let [added? (atom false)]
    (.forEach records
              (fn [r]
                (if (= "attributes" (.-type r))
                  (when (.hasAttribute (.-target r) "data-ui-color-picker")
                    (sync! (.-target r)))
                  (when (pos? (.. r -addedNodes -length))
                    (reset! added? true)))))
    (when @added? (scan!))))

(defn init! []
  ;; Capture phase: a dialog panel that stops click propagation (so clicks
  ;; inside don't reach its overlay) must not swallow the picker's events.
  (doseq [[type f] [["pointerdown" on-pointerdown] ["pointermove" on-pointermove]
                    ["pointerup" on-pointerup] ["pointercancel" on-pointerup]
                    ["input" on-input] ["change" on-change]
                    ["click" on-click] ["keydown" on-keydown]]]
    (.addEventListener js/document type f true))
  (scan!)
  (.observe (js/MutationObserver. on-mutations) js/document.documentElement
            #js {:childList true :subtree true
                 :attributes true :attributeFilter #js ["data-value"]}))

(aset js/window "__uiColorPicker" #js {:create create :setValue set-value!})

(if (= "loading" (.-readyState js/document))
  (.addEventListener js/document "DOMContentLoaded" init!)
  (init!))
