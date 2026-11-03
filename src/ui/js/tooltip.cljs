(ns tooltip
  "Runtime for ui.tooltip: one shared floating layer for every `.tooltip`
   trigger on the page, driven by delegated pointer and focus events, so
   server-rendered markup and SPA re-renders need no wiring.

   The first tooltip waits for its delay (`data-tooltip-delay` on the trigger
   or any ancestor, default 500ms; keyboard focus and touch skip it). While
   one is open, and for `skip-ms` after it closed, the next trigger opens
   instantly: the layer jumps and swaps its content with transitions off.
   Content is cloned from the trigger's `template.tooltip-content`; an
   element with a `data-tooltip` attribute instead (ui.tooltip/tooltip-attrs,
   no wrapper) shows that text, with `data-tooltip-shortcut` keycaps.

   The bubble inverts the page under its trigger (ink on light, white on
   dark) unless `data-tooltip-theme` on it or an ancestor says light or dark.
   It takes the first side with room: preferred, opposite, perpendicular.")

(def ^:private default-delay 500)
(def ^:private skip-ms 400)
(def ^:private close-ms 80)
;; cards stay open long enough for the pointer to travel into them
(def ^:private card-close-ms 160)
(def ^:private gap 6)
(def ^:private edge 8)
(def ^:private focusable "a[href],button,input,select,textarea,summary,[tabindex]")
;; triggers whose tap does something else: touch never toggles their tooltip
(def ^:private interactive "a[href],button,input,select,textarea,summary,label")

(defn- get-state []
  (or (aget js/window "__uiTooltipState")
      (let [s #js {:layer nil :bubble nil :open nil :pending nil
                   :showTimer nil :hideTimer nil :lastClose -100000
                   :suppressed nil :pointer "mouse" :observer nil}]
        (aset js/window "__uiTooltipState" s)
        s)))

(defn- now [] (.now js/performance))

(defn- trigger-of
  "The nearest .tooltip wrapper or element with a non-blank data-tooltip."
  [node]
  (when (and node (.-closest node))
    (let [t (.closest node ".tooltip, [data-tooltip]")]
      (cond
        (nil? t) nil
        (.matches t ".tooltip") t
        (= "" (.trim (or (.getAttribute t "data-tooltip") ""))) (trigger-of (.-parentElement t))
        :else t))))

;; ── Content and placement ───────────────────────────────────────────

(defn- el [tag cls text]
  (let [e (.createElement js/document tag)]
    (when cls (set! (.-className e) cls))
    (when text (set! (.-textContent e) text))
    e))

(defn- attr-label
  "The label for an attribute trigger: data-tooltip, then the keycaps of a
   space-separated data-tooltip-shortcut (the markup ui.tooltip/tooltip-label
   renders)."
  [trigger]
  (let [label (el "span" "tooltip-label" nil)
        caps  (.filter (.split (or (.getAttribute trigger "data-tooltip-shortcut") "") " ")
                       (fn [k] (not= k "")))]
    (.appendChild label (el "span" nil (.getAttribute trigger "data-tooltip")))
    (when (pos? (.-length caps))
      (let [row (el "span" "tooltip-keys" nil)]
        (.forEach caps (fn [k] (.appendChild row (el "kbd" "tooltip-key" k))))
        (.appendChild label row)))
    label))

(defn- content-of
  "A fragment with copies of the trigger's template children, or the label of
   an attribute trigger. Server-rendered templates keep their children in
   .content, DOM-built ones (Replicant, Eucalypt) as plain children."
  [trigger]
  (let [frag (.createDocumentFragment js/document)
        tpl  (.querySelector trigger ":scope > template.tooltip-content")]
    (if tpl
      (let [src (if (and (.-content tpl) (.. tpl -content -firstChild))
                  (.-content tpl)
                  tpl)]
        (.forEach (.-childNodes src)
                  (fn [n] (.appendChild frag (.cloneNode n true)))))
      (.appendChild frag (attr-label trigger)))
    frag))

(defn- page-dark?
  "Whether the page under the trigger is dark: its nearest data-theme, else
   the system appearance (ui.js.theme removes the attribute for auto)."
  [trigger]
  (let [host  (.closest trigger "[data-theme]")
        theme (when host (.getAttribute host "data-theme"))]
    (cond
      (= theme "dark")  true
      (= theme "light") false
      :else (.-matches (js/matchMedia "(prefers-color-scheme: dark)")))))

