(ns popover
  "Positioning runtime for anchored popovers built on the native Popover API.

   Toggle/dismiss/Escape are handled by the browser (popover=\"auto\"). This
   runtime only anchors the panel to its trigger: it listens for `toggle`
   events on [popover].popover-content elements (capturing phase, since the
   toggle event does not bubble) and positions the panel relative to the
   matching trigger ([popovertarget=<id>]) using :side / :align, clamped to
   the viewport. Repositions on scroll/resize while open.")

(def ^:private gap 8)
(def ^:private edge 8)

(defn- clamp [v lo hi]
  (Math/max lo (Math/min v hi)))

(defn- align-h [tr cw align]
  (cond
    (= align "start") (.-left tr)
    (= align "end")   (- (.-right tr) cw)
    :else             (+ (.-left tr) (/ (- (.-width tr) cw) 2))))

(defn- align-v [tr ch align]
  (cond
    (= align "start") (.-top tr)
    (= align "end")   (- (.-bottom tr) ch)
    :else             (+ (.-top tr) (/ (- (.-height tr) ch) 2))))

(defn- position! [content trigger]
  (let [side  (or (.. content -dataset -popoverSide) "bottom")
        align (or (.. content -dataset -popoverAlign) "center")
        tr    (.getBoundingClientRect trigger)
        cr    (.getBoundingClientRect content)
        cw    (.-width cr)
        ch    (.-height cr)
        vw    (.-innerWidth js/window)
        vh    (.-innerHeight js/window)
        left  (cond
                (= side "left")  (- (.-left tr) cw gap)
                (= side "right") (+ (.-right tr) gap)
                :else            (align-h tr cw align))
        top   (cond
                (= side "top")    (- (.-top tr) ch gap)
                (= side "bottom") (+ (.-bottom tr) gap)
                :else             (align-v tr ch align))]
    (set! (.. content -style -left) (str (clamp left edge (- vw cw edge)) "px"))
    (set! (.. content -style -top)  (str (clamp top edge (- vh ch edge)) "px"))))

;; ── Currently open popover (for scroll/resize repositioning) ────────

(def ^:private current #js {:content nil :trigger nil})

(defn- reposition! []
  (when (.-content current)
    (position! (.-content current) (.-trigger current))))

(defn- on-toggle [e]
  (let [content (.-target e)]
    (when (and content
               (.-matches content)
               (.matches content "[popover].popover-content"))
      (if (= (.-newState e) "open")
        (let [id      (.-id content)
              trigger (when id (.querySelector js/document
                                               (str "[popovertarget=\"" id "\"]")))]
          (when trigger
            (set! (.-content current) content)
            (set! (.-trigger current) trigger)
            (position! content trigger)))
        (when (identical? (.-content current) content)
          (set! (.-content current) nil)
          (set! (.-trigger current) nil))))))

(defn init! []
  (.addEventListener js/document "toggle" on-toggle true)
  (.addEventListener js/window "scroll" reposition! true)
  (.addEventListener js/window "resize" reposition!))

(init!)

(aset js/window "__uiPopover" #js {:reposition reposition!})
