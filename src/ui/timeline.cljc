(ns ui.timeline
  "Motion-Studio-style editing timeline — ruler with ticks, labeled tracks
   with draggable/resizable segment bars, a playhead, click/drag scrubbing
   and an optional floating transport (skip / play / time readout / loop)."
  (:require [clojure.string :as str]
            [ui.icon :as icon]))

;; ── Time helpers (pure, testable) ───────────────────────────────────

(defn total-time
  "Full ruler extent in seconds — the duration plus ~5% end padding,
   matching Motion Studio's ruler overshoot (duration sits at 95%)."
  [duration]
  (/ (max (or duration 0) 0.001) 0.95))

(def tick-candidates
  "Tick step candidates in seconds, smallest first."
  [0.1 0.2 0.5 1 2 5 10 15 30 60 120 300 600])

(defn tick-step
  "Smallest candidate step that yields at most ~8 ticks over the ruler."
  [duration]
  (let [total (total-time duration)]
    (or (some (fn [c] (when (<= (/ total c) 8.001) c)) tick-candidates)
        (peek tick-candidates))))

(defn ticks
  "Tick times (seconds) from 0 to the end of the ruler, inclusive."
  [duration]
  (let [total (total-time duration)
        step  (tick-step duration)
        n     (int (Math/floor (+ (/ total step) 1e-9)))]
    (mapv (fn [i] (* i step)) (range (inc n)))))

(defn time->pct
  "CSS percentage string positioning time `t` on the ruler."
  [t duration]
  (str (* 100 (/ (or t 0) (total-time duration))) "%"))

(defn span->pct
  "CSS percentage string for the width of the span [a b] on the ruler."
  [a b duration]
  (str (* 100 (/ (- b a) (total-time duration))) "%"))

(defn- fixed2 [x]
  #?(:squint (.toFixed x 2)
     :cljs   (.toFixed x 2)
     :clj    (format "%.2f" (double x))))

(defn- mmss [t]
  (let [t (int (Math/floor (max (or t 0) 0)))
        m (quot t 60)
        s (rem t 60)]
    (str m ":" (when (< s 10) "0") s)))

