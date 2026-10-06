(ns lightbox
  "Pinch-zoom / pan runtime for ui.lightbox images.

   Installs document-level Pointer Event handlers on `.lightbox-overlay`
   so every lightbox (hiccup, squint, replicant) can be zoomed without
   per-app wiring:

   - two-finger pinch zooms around the pinch midpoint (and pans with it)
   - one-finger / mouse drag pans a zoomed image
   - double-tap / double-click on the image toggles 1x ↔ 2.5x at the
     tapped point
   - mouse wheel / trackpad pinch (ctrl+wheel) zooms around the cursor
   - on release the zoom snaps back into bounds: scale 1x–5x, and a
     zoomed image is never panned past its edges
   - the click that ends a drag/pinch is suppressed, so releasing over
     the backdrop doesn't close the lightbox

   The zoom lives in the image's inline `transform` (plus a
   `data-zoomed` attribute while zoomed in) — no component state.
   It resets when the image (re)loads, so swapping :src starts fresh.
   Pair with ui/lightbox.css (`touch-action: none` on the overlay).

   Loaded automatically via ui-runtime.js (hiccup apps) or a
   side-effect require of [ui.js.lightbox] (squint/replicant SPAs).")

(def ^:private overlay-sel ".lightbox-overlay")
(def ^:private image-sel ".lightbox-image")
(def ^:private max-scale 5)
;; how far a pinch may overshoot the bounds before release snaps back
(def ^:private pinch-min 0.75)
(def ^:private pinch-max 6)
(def ^:private double-tap-scale 2.5)
(def ^:private double-tap-ms 300)
(def ^:private tap-slop-px 10)
(def ^:private double-tap-slop-px 30)
(def ^:private identity-zoom {:s 1 :tx 0 :ty 0})

;; pointerId -> {:x :y :x0 :y0} for pointers pressed inside an overlay
(def ^:private pointers (js/Map.))
;; {:img :z0 :pts0 :moved :on-image :max-pointers} while pointers are down
(def ^:private gesture (atom nil))
(def ^:private suppress-click? (atom false))
;; {:t :x :y} of the previous tap, for double-tap detection
(def ^:private last-tap (atom nil))

(defn- clamp [lo hi v]
  (js/Math.min hi (js/Math.max lo v)))

(defn- rel
  "Viewport coords → offset from the viewport center (the image's
   untransformed center: the overlay is fixed + flex-centered)."
  [x y]
  [(- x (/ js/window.innerWidth 2))
   (- y (/ js/window.innerHeight 2))])

(defn- zoom-of [img]
  (or (aget img "__uiZoom") identity-zoom))

(defn- zoom-at
  "Scale z to s while keeping the point px/py (center-relative) fixed."
  [z s px py]
  (let [k (/ s (:s z))]
    (assoc z
           :s s
           :tx (- px (* k (- px (:tx z))))
           :ty (- py (* k (- py (:ty z)))))))

(defn- settle
  "Snap z into bounds: scale within [1, max-scale]; translation so the
   scaled image never exposes backdrop on an axis where it overflows."
  [img z]
  (let [s (clamp 1 max-scale (:s z))
        mx (js/Math.max 0 (/ (- (* s (.-offsetWidth img)) js/window.innerWidth) 2))
        my (js/Math.max 0 (/ (- (* s (.-offsetHeight img)) js/window.innerHeight) 2))]
    (assoc z
           :s s
           :tx (clamp (- mx) mx (:tx z))
           :ty (clamp (- my) my (:ty z)))))

(defn- apply-zoom!
  "Store z on img and render it as an inline transform. animate? uses
   the CSS transition (ui/lightbox.css); otherwise it tracks the finger."
  [img z animate?]
  (aset img "__uiZoom" z)
  (let [st (.-style img)]
    (set! (.-transition st) (if animate? "" "none"))
    (set! (.-transform st)
          (if (= 1 (:s z))
            ""
            (str "translate(" (:tx z) "px, " (:ty z) "px) scale(" (:s z) ")"))))
  (if (> (:s z) 1)
    (.setAttribute img "data-zoomed" "")
    (.removeAttribute img "data-zoomed")))

(defn- reset-zoom! [img]
  (aset img "__uiZoom" nil)
  (set! (.-transform (.-style img)) "")
  (.removeAttribute img "data-zoomed"))

(defn- current-points []
  (js/Array.from (.values pointers)))

(defn- rebase!
  "Re-anchor the gesture on the current pointers + zoom (pointer added
   or lifted), so the next move continues smoothly from here."
  []
  (when-let [g @gesture]
    (let [pts (current-points)]
      (reset! gesture
              (assoc g
                     :z0 (zoom-of (:img g))
                     :pts0 pts
                     :max-pointers (js/Math.max (:max-pointers g) (.-length pts)))))))

(defn- midpoint [a b]
  (rel (/ (+ (:x a) (:x b)) 2)
       (/ (+ (:y a) (:y b)) 2)))

(defn- distance [a b]
  (js/Math.hypot (- (:x a) (:x b)) (- (:y a) (:y b))))

