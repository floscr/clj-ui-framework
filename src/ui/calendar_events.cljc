(ns ui.calendar-events
  "Event-aware calendar components. See src/ui/calendar.md for full documentation."
  (:require [clojure.string :as str]
            [ui.util :as util]
            [ui.calendar :as cal]
            [ui.icon :as icon]))

;; ── Event Data Helpers ──────────────────────────────────────────────
;; Events are maps with:
;;   :title      - string
;;   :date       - "YYYY-MM-DD" string
;;   :time-start - "HH:MM" string or nil
;;   :time-end   - "HH:MM" string or nil
;;   :color      - one of event-colors (as keyword or string), or nil (default gray)
;;   :done?      - boolean

(def event-colors
  "Named event colors. The first four are the theme's semantic tokens; the
   rest are the extra categorical palettes (see theme/tokens.edn). Each has a
   matching .cal-event-<name> class with light/dark variants."
  #{"accent" "danger" "success" "warning" "blue" "teal" "pink" "orange"})

(defn event-color-class
  "Returns the CSS class for an event color."
  [color]
  (let [c (some-> color util/kw-name)]
    (if (and c (contains? event-colors c))
      (str "cal-event-" c)
      "cal-event-default")))

(defn events-for-date
  "Filter events matching a date string, sorted by time-start. Untimed
   all-day events sort first (empty string precedes any 'HH:MM')."
  [events date-str]
  (let [matching (filterv (fn [evt] (= date-str (:date evt))) events)]
    (sort-by (fn [evt] (or (:time-start evt) "")) matching)))

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

(defn parse-int*
  "Parse an integer string, cross-target."
  [s]
  #?(:squint (js/parseInt s 10)
     :cljs (js/parseInt s 10)
     :clj (Integer/parseInt s)))

(defn round*
  "Round a number to the nearest integer, cross-target."
  [x]
  #?(:squint (js/Math.round x)
     :cljs (js/Math.round x)
     :clj (Math/round (double x))))

