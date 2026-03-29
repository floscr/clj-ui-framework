(ns ui.calendar-events
  "Event-aware calendar components. See src/ui/calendar.md for full documentation."
  (:require [clojure.string :as str]
            [ui.calendar :as cal]))

;; In squint, keywords are strings — name is identity
#?(:squint (defn- kw-name [s] s)
   :cljs   (defn- kw-name [s] (name s))
   :clj    (defn- kw-name [s] (name s)))

;; ── Event Data Helpers ──────────────────────────────────────────────
;; Events are maps with:
;;   :title      - string
;;   :date       - "YYYY-MM-DD" string
;;   :time-start - "HH:MM" string or nil
;;   :time-end   - "HH:MM" string or nil
;;   :color      - :accent, :danger, :success, :warning, or nil (default)
;;   :done?      - boolean

(def event-colors #{"accent" "danger" "success" "warning"})

(defn event-color-class
  "Returns the CSS class for an event color."
  [color]
  (let [c (some-> color kw-name)]
    (if (and c (contains? event-colors c))
      (str "cal-event-" c)
      "cal-event-default")))

(defn events-for-date
  "Filter events matching a date string, sorted by time-start."
  [events date-str]
  (let [matching (filterv (fn [evt] (= date-str (:date evt))) events)]
    (sort-by (fn [evt] (or (:time-start evt) "99:99")) matching)))

(defn format-time
  "Format a HH:MM time string for display."
  [time-str]
  (when time-str
    (let [parts (str/split time-str #":")]
      (str (first parts) ":" (second parts)))))

(defn event-time-display
  "Build a time range string like '14:00 – 15:30' or just '14:00'."
  [evt]
  (let [start (:time-start evt)
        end   (:time-end evt)]
    (when start
      (if end
        (str (format-time start) " \u2013 " (format-time end))
        (format-time start)))))

;; ── Class Generation ────────────────────────────────────────────────

(defn event-pill-class-list
  "Returns a vector of CSS class strings for an event pill.
   Options:
     :color - :accent, :danger, :success, :warning, or nil
     :done? - boolean"
  [{:keys [color done?]}]
  (cond-> ["cal-event-pill" (event-color-class color)]
    done? (conj "cal-event-done")))

(defn event-pill-classes
  "Returns a space-joined class string for an event pill."
  [opts]
  (str/join " " (event-pill-class-list opts)))

(defn ticker-day-class-list
  "Returns a vector of CSS class strings for a ticker day.
   Options:
     :today?    - boolean
     :selected? - boolean"
  [{:keys [today? selected?]}]
  (cond-> ["cal-ticker-day"]
    today?    (conj "cal-ticker-today")
    selected? (conj "cal-ticker-selected")))

(defn ticker-day-classes
  "Returns a space-joined class string for a ticker day."
  [opts]
  (str/join " " (ticker-day-class-list opts)))

(defn agenda-event-class-list
  "Returns a vector of CSS class strings for an agenda event row.
   Options:
     :done? - boolean"
  [{:keys [done?]}]
  (cond-> ["cal-agenda-event"]
    done? (conj "cal-agenda-event-done")))

(defn agenda-event-classes
  "Returns a space-joined class string for an agenda event row."
  [opts]
  (str/join " " (agenda-event-class-list opts)))

;; ── Components ──────────────────────────────────────────────────────

(defn event-pill
  "Render a small event chip for use inside calendar day cells.

   Props:
     :event    - event map
     :on-click - click handler (receives event map)"
  [{:keys [event on-click]}]
  (let [title (:title event)
        time-str (event-time-display event)
        color (:color event)
        done? (:done? event)]
    #?(:squint
       [:div {:class (event-pill-classes {:color color :done? done?})
              :on-click (when on-click
                          (fn [e]
                            (.stopPropagation e)
                            (on-click event)))}
        (when time-str
          [:span {:class "cal-event-time"} time-str])
        [:span {:class "cal-event-title"} title]]

       :cljs
       [:div {:class (event-pill-class-list {:color color :done? done?})
              :on (when on-click
                    {:click (fn [e]
                              (.stopPropagation e)
                              (on-click event))})}
        (when time-str
          [:span {:class ["cal-event-time"]} time-str])
        [:span {:class ["cal-event-title"]} title]]

       :clj
       [:div {:class (event-pill-classes {:color color :done? done?})}
        (when time-str
          [:span {:class "cal-event-time"} time-str])
        [:span {:class "cal-event-title"} title]])))

(defn event-day-cell
  "Render a day cell with event pills for the calendar event grid.

   Props:
     :day           - day info map from calendar-days
     :events        - all events (will be filtered to this date)
     :today-str     - YYYY-MM-DD string for today
     :selected-date - YYYY-MM-DD string of selected date
     :on-select     - callback for day selection
     :on-event-click - callback for event click
     :max-visible   - max events to show before '+N more' (default 3)"
  [{:keys [day events today-str selected-date on-select on-event-click max-visible]}]
  (let [{:keys [current-month? date-str]} day
        d           (:day day)
        today?      (= date-str today-str)
        selected?   (= date-str selected-date)
        day-events  (events-for-date events date-str)
        max-vis     (or max-visible 3)
        visible-evts (take max-vis day-events)
        overflow    (- (count day-events) max-vis)
        cls-opts    {:today? today?
                     :selected? selected?
                     :current-month? current-month?}]
    #?(:squint
       [:div {:class (str (cal/day-cell-classes cls-opts) " cal-event-day")
              :on-click (when (and on-select (not (empty? date-str)))
                          (fn [_e] (on-select date-str)))
              :data-date date-str}
        [:div {:class "cal-day-number"} (str d)]
        (into [:div {:class "cal-day-events"}]
              (concat
               (map (fn [evt] (event-pill {:event evt :on-click on-event-click}))
                    visible-evts)
               (when (pos? overflow)
                 [[:div {:class "cal-event-more"} (str "+" overflow " more")]])))]

       :cljs
       [:div {:class (conj (cal/day-cell-class-list cls-opts) "cal-event-day")
              :on (when on-select
                    {:click (fn [_e] (on-select date-str))})
              :data-date date-str}
        [:div {:class ["cal-day-number"]} (str d)]
        (into [:div {:class ["cal-day-events"]}]
              (concat
               (map (fn [evt] (event-pill {:event evt :on-click on-event-click}))
                    visible-evts)
               (when (pos? overflow)
                 [[:div {:class ["cal-event-more"]} (str "+" overflow " more")]])))]

       :clj
       [:div {:class (str (cal/day-cell-classes cls-opts) " cal-event-day")
              :data-date date-str}
        [:div {:class "cal-day-number"} (str d)]
        (into [:div {:class "cal-day-events"}]
              (concat
               (map (fn [evt] (event-pill {:event evt}))
                    visible-evts)
               (when (pos? overflow)
                 [[:div {:class "cal-event-more"} (str "+" overflow " more")]])))])))

(defn calendar-event-grid
  "Render a month grid calendar with events displayed in day cells.

   Props:
     :year           - displayed year
     :month          - displayed month (1-12)
     :today-str      - YYYY-MM-DD string for today
     :selected-date  - YYYY-MM-DD string of selected date
     :events         - vector of event maps
     :on-select      - callback for day selection
     :on-prev-month  - callback for prev month nav
     :on-next-month  - callback for next month nav
     :on-event-click - callback for event click
     :max-visible    - max events per cell (default 3)
     :class          - additional CSS classes
     :attrs          - additional HTML attributes"
  [{:keys [year month today-str selected-date events on-select
           on-prev-month on-next-month on-event-click max-visible
           class attrs]}]
  (let [days (cal/calendar-days year month)]
    #?(:squint
       (let [classes (cond-> "cal cal-has-events" class (str " " class))
             base-attrs (merge {:class classes} attrs)]
         [:div base-attrs
          (cal/calendar-header {:year year :month month
                                :on-prev-month on-prev-month
                                :on-next-month on-next-month})
          (cal/calendar-weekdays {})
          (into [:div {:class "cal-grid cal-grid-events"}]
                (map (fn [day-info]
                       (event-day-cell {:day day-info
                                        :events events
                                        :today-str today-str
                                        :selected-date selected-date
                                        :on-select on-select
                                        :on-event-click on-event-click
                                        :max-visible max-visible}))
                     days))])

       :cljs
       (let [cls ["cal" "cal-has-events"]
             classes (cond-> cls class (conj class))
             base-attrs (merge {:class classes} attrs)]
         [:div base-attrs
          (cal/calendar-header {:year year :month month
                                :on-prev-month on-prev-month
                                :on-next-month on-next-month})
          (cal/calendar-weekdays {})
          (into [:div {:class ["cal-grid" "cal-grid-events"]}]
                (map (fn [day-info]
                       (event-day-cell {:day day-info
                                        :events events
                                        :today-str today-str
                                        :selected-date selected-date
                                        :on-select on-select
                                        :on-event-click on-event-click
                                        :max-visible max-visible}))
                     days))])

       :clj
       (let [classes (cond-> "cal cal-has-events" class (str " " class))
             base-attrs (merge {:class classes} attrs)]
         [:div base-attrs
          (cal/calendar-header {:year year :month month})
          (cal/calendar-weekdays {})
          (into [:div {:class "cal-grid cal-grid-events"}]
                (map (fn [day-info]
                       (event-day-cell {:day day-info
                                        :events events
                                        :today-str today-str
                                        :selected-date selected-date
                                        :max-visible max-visible}))
                     days))]))))

