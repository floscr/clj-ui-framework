(ns flip
  "Generic FLIP (First → Last → Invert → Play) animation helper for
   DOM reflows: reorders, insertions, and other layout changes in any
   component.

   Capture element positions, mutate the DOM however you like, then
   play — every surviving element animates (WAAPI transform) from its
   old position to its new one. Honors prefers-reduced-motion (plays
   nothing). Standalone: no component imports this; it is a utility
   for runtime modules and page scripts alike.

   API (window.__uiFlip):

     const snap = __uiFlip.capture(target)
     // ...reorder / insert / remove nodes...
     __uiFlip.play(snap, opts)

   or the one-shot form:

     __uiFlip.wrap(target, () => { ...mutate the DOM... }, opts)

   `target` is a container Element (its element children are
   tracked), a CSS selector string (all matches), or an
   array/NodeList of elements.

   opts (all optional):
     duration  ms, default 250
     easing    default \"cubic-bezier(0.32, 0.72, 0, 1)\"
     scale     also animate size changes (top-left origin)
     lift      z-index: 1 while an element is in flight
     enter     fade/scale-in elements present now but absent from
               the capture"

  )

(def ^:private default-duration 250)
(def ^:private default-easing "cubic-bezier(0.32, 0.72, 0, 1)")

(defn- reduced-motion? []
  (.-matches (js/window.matchMedia "(prefers-reduced-motion: reduce)")))

(defn- resolve-els
  "Normalize a capture target to an array of elements."
  [target]
  (cond
    (string? target) (js/Array.from (js/document.querySelectorAll target))
    (instance? js/Element target) (js/Array.from (.-children target))
    :else (js/Array.from target)))

(defn capture
  "Snapshot the current positions (gBCR) of the target's elements.
   Returns an opaque snapshot for `play`."
  [target]
  (let [rects (js/Map.)]
    (doseq [el (resolve-els target)]
      (.set rects el (.getBoundingClientRect el)))
    #js {:target target :rects rects}))

(defn- play-moves!
  "Animate every captured, still-connected element from its old rect
   to where it sits now."
  [rects timing scale? lift?]
  (.forEach rects
    (fn [rect el]
      (when (.-isConnected el)
        (let [now (.getBoundingClientRect el)
              dx (- (.-x rect) (.-x now))
              dy (- (.-y rect) (.-y now))
              sx (if (and scale? (> (.-width now) 0))
                   (/ (.-width rect) (.-width now)) 1)
              sy (if (and scale? (> (.-height now) 0))
                   (/ (.-height rect) (.-height now)) 1)
              moved? (or (>= (js/Math.abs dx) 0.5)
                         (>= (js/Math.abs dy) 0.5)
                         (>= (js/Math.abs (- sx 1)) 0.005)
                         (>= (js/Math.abs (- sy 1)) 0.005))]
          (when moved?
            (when lift? (set! (.. el -style -zIndex) "1"))
            (let [from (str "translate(" dx "px, " dy "px)"
                            (if scale? (str " scale(" sx ", " sy ")") ""))
                  kf #js {:transform #js [from "none"]}
                  _ (when scale?
                      ;; scale must resolve from the same corner the
                      ;; translate delta was measured from
                      (aset kf "transformOrigin" #js ["0 0" "0 0"]))
                  anim (.animate el kf timing)
                  unlift (fn [] (set! (.. el -style -zIndex) ""))]
              (when lift?
                (set! (.-onfinish anim) unlift)
                (set! (.-oncancel anim) unlift)))))))))

(defn- play-enters!
  "Fade/scale-in elements present in the target now but not captured."
  [target rects timing]
  (doseq [el (resolve-els target)]
    (when-not (.has rects el)
      (.animate el
                #js {:opacity #js [0 1]
                     :transform #js ["scale(0.96)" "none"]}
                timing))))

(defn play
  "Invert + play: animate elements from their captured positions to
   their current ones. Call after mutating the DOM."
  ([snap] (play snap nil))
  ([snap opts]
   (when-not (reduced-motion?)
     (let [opts (or opts #js {})
           timing #js {:duration (or (aget opts "duration") default-duration)
                       :easing (or (aget opts "easing") default-easing)}
           rects (aget snap "rects")]
       (play-moves! rects timing
                    (if (aget opts "scale") true false)
                    (if (aget opts "lift") true false))
       (when (aget opts "enter")
         (play-enters! (aget snap "target") rects timing))))
   nil))

(defn wrap
  "capture → mutate → play in one call. `mutate` is a nullary fn that
   changes the DOM synchronously."
  ([target mutate] (wrap target mutate nil))
  ([target mutate opts]
   (let [snap (capture target)]
     (mutate)
     (play snap opts))))

(set! (.-__uiFlip js/window)
      #js {:capture capture :play play :wrap wrap})