(defn- bubble-theme
  "data-tooltip-theme (light or dark) on the trigger or an ancestor, else the
   inverse of the page; the danger tone matches the page instead."
  [trigger]
  (let [host     (.closest trigger "[data-tooltip-theme]")
        explicit (when host (.. host -dataset -tooltipTheme))
        dark?    (page-dark? trigger)]
    (cond
      (or (= explicit "light") (= explicit "dark")) explicit
      (.. trigger -dataset -tooltipTone) (if dark? "dark" "light")
      :else (if dark? "light" "dark"))))

(defn- opposite [side]
  (case side "top" "bottom" "bottom" "top" "left" "right" "right" "left" "top"))

(defn- perpendicular [side]
  (if (or (= side "top") (= side "bottom")) ["right" "left"] ["top" "bottom"]))

(defn- room
  "Free space beside the trigger on `side`, along the axis the bubble needs."
  [side tr vw vh]
  (case side
    "bottom" (- vh (.-bottom tr) gap edge)
    "left"   (- (.-left tr) gap edge)
    "right"  (- vw (.-right tr) gap edge)
    (- (.-top tr) gap edge)))

(defn- need [side w h]
  (if (or (= side "top") (= side "bottom")) h w))

(defn- pick-side
  "The preferred side, else its opposite, else a perpendicular one, whichever
   fits first; when none does, the one with the most room to spare."
  [pref tr w h vw vh]
  (let [order (into [pref (opposite pref)] (perpendicular pref))
        spare (fn [side] (- (room side tr vw vh) (need side w h)))]
    (or (some (fn [side] (when (>= (spare side) 0) side)) order)
        (reduce (fn [best side] (if (> (spare side) (spare best)) side best)) order))))

(defn- clamp [v lo hi]
  (Math/max lo (Math/min v hi)))

(defn- position! [layer trigger]
  ;; clientWidth/Height leave out the scrollbars, innerWidth does not
  (let [vw (.. js/document -documentElement -clientWidth)
        vh (.. js/document -documentElement -clientHeight)]
    (set! (.. layer -style -left) "0px")
    (set! (.. layer -style -top) "0px")
    ;; caps the bubble before it is measured, so it never outgrows the viewport
    (.setProperty (.-style layer) "--tooltip-max-width" (str (- vw (* 2 edge)) "px"))
    (let [tr   (.getBoundingClientRect trigger)
          w    (.-offsetWidth layer)
          h    (.-offsetHeight layer)
          side (pick-side (or (.. trigger -dataset -tooltipSide) "top") tr w h vw vh)
          cx   (+ (.-left tr) (/ (.-width tr) 2))
          cy   (+ (.-top tr) (/ (.-height tr) 2))
          left (clamp (case side
                        "left"  (- (.-left tr) gap w)
                        "right" (+ (.-right tr) gap)
                        (- cx (/ w 2)))
                      edge (- vw w edge))
          top  (clamp (case side
                        "top"    (- (.-top tr) gap h)
                        "bottom" (+ (.-bottom tr) gap)
                        (- cy (/ h 2)))
                      edge (- vh h edge))]
      (set! (.. layer -style -left) (str left "px"))
      (set! (.. layer -style -top) (str top "px"))
      (.setAttribute layer "data-side" side)
      ;; the bubble scales from the point nearest the trigger
      (.setProperty (.-style layer) "--tooltip-origin-x" (str (clamp (- cx left) 0 w) "px"))
      (.setProperty (.-style layer) "--tooltip-origin-y" (str (clamp (- cy top) 0 h) "px")))))

;; ── Open and close ──────────────────────────────────────────────────

(defn- describe! [trigger on?]
  (let [target (or (.querySelector trigger focusable) trigger)]
    (if on?
      (.setAttribute target "aria-describedby" "ui-tooltip")
      (when (= (.getAttribute target "aria-describedby") "ui-tooltip")
        (.removeAttribute target "aria-describedby")))))

(defn- clear-pending! []
  (let [s (get-state)]
    (js/clearTimeout (.-showTimer s))
    (set! (.-pending s) nil)))