;; ── Day Ticker ──────────────────────────────────────────────────────

(defn ticker-dot
  "Render a small colored dot for an event in the ticker."
  [{:keys [event]}]
  (let [color (:color event)]
    #?(:squint
       [:span {:class (str "cal-ticker-dot " (event-color-class color))}]
       :cljs
       [:span {:class ["cal-ticker-dot" (event-color-class color)]}]
       :clj
       [:span {:class (str "cal-ticker-dot " (event-color-class color))}])))

(defn ticker-day-item
  "Render a single day in the ticker strip.

   Props:
     :date       - YYYY-MM-DD string
     :day-num    - day number
     :day-label  - short day name (e.g. 'Mo')
     :today-str  - YYYY-MM-DD string for today
     :selected   - YYYY-MM-DD string of selected date
     :events     - all events (filtered internally)
     :on-select  - callback for selection"
  [{:keys [date day-num day-label today-str selected events on-select]}]
  (let [today?    (= date today-str)
        selected? (= date selected)
        day-evts  (events-for-date events date)]
    #?(:squint
       [:div {:class (ticker-day-classes {:today? today? :selected? selected?})
              :on-click (when on-select (fn [_e] (on-select date)))}
        [:div {:class "cal-ticker-day-name"} day-label]
        [:div {:class "cal-ticker-day-num"} (str day-num)]
        (into [:div {:class "cal-ticker-dots"}]
              (map (fn [evt] (ticker-dot {:event evt}))
                   (take 4 day-evts)))]

       :cljs
       [:div {:class (ticker-day-class-list {:today? today? :selected? selected?})
              :on (when on-select {:click (fn [_e] (on-select date))})}
        [:div {:class ["cal-ticker-day-name"]} day-label]
        [:div {:class ["cal-ticker-day-num"]} (str day-num)]
        (into [:div {:class ["cal-ticker-dots"]}]
              (map (fn [evt] (ticker-dot {:event evt}))
                   (take 4 day-evts)))]

       :clj
       [:div {:class (ticker-day-classes {:today? today? :selected? selected?})}
        [:div {:class "cal-ticker-day-name"} day-label]
        [:div {:class "cal-ticker-day-num"} (str day-num)]
        (into [:div {:class "cal-ticker-dots"}]
              (map (fn [evt] (ticker-dot {:event evt}))
                   (take 4 day-evts)))])))

