(ns ui.js.time-picker
  "Runtime for [data-ui-time-picker] fields (markup from ui.time-picker).

   Document-level listeners, so server-rendered fields and squint/replicant
   ones work the same:

     - focusing a segment opens its slot list under it: every step of the
       day for the start, from one step after the start for the end, each
       end slot with its length
     - typing shows the slot it means (`1430`, `2:30pm`, `9`) and marks
       unreadable text; ArrowUp/ArrowDown step by one slot; Enter or leaving
       the segment commits, Escape puts the committed time back
     - picking a start keeps the length of an existing range; picking a
       start with no end in range mode moves on to the end
     - the clear button empties the time

   A commit writes `HH:MM` / `HH:MM-HH:MM` (or empty) to the hidden
   [data-ui-time-value] input, repaints the segments and the length, and
   fires `input` and `change` on that input, so controlled squint/replicant
   components re-render through their :on-change and hiccup forms submit it."
  (:require [ui.time-picker :as tp]))

;; ── State (on window, like the other runtimes) ──────────────────────

(defn- state []
  (or (aget js/window "__uiTimeState")
      (let [s #js {:list nil :seg nil :field nil :hint nil}]
        (aset js/window "__uiTimeState" s)
        s)))

;; ── Field access ────────────────────────────────────────────────────

(defn- closest [node sel]
  (when (and node (.-closest node)) (.closest node sel)))

(defn- live-field [node]
  (when-let [el (closest node "[data-ui-time-picker]")]
    (when-not (.hasAttribute el "data-ui-time-disabled") el)))

(defn- hidden-of [el] (.querySelector el "[data-ui-time-value]"))

(defn- seg-of [el which] (.querySelector el (str "[data-ui-time-seg=\"" which "\"]")))

(defn- which-of [seg] (.getAttribute seg "data-ui-time-seg"))

(defn- step-of [el]
  (let [n (js/parseInt (.getAttribute el "data-step") 10)]
    (if (and (not (js/isNaN n)) (pos? n)) n tp/default-step)))

(defn- range? [el] (.contains (.-classList el) "time-picker--range"))

(defn- current [el] (tp/parse-value (some-> (hidden-of el) .-value)))

;; ── Painting and committing ─────────────────────────────────────────

(defn- paint! [el v]
  (let [s (:start v) e (:end v)]
    (when-let [seg (seg-of el "start")] (set! (.-value seg) (or (tp/format-time s) "")))
    (when-let [seg (seg-of el "end")] (set! (.-value seg) (or (tp/format-time e) "")))
    (when-let [l (.querySelector el "[data-ui-time-length]")]
      (set! (.-textContent l) (or (when (and (some? s) (some? e)) (tp/format-length (- e s))) "")))
    (.forEach (.querySelectorAll el "[data-ui-time-seg]")
              (fn [seg] (.remove (.-classList seg) "time-picker-seg--invalid")))
    (.toggle (.-classList el) "time-picker--empty" (nil? s))))

(defn- fire! [input type]
  (.dispatchEvent input (js/Event. type #js {:bubbles true})))

(defn- commit! [el v]
  (let [input (hidden-of el)
        value (tp/value-str (:start v) (:end v))]
    (paint! el (tp/parse-value value))
    (when (and input (not= value (.-value input)))
      (set! (.-value input) value)
      (fire! input "input")
      (fire! input "change"))))

;; ── Slot list ───────────────────────────────────────────────────────

(defn- slots-for [el which]
  (let [v (current el) step (step-of el)]
    (if (= which "end")
      (tp/end-slots (:start v) step)
      (tp/start-slots step))))

(defn- slot-el [list m]
  (.querySelector list (str "[data-m=\"" m "\"]")))

(defn- center! [list node]
  (when node
    (set! (.-scrollTop list)
          (- (+ (.-offsetTop node) (/ (.-offsetHeight node) 2)) (/ (.-clientHeight list) 2)))))

(defn- mark-hint! [m]
  (let [st (state) list (.-list st)]
    (set! (.-hint st) m)
    (when list
      (.forEach (.querySelectorAll list ".time-picker-slot--hint")
                (fn [n] (.remove (.-classList n) "time-picker-slot--hint")))
      (when (some? m)
        (when-let [n (slot-el list m)]
          (.add (.-classList n) "time-picker-slot--hint")
          (center! list n))))))

(defn close! []
  (let [st (state)]
    (when (.-list st) (.remove (.-list st)))
    (when (.-seg st) (.setAttribute (.-seg st) "aria-expanded" "false"))
    (set! (.-list st) nil)
    (set! (.-seg st) nil)
    (set! (.-field st) nil)
    (set! (.-hint st) nil)))

(defn pick!
  "Commit `m` minutes as the `which` (\"start\"/\"end\") time of field `el`."
  [el which m]
  (let [v  (current el)
        nv (if (= which "end")
             {:start (:start v) :end m}
             (tp/move-start v m))]
    (close!)
    (commit! el nv)
    (let [seg (seg-of el which)]
      (if (and (= which "start") (range? el) (nil? (:end nv)) (seg-of el "end"))
        (.focus (seg-of el "end"))
        (when seg (.blur seg))))))

(defn- position! [list seg]
  (let [r  (.getBoundingClientRect seg)
        _  (.appendChild js/document.body list)
        lr (.getBoundingClientRect list)
        vw (.-innerWidth js/window)
        vh (.-innerHeight js/window)
        left  (js/Math.max 8 (js/Math.min (- (.-left r) 8) (- vw (.-width lr) 8)))
        below (+ (.-bottom r) 6)
        top   (if (> (+ below (.-height lr)) vh)
                (js/Math.max 8 (- (.-top r) (.-height lr) 6))
                below)]
    (set! (.. list -style -left) (str left "px"))
    (set! (.. list -style -top) (str top "px"))))

(defn- open! [el seg]
  (let [which (which-of seg)
        v     (current el)
        st    (state)]
    (when-not (and (= seg (.-seg st)) (.-list st))
      (close!)
      (when-not (and (= which "end") (nil? (:start v)))
        (let [list (.createElement js/document "div")
              sel  (if (= which "end") (:end v) (:start v))]
          (set! (.-className list) "time-picker-list")
          (.setAttribute list "role" "listbox")
          (.setAttribute list "aria-label" (if (= which "end") "End times" "Start times"))
          (doseq [m (slots-for el which)]
            (let [b (.createElement js/document "button")]
              (set! (.-className b) (if (= m sel) "time-picker-slot time-picker-slot--selected" "time-picker-slot"))
              (.setAttribute b "type" "button")
              (.setAttribute b "tabindex" "-1")
              (.setAttribute b "role" "option")
              (.setAttribute b "aria-selected" (if (= m sel) "true" "false"))
              (.setAttribute b "data-m" (str m))
              (.append b (tp/format-time m))
              (when (= which "end")
                (let [l (.createElement js/document "span")]
                  (set! (.-className l) "time-picker-slot-length")
                  (set! (.-textContent l) (tp/format-length (- m (:start v))))
                  (.append b l)))
              (.append list b)))
          ;; keep the focus in the segment: a tap on the list never blurs it
          (.addEventListener list "mousedown" (fn [e] (.preventDefault e)))
          (.addEventListener list "click"
                             (fn [e]
                               (when-let [b (closest (.-target e) "[data-m]")]
                                 (.preventDefault e)
                                 (pick! el which (js/parseInt (.getAttribute b "data-m") 10)))))
          (position! list seg)
          (set! (.-list st) list)
          (set! (.-seg st) seg)
          (set! (.-field st) el)
          (.setAttribute seg "aria-expanded" "true")
          (center! list (or (slot-el list sel)
                            (slot-el list (if (= which "end") (+ (:start v) 60) 540))
                            (.-firstChild list))))))))


;; ── Typed text ──────────────────────────────────────────────────────

(defn- typed
  "Minutes the segment's text means, :blank, or nil when unreadable (an end
   must come after the start)."
  [el seg]
  (let [text (.trim (.-value seg))]
    (if (= text "")
      :blank
      (let [m (tp/parse-time text)
            s (:start (current el))]
        (if (and (some? m) (= (which-of seg) "end") (or (nil? s) (<= m s)))
          nil
          m)))))

(defn- settle!
  "Leaving a segment: commit what it reads, clear a start left empty, or put
   the committed time back."
  [el seg]
  (let [which (which-of seg)
        v     (current el)
        t     (typed el seg)
        was   (if (= which "end") (:end v) (:start v))]
    (cond
      (= t :blank) (if (= which "end")
                     (commit! el {:start (:start v) :end nil})
                     (commit! el {:start nil :end nil}))
      (nil? t)     (paint! el v)
      (not= t was) (commit! el (if (= which "end") {:start (:start v) :end t} (tp/move-start v t)))
      :else        (paint! el v))))

;; ── Listeners ───────────────────────────────────────────────────────

(defn- seg-target [e]
  (when-let [seg (closest (.-target e) "[data-ui-time-seg]")]
    (when-let [el (live-field seg)] #js [el seg])))

(defn- on-focusin [e]
  (when-let [hit (seg-target e)]
    (let [[el seg] hit]
      (open! el seg)
      (.select seg))))

(defn- on-focusout [e]
  (when-let [hit (seg-target e)]
    (let [[el seg] hit]
      (when (= seg (.-seg (state))) (close!))
      (settle! el seg))))

(defn- on-input [e]
  (when-let [hit (seg-target e)]
    (let [[el seg] hit
          t (typed el seg)]
      (open! el seg)
      (.toggle (.-classList seg) "time-picker-seg--invalid" (nil? t))
      (mark-hint! (when (number? t) t)))))

(defn- step-hint! [el seg dir]
  (let [which (which-of seg)
        slots (slots-for el which)
        v     (current el)
        st    (state)
        base  (or (.-hint st)
                  (let [t (typed el seg)] (when (number? t) t))
                  (if (= which "end") (:end v) (:start v)))
        ;; nothing yet: the first press lands where the list opened
        idx   (if (some? base)
                (let [i (.findIndex slots (fn [m] (>= m base)))
                      i (if (< i 0) (count slots) i)
                      exact (= (nth slots i nil) base)]
                  (if (pos? dir) (if exact (inc i) i) (dec i)))
                (let [d (if (= which "end") (+ (:start v) 60) 540)
                      i (.findIndex slots (fn [m] (>= m d)))]
                  (if (< i 0) 0 i)))
        m     (nth slots (max 0 (min (dec (count slots)) idx)) nil)]
    (when (some? m)
      (open! el seg)
      (set! (.-value seg) (tp/format-time m))
      (.remove (.-classList seg) "time-picker-seg--invalid")
      (mark-hint! m))))

(defn- on-keydown [e]
  (when-let [hit (seg-target e)]
    (let [[el seg] hit
          key (.-key e)
          open? (some? (.-list (state)))]
      (cond
        (= key "Enter")
        (do (.preventDefault e)
            (let [t (typed el seg) which (which-of seg)]
              (if (number? t)
                (pick! el which t)
                (do (close!) (settle! el seg)))))

        (and (= key "Escape") open?)
        (do (.preventDefault e)
            (.stopPropagation e)
            (paint! el (current el))
            (close!))

        (= key "ArrowDown") (do (.preventDefault e) (step-hint! el seg 1))
        (= key "ArrowUp")   (do (.preventDefault e) (step-hint! el seg -1))))))

(defn- on-click [e]
  (let [t (.-target e)]
    (if-let [btn (closest t "[data-ui-time-clear]")]
      (when-let [el (live-field btn)]
        (.preventDefault e)
        (close!)
        (commit! el {:start nil :end nil}))
      ;; a click on the focused segment after Escape opens its list again
      (when-let [hit (seg-target e)]
        (let [[el seg] hit] (open! el seg))))))

(defn- on-scroll [e]
  (let [list (.-list (state))]
    (when (and list (not (.contains list (.-target e))))
      (close!))))

(defn init! []
  (.addEventListener js/document "focusin" on-focusin)
  (.addEventListener js/document "focusout" on-focusout)
  (.addEventListener js/document "input" on-input)
  (.addEventListener js/document "keydown" on-keydown true)
  (.addEventListener js/document "click" on-click)
  (.addEventListener js/window "scroll" on-scroll true)
  (.addEventListener js/window "resize" (fn [_] (close!))))

(aset js/window "__uiTimePicker" #js {:pick pick! :close close!})

(init!)