(defn hhmm->minutes
  "Convert a 'HH:MM' string to minutes-from-midnight, or nil."
  [time-str]
  (when time-str
    (let [parts (str/split time-str #":")]
      (+ (* 60 (parse-int* (first parts)))
         (parse-int* (second parts))))))

(defn event-duration-minutes
  "Duration of an event in minutes. Falls back to 60 when there is a
   start but no end. Returns nil when there is no start time."
  [evt]
  (let [s (hhmm->minutes (:time-start evt))
        e (hhmm->minutes (:time-end evt))]
    (when s
      (if (and e (> e s)) (- e s) 60))))

(defn format-duration
  "Format a minute count as '1h', '1h 30m' or '45m'."
  [mins]
  (when (and mins (pos? mins))
    (let [h (quot mins 60)
          m (rem mins 60)]
      (cond
        (and (pos? h) (pos? m)) (str h "h " m "m")
        (pos? h)                (str h "h")
        :else                   (str m "m")))))

(defn format-hour-label
  "Format an hour (0–24) as a zero-padded 24-hour label like '11:00' or '13:00'."
  [hour]
  (let [h24 (mod hour 24)]
    (str (when (< h24 10) "0") h24 ":00")))

;; ── Class Generation ────────────────────────────────────────────────

(defn event-pill-class-list
  "Returns a vector of CSS class strings for an event pill.
   Options:
     :color  - one of event-colors, or nil
     :done?  - boolean
     :task?  - boolean (render as a checkbox to-do item, not a solid pill)
     :layout - :inline (default) one row: stripe, time, title;
               :stacked a filled block with the muted time range above a
               bold title (roomy desktop month grids)"
  [{:keys [color done? task? layout]}]
  (cond-> ["cal-event-pill" (event-color-class color)]
    (= "stacked" (some-> layout util/kw-name)) (conj "cal-event-pill-stacked")
    task? (conj "cal-event-task")
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

(defn task-check
  "A checkbox glyph for to-do (repeating) calendar items. Checked when done?.
   Pure markup + CSS, so it renders identically across clj/cljs/squint.

   With an optional on-toggle handler the glyph becomes clickable: the click is
   stopped from propagating (so it won't also open the event) and on-toggle is
   called with the DOM event."
  ([done?] (task-check done? nil))
  ([done? on-toggle]
   (let [classes (cond-> ["cal-task-check"]
                   done? (conj "cal-task-check-done")
                   on-toggle (conj "cal-task-check-clickable"))
         click (when on-toggle
                 (fn [e]
                   (.stopPropagation e)
                   (on-toggle e)))]
     #?(:squint [:span (cond-> {:class (str/join " " classes)}
                         click (assoc :on-click click))]
        :cljs [:span (cond-> {:class classes}
                       click (assoc :on-click click))]
        :clj [:span {:class (str/join " " classes)}]))))

(defn event-pill
  "Render a small event chip for use inside calendar day cells.

   Props:
     :event    - event map
     :on-click - click handler (receives event map)
     :on-context-menu - context-menu handler (receives [event dom-event]); wired
                        to right-click and (via ui.js.gestures) long-press
     :layout   - :inline (default) or :stacked (see event-pill-class-list)"
  [{:keys [event on-click on-context-menu layout]}]
  (let [title (:title event)
        time-str (event-time-display event)
        color (:color event)
        done? (:done? event)
        task? (:task? event)]
    #?(:squint
       [:div {:class (event-pill-classes {:color color :done? done? :task? task? :layout layout})
              :on-click (when on-click
                          (fn [e]
                            (.stopPropagation e)
                            (on-click event)))
              :on-contextmenu (when on-context-menu
                                (fn [e]
                                  (.preventDefault e)
                                  (.stopPropagation e)
                                  (on-context-menu event e)))}
        (when task? (task-check done?))
        (when time-str
          [:span {:class "cal-event-time"} time-str])
        [:span {:class "cal-event-title"} title]]

       :cljs
       [:div {:class (event-pill-class-list {:color color :done? done? :task? task? :layout layout})
              :on (cond-> {}
                    on-click (assoc :click (fn [e]
                                             (.stopPropagation e)
                                             (on-click event)))
                    on-context-menu (assoc :contextmenu
                                           (fn [e]
                                             (.preventDefault e)
                                             (.stopPropagation e)
                                             (on-context-menu event e))))}
        (when task? (task-check done?))
        (when time-str
          [:span {:class ["cal-event-time"]} time-str])
        [:span {:class ["cal-event-title"]} title]]

       :clj
       [:div {:class (event-pill-classes {:color color :done? done? :task? task? :layout layout})}
        (when task? (task-check done?))
        (when time-str
          [:span {:class "cal-event-time"} time-str])
        [:span {:class "cal-event-title"} title]])))

(defn event-day-cell
  "Render a day cell for the calendar event grid.

   Props:
     :day           - day info map from calendar-days
     :events        - all events (will be filtered to this date)
     :today-str     - YYYY-MM-DD string for today
     :selected-date - YYYY-MM-DD string of selected date
     :on-select     - callback for day selection
     :on-event-click - callback for event click
     :on-more-click - callback (fn [date-str]) for the '+N more' overflow indicator
     :indicator     - :pills (default) shows in-cell event pills;
                      :dots shows a compact row of coloured dots (mobile-friendly)
     :max-visible   - max events to show before '+N more' (pills mode, default 3)
     :pill-layout   - event-pill :layout for pills mode (:inline default, :stacked)
     :max-dots      - max dots to show (dots mode, default 4)"
  [{:keys [day events today-str selected-date on-select on-event-click
           on-event-context-menu on-more-click indicator max-visible max-dots pill-layout]}]
  (let [{:keys [current-month? date-str]} day
        d           (:day day)
        today?      (= date-str today-str)
        selected?   (= date-str selected-date)
        day-events  (events-for-date events date-str)
        dots?       (= indicator :dots)
        max-vis     (or max-visible 3)
        visible-evts (take max-vis day-events)
        overflow    (- (count day-events) max-vis)
        dot-evts    (take (or max-dots 4) day-events)
        cls-opts    {:today? today?
                     :selected? selected?
                     :current-month? current-month?}]
    #?(:squint
       [:div {:class (str (cal/day-cell-classes cls-opts) " cal-event-day"
                          (when dots? " cal-event-day-dots"))
              :on-click (when (and on-select (not (empty? date-str)))
                          (fn [_e] (on-select date-str)))
              :data-date date-str}
        [:div {:class "cal-day-number"} (str d)]
        (if dots?
          (into [:div {:class "cal-day-dots"}]
                (map (fn [evt]
                       [:span {:class (str "cal-day-dot " (event-color-class (:color evt)))}])
                     dot-evts))
          (into [:div {:class "cal-day-events"}]
                (concat
                 (map (fn [evt] (event-pill {:event evt :on-click on-event-click
                                             :on-context-menu on-event-context-menu
                                             :layout pill-layout}))
                      visible-evts)
                 (when (pos? overflow)
                   [[:div {:class "cal-event-more"
                           :on-click (when on-more-click
                                       (fn [e] (.stopPropagation e) (on-more-click date-str)))}
                     (str "+" overflow " more")]]))))]

       :cljs
       [:div {:class (cond-> (conj (cal/day-cell-class-list cls-opts) "cal-event-day")
                       dots? (conj "cal-event-day-dots"))
              :on (when on-select
                    {:click (fn [_e] (on-select date-str))})
              :data-date date-str}
        [:div {:class ["cal-day-number"]} (str d)]
        (if dots?
          (into [:div {:class ["cal-day-dots"]}]
                (map (fn [evt]
                       [:span {:class ["cal-day-dot" (event-color-class (:color evt))]}])
                     dot-evts))
          (into [:div {:class ["cal-day-events"]}]
                (concat
                 (map (fn [evt] (event-pill {:event evt :on-click on-event-click
                                             :on-context-menu on-event-context-menu
                                             :layout pill-layout}))
                      visible-evts)
                 (when (pos? overflow)
                   [[:div {:class ["cal-event-more"]
                           :on (when on-more-click
                                 {:click (fn [e] (.stopPropagation e) (on-more-click date-str))})}
                     (str "+" overflow " more")]]))))]

       :clj
       [:div {:class (str (cal/day-cell-classes cls-opts) " cal-event-day"
                          (when dots? " cal-event-day-dots"))
              :data-date date-str}
        [:div {:class "cal-day-number"} (str d)]
        (if dots?
          (into [:div {:class "cal-day-dots"}]
                (map (fn [evt]
                       [:span {:class (str "cal-day-dot " (event-color-class (:color evt)))}])
                     dot-evts))
          (into [:div {:class "cal-day-events"}]
                (concat
                 (map (fn [evt] (event-pill {:event evt :layout pill-layout}))
                      visible-evts)
                 (when (pos? overflow)
                   [[:div {:class "cal-event-more"} (str "+" overflow " more")]]))))])))

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
     :pill-layout    - event-pill :layout (:inline default, :stacked)
     :class          - additional CSS classes
     :attrs          - additional HTML attributes"
  [{:keys [year month today-str selected-date events on-select
           on-prev-month on-next-month on-event-click on-event-context-menu
           max-visible pill-layout class attrs]}]
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
                                        :on-event-context-menu on-event-context-menu
                                        :max-visible max-visible
                                        :pill-layout pill-layout}))
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
                                        :on-event-context-menu on-event-context-menu
                                        :max-visible max-visible
                                        :pill-layout pill-layout}))
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
                                        :max-visible max-visible
                                        :pill-layout pill-layout}))
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
  [{:keys [event on-click on-context-menu on-toggle-done]}]
  (let [title    (:title event)
        time-str (event-time-display event)
        color    (:color event)
        done?    (:done? event)
        task?    (:task? event)
        toggle   (when on-toggle-done (fn [_e] (on-toggle-done event)))]
    #?(:squint
       [:div {:class (agenda-event-classes {:done? done?})
              :on-click (when on-click (fn [_e] (on-click event)))
              :on-contextmenu (when on-context-menu
                                (fn [e]
                                  (.preventDefault e)
                                  (on-context-menu event e)))}
        (if task?
          (task-check done? toggle)
          [:div {:class (str "cal-agenda-dot " (event-color-class color))}])
        [:div {:class "cal-agenda-event-body"}
         (when time-str
           [:div {:class "cal-agenda-event-time"} time-str])
         [:div {:class "cal-agenda-event-title"} title]]]

       :cljs
       [:div {:class (agenda-event-class-list {:done? done?})
              :on (cond-> {}
                    on-click (assoc :click (fn [_e] (on-click event)))
                    on-context-menu (assoc :contextmenu
                                           (fn [e]
                                             (.preventDefault e)
                                             (on-context-menu event e))))}
        (if task?
          (task-check done? toggle)
          [:div {:class ["cal-agenda-dot" (event-color-class color)]}])
        [:div {:class ["cal-agenda-event-body"]}
         (when time-str
           [:div {:class ["cal-agenda-event-time"]} time-str])
         [:div {:class ["cal-agenda-event-title"]} title]]]

       :clj
       [:div {:class (agenda-event-classes {:done? done?})}
        (if task?
          (task-check done? toggle)
          [:div {:class (str "cal-agenda-dot " (event-color-class color))}])
        [:div {:class "cal-agenda-event-body"}
         (when time-str
           [:div {:class "cal-agenda-event-time"} time-str])
         [:div {:class "cal-agenda-event-title"} title]]])))