(def ^:private weekday-short-names
  ["Mon" "Tue" "Wed" "Thu" "Fri" "Sat" "Sun"])

(defn ticker-strip
  "Render a horizontal scrollable day ticker strip.
   Shows days from the given list with event dot indicators.

   Props:
     :days       - vector of {:date :day-num :day-label} maps
     :today-str  - YYYY-MM-DD string for today
     :selected   - YYYY-MM-DD string of selected date
     :events     - all events
     :on-select  - callback for day selection
     :class      - additional CSS classes
     :attrs      - additional HTML attributes"
  [{:keys [days today-str selected events on-select class attrs]}]
  #?(:squint
     (let [classes (cond-> "cal-ticker" class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       [:div base-attrs
        (into [:div {:class "cal-ticker-scroll"}]
              (map (fn [d]
                     (ticker-day-item {:date (:date d)
                                       :day-num (:day-num d)
                                       :day-label (:day-label d)
                                       :today-str today-str
                                       :selected selected
                                       :events events
                                       :on-select on-select}))
                   days))])

     :cljs
     (let [classes (cond-> ["cal-ticker"] class (conj class))
           base-attrs (merge {:class classes} attrs)]
       [:div base-attrs
        (into [:div {:class ["cal-ticker-scroll"]}]
              (map (fn [d]
                     (ticker-day-item {:date (:date d)
                                       :day-num (:day-num d)
                                       :day-label (:day-label d)
                                       :today-str today-str
                                       :selected selected
                                       :events events
                                       :on-select on-select}))
                   days))])

     :clj
     (let [classes (cond-> "cal-ticker" class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       [:div base-attrs
        (into [:div {:class "cal-ticker-scroll"}]
              (map (fn [d]
                     (ticker-day-item {:date (:date d)
                                       :day-num (:day-num d)
                                       :day-label (:day-label d)
                                       :today-str today-str
                                       :selected selected
                                       :events events}))
                   days))])))

;; ── Agenda List ─────────────────────────────────────────────────────

(defn agenda-event-row
  "Render a single event row in the agenda list.

   Props:
     :event    - event map
     :on-click - click handler"
  [{:keys [event on-click]}]
  (let [title    (:title event)
        time-str (event-time-display event)
        color    (:color event)
        done?    (:done? event)]
    #?(:squint
       [:div {:class (agenda-event-classes {:done? done?})
              :on-click (when on-click (fn [_e] (on-click event)))}
        [:div {:class (str "cal-agenda-dot " (event-color-class color))}]
        [:div {:class "cal-agenda-event-body"}
         (when time-str
           [:div {:class "cal-agenda-event-time"} time-str])
         [:div {:class "cal-agenda-event-title"} title]]]

       :cljs
       [:div {:class (agenda-event-class-list {:done? done?})
              :on (when on-click {:click (fn [_e] (on-click event))})}
        [:div {:class ["cal-agenda-dot" (event-color-class color)]}]
        [:div {:class ["cal-agenda-event-body"]}
         (when time-str
           [:div {:class ["cal-agenda-event-time"]} time-str])
         [:div {:class ["cal-agenda-event-title"]} title]]]

       :clj
       [:div {:class (agenda-event-classes {:done? done?})}
        [:div {:class (str "cal-agenda-dot " (event-color-class color))}]
        [:div {:class "cal-agenda-event-body"}
         (when time-str
           [:div {:class "cal-agenda-event-time"} time-str])
         [:div {:class "cal-agenda-event-title"} title]]])))

(defn agenda-day-group
  "Render a day group in the agenda list with header and event rows.

   Props:
     :date       - YYYY-MM-DD string
     :label      - display label (e.g. 'Today', 'Tomorrow', 'Mon')
     :events     - all events (filtered internally)
     :on-event-click - callback for event click"
  [{:keys [date label events on-event-click]}]
  (let [day-evts (events-for-date events date)]
    (when (seq day-evts)
      #?(:squint
         [:div {:class "cal-agenda-day-group"}
          [:div {:class "cal-agenda-day-header"}
           [:span {:class "cal-agenda-day-label"} label]
           [:span {:class "cal-agenda-day-date"} (str/replace date "-" "/")]]
          (into [:div {:class "cal-agenda-day-events"}]
                (map (fn [evt]
                       (agenda-event-row {:event evt :on-click on-event-click}))
                     day-evts))]

         :cljs
         [:div {:class ["cal-agenda-day-group"]}
          [:div {:class ["cal-agenda-day-header"]}
           [:span {:class ["cal-agenda-day-label"]} label]
           [:span {:class ["cal-agenda-day-date"]} (str/replace date "-" "/")]]
          (into [:div {:class ["cal-agenda-day-events"]}]
                (map (fn [evt]
                       (agenda-event-row {:event evt :on-click on-event-click}))
                     day-evts))]

         :clj
         [:div {:class "cal-agenda-day-group"}
          [:div {:class "cal-agenda-day-header"}
           [:span {:class "cal-agenda-day-label"} label]
           [:span {:class "cal-agenda-day-date"} (str/replace date "-" "/")]]
          (into [:div {:class "cal-agenda-day-events"}]
                (map (fn [evt]
                       (agenda-event-row {:event evt}))
                     day-evts))]))))

(defn agenda-list
  "Render the full agenda list view with grouped events by day.

   Props:
     :days           - vector of {:date :label} maps for days to show
     :events         - all events
     :on-event-click - callback for event click
     :class          - additional CSS classes
     :attrs          - additional HTML attributes"
  [{:keys [days events on-event-click class attrs]}]
  (let [groups (keep (fn [d]
                       (agenda-day-group {:date (:date d)
                                          :label (:label d)
                                          :events events
                                          :on-event-click on-event-click}))
                     days)
        empty? (not (seq groups))]
    #?(:squint
       (let [classes (cond-> "cal-agenda-list" class (str " " class))
             base-attrs (merge {:class classes} attrs)]
         (if empty?
           [:div base-attrs
            [:div {:class "cal-agenda-empty"} "No events"]]
           (into [:div base-attrs] groups)))

       :cljs
       (let [classes (cond-> ["cal-agenda-list"] class (conj class))
             base-attrs (merge {:class classes} attrs)]
         (if empty?
           [:div base-attrs
            [:div {:class ["cal-agenda-empty"]} "No events"]]
           (into [:div base-attrs] groups)))

       :clj
       (let [classes (cond-> "cal-agenda-list" class (str " " class))
             base-attrs (merge {:class classes} attrs)]
         (if empty?
           [:div base-attrs
            [:div {:class "cal-agenda-empty"} "No events"]]
           (into [:div base-attrs] groups))))))
