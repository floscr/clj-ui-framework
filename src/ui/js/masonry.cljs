(ns masonry
  "Masonry layout runtime for .tile-grid-masonry containers.

   Firefox lays out `grid-template-rows: masonry` natively. For every
   other browser we reproduce it in JS (technique from
   css-tricks.com/making-a-masonry-layout-that-works-today): keep the
   element a normal CSS Grid, collapse the rows (`grid-auto-rows: 0;
   row-gap: 1px`), then give each tile a `grid-row-end: span <height>`
   measured from its rendered box.

   Self-initializing: scans for .tile-grid-masonry at load, re-scans on
   DOM changes (MutationObserver on childList + class attributes) and
   re-lays-out when a container resizes (ResizeObserver). Containers
   that leave masonry layout (class removed) get their polyfill inline
   styles stripped.

   Manual re-run (e.g. after wiping inline styles): window.__uiMasonry()")

(def ^:private raf (atom nil))
(def ^:private observed (js/Set.))

(declare schedule!)

(def ^:private ro (js/ResizeObserver. (fn [_] (schedule!))))

(defn- native?
  "True when the browser (Firefox) honours grid-template-rows: masonry."
  [container]
  (= "masonry" (.-gridTemplateRows (js/getComputedStyle container))))

(defn- clear!
  "Strip any polyfill inline styles so the element renders as a plain grid."
  [container]
  (set! (.. container -style -gridAutoRows) "")
  (.removeProperty (.-style container) "row-gap")
  (doseq [item (js/Array.from (.-children container))]
    (set! (.. item -style -gridRowEnd) "")))

(defn- layout! [container]
  (let [items (js/Array.from (.-children container))]
    (set! (.. container -style -gridAutoRows) "0px")
    (.setProperty (.-style container) "row-gap" "1px" "important")
    ;; Batch every read before every write: measure all tiles first, then
    ;; assign all spans. Interleaving reads and writes forces a reflow per
    ;; tile (O(n) layouts) — painfully slow in Firefox. This does one.
    (let [col-gap (js/parseFloat (.-columnGap (js/getComputedStyle container)))
          col-gap (if (js/isNaN col-gap) 0 col-gap)
          heights (mapv (fn [item] (.-height (.getBoundingClientRect item))) items)]
      (doseq [[item h] (map vector items heights)]
        (set! (.. item -style -gridRowEnd)
              (str "span " (js/Math.round (+ h col-gap))))))))

(defn- run! []
  (let [els (js/Array.from (js/document.querySelectorAll ".tile-grid-masonry"))]
    ;; Observe new containers. ResizeObserver fires once on observe, which
    ;; schedules one extra (idempotent) pass and then settles.
    (doseq [el els]
      (when-not (.has observed el)
        (.add observed el)
        (.observe ro el)))
    ;; Drop containers that were removed from the DOM or switched back to
    ;; the square layout — and strip their polyfill styles.
    (doseq [el (js/Array.from observed)]
      (when (or (not (.-isConnected el))
                (not (.contains (.-classList el) "tile-grid-masonry")))
        (.delete observed el)
        (.unobserve ro el)
        (when (.-isConnected el)
          (clear! el))))
    (doseq [el els]
      (if (native? el)
        (clear! el)
        (layout! el)))))

(defn- schedule!
  "Coalesce relayout requests into one call on the next animation frame
   (after the consumer's renderer has flushed its DOM patches)."
  []
  (when @raf (js/cancelAnimationFrame @raf))
  (reset! raf
          (js/requestAnimationFrame
           (fn [_] (reset! raf nil) (run!)))))

(defn init! []
  (schedule!)
  ;; childList catches grids (re)rendered into the page; the class
  ;; attribute filter catches layout toggles patched onto an existing
  ;; element. Our own writes only touch the style attribute, so they
  ;; never re-trigger the observer.
  (let [obs (js/MutationObserver. (fn [_ _] (schedule!)))]
    (.observe obs js/document.body
              {:childList true :subtree true
               :attributes true :attributeFilter ["class"]}))
  ;; Lazily-loaded images arrive after layout, but a tile's span is fixed
  ;; inline, so the container never resizes and the ResizeObserver stays
  ;; silent. Relayout when an image inside a masonry grid finishes loading
  ;; (load doesn't bubble, hence capture).
  (.addEventListener js/document "load"
                     (fn [e]
                       (let [target (.-target e)]
                         (when (and (= "IMG" (.-tagName target))
                                    (.closest target ".tile-grid-masonry"))
                           (schedule!))))
                     true))

(aset js/window "__uiMasonry" schedule!)

(if (= "loading" (.-readyState js/document))
  (.addEventListener js/document "DOMContentLoaded" init!)
  (init!))