(defn agenda-day-group-class-list
  "Classes for one agenda day group. Today's group gets
   cal-agenda-day-today so it stands out in the list."
  [{:keys [today?]}]
  (cond-> ["cal-agenda-day-group"]
    today? (conj "cal-agenda-day-today")))

(defn agenda-day-empty
  "Placeholder card for a day that is always shown but has no events (today).
   A calendar icon bubble, a title + hint, and — when :on-add is given — an Add
   button that calls (on-add date).

   Props:
     :date   - YYYY-MM-DD string passed to :on-add
     :title  - headline (default \"Nothing scheduled\")
     :hint   - muted subline (default \"A clear day\")
     :on-add - optional (fn [date]) for the Add button"
  [{:keys [date title hint on-add]}]
  (let [title (or title "Nothing scheduled")
        hint  (or hint "A clear day")]
    #?(:squint
       [:div {:class "cal-agenda-day-empty"}
        [:span {:class "cal-agenda-day-empty-icon"} (icon/icon {:icon-name "calendar" :size "sm"})]
        [:div {:class "cal-agenda-day-empty-text"}
         [:div {:class "cal-agenda-day-empty-title"} title]
         [:div {:class "cal-agenda-day-empty-hint"} hint]]
        (when on-add
          [:button {:class "cal-agenda-day-empty-add" :type "button"
                    :on-click (fn [e] (.stopPropagation e) (on-add date))}
           (icon/icon {:icon-name "plus" :size "sm"}) "Add"])]

       :cljs
       [:div {:class ["cal-agenda-day-empty"]}
        [:span {:class ["cal-agenda-day-empty-icon"]} (icon/icon {:icon-name "calendar" :size "sm"})]
        [:div {:class ["cal-agenda-day-empty-text"]}
         [:div {:class ["cal-agenda-day-empty-title"]} title]
         [:div {:class ["cal-agenda-day-empty-hint"]} hint]]
        (when on-add
          [:button {:class ["cal-agenda-day-empty-add"] :type "button"
                    :on {:click (fn [e] (.stopPropagation e) (on-add date))}}
           (icon/icon {:icon-name "plus" :size "sm"}) "Add"])]

       :clj
       [:div {:class "cal-agenda-day-empty"}
        [:span {:class "cal-agenda-day-empty-icon"} (icon/icon {:icon-name "calendar" :size "sm"})]
        [:div {:class "cal-agenda-day-empty-text"}
         [:div {:class "cal-agenda-day-empty-title"} title]
         [:div {:class "cal-agenda-day-empty-hint"} hint]]
        (when on-add
          [:button {:class "cal-agenda-day-empty-add" :type "button"}
           (icon/icon {:icon-name "plus" :size "sm"}) "Add"])])))

(defn agenda-day-group
  "Render a day group in the agenda list with header and event rows.

   Props:
     :date       - YYYY-MM-DD string
     :label      - display label (e.g. 'Today', 'Tomorrow', 'Mon')
     :events     - all events (filtered internally)
     :today-str  - today's YYYY-MM-DD. Today's group is highlighted and always
                   rendered as an agenda-day-empty card when it has no events.
     :empty-title / :empty-hint - text for that card (see agenda-day-empty)
     :on-add-event - optional (fn [date]) wired to the empty card's Add button
     :on-event-click - callback for event click"
  [{:keys [date label events today-str empty-title empty-hint on-add-event on-event-click on-event-context-menu on-toggle-done]}]
  (let [day-evts (events-for-date events date)
        today?   (and today-str (= date today-str))]
    (when (or (seq day-evts) today?)
      #?(:squint
         [:div {:class (str/join " " (agenda-day-group-class-list {:today? today?}))}
          [:div {:class "cal-agenda-day-header"}
           [:span {:class "cal-agenda-day-label"} label]
           [:span {:class "cal-agenda-day-date"} (str/replace date "-" "/")]]
          (if (seq day-evts)
            (into [:div {:class "cal-agenda-day-events"}]
                  (map (fn [evt]
                         (agenda-event-row {:event evt :on-click on-event-click
                                            :on-context-menu on-event-context-menu
                                            :on-toggle-done on-toggle-done}))
                       day-evts))
            (agenda-day-empty {:date date :title empty-title :hint empty-hint :on-add on-add-event}))]

         :cljs
         [:div {:class (agenda-day-group-class-list {:today? today?})}
          [:div {:class ["cal-agenda-day-header"]}
           [:span {:class ["cal-agenda-day-label"]} label]
           [:span {:class ["cal-agenda-day-date"]} (str/replace date "-" "/")]]
          (if (seq day-evts)
            (into [:div {:class ["cal-agenda-day-events"]}]
                  (map (fn [evt]
                         (agenda-event-row {:event evt :on-click on-event-click
                                            :on-context-menu on-event-context-menu
                                            :on-toggle-done on-toggle-done}))
                       day-evts))
            (agenda-day-empty {:date date :title empty-title :hint empty-hint :on-add on-add-event}))]

         :clj
         [:div {:class (str/join " " (agenda-day-group-class-list {:today? today?}))}
          [:div {:class "cal-agenda-day-header"}
           [:span {:class "cal-agenda-day-label"} label]
           [:span {:class "cal-agenda-day-date"} (str/replace date "-" "/")]]
          (if (seq day-evts)
            (into [:div {:class "cal-agenda-day-events"}]
                  (map (fn [evt]
                         (agenda-event-row {:event evt}))
                       day-evts))
            (agenda-day-empty {:date date :title empty-title :hint empty-hint :on-add on-add-event}))]))))