(defn- unwatch! []
  (some-> (.-observer (get-state)) (.disconnect)))

(defn- hide! []
  (let [s (get-state)]
    (clear-pending!)
    (js/clearTimeout (.-hideTimer s))
    (when-let [t (.-open s)]
      (unwatch!)
      (describe! t false)
      (set! (.-open s) nil)
      (set! (.-lastClose s) (now))
      (.remove (.. s -layer -classList) "tooltip-layer--open" "tooltip-layer--instant"))))

(defn- schedule-hide! []
  (let [s (get-state)]
    (js/clearTimeout (.-hideTimer s))
    (when (.-open s)
      (set! (.-hideTimer s)
            (js/setTimeout hide! (if (= (.getAttribute (.-layer s) "data-kind") "card")
                                   card-close-ms
                                   close-ms))))))

(defn- layer! []
  (let [s (get-state)]
    (or (.-layer s)
        (let [layer  (.createElement js/document "div")
              bubble (.createElement js/document "div")]
          (set! (.-id layer) "ui-tooltip")
          (set! (.-className layer) "tooltip-layer")
          (.setAttribute layer "role" "tooltip")
          (set! (.-className bubble) "tooltip-bubble")
          (.appendChild layer bubble)
          (.appendChild (.-body js/document) layer)
          (.addEventListener layer "pointerenter"
                             (fn [_] (js/clearTimeout (.-hideTimer (get-state)))))
          (.addEventListener layer "pointerleave"
                             (fn [e] (when (not= (.-pointerType e) "touch") (schedule-hide!))))
          (set! (.-layer s) layer)
          (set! (.-bubble s) bubble)
          layer))))

(defn- render! [layer trigger]
  (let [tone (.. trigger -dataset -tooltipTone)]
    (.replaceChildren (.-bubble (get-state)) (content-of trigger))
    (.setAttribute layer "data-kind" (if (= (.. trigger -dataset -tooltipKind) "card") "card" "label"))
    ;; the layer's own theme tokens, so composed ui.* content follows it
    (.setAttribute layer "data-theme" (bubble-theme trigger))
    (if tone
      (.setAttribute layer "data-tone" tone)
      (.removeAttribute layer "data-tone"))))

(defn- refresh!
  "Mirror the open trigger again after the app re-rendered it (Replicant,
   Eucalypt): the content is a copy, so it follows changes only this way."
  []
  (let [s (get-state)
        t (.-open s)]
    (when t
      (if (.-isConnected t)
        (do (render! (.-layer s) t)
            (position! (.-layer s) t))
        (hide!)))))