(defn- track! []
  (let [{:keys [img z0 pts0]} @gesture
        pts (current-points)]
    (if (>= (.-length pts) 2)
      ;; pinch: scale by finger spread around the starting midpoint,
      ;; then follow the midpoint's movement
      (let [a0 (aget pts0 0) b0 (aget pts0 1)
            a (aget pts 0) b (aget pts 1)
            [mx0 my0] (midpoint a0 b0)
            [mx my] (midpoint a b)
            s (clamp pinch-min pinch-max
                     (* (:s z0) (/ (distance a b)
                                   (js/Math.max 1 (distance a0 b0)))))
            z (zoom-at z0 s mx0 my0)]
        (apply-zoom! img
                     (assoc z
                            :tx (+ (:tx z) (- mx mx0))
                            :ty (+ (:ty z) (- my my0)))
                     false))
      ;; pan: only meaningful while zoomed in
      (when (> (:s z0) 1)
        (let [p0 (aget pts0 0)
              p (aget pts 0)]
          (apply-zoom! img
                       (assoc z0
                              :tx (+ (:tx z0) (- (:x p) (:x p0)))
                              :ty (+ (:ty z0) (- (:y p) (:y p0))))
                       false))))))

(defn- tap! [img x y]
  (let [now (js/Date.now)
        prev @last-tap]
    (if (and prev
             (< (- now (:t prev)) double-tap-ms)
             (< (js/Math.hypot (- x (:x prev)) (- y (:y prev))) double-tap-slop-px))
      (let [z (zoom-of img)
            [px py] (rel x y)]
        (reset! last-tap nil)
        (apply-zoom! img
                     (if (> (:s z) 1)
                       identity-zoom
                       (settle img (zoom-at z double-tap-scale px py)))
                     true))
      (reset! last-tap {:t now :x x :y y}))))

(defn- on-pointerdown [e]
  (let [t (.-target e)
        overlay (some-> t (.closest overlay-sel))]
    (when (and overlay
               (not (.closest t ".lightbox-close"))
               (or (not= "mouse" (.-pointerType e)) (zero? (.-button e))))
      (when-let [img (.querySelector overlay image-sel)]
        ;; fresh gesture — also drops pointers orphaned by a lightbox that
        ;; closed mid-gesture (their pointerup may never have reached us)
        (when (or (zero? (.-size pointers))
                  (not= img (:img @gesture)))
          (.clear pointers)
          (reset! suppress-click? false)
          (reset! gesture {:img img
                           :moved false
                           :on-image (.contains img t)
                           :max-pointers 0}))
        (let [x (.-clientX e)
              y (.-clientY e)]
          (.set pointers (.-pointerId e) {:x x :y y :x0 x :y0 y}))
        ;; no native image drag / text selection while panning with a mouse
        (when (= "mouse" (.-pointerType e))
          (.preventDefault e))
        (rebase!)))))

(defn- on-pointermove [e]
  (let [id (.-pointerId e)]
    (when (.has pointers id)
      (let [p (.get pointers id)
            x (.-clientX e)
            y (.-clientY e)]
        (.set pointers id (assoc p :x x :y y))
        (when (> (js/Math.hypot (- x (:x0 p)) (- y (:y0 p))) tap-slop-px)
          (swap! gesture assoc :moved true))
        (track!)))))

(defn- on-pointer-end [e]
  (let [id (.-pointerId e)]
    (when (.has pointers id)
      (.delete pointers id)
      (if (pos? (.-size pointers))
        (rebase!)
        (let [{:keys [img moved on-image max-pointers]} @gesture]
          (reset! gesture nil)
          (when moved
            (reset! suppress-click? true))
          (if (and (not moved)
                   on-image
                   (= 1 max-pointers)
                   (= "pointerup" (.-type e)))
            (tap! img (.-clientX e) (.-clientY e))
            (apply-zoom! img (settle img (zoom-of img)) true)))))))

(defn- on-wheel [e]
  (when-let [overlay (some-> (.-target e) (.closest overlay-sel))]
    (when-let [img (.querySelector overlay image-sel)]
      (.preventDefault e)
      (let [z (zoom-of img)
            ;; deltaMode 1 = lines; ctrl+wheel = trackpad pinch (finer deltas)
            dy (* (.-deltaY e) (if (= 1 (.-deltaMode e)) 16 1))
            k (js/Math.exp (* (- dy) (if (.-ctrlKey e) 0.01 0.002)))
            [px py] (rel (.-clientX e) (.-clientY e))]
        (apply-zoom! img
                     (settle img (zoom-at z (clamp 1 max-scale (* (:s z) k)) px py))
                     false)))))

(defn- on-click-capture [e]
  (when @suppress-click?
    (reset! suppress-click? false)
    (.preventDefault e)
    (.stopPropagation e)))

(defn- on-load-capture [e]
  (let [t (.-target e)]
    (when (and (.-matches t) (.matches t image-sel))
      (reset-zoom! t))))

(defn- on-dragstart [e]
  ;; the target can be a text node (selection drag) — no .closest there
  (let [t (.-target e)]
    (when (and t (.-closest t) (.closest t overlay-sel))
      (.preventDefault e))))

;; guard against double install (ui-runtime.js + a side-effect require)
(when-not (aget js/window "__uiLightboxZoom")
  (aset js/window "__uiLightboxZoom" true)
  (.addEventListener js/document "pointerdown" on-pointerdown true)
  (.addEventListener js/document "pointermove" on-pointermove true)
  (.addEventListener js/document "pointerup" on-pointer-end true)
  (.addEventListener js/document "pointercancel" on-pointer-end true)
  (.addEventListener js/document "click" on-click-capture true)
  (.addEventListener js/document "wheel" on-wheel {:capture true :passive false})
  ;; load doesn't bubble — catch it in the capture phase
  (.addEventListener js/document "load" on-load-capture true)
  (.addEventListener js/document "dragstart" on-dragstart true))