(defn agenda-list
  "Render the full agenda list view with grouped events by day.

   Props:
     :days           - vector of {:date :label} maps for days to show
     :events         - all events
     :today-str      - today's YYYY-MM-DD; highlights today's group and keeps
                       it visible (as an agenda-day-empty card) even when empty
     :today-empty-title / :today-empty-hint - text for that card
     :on-add-event   - optional (fn [date]) for the empty card's Add button
     :on-event-click - callback for event click
     :class          - additional CSS classes
     :attrs          - additional HTML attributes"
  [{:keys [days events today-str today-empty-title today-empty-hint on-add-event on-event-click on-event-context-menu on-toggle-done class attrs]}]
  (let [groups (keep (fn [d]
                       (agenda-day-group {:date (:date d)
                                          :label (:label d)
                                          :events events
                                          :today-str today-str
                                          :empty-title today-empty-title
                                          :empty-hint today-empty-hint
                                          :on-add-event on-add-event
                                          :on-event-click on-event-click
                                          :on-event-context-menu on-event-context-menu
                                          :on-toggle-done on-toggle-done}))
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

(defn day-timeline
  "Render a single-day vertical timeline: an hour gutter down the left with
   colored event cards positioned and sized by start time and duration, plus
   an optional blue 'now' indicator line.

   Events without a :time-start are ignored (they have no place on the
   timeline). The visible hour range is derived from the day's events,
   falling back to 8 AM – 6 PM when the day is empty.

   Props:
     :events         - all events (filtered to :date internally)
     :date           - 'YYYY-MM-DD' day to render
     :today-str      - today's date string (enables the now line)
     :now-minutes    - current time as minutes-from-midnight; shows the now
                       line when :date is today and it falls in range
     :on-event-click - callback receiving the clicked event map
     :hour-height    - pixels per hour (default 72)
     :class          - extra classes
     :attrs          - extra attributes"
  [{:keys [events date today-str now-minutes on-event-click on-event-context-menu hour-height class attrs]}]
  (let [day-evts   (events-for-date events date)
        timed      (filterv :time-start day-evts)
        hour-h     (or hour-height 72)
        px-min     (/ hour-h 60)
        px         (fn [mins] (round* (* px-min mins)))
        starts     (mapv #(hhmm->minutes (:time-start %)) timed)
        ends       (mapv (fn [e] (+ (hhmm->minutes (:time-start e))
                                    (event-duration-minutes e)))
                         timed)
        min-start  (if (seq starts) (apply min starts) (* 8 60))
        max-end    (if (seq ends) (apply max ends) (* 18 60))
        start-hour (quot min-start 60)
        end-hour   (quot (+ max-end 59) 60)
        start-off  (* start-hour 60)
        hours      (vec (range start-hour (inc end-hour)))
        total-px   (px (* (- end-hour start-hour) 60))
        show-now?  (boolean (and now-minutes (= date today-str)
                                 (>= now-minutes start-off)
                                 (<= now-minutes (* end-hour 60))))
        now-px     (when show-now? (px (- now-minutes start-off)))]
    #?(:squint
       (let [classes (cond-> "cal-day-timeline" class (str " " class))
             rows (map (fn [h]
                         [:div {:class "cal-timeline-row"
                                :style {"top" (str (px (- (* h 60) start-off)) "px")}}
                          [:span {:class "cal-timeline-time"} (format-hour-label h)]
                          [:div {:class "cal-timeline-line"}]])
                       hours)
             cards (map (fn [evt]
                          (let [s   (hhmm->minutes (:time-start evt))
                                dur (event-duration-minutes evt)]
                            [:div {:class (str "cal-timeline-event " (event-color-class (:color evt)))
                                   :style {"top" (str (px (- s start-off)) "px")
                                           "height" (str (px dur) "px")}
                                   :on-click (when on-event-click
                                               (fn [_e] (on-event-click evt)))
                                   :on-contextmenu (when on-event-context-menu
                                                     (fn [e]
                                                       (.preventDefault e)
                                                       (.stopPropagation e)
                                                       (on-event-context-menu evt e)))}
                             [:div {:class "cal-timeline-event-head"}
                              [:span {:class "cal-timeline-event-title"} (:title evt)]
                              (when-let [d (format-duration dur)]
                                [:span {:class "cal-timeline-event-dur"} d])]]))
                        timed)
             now-node (when show-now?
                        [:div {:class "cal-timeline-now"
                               :style {"top" (str now-px "px")}}
                         [:span {:class "cal-timeline-now-dot"}]])]
         [:div (merge {:class classes} attrs)
          (into [:div {:class "cal-timeline-track"
                       :style {"height" (str total-px "px")}}]
                (concat rows cards (when now-node [now-node])))])

       :cljs
       (let [classes (cond-> ["cal-day-timeline"] class (conj class))
             rows (map (fn [h]
                         [:div {:class ["cal-timeline-row"]
                                :style {:top (str (px (- (* h 60) start-off)) "px")}}
                          [:span {:class ["cal-timeline-time"]} (format-hour-label h)]
                          [:div {:class ["cal-timeline-line"]}]])
                       hours)
             cards (map (fn [evt]
                          (let [s   (hhmm->minutes (:time-start evt))
                                dur (event-duration-minutes evt)]
                            [:div {:class ["cal-timeline-event" (event-color-class (:color evt))]
                                   :style {:top (str (px (- s start-off)) "px")
                                           :height (str (px dur) "px")}
                                   :on (cond-> {}
                                         on-event-click (assoc :click (fn [_e] (on-event-click evt)))
                                         on-event-context-menu (assoc :contextmenu
                                                                      (fn [e]
                                                                        (.preventDefault e)
                                                                        (.stopPropagation e)
                                                                        (on-event-context-menu evt e))))}
                             [:div {:class ["cal-timeline-event-head"]}
                              [:span {:class ["cal-timeline-event-title"]} (:title evt)]
                              (when-let [d (format-duration dur)]
                                [:span {:class ["cal-timeline-event-dur"]} d])]]))
                        timed)
             now-node (when show-now?
                        [:div {:class ["cal-timeline-now"]
                               :style {:top (str now-px "px")}}
                         [:span {:class ["cal-timeline-now-dot"]}]])]
         [:div (merge {:class classes} attrs)
          (into [:div {:class ["cal-timeline-track"]
                       :style {:height (str total-px "px")}}]
                (concat rows cards (when now-node [now-node])))])

       :clj
       (let [classes (cond-> "cal-day-timeline" class (str " " class))
             rows (map (fn [h]
                         [:div {:class "cal-timeline-row"
                                :style (str "top: " (px (- (* h 60) start-off)) "px")}
                          [:span {:class "cal-timeline-time"} (format-hour-label h)]
                          [:div {:class "cal-timeline-line"}]])
                       hours)
             cards (map (fn [evt]
                          (let [s   (hhmm->minutes (:time-start evt))
                                dur (event-duration-minutes evt)]
                            [:div {:class (str "cal-timeline-event " (event-color-class (:color evt)))
                                   :style (str "top: " (px (- s start-off)) "px; height: " (px dur) "px")}
                             [:div {:class "cal-timeline-event-head"}
                              [:span {:class "cal-timeline-event-title"} (:title evt)]
                              (when-let [d (format-duration dur)]
                                [:span {:class "cal-timeline-event-dur"} d])]]))
                        timed)
             now-node (when show-now?
                        [:div {:class "cal-timeline-now"
                               :style (str "top: " now-px "px")}
                         [:span {:class "cal-timeline-now-dot"}]])]
         [:div (merge {:class classes} attrs)
          (into [:div {:class "cal-timeline-track"
                       :style (str "height: " total-px "px")}]
                (concat rows cards (when now-node [now-node])))]))))

(defn week-timeline
  "Render a 7-day week grid: a sticky day-header row, an all-day event row,
   and a scrollable hour grid with timed event cards positioned by start time
   and duration within per-day columns, plus a red 'now' line spanning the
   week and a subtle tint on the current day's column.

   The visible hour range is derived from the week's timed events, falling
   back to 8 AM \u2013 6 PM when the week has none. Events without a :time-start
   render as bars in the all-day row.

   Props:
     :days           - vector of 7 {:date :day-num :dow} maps (Mon\u2013Sun);
                       :dow is 0=Mon..6=Sun, used for the weekday label
     :events         - all events (filtered to the week internally)
     :today-str      - today's date string (enables today tint + now line)
     :now-minutes    - current time as minutes-from-midnight
     :on-event-click - callback receiving the clicked event map
     :hour-height    - pixels per hour (default 56)
     :class          - extra classes
     :attrs          - extra attributes"
  [{:keys [days events today-str now-minutes on-event-click on-event-context-menu hour-height class attrs]}]
  (let [dates      (mapv :date days)
        date-set   (set dates)
        in-week    (filterv (fn [e] (contains? date-set (:date e))) events)
        timed      (filterv :time-start in-week)
        all-day    (filterv (fn [e] (not (:time-start e))) in-week)
        hour-h     (or hour-height 56)
        px-min     (/ hour-h 60)
        px         (fn [mins] (round* (* px-min mins)))
        starts     (mapv #(hhmm->minutes (:time-start %)) timed)
        ends       (mapv (fn [e] (+ (hhmm->minutes (:time-start e))
                                    (event-duration-minutes e)))
                         timed)
        min-start  (if (seq starts) (apply min starts) (* 8 60))
        max-end    (if (seq ends) (apply max ends) (* 18 60))
        start-hour (quot min-start 60)
        end-hour   (quot (+ max-end 59) 60)
        start-off  (* start-hour 60)
        hours      (vec (range start-hour (inc end-hour)))
        total-px   (px (* (- end-hour start-hour) 60))
        today-idx  (some (fn [[i d]] (when (= (:date d) today-str) i))
                         (map-indexed vector days))
        show-now?  (boolean (and now-minutes today-idx
                                 (>= now-minutes start-off)
                                 (<= now-minutes (* end-hour 60))))
        now-px     (when show-now? (px (- now-minutes start-off)))
        now-left   (when show-now? (* today-idx (/ 100.0 7)))]
    #?(:squint
       (let [wrap-cls (cond-> "cal-week" class (str " " class))
             head (into [:div {:class "cal-week-headcols"}]
                        (map (fn [d]
                               [:div {:class (str "cal-week-headcell"
                                                  (when (= (:date d) today-str) " is-today"))}
                                [:span {:class "cal-week-headname"} (nth weekday-short-names (:dow d))]
                                [:span {:class "cal-week-headnum"} (str (:day d))]])
                             days))
             allday (into [:div {:class "cal-week-alldaycols"}]
                          (map (fn [d]
                                 (into [:div {:class (str "cal-week-alldaycol"
                                                         (when (= (:date d) today-str) " is-today"))}]
                                       (map (fn [evt]
                                              [:div {:class (str "cal-week-alldayevent " (event-color-class (:color evt))
                                                                 (when (:task? evt) " cal-week-alldayevent-task")
                                                                 (when (:done? evt) " cal-event-done"))
                                                     :on-click (when on-event-click (fn [_e] (on-event-click evt)))
                                                     :on-contextmenu (when on-event-context-menu
                                                                       (fn [e]
                                                                         (.preventDefault e)
                                                                         (.stopPropagation e)
                                                                         (on-event-context-menu evt e)))}
                                               (if (:task? evt)
                                                 (task-check (:done? evt))
                                                 [:span {:class "cal-week-alldayevent-dot"}])
                                               [:span {:class "cal-week-alldayevent-title"} (:title evt)]])
                                            (events-for-date all-day (:date d)))))
                               days))
             hour-labels (map (fn [h]
                                [:div {:class "cal-week-hour"
                                       :style {"top" (str (px (- (* h 60) start-off)) "px")}}
                                 (format-hour-label h)])
                              hours)
             hour-lines (map (fn [h]
                               [:div {:class "cal-week-line"
                                      :style {"top" (str (px (- (* h 60) start-off)) "px")}}])
                             hours)
             day-cols (into [:div {:class "cal-week-daycols"}]
                            (map (fn [d]
                                   (into [:div {:class (str "cal-week-daycol"
                                                           (when (= (:date d) today-str) " is-today"))}]
                                         (map (fn [evt]
                                                (let [s   (hhmm->minutes (:time-start evt))
                                                      dur (event-duration-minutes evt)]
                                                  [:div {:class (str "cal-week-event " (event-color-class (:color evt)))
                                                         :style {"top" (str (px (- s start-off)) "px")
                                                                 "height" (str (px dur) "px")}
                                                         :on-click (when on-event-click (fn [_e] (on-event-click evt)))
                                                         :on-contextmenu (when on-event-context-menu
                                                                           (fn [e]
                                                                             (.preventDefault e)
                                                                             (.stopPropagation e)
                                                                             (on-event-context-menu evt e)))}
                                                   [:div {:class "cal-week-event-head"}
                                                    [:span {:class "cal-week-event-title"} (:title evt)]
                                                    (when-let [dd (format-duration dur)]
                                                      [:span {:class "cal-week-event-dur"} dd])]]))
                                              (events-for-date timed (:date d)))))
                                 days))
             now-node (when show-now?
                        [:div {:class "cal-week-now" :style {"top" (str now-px "px")}}
                         [:span {:class "cal-week-now-dot" :style {"left" (str now-left "%")}}]])]
         [:div (merge {:class wrap-cls} attrs)
          [:div {:class "cal-week-body"}
           [:div {:class "cal-week-topbar"}
            [:div {:class "cal-week-header"}
             [:div {:class "cal-week-corner"}]
             head]
            [:div {:class "cal-week-allday"}
             [:div {:class "cal-week-allday-label"} "All day"]
             allday]]
           [:div {:class "cal-week-grid" :style {"height" (str total-px "px")}}
            (into [:div {:class "cal-week-gutter"}] hour-labels)
            (into [:div {:class "cal-week-canvas"}]
                  (concat hour-lines [day-cols] (when now-node [now-node])))]]])

       :cljs
       (let [wrap-cls (cond-> ["cal-week"] class (conj class))
             head (into [:div {:class ["cal-week-headcols"]}]
                        (map (fn [d]
                               [:div {:class ["cal-week-headcell" (when (= (:date d) today-str) "is-today")]}
                                [:span {:class ["cal-week-headname"]} (nth weekday-short-names (:dow d))]
                                [:span {:class ["cal-week-headnum"]} (str (:day d))]])
                             days))
             allday (into [:div {:class ["cal-week-alldaycols"]}]
                          (map (fn [d]
                                 (into [:div {:class ["cal-week-alldaycol" (when (= (:date d) today-str) "is-today")]}]
                                       (map (fn [evt]
                                              [:div {:class ["cal-week-alldayevent" (event-color-class (:color evt))
                                                             (when (:task? evt) "cal-week-alldayevent-task")
                                                             (when (:done? evt) "cal-event-done")]
                                                     :on (cond-> {}
                                                           on-event-click (assoc :click (fn [_e] (on-event-click evt)))
                                                           on-event-context-menu (assoc :contextmenu
                                                                                        (fn [e]
                                                                                          (.preventDefault e)
                                                                                          (.stopPropagation e)
                                                                                          (on-event-context-menu evt e))))}
                                               (if (:task? evt)
                                                 (task-check (:done? evt))
                                                 [:span {:class ["cal-week-alldayevent-dot"]}])
                                               [:span {:class ["cal-week-alldayevent-title"]} (:title evt)]])
                                            (events-for-date all-day (:date d)))))
                               days))
             hour-labels (map (fn [h]
                                [:div {:class ["cal-week-hour"]
                                       :style {:top (str (px (- (* h 60) start-off)) "px")}}
                                 (format-hour-label h)])
                              hours)
             hour-lines (map (fn [h]
                               [:div {:class ["cal-week-line"]
                                      :style {:top (str (px (- (* h 60) start-off)) "px")}}])
                             hours)
             day-cols (into [:div {:class ["cal-week-daycols"]}]
                            (map (fn [d]
                                   (into [:div {:class ["cal-week-daycol" (when (= (:date d) today-str) "is-today")]}]
                                         (map (fn [evt]
                                                (let [s   (hhmm->minutes (:time-start evt))
                                                      dur (event-duration-minutes evt)]
                                                  [:div {:class ["cal-week-event" (event-color-class (:color evt))]
                                                         :style {:top (str (px (- s start-off)) "px")
                                                                 :height (str (px dur) "px")}
                                                         :on (cond-> {}
                                                               on-event-click (assoc :click (fn [_e] (on-event-click evt)))
                                                               on-event-context-menu (assoc :contextmenu
                                                                                            (fn [e]
                                                                                              (.preventDefault e)
                                                                                              (.stopPropagation e)
                                                                                              (on-event-context-menu evt e))))}
                                                   [:div {:class ["cal-week-event-head"]}
                                                    [:span {:class ["cal-week-event-title"]} (:title evt)]
                                                    (when-let [dd (format-duration dur)]
                                                      [:span {:class ["cal-week-event-dur"]} dd])]]))
                                              (events-for-date timed (:date d)))))
                                 days))
             now-node (when show-now?
                        [:div {:class ["cal-week-now"] :style {:top (str now-px "px")}}
                         [:span {:class ["cal-week-now-dot"] :style {:left (str now-left "%")}}]])]
         [:div (merge {:class wrap-cls} attrs)
          [:div {:class ["cal-week-body"]}
           [:div {:class ["cal-week-topbar"]}
            [:div {:class ["cal-week-header"]}
             [:div {:class ["cal-week-corner"]}]
             head]
            [:div {:class ["cal-week-allday"]}
             [:div {:class ["cal-week-allday-label"]} "All day"]
             allday]]
           [:div {:class ["cal-week-grid"] :style {:height (str total-px "px")}}
            (into [:div {:class ["cal-week-gutter"]}] hour-labels)
            (into [:div {:class ["cal-week-canvas"]}]
                  (concat hour-lines [day-cols] (when now-node [now-node])))]]])

       :clj
       (let [wrap-cls (cond-> "cal-week" class (str " " class))
             head (into [:div {:class "cal-week-headcols"}]
                        (map (fn [d]
                               [:div {:class (str "cal-week-headcell"
                                                  (when (= (:date d) today-str) " is-today"))}
                                [:span {:class "cal-week-headname"} (nth weekday-short-names (:dow d))]
                                [:span {:class "cal-week-headnum"} (str (:day d))]])
                             days))
             allday (into [:div {:class "cal-week-alldaycols"}]
                          (map (fn [d]
                                 (into [:div {:class (str "cal-week-alldaycol"
                                                         (when (= (:date d) today-str) " is-today"))}]
                                       (map (fn [evt]
                                              [:div {:class (str "cal-week-alldayevent " (event-color-class (:color evt))
                                                                 (when (:task? evt) " cal-week-alldayevent-task")
                                                                 (when (:done? evt) " cal-event-done"))}
                                               (if (:task? evt)
                                                 (task-check (:done? evt))
                                                 [:span {:class "cal-week-alldayevent-dot"}])
                                               [:span {:class "cal-week-alldayevent-title"} (:title evt)]])
                                            (events-for-date all-day (:date d)))))
                               days))
             hour-labels (map (fn [h]
                                [:div {:class "cal-week-hour"
                                       :style (str "top: " (px (- (* h 60) start-off)) "px")}
                                 (format-hour-label h)])
                              hours)
             hour-lines (map (fn [h]
                               [:div {:class "cal-week-line"
                                      :style (str "top: " (px (- (* h 60) start-off)) "px")}])
                             hours)
             day-cols (into [:div {:class "cal-week-daycols"}]
                            (map (fn [d]
                                   (into [:div {:class (str "cal-week-daycol"
                                                           (when (= (:date d) today-str) " is-today"))}]
                                         (map (fn [evt]
                                                (let [s   (hhmm->minutes (:time-start evt))
                                                      dur (event-duration-minutes evt)]
                                                  [:div {:class (str "cal-week-event " (event-color-class (:color evt)))
                                                         :style (str "top: " (px (- s start-off)) "px; height: " (px dur) "px")}
                                                   [:div {:class "cal-week-event-head"}
                                                    [:span {:class "cal-week-event-title"} (:title evt)]
                                                    (when-let [dd (format-duration dur)]
                                                      [:span {:class "cal-week-event-dur"} dd])]]))
                                              (events-for-date timed (:date d)))))
                                 days))
             now-node (when show-now?
                        [:div {:class "cal-week-now" :style (str "top: " now-px "px")}
                         [:span {:class "cal-week-now-dot" :style (str "left: " now-left "%")}]])]
         [:div (merge {:class wrap-cls} attrs)
          [:div {:class "cal-week-body"}
           [:div {:class "cal-week-topbar"}
            [:div {:class "cal-week-header"}
             [:div {:class "cal-week-corner"}]
             head]
            [:div {:class "cal-week-allday"}
             [:div {:class "cal-week-allday-label"} "All day"]
             allday]]
           [:div {:class "cal-week-grid" :style (str "height: " total-px "px")}
            (into [:div {:class "cal-week-gutter"}] hour-labels)
            (into [:div {:class "cal-week-canvas"}]
                  (concat hour-lines [day-cols] (when now-node [now-node])))]]]))))
