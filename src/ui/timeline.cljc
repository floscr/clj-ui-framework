(ns ui.timeline
  "Motion-Studio-style editing timeline — ruler with ticks, labeled tracks
   with draggable/resizable segment bars, a playhead, click/drag scrubbing
   and an optional floating transport (skip / play / time readout / loop).
   Interactive on :squint only — other targets render a static timeline."
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

;; ── Drag interaction (squint-only) ──────────────────────────────────
;; Drags attach window-level pointer listeners for their lifetime (the
;; original screen-studio approach): no pointer capture to lose when the
;; DOM re-renders mid-drag, and moves are rAF-throttled so a re-render
;; happens at most once per frame.

(defonce ^:private !drag (atom nil))

(defn dragging?
  "True while a segment on `track-id` is being dragged/resized."
  [track-id]
  (when-let [d @!drag]
    (= (:track-id d) track-id)))

(defn- abs* [x] (if (neg? x) (- x) x))

(defn- snap-targets
  "Times worth snapping to while shift-dragging: the playhead plus every
   other segment's start/end."
  [tracks track segment current]
  (concat (when (some? current) [current])
          (mapcat (fn [tr]
                    (mapcat (fn [sg]
                              (when-not (and (= (:id tr) (:id track))
                                             (= (:id sg) (:id segment)))
                                [(:start sg) (:end sg)]))
                            (:segments tr)))
                  (or tracks []))))

(defn- snap
  "Return the target within tol closest to t, or t if none is in range."
  [t targets tol]
  (let [best (reduce (fn [best tg]
                       (if (and (<= (abs* (- tg t)) tol)
                                (or (nil? best)
                                    (< (abs* (- tg t)) (abs* (- best t)))))
                         tg
                         best))
                     nil
                     targets)]
    (if (some? best) best t)))

(defn- drag-listen!
  "Attach window pointermove/pointerup listeners for the lifetime of one
   drag. `on-move` is called at most once per animation frame with the
   latest event; `on-end` once with the release event."
  [on-move on-end]
  #?(:squint
     (let [!raf  (atom nil)
           !last (atom nil)
           move  (fn [e]
                   (.preventDefault e)
                   (reset! !last e)
                   (when-not @!raf
                     (reset! !raf (js/requestAnimationFrame
                                   (fn []
                                     (reset! !raf nil)
                                     (on-move @!last))))))
           up    (fn up* [e]
                   (js/window.removeEventListener "pointermove" move)
                   (js/window.removeEventListener "pointerup" up*)
                   (js/window.removeEventListener "pointercancel" up*)
                   (when-let [r @!raf]
                     (js/cancelAnimationFrame r)
                     (reset! !raf nil))
                   (on-end e))]
       (js/window.addEventListener "pointermove" move)
       (js/window.addEventListener "pointerup" up)
       (js/window.addEventListener "pointercancel" up))))

