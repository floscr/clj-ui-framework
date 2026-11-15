(ns lightbox
  "Runtime for ui.lightbox: everything about the viewer that moves.

   The viewer (`.ui-lightbox[data-lightbox]`, rendered by ui.lightbox):
   - open / close: notices a lightbox mounting (or unhiding) through the
     CSS `ui-lightbox-mount` animation, so it needs no document-wide
     MutationObserver. Closing animates first, then hides a managed
     lightbox or dispatches a non-bubbling `close` event for the host to
     unmount it. Index changes dispatch `change` (detail.index).
   - morph: FLIP from the thumbnail (`[data-lightbox-key]`) into the slide
     and back, with clip-path un-cropping an `object-fit: cover` tile and
     the corner radius carried over; closes from the current zoom / pull
     pose; fades when the thumbnail is gone or off screen. No View
     Transitions: they need a list of per-browser workarounds (Android
     URL-bar aborts, WebKit named groups, Firefox full-bleed capture).
   - gallery: native scroll-snap slides. A swipe in flight locks keyboard
     and button navigation (a programmatic scroll can't cancel a
     compositor fling); `scrollend` with a quiet-gap fallback (Safari
     < 17.4 has none, Firefox drops it after snapping, iOS after a rubber
     band). Jumps go to the exact offset with snapping briefly off.
   - images: the runtime sets every src (the markup has none, so a VDOM
     re-render never resets it): the thumbnail first, sized to the fit
     box, then the full image once decoded; only the shown slide and its
     neighbours load.
   - zoom: pinch (focal, rubber band past the bounds, a two-finger pan
     doesn't zoom until the fingers really pinch), one-finger / mouse pan
     with momentum, double-tap / double-click toggle, ctrl / pinch wheel
     to the cursor, Safari trackpad pinch through GestureEvent, wheel pans
     when zoomed, +/−/0 keys and buttons. Measured against the slide, not
     the window, and capped at the image's own resolution.
   - pull to dismiss: drag down or up (touch, pen, mouse) or a vertical
     trackpad flick at 1×; --lightbox-pull-progress on the root.
   - keyboard: Escape, ←/→ (pan when zoomed), Home / End, + = − _ 0.
   - history: data-history pushes an entry on open; Back closes.
   - page: html[data-lightbox-open] locks the scroll behind.

   Legacy overlays (`.lightbox-overlay` around a `.lightbox-image`, hand-
   built in apps not yet on ui.lightbox) keep their old zoom: pinch, pan,
   double-tap and a bare wheel zoom, CSS-transition snap-back, reset on
   image load, plus the page scroll lock.

   Wheel listeners only exist while something is open, passive on WebKit:
   a non-passive wheel listener above a scroller drops Safari's momentum
   scrolling. Pair with ui/lightbox.css.

   Loaded via ui-runtime.js (hiccup apps) or a side-effect require of
   [ui.js.lightbox] (squint / replicant SPAs).")

;; ── Constants ───────────────────────────────────────────────────────

(def ^:private root-sel ".ui-lightbox[data-lightbox]")
(def ^:private legacy-sel ".lightbox-overlay")
(def ^:private legacy-image-sel ".lightbox-image")
;; presses on these never start a zoom, pan, pull or tap
(def ^:private no-gesture-sel
  (str "button, a, input, textarea, select, label, video, audio, [contenteditable],"
       " .ui-lightbox-content, .ui-lightbox-top, .ui-lightbox-footer,"
       " .ui-lightbox-thumbs, .ui-lightbox-caption, .ui-lightbox-error,"
       " .video-player, [data-lightbox-no-gesture], .lightbox-close, .lightbox-caption"))

(def ^:private ease-out "cubic-bezier(0.32, 0.72, 0, 1)")
(def ^:private ease-coast "cubic-bezier(0.2, 0.8, 0.2, 1)")
(def ^:private morph-ms 340)
(def ^:private fade-ms 220)
(def ^:private settle-ms 260)
(def ^:private coast-ms 480)

(def ^:private legacy-max-scale 5)
(def ^:private double-tap-scale 2.5)
(def ^:private double-tap-ms 300)
(def ^:private tap-slop-px 10)
(def ^:private double-tap-slop-px 30)
(def ^:private axis-lock-px 8)
;; a two-finger slide only starts zooming once the spread changes this much
(def ^:private pinch-slop-px 14)
(def ^:private scroll-idle-ms 140)
(def ^:private wheel-idle-ms 160)
(def ^:private open-wait-ms 160)
(def ^:private identity-zoom {:s 1 :tx 0 :ty 0})

(def ^:private webkit?
  ;; GestureEvent has only ever shipped in WebKit (Safari, every iOS browser)
  (some? (aget js/window "GestureEvent")))

(def ^:private touch-screen?
  (pos? (or (.-maxTouchPoints js/navigator) 0)))

;; ── Small helpers ───────────────────────────────────────────────────

(defn- clamp [lo hi v]
  (js/Math.min hi (js/Math.max lo v)))

(defn- now [] (js/performance.now))

(defn- reduced-motion? []
  (.-matches (js/matchMedia "(prefers-reduced-motion: reduce)")))

(defn- closest [t sel]
  (when (and t (.-closest t)) (.closest t sel)))

(defn- state [root] (when root (aget root "__lb")))

(defn- open-root-of [t]
  (let [root (closest t root-sel)]
    (when (state root) root)))

(defn- px [v] (str v "px"))

(defn- pose-css [{:keys [s tx ty]}]
  (str "translate(" tx "px, " ty "px) scale(" s ")"))

(defn- parse-matrix
  "A computed transform (matrix(a, b, c, d, e, f)) as a pose; our poses
   only ever translate and scale uniformly."
  [m]
  (if (or (not m) (= m "none"))
    identity-zoom
    (let [nums (.map (.split (.replace m #"^matrix(3d)?\(|\)$" "") ",") js/parseFloat)]
      (if (= 16 (.-length nums))
        {:s (aget nums 0) :tx (aget nums 12) :ty (aget nums 13)}
        {:s (aget nums 0) :tx (aget nums 4) :ty (aget nums 5)}))))

(defn- radius-px
  "The first corner of a computed border-radius in px (a % resolves
   against the smaller side)."
  [el w h]
  (let [r (or (.-borderTopLeftRadius (js/getComputedStyle el)) "0")
        v (js/parseFloat r)]
    (cond
      (js/isNaN v)         0
      (.endsWith r "%")    (* (/ v 100) (js/Math.min w h))
      :else                v)))

(defn- vibrate! [ms]
  (when (.-vibrate js/navigator)
    (try (.vibrate js/navigator ms) (catch :default _ nil))))

(defn- dispatch! [root type detail]
  (.dispatchEvent root (js/CustomEvent. type {:bubbles false :detail detail})))

;; ── Open surfaces: scroll lock and wheel listeners ─────────────────
;; Everything here only exists while a viewer or legacy overlay is open.

(def ^:private surfaces (js/Set.))
(def ^:private wheel-installed (atom nil))
(def ^:private disconnect-mo (atom nil))

(declare on-wheel on-gesture-start on-gesture-change on-gesture-end
         surface-removed! open!)

(defn- lock-page! []
  (let [de js/document.documentElement]
    (when-not (.hasAttribute de "data-lightbox-open")
      (.setProperty (.-style de) "--lightbox-scrollbar"
                    (px (js/Math.max 0 (- js/window.innerWidth (.-clientWidth de)))))
      (.setAttribute de "data-lightbox-open" ""))))

(defn- unlock-page! []
  (let [de js/document.documentElement]
    (.removeAttribute de "data-lightbox-open")
    (.removeProperty (.-style de) "--lightbox-scrollbar")))

(defn- page-surface?
  "Surfaces that lock the page: legacy overlays and non-contained viewers."
  [el]
  (not (and (.matches el root-sel) (.hasAttribute el "data-contained"))))

(defn- sync-surfaces! []
  ;; page lock
  (if (some page-surface? (js/Array.from surfaces))
    (lock-page!)
    (unlock-page!))
  ;; wheel + Safari gesture listeners
  (let [want (pos? (.-size surfaces))]
    (when (and want (not @wheel-installed))
      (let [opts {:capture true :passive webkit?}]
        (.addEventListener js/document "wheel" on-wheel opts)
        (when (and webkit? (not touch-screen?))
          (.addEventListener js/document "gesturestart" on-gesture-start true)
          (.addEventListener js/document "gesturechange" on-gesture-change true)
          (.addEventListener js/document "gestureend" on-gesture-end true))
        (reset! wheel-installed opts)))
    (when (and (not want) @wheel-installed)
      (.removeEventListener js/document "wheel" on-wheel @wheel-installed)
      (.removeEventListener js/document "gesturestart" on-gesture-start true)
      (.removeEventListener js/document "gesturechange" on-gesture-change true)
      (.removeEventListener js/document "gestureend" on-gesture-end true)
      (reset! wheel-installed nil)))
  ;; notice surfaces the host unmounts without closing them
  (let [want (pos? (.-size surfaces))]
    (when (and want (not @disconnect-mo))
      (let [mo (js/MutationObserver.
                (fn [_]
                  (let [gone (filter #(not (.-isConnected %)) (js/Array.from surfaces))]
                    (when (seq gone)
                      (doseq [el gone] (surface-removed! el))
                      ;; a host that re-created the viewer: take the new node
                      ;; over before it paints (its mount animation would only
                      ;; start a frame later)
                      (doseq [r (js/Array.from (.querySelectorAll js/document (str root-sel ":not([hidden])")))]
                        (when-not (state r) (open! r)))))))]
        (.observe mo js/document.body {:childList true :subtree true})
        (reset! disconnect-mo mo)))
    (when (and (not want) @disconnect-mo)
      (.disconnect @disconnect-mo)
      (reset! disconnect-mo nil))))

(defn- add-surface! [el]
  (.add surfaces el)
  (sync-surfaces!))

(defn- remove-surface! [el]
  (.delete surfaces el)
  (sync-surfaces!))

;; ── Slides ──────────────────────────────────────────────────────────

(defn- scroller [root] (.querySelector root ".ui-lightbox-slides"))

(defn- slide-els [root]
  (js/Array.from (.querySelectorAll root ".ui-lightbox-slide")))

(defn- slide-at [root i]
  (aget (slide-els root) i))

(defn- media-of [slide]
  (when slide (.querySelector slide ".ui-lightbox-media")))

(defn- image? [media]
  (and media (= "IMG" (.-tagName media))))

(defn- player-of
  "The ui.video-player root inside a video slide's media box."
  [media]
  (when (and media (.contains (.-classList media) "ui-lightbox-player"))
    (.querySelector media "[data-video-player]")))

(defn- controller
  "The video-player runtime's controller for a media box, once it exists."
  [media]
  (when-let [p (player-of media)]
    (when-let [f (aget js/window "__uiVideoPlayer")]
      (f p))))

(defn- inner-video [media]
  (cond
    (nil? media) nil
    (= "VIDEO" (.-tagName media)) media
    :else (.querySelector media "video")))

(defn- active-slide [root]
  (slide-at root (.-index (state root))))

(defn- slide-width [root]
  (js/Math.max 1 (.-clientWidth (scroller root))))

;; ── Image loading and the fit box ──────────────────────────────────
;; The img element box is always the fit box (contain inside the
;; slide's content box), set in px from the best aspect known: the
;; :width / :height hint, else the loaded image, else the thumbnail. The
;; morph, zoom bounds and the thumbnail-to-full swap all rely on that.

(defn- content-box [slide]
  (let [cs (js/getComputedStyle slide)]
    {:w (- (.-clientWidth slide) (js/parseFloat (.-paddingLeft cs)) (js/parseFloat (.-paddingRight cs)))
     :h (- (.-clientHeight slide) (js/parseFloat (.-paddingTop cs)) (js/parseFloat (.-paddingBottom cs)))}))

(defn- natural-size
  "[w h cap?] of a media element: the width/height hint (also a size
   cap), else the full image's natural size once loaded (a cap), else
   the thumbnail's (no cap: the full image is bigger)."
  [m]
  (let [hw (js/parseFloat (or (.getAttribute m "width") (.getAttribute m "data-width")))
        hh (js/parseFloat (or (.getAttribute m "height") (.getAttribute m "data-height")))
        v (when-not (image? m) (inner-video m))]
    (cond
      (and (pos? hw) (pos? hh)) [hw hh true]
      (image? m)
      (when (pos? (.-naturalWidth m))
        [(.-naturalWidth m) (.-naturalHeight m) (= "full" (aget m "__lbShown"))])
      (and v (pos? (.-videoWidth v)))
      [(.-videoWidth v) (.-videoHeight v) false]
      ;; a player before its metadata: 16:9 until the video says otherwise
      (player-of m) [1600 900 false]
      :else nil)))

(defn- fit! [slide]
  (let [m (media-of slide)]
    (when m
      (when-let [[nw nh cap?] (natural-size m)]
        (let [{:keys [w h]} (content-box slide)
              k (js/Math.min (/ w nw) (/ h nh) (if cap? 1 js/Infinity))]
          (when (and (pos? w) (pos? h))
            (set! (.. m -style -width) (px (js/Math.round (* nw k))))
            (set! (.. m -style -height) (px (js/Math.round (* nh k))))))))))

(defn- set-slide-state! [slide v]
  (if v
    (.setAttribute slide "data-state" v)
    (.removeAttribute slide "data-state")))

(defn- show-src! [slide img src which]
  (aset img "__lbShown" which)
  (set! (.-src img) src))

(defn- load-full! [slide img]
  (let [full (.getAttribute img "data-src")
        pre  (js/Image.)]
    (set! (.-src pre) full)
    (-> (.decode pre)
        (.then (fn []
                 (show-src! slide img full "full")
                 (set-slide-state! slide nil)
                 (fit! slide)))
        (.catch (fn [_]
                  ;; decode() also rejects for formats it can't decode
                  ;; off-thread; try the element itself before giving up
                  (show-src! slide img full "full")
                  (set! (.-onload img) (fn [] (set-slide-state! slide nil) (fit! slide)))
                  (set! (.-onerror img) (fn [] (set-slide-state! slide "error"))))))))

(defn- load-slide! [slide]
  (when-let [m (media-of slide)]
    (when-not (aget m "__lbLoading")
      (aset m "__lbLoading" true)
      (cond
        (image? m)
        (let [thumb (.getAttribute m "data-thumb")
              full  (.getAttribute m "data-src")]
          (if (and thumb (not= thumb full))
            (do (set-slide-state! slide "loading")
                (set! (.-onload m) (fn []
                                     (set! (.-onload m) nil)
                                     ;; the thumbnail shows: no spinner over it
                                     (when (= "thumb" (aget m "__lbShown"))
                                       (set-slide-state! slide "preview"))
                                     (fit! slide)))
                (show-src! slide m thumb "thumb")
                (load-full! slide m))
            (do (set-slide-state! slide "loading")
                (set! (.-onload m) (fn [] (set-slide-state! slide nil) (fit! slide)))
                (set! (.-onerror m) (fn [] (set-slide-state! slide "error")))
                (show-src! slide m full "full")))
          (fit! slide))

        (player-of m)
        ;; the source itself waits for the slide to be shown (sync-videos!)
        (do (when-let [v (inner-video m)]
              (.addEventListener v "loadedmetadata" (fn [_] (fit! slide))))
            (fit! slide))

        (= "VIDEO" (.-tagName m))
        (do (set! (.-onloadedmetadata m) (fn [] (fit! slide)))
            (set! (.-preload m) "metadata")
            (set! (.-src m) (.getAttribute m "data-src"))
            (fit! slide))))))

(defn- load-near! [root]
  (let [i (.-index (state root))]
    (doseq [j [i (inc i) (dec i)]]
      (when-let [s (slide-at root j)] (load-slide! s)))))

(defn- fit-all! [root]
  (doseq [s (slide-els root)]
    (when-let [m (media-of s)]
      (when (aget m "__lbLoading") (fit! s)))))

;; ── Zoom engine ─────────────────────────────────────────────────────
;; A zoom context: {:legacy? :root :surface :stage :media}. The pose
;; lives on the media element (__uiZoom) and as its inline transform.

(declare update-zoom-chrome!)

(defn- zoom-of [m] (or (aget m "__uiZoom") identity-zoom))

(defn- zoomed? [m] (and m (> (:s (zoom-of m)) 1.001)))

(defn- viewport-rect [{:keys [legacy? stage]}]
  (if legacy?
    {:left 0 :top 0 :right js/window.innerWidth :bottom js/window.innerHeight}
    (let [r (.getBoundingClientRect stage)]
      {:left (.-left r) :top (.-top r) :right (.-right r) :bottom (.-bottom r)})))

(defn- layout-center
  "The media's untransformed center in viewport coords: scaling about
   the center keeps it, so only the translation needs taking back out."
  [m]
  (let [r (.getBoundingClientRect m)
        z (zoom-of m)]
    [(- (+ (.-left r) (/ (.-width r) 2)) (:tx z))
     (- (+ (.-top r) (/ (.-height r) 2)) (:ty z))]))

(defn- max-scale [{:keys [legacy? root media]}]
  (if legacy?
    legacy-max-scale
    (let [attr (js/parseFloat (.getAttribute root "data-max-zoom"))]
      (if (pos? attr)
        attr
        (let [w (.-offsetWidth media)
              nat (or (first (natural-size media)) w)]
          (clamp 2 8 (if (pos? w) (/ nat w) 2)))))))

(defn- zoom-at
  "Scale z to s while keeping the point px/py (relative to the layout
   center) where it is."
  [z s px py]
  (let [k (/ s (:s z))]
    (assoc z
           :s s
           :tx (- px (* k (- px (:tx z))))
           :ty (- py (* k (- py (:ty z)))))))

(defn- axis-range
  "[lo hi] translation on one axis: with the scaled media overflowing the
   viewport it may pan until an edge is flush; while it fits it stays
   inside. One formula, so the range is continuous through the fit point."
  [vp-lo vp-hi c size s]
  (let [a (+ (- vp-lo c) (/ (* size s) 2))
        b (- (- vp-hi c) (/ (* size s) 2))]
    [(js/Math.min a b) (js/Math.max a b) (<= a b)]))

(defn- settle
  "Snap z into bounds: scale within [1, max]; an axis that fits goes back
   home (as centered as the viewport allows), one that overflows pans no
   further than its edges."
  [ctx z]
  (let [m (:media ctx)
        s (clamp 1 (max-scale ctx) (:s z))
        vp (viewport-rect ctx)
        [cx cy] (layout-center m)
        [xlo xhi xfit] (axis-range (:left vp) (:right vp) cx (.-offsetWidth m) s)
        [ylo yhi yfit] (axis-range (:top vp) (:bottom vp) cy (.-offsetHeight m) s)]
    (if (<= s 1.001)
      identity-zoom
      {:s s
       :tx (clamp xlo xhi (if xfit 0 (:tx z)))
       :ty (clamp ylo yhi (if yfit 0 (:ty z)))})))

(defn- rubber
  "iOS-style resistance: how far content moves when dragged x past its
   bound, for a viewport dimension d."
  [x d]
  (* (- 1 (/ 1 (+ 1 (/ (* x 0.55) (js/Math.max 1 d))))) d))

(defn- elastic-pan
  "Translation t on an axis, rubber-banded past its range."
  [t [lo hi fit?] d]
  (let [[lo hi] (if fit? [(clamp lo hi 0) (clamp lo hi 0)] [lo hi])]
    (cond
      (< t lo) (- lo (rubber (- lo t) d))
      (> t hi) (+ hi (rubber (- t hi) d))
      :else t)))

(defn- elastic-scale [s mx]
  (cond
    (> s mx) (* mx (js/Math.pow (/ s mx) 0.3))
    (< s 1)  (js/Math.pow s 0.3)
    :else    s))

(defn- cancel-anim! [m]
  (when-let [a (aget m "__lbAnim")]
    (aset m "__lbAnim" nil)
    (.cancel a)))

(defn- live-pose!
  "The pose a media element shows right now; a running animation is
   stopped where it is."
  [m]
  (if-let [a (aget m "__lbAnim")]
    (let [z (parse-matrix (.-transform (js/getComputedStyle m)))]
      (aset m "__lbAnim" nil)
      (.cancel a)
      (aset m "__uiZoom" z)
      (set! (.. m -style -transform) (pose-css z))
      z)
    (zoom-of m)))

(defn- set-zoom-attrs! [ctx z]
  (let [m (:media ctx)
        on? (> (:s z) 1.001)]
    (if on? (.setAttribute m "data-zoomed" "") (.removeAttribute m "data-zoomed"))
    (when-not (:legacy? ctx)
      (if on?
        (.setAttribute (:root ctx) "data-zoomed" "")
        (.removeAttribute (:root ctx) "data-zoomed"))
      (update-zoom-chrome! (:root ctx)))))

(defn- apply-zoom!
  "Store z on the media and render it. opts :ms animates there (WAAPI on
   viewers, the CSS transition on legacy overlays), with :easing."
  [ctx z opts]
  (let [m (:media ctx)
        ms (when (and opts (:ms opts) (not (reduced-motion?))) (:ms opts))
        from (zoom-of m)]
    (if (:legacy? ctx)
      (let [st (.-style m)]
        (set! (.-transition st) (if ms "" "none"))
        (set! (.-transform st) (if (= 1 (:s z)) "" (pose-css z))))
      (do (cancel-anim! m)
          (set! (.. m -style -transform) (if (and (= 1 (:s z)) (= 0 (:tx z)) (= 0 (:ty z)))
                                           "" (pose-css z)))
          (when ms
            (aset m "__lbAnim"
                  (.animate m [{:transform (pose-css from)} {:transform (pose-css z)}]
                            {:duration ms :easing (or (:easing opts) ease-out)})))))
    (aset m "__uiZoom" z)
    (set-zoom-attrs! ctx z)))

(defn- reset-zoom! [ctx]
  (let [m (:media ctx)]
    (when m
      (when-not (:legacy? ctx) (cancel-anim! m))
      (aset m "__uiZoom" nil)
      (set! (.. m -style -transform) "")
      (set-zoom-attrs! ctx identity-zoom))))

(defn- viewer-ctx
  "Zoom context of a viewer's active slide (images only)."
  [root]
  (let [slide (active-slide root)
        m (media-of slide)]
    (when (image? m)
      {:legacy? false :root root :surface root :stage slide :media m})))

(defn- ctx-at
  "Zoom context for an event target, or nil."
  [t]
  (if-let [root (open-root-of t)]
    (viewer-ctx root)
    (when-let [overlay (closest t legacy-sel)]
      (when-let [m (.querySelector overlay legacy-image-sel)]
        {:legacy? true :root nil :surface overlay :stage overlay :media m}))))

(defn- zoom-by!
  "Zoom the active image by factor k about its center (buttons, keys);
   k nil resets."
  [root k]
  (when-let [ctx (viewer-ctx root)]
    (let [m (:media ctx)
          z (live-pose! m)
          target (if k
                   (settle ctx (zoom-at z (clamp 1 (max-scale ctx) (* (:s z) k)) 0 0))
                   identity-zoom)]
      (apply-zoom! ctx target {:ms settle-ms}))))

(defn- pan-by! [root dx dy]
  (when-let [ctx (viewer-ctx root)]
    (let [z (live-pose! (:media ctx))]
      (apply-zoom! ctx (settle ctx (assoc z :tx (+ (:tx z) dx) :ty (+ (:ty z) dy)))
                   {:ms fade-ms}))))

(defn- update-zoom-chrome! [root]
  (let [ctx (viewer-ctx root)
        m (:media ctx)
        z (if m (zoom-of m) identity-zoom)
        mx (if ctx (max-scale ctx) 1)]
    (when-let [b (.querySelector root "[data-lightbox-action='zoom-out']")]
      (set! (.-disabled b) (<= (:s z) 1.001)))
    (when-let [b (.querySelector root "[data-lightbox-action='zoom-in']")]
      (set! (.-disabled b) (>= (:s z) (- mx 0.001))))))

;; ── Gallery: index, chrome, navigation ─────────────────────────────

(declare hide-source! request-close!)

(defn- start-player!
  "Hand a shown video slide's player its source, then autoplay it when
   the item asks."
  [m]
  (let [p (player-of m)
        v (inner-video m)
        autoplay? (.hasAttribute m "data-autoplay")
        first-show? (and p (= "" (or (.getAttribute p "data-src") "")))]
    (when v
      (when (.hasAttribute m "data-loop") (set! (.-loop v) true))
      ;; iOS only autoplays a video that is muted, as a property, when
      ;; play() is called
      (when (.hasAttribute m "data-muted") (set! (.-muted v) true)))
    (if first-show?
      (do
        ;; the player autoplays once its source has loaded (and falls back
        ;; to muted when the browser refuses sound); play() now would run
        ;; before there is anything to play
        (when autoplay? (.setAttribute p "data-autoplay" "true"))
        ;; the video-player runtime loads on this attribute change
        (.setAttribute p "data-src" (.getAttribute p "data-lightbox-src")))
      (when (and autoplay? v)
        (if-let [c (controller m)]
          ((.-play c))
          (.catch (.play v) (fn [_] nil)))))))

(defn- pause-media! [m]
  (if-let [c (controller m)]
    ((.-pause c))
    (when-let [v (inner-video m)]
      (when-not (.-paused v) (.pause v)))))

(defn- sync-videos!
  "Only the shown slide's video plays."
  [root]
  (let [st (state root)
        i (.-index st)]
    (doseq [[j s] (map-indexed vector (slide-els root))]
      (let [m (media-of s)]
        (when (and m (inner-video m))
          (if (= i j)
            (when-not (.hasAttribute root "data-opening")
              (if (player-of m)
                (start-player! m)
                (when (.hasAttribute m "data-autoplay")
                  (when (.hasAttribute m "data-muted") (set! (.-muted m) true))
                  (.catch (.play m) (fn [_] nil)))))
            (pause-media! m)))))))

(defn- center-thumb! [root smooth?]
  (let [strip (.querySelector root ".ui-lightbox-thumbs")
        i (.-index (state root))]
    (when strip
      (when-let [b (.querySelector strip (str "[data-index='" i "']"))]
        (.scrollTo strip {:left (- (+ (.-offsetLeft b) (/ (.-offsetWidth b) 2))
                                   (/ (.-clientWidth strip) 2))
                          :behavior (if (and smooth? (not (reduced-motion?))) "smooth" "instant")})))))

(defn- sync-chrome! [root]
  (let [st (state root)
        i (.-index st)
        n (.-length (slide-els root))
        slide (slide-at root i)]
    (when-let [c (.querySelector root ".ui-lightbox-counter")]
      (set! (.-textContent c) (str (inc i) " / " n)))
    (when-let [b (.querySelector root "[data-lightbox-action='prev']")]
      (set! (.-disabled b) (<= i 0)))
    (when-let [b (.querySelector root "[data-lightbox-action='next']")]
      (set! (.-disabled b) (>= i (dec n))))
    (doseq [b (js/Array.from (.querySelectorAll root ".ui-lightbox-thumb-button"))]
      (if (= (str i) (.getAttribute b "data-index"))
        (.setAttribute b "aria-current" "true")
        (.removeAttribute b "aria-current")))
    (.setAttribute root "data-active-type" (or (some-> slide (.getAttribute "data-type")) "image"))
    (update-zoom-chrome! root)))

(defn- set-index!
  "The shown slide changed to i (a swipe, a key, a button, the host)."
  [root i]
  (let [st (state root)
        prev (.-index st)]
    (when (not= i prev)
      ;; leaving a slide drops its zoom
      (when-let [m (media-of (slide-at root prev))]
        (when (image? m)
          (reset-zoom! {:legacy? false :root root :media m})))
      (set! (.-index st) i)
      (sync-chrome! root)
      (load-near! root)
      (sync-videos! root)
      (center-thumb! root true)
      (hide-source! root)
      (dispatch! root "change" {:index i}))))

(defn- index-from-scroll [root]
  (let [sc (scroller root)]
    (clamp 0 (dec (.-length (slide-els root)))
           (js/Math.round (/ (js/Math.abs (.-scrollLeft sc)) (slide-width root))))))

(defn- mid-swipe?
  "The strip is scrolling, or resting off a snap point (a fling just
   started, before its first scroll event)."
  [root]
  (let [st (state root)
        sc (scroller root)
        w (slide-width root)
        off (js/Math.abs (- (.-scrollLeft sc) (* w (js/Math.round (/ (.-scrollLeft sc) w)))))]
    (or (.-scrolling st) (> off 2))))

(defn- settle-scroll! [root]
  (let [st (state root)]
    (when st
      (js/clearTimeout (.-scrollTimer st))
      (set! (.-scrolling st) false)
      (when-not (.-resizing st)
        (set-index! root (index-from-scroll root))))))

(defn- on-slides-scroll [root]
  (let [st (state root)]
    (when (and st (not (.-resizing st)) (not (.-jumping st)))
      (set! (.-scrolling st) true)
      (let [i (index-from-scroll root)]
        (when (not= i (.-index st)) (set-index! root i)))
      (js/clearTimeout (.-scrollTimer st))
      (set! (.-scrollTimer st)
            (js/setTimeout #(settle-scroll! root) scroll-idle-ms)))))

(defn- repaint-slide!
  "WebKit can leave a half-decoded image after a long jump; a synchronous
   visibility toggle with a reflow between re-rasterizes it, and no frame
   ever paints it hidden."
  [slide]
  (when (and webkit? slide)
    (let [st (.-style slide)]
      (set! (.-visibility st) "hidden")
      (.-offsetWidth slide)
      (set! (.-visibility st) ""))))

(defn- jump-to!
  "Put slide i in place at once: snapping off for the write (else
   scroll-snap-stop can catch it), back on the next frame."
  [root i]
  (let [st (state root)
        sc (scroller root)]
    (set! (.-jumping st) true)
    (set! (.. sc -style -scrollSnapType) "none")
    (set! (.-scrollLeft sc) (* i (slide-width root)))
    (js/requestAnimationFrame
     (fn []
       (set! (.. sc -style -scrollSnapType) "")
       (set! (.-scrollLeft sc) (* i (slide-width root)))
       (set! (.-jumping st) false)
       (js/clearTimeout (.-repaintTimer st))
       (set! (.-repaintTimer st)
             (js/setTimeout #(repaint-slide! (slide-at root i)) 120))))))

(defn- go-to!
  "Navigate to slide i (keys, buttons, thumbnails, the host). Dropped
   while a swipe is in flight: its fling can't be cancelled from JS."
  [root i]
  (let [st (state root)
        n (.-length (slide-els root))
        i (clamp 0 (dec n) i)]
    (when (and st (not (.-scrolling st)) (not= i (.-index st)))
      ;; an instant jump, then the new photo slides in from its side: a
      ;; smooth scroll across a wide screen takes most of a second, and
      ;; would hold every key press after it
      (let [dir (if (> i (.-index st)) 1 -1)]
        (jump-to! root i)
        (set-index! root i)
        (when-not (reduced-motion?)
          (let [slide (slide-at root i)
                target (or (media-of slide) (.querySelector slide ".ui-lightbox-content"))]
            (when target
              (.animate target [{:transform (str "translateX(" (* dir 6) "%)") :opacity 0}
                                {:transform "none" :opacity 1}]
                        {:duration fade-ms :easing ease-out}))))))))

(defn- on-resize [root]
  (let [st (state root)]
    (when st
      (set! (.-resizing st) true)
      (fit-all! root)
      (jump-to! root (.-index st))
      (when-let [ctx (viewer-ctx root)]
        (apply-zoom! ctx (settle ctx (zoom-of (:media ctx))) nil))
      (center-thumb! root false)
      (js/requestAnimationFrame
       (fn [] (js/requestAnimationFrame (fn [] (set! (.-resizing st) false))))))))

;; ── Morph ───────────────────────────────────────────────────────────

(def ^:private last-trigger (atom nil))

(defn- visible-rect
  "The element's rect when it is connected, laid out and at least partly
   on screen."
  [el]
  (when (and el (.-isConnected el))
    (let [r (.getBoundingClientRect el)]
      (when (and (pos? (.-width r)) (pos? (.-height r))
                 (< (.-top r) js/window.innerHeight) (> (.-bottom r) 0)
                 (< (.-left r) js/window.innerWidth) (> (.-right r) 0))
        r))))

(defn- source-el
  "The thumbnail standing for slide i: the one just clicked when it
   matches (the same src can show twice on a page), else the first with
   the slide's key."
  [root i]
  (let [slide (slide-at root i)
        k (some-> slide (.getAttribute "data-key"))
        {:keys [el t]} @last-trigger]
    (when (and k (not= "" k))
      (if (and el (.-isConnected el) (< (- (now) t) 2000)
               (= k (.getAttribute el "data-lightbox-key")))
        el
        (some (fn [c] (when (and (= k (.getAttribute c "data-lightbox-key"))
                                 (not (.contains root c)))
                        c))
              (js/Array.from (.querySelectorAll js/document "[data-lightbox-key]")))))))

(defn- source-for [root i]
  (let [el (source-el root i)]
    (when (and el (= (.getAttribute el "data-lightbox-key")
                     (.getAttribute (slide-at root i) "data-key")))
      el)))

(defn- hide-source!
  "Only the current slide's thumbnail is \"inside\" the viewer."
  [root]
  (let [st (state root)]
    (when-let [old (.-source st)] (.removeAttribute old "data-lightbox-source"))
    (set! (.-source st) nil)
    (when (and (not (.-closed st)) (not (.hasAttribute root "data-contained")))
      (when-let [el (source-for root (.-index st))]
        (.setAttribute el "data-lightbox-source" "")
        (set! (.-source st) el)))))

(defn- thumb-pose
  "Keyframe values that make the media (layout box d, untransformed)
   look like the thumbnail at rect s: scaled to cover it, centered on it,
   and clipped to its box with its corner radius."
  [d s src-el]
  (let [cover? (not= "contain" (.-objectFit (js/getComputedStyle src-el)))
        k (if cover?
            (js/Math.max (/ (.-width s) (.-width d)) (/ (.-height s) (.-height d)))
            (js/Math.min (/ (.-width s) (.-width d)) (/ (.-height s) (.-height d))))
        tx (- (+ (.-left s) (/ (.-width s) 2)) (+ (.-left d) (/ (.-width d) 2)))
        ty (- (+ (.-top s) (/ (.-height s) 2)) (+ (.-top d) (/ (.-height d) 2)))
        ix (js/Math.max 0 (/ (- (.-width d) (/ (.-width s) k)) 2))
        iy (js/Math.max 0 (/ (- (.-height d) (/ (.-height s) k)) 2))
        r (/ (radius-px src-el (.-width s) (.-height s)) k)]
    {:transform (pose-css {:s k :tx tx :ty ty})
     :clipPath (str "inset(" iy "px " ix "px round " r "px)")}))

(defn- untransformed-rect [m]
  (let [st (.-style m)
        t (.-transform st)]
    (set! (.-transform st) "none")
    (let [r (.getBoundingClientRect m)]
      (set! (.-transform st) t)
      r)))

(defn- chrome-els [root]
  (js/Array.from (.querySelectorAll root ".ui-lightbox-chrome")))

(defn- play-open! [root]
  (let [st (state root)
        i (.-index st)
        slide (slide-at root i)
        m (media-of slide)
        backdrop (.querySelector root ".ui-lightbox-backdrop")
        contained? (.hasAttribute root "data-contained")
        src-el (when-not contained? (source-for root i))
        s (visible-rect src-el)
        d (when m (.getBoundingClientRect m))
        done (fn []
               (.removeAttribute root "data-opening")
               ;; a video starts once it has landed
               (sync-videos! root))]
    (.setAttribute root "data-ready" "")
    (cond
      (or contained? (reduced-motion?))
      (done)

      (and s d (pos? (.-width d)))
      (let [dr (radius-px m (.-width d) (.-height d))]
        (.animate m [(thumb-pose d s src-el)
                     {:transform (pose-css identity-zoom)
                      :clipPath (str "inset(0px 0px round " dr "px)")}]
                  {:duration morph-ms :easing ease-out})
        (.animate backdrop [{:opacity 0} {:opacity 1}] {:duration morph-ms :easing ease-out})
        (js/setTimeout done (* 0.6 morph-ms)))

      :else
      (let [target (or m slide)]
        (.animate target [{:opacity 0 :transform "scale(0.96)"} {:opacity 1 :transform "none"}]
                  {:duration fade-ms :easing ease-out})
        (.animate backdrop [{:opacity 0} {:opacity 1}] {:duration fade-ms :easing ease-out})
        (js/setTimeout done (* 0.5 fade-ms))))
    (hide-source! root)))

(defn- wait-for-media
  "Resolve once the active media has a box to morph into (the thumbnail
   or the full image decoded), at most open-wait-ms."
  [root]
  (js/Promise.
   (fn [resolve]
     (let [m (media-of (active-slide root))
           t0 (now)]
       (if (or (not (image? m)) (.hasAttribute m "width"))
         (resolve nil)
         (let [check (fn check []
                       (if (or (and (.-complete m) (pos? (.-naturalWidth m)))
                               (> (- (now) t0) open-wait-ms))
                         (do (fit! (active-slide root)) (resolve nil))
                         (js/requestAnimationFrame check)))]
           (check)))))))

(defn- play-close!
  "Animate out, then call done: back into the thumbnail from wherever
   the media is now (zoomed, pulled), or a fade."
  [root done]
  (let [st (state root)
        i (.-index st)
        slide (slide-at root i)
        m (media-of slide)
        backdrop (.querySelector root ".ui-lightbox-backdrop")
        contained? (.hasAttribute root "data-contained")
        src-el (when-not contained? (source-for root i))
        _ (when (and src-el (not (visible-rect src-el)) (.-isConnected src-el))
            ;; keep the close morph on screen: bring the thumbnail into view
            ;; (behind the overlay, so the jump is never seen)
            (.scrollIntoView src-el {:block "nearest" :inline "nearest"}))
        s (visible-rect src-el)
        media? (and m (not= "error" (.getAttribute slide "data-state")))
        from-t (when media?
                 (if (aget m "__lbAnim")
                   (do (live-pose! m) (.-transform (.-style m)))
                   (let [t (.. m -style -transform)] (if (= "" t) (pose-css identity-zoom) t))))
        from-o (js/parseFloat (.-opacity (js/getComputedStyle backdrop)))
        finish (let [called (atom false)]
                 (fn [] (when-not @called (reset! called true) (done))))]
    (.setAttribute root "data-closing" "")
    (when media? (cancel-anim! m))
    (cond
      (or contained? (reduced-motion?))
      (finish)

      (and s media?)
      (let [d (untransformed-rect m)
            dr (radius-px m (.-width d) (.-height d))
            to (thumb-pose d s src-el)
            a (.animate m [{:transform from-t :clipPath (str "inset(0px 0px round " dr "px)")} to]
                        {:duration morph-ms :easing ease-out :fill "forwards"})]
        (.animate backdrop [{:opacity from-o} {:opacity 0}]
                  {:duration morph-ms :easing ease-out :fill "forwards"})
        (.then (.-finished a) finish finish)
        (js/setTimeout finish (+ morph-ms 120)))

      :else
      (let [target (if media? m slide)
            a (.animate target [{:opacity 1 :transform (or from-t "none")}
                                {:opacity 0 :transform (str (or from-t "") " scale(0.92)")}]
                        {:duration fade-ms :easing ease-out :fill "forwards"})]
        (.animate backdrop [{:opacity from-o} {:opacity 0}]
                  {:duration fade-ms :easing ease-out :fill "forwards"})
        (.then (.-finished a) finish finish)
        (js/setTimeout finish (+ fade-ms 120))))))

;; ── Lifecycle ───────────────────────────────────────────────────────

(declare on-index-attr)

(defn- clear-animations! [root]
  (doseq [a (.getAnimations root {:subtree true})] (.cancel a)))

(defn- teardown!
  "Drop everything an open viewer holds on the page."
  [root]
  (when-let [st (state root)]
    (set! (.-closed st) true)
    (js/clearTimeout (.-scrollTimer st))
    (js/clearTimeout (.-repaintTimer st))
    (some-> (.-mo st) (.disconnect))
    (some-> (.-ro st) (.disconnect))
    (when-let [sc (scroller root)]
      (.removeEventListener sc "scroll" (.-onScroll st))
      (.removeEventListener sc "scrollend" (.-onScrollEnd st)))
    (when-let [old (.-source st)] (.removeAttribute old "data-lightbox-source"))
    (doseq [s (slide-els root)]
      (when-let [m (media-of s)]
        (when (inner-video m) (pause-media! m))))
    (aset root "__lb" nil)
    (remove-surface! root)))

(defn- finish-close! [root]
  (let [st (state root)
        managed? (.hasAttribute root "id")
        history? (.-history st)]
    (teardown! root)
    ;; invisible from here on (no tear between layers while the host
    ;; unmounts it)
    (doseq [a ["data-ready" "data-closing" "data-opening" "data-zoomed" "data-pulling" "data-panning"]]
      (.removeAttribute root a))
    (.removeProperty (.-style root) "--lightbox-pull-progress")
    (when (and history? (not (.-poppedHistory st))
               (some-> js/history .-state (aget "uiLightbox")))
      (.back js/history))
    (when (.-managed st)
      (clear-animations! root)
      (doseq [s (slide-els root)]
        (when-let [m (media-of s)]
          (aset m "__uiZoom" nil)
          (set! (.. m -style -transform) "")))
      (.setAttribute root "hidden" ""))
    (dispatch! root "close" {})))

(defn- close! [root]
  (let [st (state root)]
    (when (and st (not (.-closing st)))
      (set! (.-closing st) true)
      (play-close! root #(finish-close! root)))))

(defn- request-close!
  "Close from inside (button, Escape, backdrop, pull). With history, Back
   does it, so the entry we pushed goes away."
  [root]
  (let [st (state root)]
    (when (and st (not (.-closing st)))
      (if (and (.-history st) (some-> js/history .-state (aget "uiLightbox")))
        (do (set! (.-poppedHistory st) true)
            (.back js/history))
        (close! root)))))

;; A viewer the host removed while open, for a moment: some renderers
;; re-create nodes on a re-render (Eucalypt resets a whole tree when it
;; can't patch), and the new node is the same viewer, so it takes over
;; instead of opening again.
(def ^:private handoff (atom nil))
(def ^:private handoff-ms 250)

(defn- surface-removed!
  "The host unmounted a surface without closing it."
  [el]
  (if-let [st (state el)]
    (do (when-not (.-closing st)
          (reset! handoff {:t (now) :history (.-history st) :index (.-index st)})
          ;; still gone after the hand-off window: drop our history entry
          (js/setTimeout
           (fn []
             (when-let [h @handoff]
               (when (and (:history h) (> (- (now) (:t h)) (- handoff-ms 1)))
                 (reset! handoff nil)
                 (when (and (not (some state (js/Array.from (.querySelectorAll js/document root-sel))))
                            (some-> js/history .-state (aget "uiLightbox")))
                   (.back js/history)))))
           handoff-ms))
        (teardown! el))
    (remove-surface! el)))

(defn- open!
  "A viewer mounted or unhid: take it over."
  [root]
  (when-not (state root)
    (let [pending (aget root "__lbPending")
          n (.-length (slide-els root))
          index (clamp 0 (js/Math.max 0 (dec n))
                       (or (:index pending)
                           (js/parseInt (or (.getAttribute root "data-index") "0") 10)
                           0))
          contained? (.hasAttribute root "data-contained")
          st #js {:index index :managed (some? pending)}
          h @handoff
          continued? (and h (not pending) (< (- (now) (:t h)) handoff-ms))]
      (reset! handoff nil)
      (aset root "__lbPending" nil)
      (when-let [t (:trigger pending)]
        (reset! last-trigger {:el t :t (now)}))
      (aset root "__lb" st)
      (.setAttribute root "data-opening" "")
      (clear-animations! root)
      (add-surface! root)
      (let [sc (scroller root)
            on-scroll (fn [_] (on-slides-scroll root))
            on-end (fn [_] (settle-scroll! root))]
        (set! (.-onScroll st) on-scroll)
        (set! (.-onScrollEnd st) on-end)
        (.addEventListener sc "scroll" on-scroll {:passive true})
        (.addEventListener sc "scrollend" on-end {:passive true}))
      ;; the host moving data-index, slides coming and going
      (let [mo (js/MutationObserver. (fn [records] (on-index-attr root records)))]
        (.observe mo root {:attributes true :attributeFilter ["data-index"]})
        (.observe mo (scroller root) {:childList true})
        (set! (.-mo st) mo))
      (let [ro (js/ResizeObserver. (fn [_] (on-resize root)))]
        (.observe ro (scroller root))
        (set! (.-ro st) ro))
      (if continued?
        (set! (.-history st) (:history h))
        (when (and (.hasAttribute root "data-history") (not contained?))
          (.pushState js/history {:uiLightbox true} "")
          (set! (.-history st) true)))
      (sync-chrome! root)
      (load-near! root)
      (fit-all! root)
      (jump-to! root index)
      (center-thumb! root false)
      (sync-videos! root)
      (if continued?
        ;; the same viewer, re-created by the host: no second morph
        (do (.setAttribute root "data-ready" "")
            (.removeAttribute root "data-opening")
            (hide-source! root)
            (sync-videos! root))
        (.then (wait-for-media root)
               (fn []
                 (when (identical? st (state root))
                   (play-open! root))))))))

(defn- on-index-attr [root records]
  (let [st (state root)]
    (when st
      (when (some #(= "childList" (.-type %)) records)
        (doseq [s (slide-els root)]
          (when-let [m (media-of s)] (when (aget m "__lbLoading") (fit! s))))
        (load-near! root)
        (sync-chrome! root))
      (when (some #(= "attributes" (.-type %)) records)
        (let [i (js/parseInt (.getAttribute root "data-index") 10)]
          (when (and (not (js/isNaN i)) (not= i (.-index st)))
            (if (.-scrolling st)
              nil
              (do (jump-to! root i) (set-index! root i)))))))))

(defn- open-managed!
  "A thumbnail with data-lightbox-open was clicked."
  [trigger]
  (let [root (js/document.getElementById (.getAttribute trigger "data-lightbox-open"))]
    (when (and root (.matches root root-sel) (not (state root)))
      (aset root "__lbPending"
            {:index (js/parseInt (or (.getAttribute trigger "data-lightbox-index") "0") 10)
             :trigger trigger})
      (.removeAttribute root "hidden")
      ;; the mount animation opens it; open! is idempotent if it already ran
      (js/requestAnimationFrame (fn [] (open! root))))))

(defn- on-animation-start [e]
  (case (.-animationName e)
    "ui-lightbox-mount"
    (let [t (.-target e)]
      (when (.matches t root-sel) (open! t)))
    "lightbox-in"
    (let [t (.-target e)]
      (when (.matches t legacy-sel) (add-surface! t)))
    nil))

(defn- on-popstate [_]
  (doseq [root (js/Array.from (.querySelectorAll js/document root-sel))]
    (when-let [st (state root)]
      (when (.-history st)
        (set! (.-poppedHistory st) true)
        (close! root)))))

;; ── Pointer gestures ───────────────────────────────────────────────

;; pointerId -> {:x :y :x0 :y0} for pointers pressed inside a surface
(def ^:private pointers (js/Map.))
(def ^:private gesture (atom nil))
(def ^:private suppress-click-until (atom 0))
(def ^:private last-tap (atom nil))

(defn- current-points [] (js/Array.from (.values pointers)))

(defn- midpoint [a b]
  [(/ (+ (:x a) (:x b)) 2) (/ (+ (:y a) (:y b)) 2)])

(defn- distance [a b]
  (js/Math.hypot (- (:x a) (:x b)) (- (:y a) (:y b))))

(defn- rel-to [[cx cy] x y] [(- x cx) (- y cy)])

(defn- rebase!
  "Re-anchor on the current pointers and pose (a pointer came or went)."
  []
  (when-let [g @gesture]
    (let [pts (current-points)
          ctx (:ctx g)]
      (reset! gesture
              (assoc g
                     :z0 (if ctx (zoom-of (:media ctx)) identity-zoom)
                     :pts0 pts
                     :pinch-on false
                     :raw-s (when ctx (:s (zoom-of (:media ctx))))
                     :center (when ctx (layout-center (:media ctx)))
                     :max-pointers (js/Math.max (:max-pointers g) (.-length pts)))))))

(defn- sample! [x y]
  (swap! gesture update :samples
         (fn [ss]
           (let [t (now)
                 ss (conj (or ss []) {:x x :y y :t t})]
             (vec (filter #(< (- t (:t %)) 100) ss))))))

(defn- velocity
  "px/ms over the last ~100ms of samples."
  [g]
  (let [ss (:samples g)]
    (if (< (count ss) 2)
      [0 0]
      (let [a (first ss) b (last ss)
            dt (js/Math.max 1 (- (:t b) (:t a)))]
        [(/ (- (:x b) (:x a)) dt) (/ (- (:y b) (:y a)) dt)]))))

;; pull to dismiss

(defn- pull-threshold [] (* 0.22 js/window.innerHeight))

(defn- set-pull! [root m dx dy]
  (let [p (clamp 0 1 (/ (js/Math.abs dy) (pull-threshold)))
        k (- 1 (* 0.18 p))]
    (.setProperty (.-style root) "--lightbox-pull-progress" (str p))
    (when m
      (set! (.. m -style -transform) (pose-css {:s k :tx (* 0.6 dx) :ty dy})))))

(defn- end-pull!
  "Release a pull: close past the threshold or on a flick, else spring
   back."
  [root m dy vy]
  (if (or (>= (js/Math.abs dy) (pull-threshold))
          (and (> (js/Math.abs vy) 0.5) (pos? (* vy dy)) (> (js/Math.abs dy) 24)))
    (do (.removeAttribute root "data-pulling")
        (request-close! root))
    (do (.removeAttribute root "data-pulling")
        (.setProperty (.-style root) "--lightbox-pull-progress" "0")
        (when m
          (let [from (.. m -style -transform)]
            (set! (.. m -style -transform) "")
            (when (and (not= "" from) (not (reduced-motion?)))
              (aset m "__lbAnim"
                    (.animate m [{:transform from} {:transform (pose-css identity-zoom)}]
                              {:duration settle-ms :easing ease-out}))))))))

;; mouse swipe (touch swipes are native scrolling)

(defn- end-swipe! [root g dx vx]
  (let [sc (scroller root)
        st (state root)
        w (slide-width root)
        i (.-index st)
        n (.-length (slide-els root))
        target (cond
                 (or (< dx (* -0.2 w)) (< vx -0.4)) (inc i)
                 (or (> dx (* 0.2 w)) (> vx 0.4))   (dec i)
                 :else i)
        target (clamp 0 (dec n) target)]
    (set! (.. sc -style -scrollSnapType) "")
    (.scrollTo sc {:left (* target w) :behavior (if (reduced-motion?) "instant" "smooth")})))

(defn- track! [e]
  (let [g @gesture
        ctx (:ctx g)
        root (:root g)
        pts (current-points)
        p (aget pts 0)
        dx (- (:x p) (:x (aget (:pts0 g) 0)))
        dy (- (:y p) (:y (aget (:pts0 g) 0)))]
    (cond
      ;; pinch (+ pan with the centroid)
      (and ctx (>= (.-length pts) 2))
      (let [a0 (aget (:pts0 g) 0) b0 (aget (:pts0 g) 1)
            a (aget pts 0) b (aget pts 1)
            d0 (js/Math.max 1 (distance a0 b0))
            d (distance a b)]
        (if (and (not (:pinch-on g)) (> (js/Math.abs (- d d0)) pinch-slop-px))
          ;; the fingers really pinch now (a two-finger slide only pans):
          ;; measure the spread from here, so the slop never pops
          (do (rebase!) (swap! gesture assoc :pinch-on true))
          (let [[mx0 my0] (midpoint a0 b0)
                [mx my] (midpoint a b)
                z0 (:z0 g)
                raw (if (:pinch-on g) (* (:s z0) (/ d d0)) (:s z0))
                s (elastic-scale raw (max-scale ctx))
                [rx ry] (rel-to (:center g) mx0 my0)
                z (zoom-at z0 s rx ry)]
            (apply-zoom! ctx (assoc z :tx (+ (:tx z) (- mx mx0)) :ty (+ (:ty z) (- my my0))) nil))))

      (= :pan (:mode g))
      (let [z0 (:z0 g)
            m (:media ctx)
            vp (viewport-rect ctx)
            [cx cy] (:center g)
            s (:s z0)
            tx (elastic-pan (+ (:tx z0) dx)
                            (axis-range (:left vp) (:right vp) cx (.-offsetWidth m) s)
                            (- (:right vp) (:left vp)))
            ty (elastic-pan (+ (:ty z0) dy)
                            (axis-range (:top vp) (:bottom vp) cy (.-offsetHeight m) s)
                            (- (:bottom vp) (:top vp)))]
        (apply-zoom! ctx (assoc z0 :tx tx :ty ty) nil))

      (= :pull (:mode g))
      (set-pull! root (:pull-media g) dx dy)

      (= :swipe (:mode g))
      (set! (.-scrollLeft (scroller root)) (- (:scroll0 g) dx))

      :else nil)))

(defn- pick-mode!
  "Decide what a single-pointer drag is once it leaves the slop."
  [e dx dy]
  (let [g @gesture
        root (:root g)
        ctx (:ctx g)
        vertical? (> (js/Math.abs dy) (js/Math.abs dx))]
    (cond
      (and ctx (zoomed? (:media ctx)))
      (do (swap! gesture assoc :mode :pan)
          (when root (.setAttribute root "data-panning" "")))

      (:legacy? ctx) nil
      (nil? root) nil
      (.hasAttribute root "data-contained") nil

      vertical?
      (let [m (media-of (active-slide root))]
        (when m (cancel-anim! m))
        (swap! gesture assoc :mode :pull :pull-media m)
        (.setAttribute root "data-pulling" ""))

      (and (= "mouse" (.-pointerType e)) (> (.-length (slide-els root)) 1))
      (let [sc (scroller root)]
        (set! (.. sc -style -scrollSnapType) "none")
        (swap! gesture assoc :mode :swipe :scroll0 (.-scrollLeft sc)))

      :else nil)))

(defn- tap! [g x y]
  (let [t (now)
        prev @last-tap
        ctx (:ctx g)]
    (if (and prev ctx
             (< (- t (:t prev)) double-tap-ms)
             (< (js/Math.hypot (- x (:x prev)) (- y (:y prev))) double-tap-slop-px))
      (let [m (:media ctx)
            z (live-pose! m)
            [px py] (rel-to (layout-center m) x y)]
        (reset! last-tap nil)
        (apply-zoom! ctx
                     (if (> (:s z) 1.001)
                       identity-zoom
                       (settle ctx (zoom-at z (js/Math.min double-tap-scale (max-scale ctx)) px py)))
                     {:ms settle-ms}))
      (reset! last-tap {:t t :x x :y y}))))

(defn- on-pointerdown [e]
  (let [t (.-target e)
        root (open-root-of t)
        surface (or root (closest t legacy-sel))]
    ;; remember the thumbnail a lightbox is about to open from
    (when-not surface
      (when-let [k (closest t "[data-lightbox-key]")]
        (reset! last-trigger {:el k :t (now)})))
    (when (and surface
               (or (not= "mouse" (.-pointerType e)) (zero? (.-button e)))
               (not (and root (.-closing (state root)))))
      (when root (set! (.-pressTarget (state root)) t))
      (when-not (closest t no-gesture-sel)
        (let [ctx (ctx-at t)
              ;; gestures on a viewer only start on its resting slide
              blocked? (and root (or (mid-swipe? root) (.hasAttribute root "data-opening")))]
          (when-not blocked?
            (when (or (zero? (.-size pointers))
                      (not (identical? surface (:surface @gesture))))
              (.clear pointers)
              (reset! suppress-click-until 0)
              (when ctx (live-pose! (:media ctx)))
              (reset! gesture {:surface surface
                               :root root
                               :ctx ctx
                               :moved false
                               :on-media (and ctx (.contains (:media ctx) t))
                               :max-pointers 0}))
            (let [x (.-clientX e) y (.-clientY e)]
              (.set pointers (.-pointerId e) {:x x :y y :x0 x :y0 y})
              (sample! x y))
            (when (= "mouse" (.-pointerType e)) (.preventDefault e))
            (rebase!)))))))

(defn- on-pointermove [e]
  (let [id (.-pointerId e)]
    (when (.has pointers id)
      (let [p (.get pointers id)
            x (.-clientX e)
            y (.-clientY e)
            g @gesture]
        (.set pointers id (assoc p :x x :y y))
        (sample! x y)
        (when (and (not (:moved g))
                   (> (js/Math.hypot (- x (:x0 p)) (- y (:y0 p))) tap-slop-px))
          (swap! gesture assoc :moved true))
        (let [g @gesture]
          (when (and (nil? (:mode g)) (= 1 (.-size pointers)) (= 1 (:max-pointers g))
                     (> (js/Math.hypot (- x (:x0 p)) (- y (:y0 p))) axis-lock-px))
            (pick-mode! e (- x (:x0 p)) (- y (:y0 p))))
          (let [g @gesture]
            (when (or (:mode g) (>= (.-size pointers) 2))
              ;; a second finger during a pull or a swipe changes nothing
              (when-not (and (>= (.-size pointers) 2) (or (= :pull (:mode g)) (= :swipe (:mode g))))
                (track! e)))))))))

(defn- end-zoom-gesture! [g]
  (let [ctx (:ctx g)
        m (:media ctx)
        z (zoom-of m)
        mx (max-scale ctx)
        out? (or (> (:s z) (+ mx 0.01)) (and (< (:s z) 0.99)))
        [vx vy] (velocity g)
        coast? (and (= :pan (:mode g)) (> (js/Math.hypot vx vy) 0.15) (not (:legacy? ctx)))
        target (settle ctx (if coast?
                             (assoc z :tx (+ (:tx z) (* vx 260)) :ty (+ (:ty z) (* vy 260)))
                             z))]
    (when out? (vibrate! 8))
    (apply-zoom! ctx target {:ms (if coast? coast-ms settle-ms)
                             :easing (if coast? ease-coast ease-out)})))

(defn- on-pointer-end [e]
  (let [id (.-pointerId e)
        last-p (.get pointers id)]
    (when (.has pointers id)
      (.delete pointers id)
      (if (pos? (.-size pointers))
        (let [g @gesture]
          ;; pinch → one finger: continue as a pan from here
          (when (and (:ctx g) (>= (:max-pointers g) 2))
            (swap! gesture assoc :mode (when (zoomed? (:media (:ctx g))) :pan) :pinch-on false))
          (rebase!))
        (let [g @gesture
              {:keys [root ctx moved on-media max-pointers mode]} g
              [vx vy] (velocity g)]
          (reset! gesture nil)
          (when root (.removeAttribute root "data-panning"))
          ;; the click a drag ends with (if any comes) does nothing; a touch drag
          ;; ending in pointercancel has none, so the guard expires
          (when moved (reset! suppress-click-until (+ (now) 400)))
          (cond
            (= :pull mode)
            (end-pull! root (:pull-media g) (- (:y last-p) (:y (aget (:pts0 g) 0))) vy)

            (= :swipe mode)
            (end-swipe! root g (- (:x last-p) (:x (aget (:pts0 g) 0))) vx)

            (and (not moved) on-media (= 1 max-pointers) (= "pointerup" (.-type e)))
            (tap! g (.-clientX e) (.-clientY e))

            ctx
            (end-zoom-gesture! g)

            :else nil))))))

;; ── Wheel and Safari trackpad pinch ────────────────────────────────

(def ^:private ctrl-held? (atom false))
(def ^:private wheel-pinch (atom nil))
(def ^:private safari-pinch (atom nil))

(defn- wheel-zoom! [ctx e k elastic?]
  (let [m (:media ctx)
        z (live-pose! m)
        mx (max-scale ctx)
        [px py] (rel-to (layout-center m) (.-clientX e) (.-clientY e))]
    (if elastic?
      ;; a trackpad pinch: accumulate the unconstrained scale and show it
      ;; rubber-banded, settle once the stream goes quiet (Blink and Gecko
      ;; send no gestureend)
      (let [wp (or @wheel-pinch {:raw (:s z)})
            raw (* (:raw wp) k)
            s (elastic-scale raw mx)]
        (js/clearTimeout (:timer wp))
        (apply-zoom! ctx (zoom-at z s px py) nil)
        (reset! wheel-pinch
                {:raw raw
                 :timer (js/setTimeout
                         (fn []
                           (reset! wheel-pinch nil)
                           (let [z (zoom-of m)]
                             (when (or (> (:s z) mx) (< (:s z) 1)) (vibrate! 8))
                             (apply-zoom! ctx (settle ctx z) {:ms settle-ms})))
                         wheel-idle-ms)}))
      ;; a real ctrl+wheel (or a plain wheel where it zooms): hard clamp
      (apply-zoom! ctx (settle ctx (zoom-at z (clamp 1 mx (* (:s z) k)) px py)) nil))))

(defn- wheel-pull! [root e]
  (let [st (state root)
        wp (or (.-wheelPull st) #js {:offset 0})
        m (media-of (active-slide root))]
    (when-not (.-committed wp)
      (set! (.-wheelPull st) wp)
      (set! (.-offset wp) (- (.-offset wp) (.-deltaY e)))
      (.setAttribute root "data-pulling" "")
      (when m (cancel-anim! m))
      (set-pull! root m 0 (.-offset wp))
      (js/clearTimeout (.-timer wp))
      (if (>= (js/Math.abs (.-offset wp)) (* 1.4 (pull-threshold)))
        (do (set! (.-committed wp) true)
            (end-pull! root m (.-offset wp) 0))
        (set! (.-timer wp)
              (js/setTimeout
               (fn []
                 (set! (.-wheelPull st) nil)
                 (end-pull! root m (.-offset wp) 0))
               wheel-idle-ms))))))

(defn- on-wheel [e]
  (let [t (.-target e)
        root (open-root-of t)
        legacy (when-not root (closest t legacy-sel))
        ctx (cond root (viewer-ctx root) legacy (ctx-at t))
        dy (* (.-deltaY e) (if (= 1 (.-deltaMode e)) 16 1))
        dx (* (.-deltaX e) (if (= 1 (.-deltaMode e)) 16 1))
        prevent! (fn [] (when (and (.-cancelable e) (not webkit?)) (.preventDefault e)))]
    (when (or root legacy)
      (cond
        (and root (or (.-closing (state root)) (.hasAttribute root "data-opening")))
        (prevent!)

        (closest t ".ui-lightbox-content, .ui-lightbox-thumbs")
        nil

        ;; ctrl / meta + wheel, or a trackpad pinch (ctrl without the key)
        (or (.-ctrlKey e) (.-metaKey e))
        (do (prevent!)
            (when (and ctx (not @safari-pinch))
              (wheel-zoom! ctx e (js/Math.exp (* (- dy) 0.01))
                           (and (.-ctrlKey e) (not @ctrl-held?)))))

        (and ctx (zoomed? (:media ctx)))
        (do (prevent!)
            (if (:legacy? ctx)
              (wheel-zoom! ctx e (js/Math.exp (* (- dy) 0.002)) false)
              (let [z (live-pose! (:media ctx))]
                (apply-zoom! ctx (settle ctx (assoc z :tx (- (:tx z) dx) :ty (- (:ty z) dy))) nil))))

        (or legacy (.hasAttribute root "data-wheel-zoom"))
        (do (prevent!)
            (when ctx (wheel-zoom! ctx e (js/Math.exp (* (- dy) 0.002)) false)))

        ;; 1×, vertical: pull to dismiss (horizontal is the strip's own scroll)
        (and (> (js/Math.abs dy) (js/Math.abs dx))
             (not (.hasAttribute root "data-contained"))
             (not (mid-swipe? root)))
        (do (prevent!) (wheel-pull! root e))

        :else nil))))

(defn- on-gesture-start [e]
  (let [ctx (ctx-at (.-target e))]
    (when ctx
      (.preventDefault e)
      (reset! safari-pinch {:ctx ctx :z0 (live-pose! (:media ctx))}))))

(defn- on-gesture-change [e]
  (when-let [{:keys [ctx z0]} @safari-pinch]
    (.preventDefault e)
    (let [m (:media ctx)
          s (elastic-scale (* (:s z0) (.-scale e)) (max-scale ctx))
          [px py] (rel-to (layout-center m) (.-clientX e) (.-clientY e))]
      (apply-zoom! ctx (zoom-at (zoom-of m) s px py) nil))))

(defn- on-gesture-end [e]
  (when-let [{:keys [ctx]} @safari-pinch]
    (.preventDefault e)
    (reset! safari-pinch nil)
    (apply-zoom! ctx (settle ctx (zoom-of (:media ctx))) {:ms settle-ms})))

;; ── Keyboard ────────────────────────────────────────────────────────

(defn- top-root []
  (let [open (filter (fn [r] (and (state r) (not (.hasAttribute r "data-contained"))
                                  (not= "off" (.getAttribute r "data-keyboard"))))
                     (js/Array.from (.querySelectorAll js/document root-sel)))]
    (last open)))

(defn- typing-target? [t]
  (some? (closest t "input, textarea, select, [contenteditable]")))

(defn- on-keydown [e]
  (when (= "Control" (.-key e)) (reset! ctrl-held? true))
  (when-let [root (top-root)]
    (let [k (.-key e)
          t (.-target e)
          mod? (or (.-ctrlKey e) (.-metaKey e) (.-altKey e))
          st (state root)
          ctx (viewer-ctx root)
          zoomed (and ctx (zoomed? (:media ctx)))
          step (* 0.1 js/window.innerWidth)
          ;; a focused video player keeps its own keys (seek, volume)
          in-player? (some? (closest t ".video-player"))
          handled
          (when-not (or mod? (.-closing st) (and in-player? (not= "Escape" k)))
            (case k
              "Escape" (when-not (.querySelector root ".video-player[data-panel-open]")
                         (request-close! root)
                         true)
              "ArrowLeft" (when-not (or (typing-target? t) (closest t "video"))
                            (if zoomed (pan-by! root step 0) (go-to! root (dec (.-index st)))) true)
              "ArrowRight" (when-not (or (typing-target? t) (closest t "video"))
                             (if zoomed (pan-by! root (- step) 0) (go-to! root (inc (.-index st)))) true)
              "ArrowUp" (when (and zoomed (not (typing-target? t))) (pan-by! root 0 step) true)
              "ArrowDown" (when (and zoomed (not (typing-target? t))) (pan-by! root 0 (- step)) true)
              "Home" (when-not (typing-target? t) (go-to! root 0) true)
              "End" (when-not (typing-target? t) (go-to! root 1e9) true)
              "+" (when (and ctx (not (typing-target? t))) (zoom-by! root 2) true)
              "=" (when (and ctx (not (typing-target? t))) (zoom-by! root 2) true)
              "-" (when (and ctx (not (typing-target? t))) (zoom-by! root 0.5) true)
              "_" (when (and ctx (not (typing-target? t))) (zoom-by! root 0.5) true)
              "0" (when (and ctx (not (typing-target? t))) (zoom-by! root nil) true)
              nil))]
      (when handled
        (.preventDefault e)
        (.stopPropagation e)))))

(defn- on-keyup [e]
  (when (= "Control" (.-key e)) (reset! ctrl-held? false)))

;; ── Clicks ──────────────────────────────────────────────────────────

(defn- on-click-capture [e]
  (if (< (now) @suppress-click-until)
    (do (reset! suppress-click-until 0)
        (.preventDefault e)
        (.stopPropagation e))
    (let [t (.-target e)
          root (open-root-of t)]
      (cond
        root
        (let [st (state root)
              action-el (closest t "[data-lightbox-action]")
              press (.-pressTarget st)]
          (cond
            action-el
            (let [a (.getAttribute action-el "data-lightbox-action")]
              (.stopPropagation e)
              (case a
                "close" (request-close! root)
                "prev" (go-to! root (dec (.-index st)))
                "next" (go-to! root (inc (.-index st)))
                "zoom-in" (zoom-by! root 2)
                "zoom-out" (zoom-by! root 0.5)
                "go" (go-to! root (js/parseInt (.getAttribute action-el "data-index") 10))
                nil))

            ;; the backdrop or a slide's empty area — and the press started
            ;; there too: a drag from the photo onto it retargets the click
            ;; to their common ancestor
            (and (or (.matches t ".ui-lightbox-slide, .ui-lightbox-backdrop, .ui-lightbox-slides"))
                 (identical? press t)
                 (not (.hasAttribute root "data-contained")))
            (request-close! root)

            :else nil))

        :else
        (when-let [trigger (closest t "[data-lightbox-open]")]
          (when (js/document.getElementById (.getAttribute trigger "data-lightbox-open"))
            (.preventDefault e)
            (open-managed! trigger)))))))

(defn- on-load-capture [e]
  (let [t (.-target e)]
    ;; legacy overlays start every (re)loaded image unzoomed
    (when (and (.-matches t) (.matches t legacy-image-sel) (closest t legacy-sel))
      (aset t "__uiZoom" nil)
      (set! (.. t -style -transform) "")
      (.removeAttribute t "data-zoomed"))))

(defn- on-dragstart [e]
  ;; the target can be a text node (selection drag) — no .closest there
  (when (or (open-root-of (.-target e)) (closest (.-target e) legacy-sel))
    (.preventDefault e)))

;; ── Install ─────────────────────────────────────────────────────────

(defn- scan!
  "Pick up viewers that mounted before the runtime loaded."
  []
  (doseq [r (js/Array.from (.querySelectorAll js/document (str root-sel ":not([hidden])")))]
    (open! r)))

;; guard against double install (ui-runtime.js + a side-effect require)
(when-not (aget js/window "__uiLightboxZoom")
  (aset js/window "__uiLightboxZoom" true)
  (.addEventListener js/document "pointerdown" on-pointerdown true)
  (.addEventListener js/document "pointermove" on-pointermove true)
  (.addEventListener js/document "pointerup" on-pointer-end true)
  (.addEventListener js/document "pointercancel" on-pointer-end true)
  (.addEventListener js/document "click" on-click-capture true)
  (.addEventListener js/document "keydown" on-keydown true)
  (.addEventListener js/document "keyup" on-keyup true)
  (.addEventListener js/window "blur" (fn [_] (reset! ctrl-held? false)))
  (.addEventListener js/document "animationstart" on-animation-start true)
  ;; load doesn't bubble — catch it in the capture phase
  (.addEventListener js/document "load" on-load-capture true)
  (.addEventListener js/document "dragstart" on-dragstart true)
  (.addEventListener js/window "popstate" on-popstate)
  (if (= "loading" js/document.readyState)
    (.addEventListener js/document "DOMContentLoaded" scan!)
    (scan!))
  (aset js/window "__uiLightbox"
        {:close (fn [el] (when-let [r (closest el root-sel)] (request-close! r)))
         :go (fn [el i] (when-let [r (closest el root-sel)] (go-to! r i)))}))