(defn format-time
  "Format a time for tick labels: `m:ss` for long timelines (duration ≥ 60s),
   otherwise seconds with up to two decimals, trailing zeros trimmed."
  [t duration]
  (if (>= duration 60)
    (mmss t)
    (let [s (str/replace (fixed2 t) #"\.?0+$" "")]
      (if (= s "") "0" s))))

(defn format-readout
  "Transport time readout, e.g. \"0.79 / 1.52s\" or \"1:23 / 4:56\"."
  [current duration]
  (if (>= duration 60)
    (str (mmss current) " / " (mmss duration))
    (str (fixed2 (or current 0)) " / " (fixed2 duration) "s")))

(defn- clamp [x lo hi]
  (max lo (min hi x)))

;; ── Class helpers ───────────────────────────────────────────────────

(defn timeline-class-list
  "Returns a vector of CSS class strings for the timeline container."
  [_opts]
  ["ui-timeline"])

(defn timeline-classes
  "Returns a space-joined class string for the timeline."
  [opts]
  (str/join " " (timeline-class-list opts)))

;; ── Per-target shims ────────────────────────────────────────────────

(defn- cls
  "Target-appropriate :class value from a vector of base classes plus an
   optional extra class string."
  [bases class]
  #?(:squint (str/join " " (cond-> (vec bases) class (conj class)))
     :cljs   (cond-> (vec bases)
               class (into (remove str/blank? (str/split (str class) #"\s+"))))
     :clj    (str/join " " (cond-> (vec bases) class (conj class)))))

(defn- sty
  "Target-appropriate :style value from a small (array-)map."
  [m]
  #?(:squint m
     :cljs   m
     :clj    (str/join "; " (map (fn [[k v]] (str (name k) ": " v)) m))))

(defn- evt-attrs
  "Target-appropriate event attributes. Accepts a (possibly nil) map with
   :pointerdown :pointermove :pointerup :pointercancel :keydown :click.
   Renders no handlers on :clj (static hiccup)."
  [{:keys [pointerdown pointermove pointerup pointercancel keydown click]}]
  #?(:squint
     (cond-> {}
       pointerdown   (assoc :on-pointerdown pointerdown)
       pointermove   (assoc :on-pointermove pointermove)
       pointerup     (assoc :on-pointerup pointerup)
       pointercancel (assoc :on-pointercancel pointercancel)
       keydown       (assoc :on-keydown keydown)
       click         (assoc :on-click click))
     :cljs
     (let [m (cond-> {}
               pointerdown   (assoc :pointerdown pointerdown)
               pointermove   (assoc :pointermove pointermove)
               pointerup     (assoc :pointerup pointerup)
               pointercancel (assoc :pointercancel pointercancel)
               keydown       (assoc :keydown keydown)
               click         (assoc :click click))]
       (if (seq m) {:on m} {}))
     :clj {}))

;; ── Drag state & browser handlers (:cljs / :squint only) ────────────
;; These use only host interop and are never invoked in :clj (the hiccup
;; target attaches no handlers), so they compile unconditionally.

(defonce ^:private !drag (atom nil))

(defn dragging?
  "True while a segment on `track-id` is being dragged/resized."
  [track-id]
  (when-let [d @!drag]
    (= (:track-id d) track-id)))

(defn- event->time
  "Map a pointer event on the ruler to a time in seconds, clamped to the
   duration (the last 5% of the ruler snaps to the end)."
  [e duration]
  (let [rect (.getBoundingClientRect (.-currentTarget e))
        frac (/ (- (.-clientX e) (.-left rect)) (max (.-width rect) 1))]
    (clamp (* frac (total-time duration)) 0 duration)))

(defn- start-drag!
  "Begin dragging a bar (:move) or one of its edges (:start / :end)."
  [ev mode track-id segment duration]
  (let [el   (.-currentTarget ev)
        lane (.closest el ".ui-timeline-lane")
        rect (.getBoundingClientRect lane)]
    (.setPointerCapture el (.-pointerId ev))
    (.stopPropagation ev)
    (reset! !drag {:mode        mode
                   :track-id    track-id
                   :segment-id  (:id segment)
                   :x0          (.-clientX ev)
                   :start0      (:start segment)
                   :end0        (:end segment)
                   :sec-per-px  (/ (total-time duration)
                                   (max (.-width rect) 1))})))

(defn- drag-update!
  "Compute the dragged segment's new bounds from the pointer event and fire
   :on-segment-change with the given phase (:drag while moving, :commit on
   release). Resets the drag state after a commit."
  [ev phase {:keys [duration min-gap on-segment-change]}]
  (when-let [d @!drag]
    (let [mode   (:mode d)
          gap    (or min-gap 0)
          dt     (* (- (.-clientX ev) (:x0 d)) (:sec-per-px d))
          start0 (:start0 d)
          end0   (:end0 d)
          len    (- end0 start0)
          move?  (= mode :move)
          start? (= mode :start)
          s      (cond
                   move?  (clamp (+ start0 dt) 0 (- duration len))
                   start? (clamp (+ start0 dt) 0 (- end0 gap))
                   :else  start0)
          e      (cond
                   move?  (+ s len)
                   start? end0
                   :else  (clamp (+ end0 dt) (+ start0 gap) duration))]
      (when on-segment-change
        (on-segment-change {:track-id   (:track-id d)
                            :segment-id (:segment-id d)
                            :start      s
                            :end        e
                            :edge       (cond start? :start move? nil :else :end)
                            :phase      phase}))
      (when (= phase :commit)
        (reset! !drag nil)))))

(defn- bar-keydown
  "Arrow keys nudge the whole segment by :step (shift: :big-step)."
  [track segment {:keys [duration step big-step on-segment-change]}]
  (fn [ev]
    (let [k     (.-key ev)
          delta (cond (= k "ArrowLeft") -1 (= k "ArrowRight") 1 :else nil)]
      (when (and delta on-segment-change)
        (.preventDefault ev)
        (let [amt (* delta (if (.-shiftKey ev) (or big-step 1) (or step 0.1)))
              len (- (:end segment) (:start segment))
              s   (clamp (+ (:start segment) amt) 0 (- duration len))]
          (on-segment-change {:track-id   (:id track)
                              :segment-id (:id segment)
                              :start      s
                              :end        (+ s len)
                              :edge       nil
                              :phase      :commit}))))))

(defn- ruler-keydown
  "Arrow keys seek by :step (shift: :big-step); Home/End jump."
  [{:keys [duration current step big-step on-seek]}]
  (fn [ev]
    (when on-seek
      (let [k (.-key ev)
            s (if (.-shiftKey ev) (or big-step 1) (or step 0.1))
            t (cond
                (= k "ArrowLeft")  (- (or current 0) s)
                (= k "ArrowRight") (+ (or current 0) s)
                (= k "Home")       0
                (= k "End")        duration
                :else              nil)]
        (when (some? t)
          (.preventDefault ev)
          (on-seek (clamp t 0 duration)))))))

;; ── Sub-elements ────────────────────────────────────────────────────

(defn- segment-els
  "Bar button + two resize handles for one segment. Returns a seq of three
   elements to splice into the lane."
  [track segment {:keys [duration] :as p}]
  (let [tid    (:id track)
        label  (or (:label segment) (:label track) "Segment")
        drag-h (fn [mode extra]
                 (evt-attrs
                  (merge
                   {:pointerdown   (fn [ev] (start-drag! ev mode tid segment duration))
                    :pointermove   (fn [ev] (when (pos? (.-buttons ev))
                                              (drag-update! ev :drag p)))
                    :pointerup     (fn [ev] (drag-update! ev :commit p))
                    :pointercancel (fn [ev] (drag-update! ev :commit p))}
                   extra)))
        handle (fn [mode]
                 [:div (merge {:class (cls ["ui-timeline-handle"
                                            (if (= mode :start)
                                              "ui-timeline-handle-start"
                                              "ui-timeline-handle-end")]
                                           nil)
                               :style (sty {:left (time->pct
                                                   (if (= mode :start)
                                                     (:start segment)
                                                     (:end segment))
                                                   duration)})}
                              (drag-h mode nil))])]
    [[:button (merge {:class      (cls ["ui-timeline-bar"] nil)
                      :style      (sty {:left  (time->pct (:start segment) duration)
                                        :width (span->pct (:start segment) (:end segment) duration)})
                      :title      label
                      :aria-label (str "Move " label)}
                     (drag-h :move {:keydown (bar-keydown track segment p)}))
      (when (:label segment)
        [:span {:class (cls ["ui-timeline-bar-label"] nil)} (:label segment)])]
     (handle :start)
     (handle :end)]))

(defn- track-el [track p]
  [:div (cond-> {:class (cls ["ui-timeline-track"] nil)}
          (dragging? (:id track)) (assoc :data-editing "true"))
   [:div {:class (cls ["ui-timeline-track-label"] nil)} (:label track)]
   (into [:div {:class (cls ["ui-timeline-lane"] nil)}]
         (mapcat (fn [segment] (segment-els track segment p))
                 (:segments track)))])

(defn- ruler-el [{:keys [duration current on-seek] :as p}]
  (into [:div (merge {:class         (cls ["ui-timeline-ruler"] nil)
                      :role          "slider"
                      :tabindex      "0"
                      :aria-label    "Playhead"
                      :aria-valuemin "0"
                      :aria-valuemax (str duration)
                      :aria-valuenow (str (or current 0))}
                     (evt-attrs
                      (when on-seek
                        {:pointerdown (fn [ev]
                                        (.setPointerCapture (.-currentTarget ev) (.-pointerId ev))
                                        (on-seek (event->time ev duration)))
                         :pointermove (fn [ev]
                                        (when (pos? (.-buttons ev))
                                          (on-seek (event->time ev duration))))
                         :keydown     (ruler-keydown p)})))]
        (mapv (fn [t]
                [:div {:class (cls ["ui-timeline-tick"] nil)
                       :style (sty {:left (time->pct t duration)})}
                 [:span {:class (cls ["ui-timeline-tick-label"] nil)}
                  (format-time t duration)]])
              (ticks duration))))

(defn- transport-el
  [{:keys [current duration playing looping on-play-pause on-skip-start on-loop]}]
  [:div {:class (cls ["ui-timeline-transport"] nil)}
   [:button (merge {:class (cls ["ui-timeline-transport-btn"] nil)
                    :title "Skip to start"}
                   (evt-attrs (when on-skip-start {:click on-skip-start})))
    (icon/icon {:icon-name :skip-back :size :sm})]
   [:button (merge {:class (cls ["ui-timeline-transport-btn" "ui-timeline-play"] nil)
                    :title (if playing "Pause" "Play")}
                   (evt-attrs (when on-play-pause {:click on-play-pause})))
    (icon/icon {:icon-name (if playing :pause :play) :size :sm :filled true})]
   [:output {:class (cls ["ui-timeline-time"] nil)}
    (format-readout current duration)]
   (when on-loop
     [:button (merge {:class (cls (cond-> ["ui-timeline-transport-btn"]
                                    looping (conj "ui-timeline-loop-active"))
                                  nil)
                      :title "Loop"
                      :aria-pressed (if looping "true" "false")}
                     (evt-attrs {:click on-loop}))
      (icon/icon {:icon-name :repeat :size :sm})])])

;; ── Component ───────────────────────────────────────────────────────

(defn timeline
  "Render an editing timeline: a tick ruler you can scrub, labeled tracks
   whose segment bars can be dragged and trimmed, a playhead, and an
   optional floating transport.

   Props:
     :duration          - total media duration in seconds (required)
     :current           - playhead time in seconds
     :tracks            - vector of {:id :label :segments [{:id :start :end :label}]}
                          (:start/:end in seconds)
     :playing           - boolean, playback active (transport play button)
     :loop              - boolean, loop active (transport loop button)
     :transport         - set false to hide the floating transport
     :min-gap           - minimum segment length in seconds when trimming (default 0)
     :step / :big-step  - keyboard nudge amounts in seconds (default 0.1 / 1)
     :on-seek           - (fn [t]) — ruler click/drag scrub and keyboard seek
     :on-segment-change - (fn [{:keys [track-id segment-id start end edge phase]}])
                          :edge is :start/:end when trimming, nil when moving;
                          :phase is :drag while the pointer moves and :commit
                          on release (persist on :commit)
     :on-play-pause     - transport play/pause click handler
     :on-skip-start     - transport skip-to-start click handler
     :on-loop           - transport loop toggle click handler (button only
                          rendered when provided)
     :class             - additional CSS classes
     :attrs             - additional HTML attributes

   The component is controlled: it renders purely from props and reports
   edits through the callbacks (handlers are :cljs/:squint only; the :clj
   target renders a static timeline)."
  [{:keys [duration current tracks playing transport min-gap step big-step
           on-seek on-segment-change on-play-pause on-skip-start on-loop
           class attrs]
    :as props}]
  (let [duration (or duration 0)
        current  (or current 0)
        p        {:duration          duration
                  :current           current
                  :min-gap           min-gap
                  :step              step
                  :big-step          big-step
                  :on-seek           on-seek
                  :on-segment-change on-segment-change}]
    [:div (merge {:class (cls (timeline-class-list {}) class)} attrs)
     [:div {:class (cls ["ui-timeline-tracks"] nil)}
      (ruler-el p)
      (into [:div {:class (cls ["ui-timeline-track-list"] nil)}]
            (mapv (fn [track] (track-el track p)) (or tracks [])))
      [:div {:class (cls ["ui-timeline-overlay"] nil)}
       [:div {:class (cls ["ui-timeline-playhead"] nil)
              :style (sty {:left (time->pct current duration)})}]]]
     (when-not (false? transport)
       (transport-el {:current       current
                      :duration      duration
                      :playing       playing
                      :looping       (:loop props)
                      :on-play-pause on-play-pause
                      :on-skip-start on-skip-start
                      :on-loop       on-loop}))]))