(defn- start-drag!
  "Begin dragging a bar (:move) or one of its edges (:start / :end). Fires
   :on-segment-change with :phase :drag on every throttled move and
   :phase :commit on release. Holding shift snaps the dragged edge(s) to
   other segments' edges and the playhead."
  [ev mode track segment {:keys [duration min-gap on-segment-change tracks current]}]
  (.preventDefault ev)
  (.stopPropagation ev)
  (let [el      (.-currentTarget ev)
        lane    (.closest el ".ui-timeline-lane")
        rect    (.getBoundingClientRect lane)
        x0      (.-clientX ev)
        start0  (:start segment)
        end0    (:end segment)
        len     (- end0 start0)
        gap     (or min-gap 0)
        spp     (/ (total-time duration) (max (.-width rect) 1))
        targets (snap-targets tracks track segment current)
        tol     (* 8 spp)
        bounds  (fn [e]
                  (let [dt    (* (- (.-clientX e) x0) spp)
                        snap? (and (.-shiftKey e) (seq targets))]
                    (cond
                      (= mode :move)
                      (let [s0 (+ start0 dt)
                            e0 (+ s0 len)
                            d  (if snap?
                                 ;; shift the whole bar by whichever edge is
                                 ;; closest to a snap target within tolerance
                                 (let [best (reduce (fn [best d]
                                                      (if (and (<= (abs* d) tol)
                                                               (or (nil? best)
                                                                   (< (abs* d) (abs* best))))
                                                        d
                                                        best))
                                                    nil
                                                    (concat (mapv #(- % s0) targets)
                                                            (mapv #(- % e0) targets)))]
                                   (if (some? best) best 0))
                                 0)
                            s  (clamp (+ s0 d) 0 (- duration len))]
                        [s (+ s len) nil])

                      (= mode :start)
                      (let [s (cond-> (+ start0 dt) snap? (snap targets tol))]
                        [(clamp s 0 (- end0 gap)) end0 :start])

                      :else
                      (let [e* (cond-> (+ end0 dt) snap? (snap targets tol))]
                        [start0 (clamp e* (+ start0 gap) duration) :end]))))
        fire    (fn [e phase]
                  (when on-segment-change
                    (let [[s e* edge] (bounds e)]
                      (on-segment-change {:track-id   (:id track)
                                          :segment-id (:id segment)
                                          :start      s
                                          :end        e*
                                          :edge       edge
                                          :phase      phase}))))]
    (when (= mode :move)
      (.focus el))
    (reset! !drag {:track-id (:id track)})
    (drag-listen! (fn [e] (fire e :drag))
                  (fn [e]
                    (fire e :commit)
                    (reset! !drag nil)))))

(defn- ruler-scrub!
  "Seek from a pointerdown on the ruler, then keep seeking while the
   pointer moves (window listeners) until release."
  [ev {:keys [duration on-seek]}]
  (.preventDefault ev)
  (let [el   (.-currentTarget ev)
        rect (.getBoundingClientRect el)
        seek (fn [e]
               (let [frac (/ (- (.-clientX e) (.-left rect))
                             (max (.-width rect) 1))]
                 (on-seek (clamp (* frac (total-time duration)) 0 duration))))]
    (.focus el)
    (seek ev)
    (drag-listen! seek (fn [_] nil))))

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
  (let [label (or (:label segment) (:label track) "Segment")]
    [[:button {:class          "ui-timeline-bar"
               :style          {:left  (time->pct (:start segment) duration)
                                :width (span->pct (:start segment) (:end segment) duration)}
               :title          label
               :aria-label     (str "Move " label)
               :on-pointerdown (fn [ev] (start-drag! ev :move track segment p))
               :on-keydown     (bar-keydown track segment p)}
      (when (:label segment)
        [:span {:class "ui-timeline-bar-label"} (:label segment)])]
     [:div {:class          "ui-timeline-handle ui-timeline-handle-start"
            :style          {:left (time->pct (:start segment) duration)}
            :on-pointerdown (fn [ev] (start-drag! ev :start track segment p))}]
     [:div {:class          "ui-timeline-handle ui-timeline-handle-end"
            :style          {:left (time->pct (:end segment) duration)}
            :on-pointerdown (fn [ev] (start-drag! ev :end track segment p))}]]))

(defn- track-el [track p]
  [:div (cond-> {:class "ui-timeline-track"}
          (dragging? (:id track)) (assoc :data-editing "true"))
   [:div {:class "ui-timeline-track-label"} (:label track)]
   (into [:div {:class "ui-timeline-lane"}]
         (mapcat (fn [segment] (segment-els track segment p))
                 (:segments track)))])

(defn- ruler-el [{:keys [duration current on-seek] :as p}]
  (into [:div (cond-> {:class         "ui-timeline-ruler"
                       :role          "slider"
                       :tabindex      "0"
                       :aria-label    "Playhead"
                       :aria-valuemin "0"
                       :aria-valuemax (str duration)
                       :aria-valuenow (str (or current 0))}
                on-seek (assoc :on-pointerdown (fn [ev] (ruler-scrub! ev p))
                               :on-keydown (ruler-keydown p)))]
        (mapv (fn [t]
                [:div {:class "ui-timeline-tick"
                       :style {:left (time->pct t duration)}}
                 [:span {:class "ui-timeline-tick-label"}
                  (format-time t duration)]])
              (ticks duration))))

(defn- transport-el
  [{:keys [current duration playing looping on-play-pause on-skip-start on-loop]}]
  [:div {:class "ui-timeline-transport"}
   [:button (cond-> {:class "ui-timeline-transport-btn"
                     :title "Skip to start"}
              on-skip-start (assoc :on-click on-skip-start))
    (icon/icon {:icon-name :skip-back :size :sm})]
   [:button (cond-> {:class "ui-timeline-transport-btn ui-timeline-play"
                     :title (if playing "Pause" "Play")}
              on-play-pause (assoc :on-click on-play-pause))
    (icon/icon {:icon-name (if playing :pause :play) :size :sm :filled true})]
   [:output {:class "ui-timeline-time"}
    (format-readout current duration)]
   (when on-loop
     [:button {:class        (str "ui-timeline-transport-btn"
                                  (when looping " ui-timeline-loop-active"))
               :title        "Loop"
               :aria-pressed (if looping "true" "false")
               :on-click     on-loop}
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
                          on release (persist on :commit). Holding shift while
                          dragging snaps to other segments' edges and the
                          playhead
     :on-play-pause     - transport play/pause click handler
     :on-skip-start     - transport skip-to-start click handler
     :on-loop           - transport loop toggle click handler (button only
                          rendered when provided)
     :class             - additional CSS classes
     :attrs             - additional HTML attributes

   The component is controlled: it renders purely from props and reports
   edits through the callbacks (interactive on :squint only)."
  [{:keys [duration current tracks playing transport min-gap step big-step
           on-seek on-segment-change on-play-pause on-skip-start on-loop
           class attrs]
    :as props}]
  (let [duration (or duration 0)
        current  (or current 0)
        p        {:duration          duration
                  :current           current
                  :tracks            tracks
                  :min-gap           min-gap
                  :step              step
                  :big-step          big-step
                  :on-seek           on-seek
                  :on-segment-change on-segment-change}]
    [:div (merge {:class (str/join " " (cond-> (timeline-class-list {})
                                         class (conj class)))}
                 attrs)
     [:div {:class "ui-timeline-tracks"}
      (ruler-el p)
      (into [:div {:class "ui-timeline-track-list"}]
            (mapv (fn [track] (track-el track p)) (or tracks [])))
      [:div {:class "ui-timeline-overlay"}
       [:div {:class "ui-timeline-playhead"
              :style {:left (time->pct current duration)}}]]]
     (when-not (false? transport)
       (transport-el {:current       current
                      :duration      duration
                      :playing       playing
                      :looping       (:loop props)
                      :on-play-pause on-play-pause
                      :on-skip-start on-skip-start
                      :on-loop       on-loop}))]))