(defn- watch!
  "Observe the open trigger, template included, for re-renders. Started
   after describe! so its own aria-describedby write is not reported."
  [trigger]
  (let [s (get-state)]
    (when-not (.-observer s)
      (set! (.-observer s) (js/MutationObserver. (fn [_] (refresh!)))))
    (.observe (.-observer s) trigger
              #js {:childList true :subtree true :characterData true :attributes true})))

(defn- host!
  "Keep the layer where it renders: inside the fullscreen element while there
   is one (the browser draws only that subtree), else in <body>."
  [layer]
  (let [host (or (.-fullscreenElement js/document) (.-body js/document))]
    (when-not (identical? (.-parentNode layer) host)
      (.appendChild host layer))))

(defn- show! [trigger instant?]
  (let [s     (get-state)
        layer (layer!)
        cl    (.-classList layer)
        prev  (.-open s)]
    (host! layer)
    (clear-pending!)
    (js/clearTimeout (.-hideTimer s))
    (unwatch!)
    (when (and prev (not (identical? prev trigger)))
      (describe! prev false))
    (set! (.-open s) trigger)
    (describe! trigger true)
    (render! layer trigger)
    (watch! trigger)
    (.toggle cl "tooltip-layer--instant" instant?)
    (position! layer trigger)
    (when-not instant?
      ;; restart the enter transition from the closed state
      (.remove cl "tooltip-layer--open")
      (.getBoundingClientRect layer))
    (.add cl "tooltip-layer--open")
    (when instant?
      (js/requestAnimationFrame
       (fn [] (js/requestAnimationFrame
               (fn [] (.remove cl "tooltip-layer--instant"))))))))

(defn- delay-of [trigger]
  (let [host (.closest trigger "[data-tooltip-delay]")
        v    (when host (js/parseFloat (.. host -dataset -tooltipDelay)))]
    (if (and (some? v) (not (js/isNaN v))) v default-delay)))

(defn- warm? [s]
  (or (.-open s) (< (- (now) (.-lastClose s)) skip-ms)))

(defn- enter! [trigger immediate?]
  (let [s (get-state)]
    (js/clearTimeout (.-hideTimer s))
    (when-not (or (identical? (.-open s) trigger)
                  (identical? (.-pending s) trigger)
                  (identical? (.-suppressed s) trigger))
      (clear-pending!)
      (let [d (delay-of trigger)]
        (cond
          (warm? s)                  (show! trigger true)
          (or immediate? (<= d 0))   (show! trigger false)
          :else
          (do (set! (.-pending s) trigger)
              (set! (.-showTimer s) (js/setTimeout (fn [] (show! trigger false)) d))))))))

(defn- leave! [trigger]
  (let [s (get-state)]
    (when (identical? (.-suppressed s) trigger) (set! (.-suppressed s) nil))
    (when (identical? (.-pending s) trigger) (clear-pending!))
    (when (identical? (.-open s) trigger) (schedule-hide!))))

(defn- reposition! []
  (let [s (get-state)
        t (.-open s)]
    (when t
      (if (.-isConnected t)
        (position! (.-layer s) t)
        (hide!)))))

;; ── Delegated events ────────────────────────────────────────────────

(defn- left-trigger? [trigger e]
  (let [to (.-relatedTarget e)]
    (not (and to (.contains trigger to)))))

(defn- on-pointer-over [e]
  (when (not= (.-pointerType e) "touch")
    (when-let [t (trigger-of (.-target e))]
      (enter! t false))))

(defn- on-pointer-out [e]
  (when (not= (.-pointerType e) "touch")
    (when-let [t (trigger-of (.-target e))]
      (when (left-trigger? t e) (leave! t)))))

(defn- on-pointer-down [e]
  (let [s (get-state)
        t (trigger-of (.-target e))]
    (set! (.-pointer s) (.-pointerType e))
    (cond
      ;; pressing a control closes its tooltip until the pointer leaves
      (and t (not= (.-pointerType e) "touch")
           (or (identical? t (.-open s)) (identical? t (.-pending s))))
      (do (hide!) (set! (.-suppressed s) t))

      (and (= (.-pointerType e) "touch") (not t)
           (not (and (.-layer s) (.contains (.-layer s) (.-target e)))))
      (hide!))))

(defn- on-click [e]
  (let [s (get-state)]
    (when (= (.-pointer s) "touch")
      (when-let [t (trigger-of (.-target e))]
        (when-not (or (.matches t interactive) (.querySelector t interactive))
          (if (identical? (.-open s) t) (hide!) (enter! t true)))))))

(defn- on-focus-in [e]
  (when-let [t (trigger-of (.-target e))]
    (when (and (not= (.-pointer (get-state)) "touch")
               (.matches (.-target e) ":focus-visible"))
      (enter! t true))))

(defn- on-focus-out [e]
  (when-let [t (trigger-of (.-target e))]
    (when (left-trigger? t e) (leave! t))))

(defn- on-key-down [e]
  (let [s (get-state)]
    (when (and (= (.-key e) "Escape") (.-open s))
      (let [t (.-open s)]
        (hide!)
        (set! (.-suppressed s) t)))))

(defn init! []
  (when-not (aget js/window "__uiTooltipInit")
    (aset js/window "__uiTooltipInit" true)
    (.addEventListener js/document "pointerover" on-pointer-over)
    (.addEventListener js/document "pointerout" on-pointer-out)
    (.addEventListener js/document "pointerdown" on-pointer-down true)
    (.addEventListener js/document "click" on-click)
    (.addEventListener js/document "focusin" on-focus-in)
    (.addEventListener js/document "focusout" on-focus-out)
    (.addEventListener js/document "keydown" on-key-down)
    (.addEventListener js/window "scroll" reposition! #js {:capture true :passive true})
    (.addEventListener js/window "resize" reposition!)))

(init!)

(aset js/window "__uiTooltip" #js {:hide hide! :reposition reposition!})
