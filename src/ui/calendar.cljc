(ns ui.calendar
  "Month-grid date picker. See src/ui/calendar.md for full documentation."
  (:require [clojure.string :as str]
            [ui.button :as button]
            [ui.icon :as icon]))

;; ── Date Utilities ──────────────────────────────────────────────────
;; Pure functions — no JS Date dependency. Work across all targets.

(def month-names
  ["January" "February" "March" "April" "May" "June"
   "July" "August" "September" "October" "November" "December"])

(def short-month-names
  ["Jan" "Feb" "Mar" "Apr" "May" "Jun"
   "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"])

(def weekday-labels
  ["Mo" "Tu" "We" "Th" "Fr" "Sa" "Su"])

(defn leap-year?
  "Returns true if year is a leap year."
  [year]
  (or (zero? (mod year 400))
      (and (zero? (mod year 4))
           (not (zero? (mod year 100))))))

(defn days-in-month
  "Returns number of days in a given month (1-indexed)."
  [year month]
  (case (int month)
    2 (if (leap-year? year) 29 28)
    (4 6 9 11) 30
    31))

(defn day-of-week
  "Returns day of week for a date (0=Mon, 1=Tue, ... 6=Sun).
   Uses Tomohiko Sakamoto's algorithm."
  [year month day]
  (let [t [0 3 2 5 0 3 5 1 4 6 2 4]
        y (if (< month 3) (dec year) year)
        dow (mod (+ y
                    (quot y 4)
                    (- (quot y 100))
                    (quot y 400)
                    (nth t (dec month))
                    day)
                 7)]
    ;; Convert from 0=Sun to 0=Mon
    (mod (+ dow 6) 7)))

(defn first-day-of-week
  "Returns day-of-week (0=Mon..6=Sun) for the 1st of the given month."
  [year month]
  (day-of-week year month 1))

(defn pad2
  "Zero-pad a number to 2 digits."
  [n]
  (if (< n 10) (str "0" n) (str n)))

(defn date-str
  "Format a date as YYYY-MM-DD string."
  [year month day]
  (str year "-" (pad2 month) "-" (pad2 day)))

(defn month-name
  "Get full month name (1-indexed)."
  [month]
  (nth month-names (dec month)))

(defn short-month-name
  "Get abbreviated month name (1-indexed)."
  [month]
  (nth short-month-names (dec month)))

(defn prev-month
  "Returns [year month] for the previous month."
  [year month]
  (if (= month 1)
    [(dec year) 12]
    [year (dec month)]))

(defn next-month
  "Returns [year month] for the next month."
  [year month]
  (if (= month 12)
    [(inc year) 1]
    [year (inc month)]))

(defn calendar-days
  "Generate the grid of day maps for a month calendar.
   Returns a vector of maps with :day, :month, :year, :date-str, :current-month?.
   Includes leading days from previous month and trailing days from next month
   to fill complete weeks (rows of 7)."
  [year month]
  (let [first-dow     (first-day-of-week year month)
        total-days    (days-in-month year month)
        [py pm]       (prev-month year month)
        prev-days     (days-in-month py pm)
        [ny nm]       (next-month year month)
        ;; Leading days from previous month
        leading       (mapv (fn [i]
                              (let [d (- prev-days (- first-dow 1) (- i))]
                                {:day d :month pm :year py
                                 :date-str (date-str py pm d)
                                 :current-month? false}))
                            (range first-dow))
        ;; Days of current month
        current       (mapv (fn [d]
                              {:day (inc d) :month month :year year
                               :date-str (date-str year month (inc d))
                               :current-month? true})
                            (range total-days))
        ;; Trailing days from next month
        all           (into leading current)
        trailing-count (let [r (mod (count all) 7)]
                         (if (zero? r) 0 (- 7 r)))
        trailing      (mapv (fn [d]
                              {:day (inc d) :month nm :year ny
                               :date-str (date-str ny nm (inc d))
                               :current-month? false})
                            (range trailing-count))]
    (into all trailing)))

;; ── Class Generation ────────────────────────────────────────────────

(defn calendar-class-list
  "Returns a vector of CSS class strings for the calendar container."
  [_opts]
  ["cal"])

(defn calendar-classes
  "Returns a space-joined class string for the calendar container."
  [opts]
  (str/join " " (calendar-class-list opts)))

(defn day-cell-class-list
  "Returns a vector of CSS class strings for a day cell.
   Options:
     :today?         - is this the current date
     :selected?      - is this date selected
     :current-month? - is this day in the displayed month
     :disabled?      - is this day disabled/unselectable"
  [{:keys [today? selected? current-month? disabled?]}]
  (cond-> ["cal-day"]
    (not current-month?) (conj "cal-day-outside")
    today?               (conj "cal-day-today")
    selected?            (conj "cal-day-selected")
    disabled?            (conj "cal-day-disabled")))

(defn day-cell-classes
  "Returns a space-joined class string for a day cell."
  [opts]
  (str/join " " (day-cell-class-list opts)))

;; ── Components ──────────────────────────────────────────────────────

(defn calendar-header
  "Render the calendar header with month/year title and navigation buttons.

   Props:
     :year          - current year
     :month         - current month (1-indexed)
     :on-prev-month - callback for previous month navigation
     :on-next-month - callback for next month navigation
     :class         - additional CSS classes
     :attrs         - additional HTML attributes"
  [{:keys [year month on-prev-month on-next-month class attrs]}]
  #?(:squint
     (let [classes (cond-> "cal-nav" class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       [:div base-attrs
        (button/button {:variant "ghost" :icon "chevron-left" :size "sm"
                        :on-click on-prev-month
                        :class "cal-nav-btn"
                        :attrs {:aria-label "Previous month"}})
        [:div {:class "cal-nav-title"}
         (str (month-name month) " " year)]
        (button/button {:variant "ghost" :icon "chevron-right" :size "sm"
                        :on-click on-next-month
                        :class "cal-nav-btn"
                        :attrs {:aria-label "Next month"}})])

     :cljs
     (let [classes (cond-> ["cal-nav"] class (conj class))
           base-attrs (merge {:class classes} attrs)]
       [:div base-attrs
        (button/button {:variant :ghost :icon :chevron-left :size :sm
                        :on-click on-prev-month
                        :class "cal-nav-btn"
                        :attrs {:aria-label "Previous month"}})
        [:div {:class ["cal-nav-title"]}
         (str (month-name month) " " year)]
        (button/button {:variant :ghost :icon :chevron-right :size :sm
                        :on-click on-next-month
                        :class "cal-nav-btn"
                        :attrs {:aria-label "Next month"}})])

     :clj
     (let [classes (cond-> "cal-nav" class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       [:div base-attrs
        (button/button {:variant :ghost :icon :chevron-left :size :sm
                        :class "cal-nav-btn"
                        :attrs {:aria-label "Previous month"}})
        [:div {:class "cal-nav-title"}
         (str (month-name month) " " year)]
        (button/button {:variant :ghost :icon :chevron-right :size :sm
                        :class "cal-nav-btn"
                        :attrs {:aria-label "Next month"}})])))

(defn calendar-weekdays
  "Render the weekday header row.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [class attrs]}]
  #?(:squint
     (let [classes (cond-> "cal-weekdays" class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       (into [:div base-attrs]
             (map (fn [label] [:div {:class "cal-weekday"} label])
                  weekday-labels)))

     :cljs
     (let [classes (cond-> ["cal-weekdays"] class (conj class))
           base-attrs (merge {:class classes} attrs)]
       (into [:div base-attrs]
             (map (fn [label] [:div {:class ["cal-weekday"]} label])
                  weekday-labels)))

     :clj
     (let [classes (cond-> "cal-weekdays" class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       (into [:div base-attrs]
             (map (fn [label] [:div {:class "cal-weekday"} label])
                  weekday-labels)))))

(defn calendar-day
  "Render a single day cell in the calendar grid.

   Props:
     :day            - day info map from calendar-days
     :today-str      - YYYY-MM-DD string of today's date
     :selected-date  - YYYY-MM-DD string of selected date (or nil)
     :on-select      - callback fn called with date-str when clicked
     :disabled?      - whether this day is disabled"
  [{:keys [day today-str selected-date on-select disabled?]}]
  (let [{:keys [current-month? date-str]} day
        d       (:day day)
        today?  (= date-str today-str)
        selected? (= date-str selected-date)
        cls-opts {:today? today?
                  :selected? selected?
                  :current-month? current-month?
                  :disabled? disabled?}]
    #?(:squint
       [:button {:class (day-cell-classes cls-opts)
                 :on-click (when (and on-select (not disabled?))
                             (fn [_e] (on-select date-str)))
                 :disabled (when disabled? true)
                 :data-date date-str
                 :type "button"}
        [:span {:class "cal-day-number"} (str d)]]

       :cljs
       [:button {:class (day-cell-class-list cls-opts)
                 :on (when (and on-select (not disabled?))
                       {:click (fn [_e] (on-select date-str))})
                 :disabled (when disabled? true)
                 :data-date date-str
                 :type "button"}
        [:span {:class ["cal-day-number"]} (str d)]]

       :clj
       [:button {:class (day-cell-classes cls-opts)
                 :disabled (when disabled? true)
                 :data-date date-str
                 :type "button"}
        [:span {:class "cal-day-number"} (str d)]])))

(defn calendar
  "Render a month calendar date picker.
   Inspired by shadcn/radix Calendar.

   Props:
     :year          - displayed year (e.g. 2026)
     :month         - displayed month (1-12)
     :today-str     - YYYY-MM-DD string for today (for highlighting)
     :selected-date - YYYY-MM-DD string of selected date (or nil)
     :on-select     - callback fn, receives date-str when a day is clicked
     :on-prev-month - callback for previous month navigation
     :on-next-month - callback for next month navigation
     :class         - additional CSS classes
     :attrs         - additional HTML attributes"
  [{:keys [year month today-str selected-date on-select
           on-prev-month on-next-month class attrs]}]
  (let [days (calendar-days year month)]
    #?(:squint
       (let [classes (cond-> (calendar-classes {}) class (str " " class))
             base-attrs (merge {:class classes} attrs)]
         [:div base-attrs
          (calendar-header {:year year :month month
                            :on-prev-month on-prev-month
                            :on-next-month on-next-month})
          (calendar-weekdays {})
          (into [:div {:class "cal-grid"}]
                (map (fn [day-info]
                       (calendar-day {:day day-info
                                      :today-str today-str
                                      :selected-date selected-date
                                      :on-select on-select}))
                     days))])

       :cljs
       (let [cls (calendar-class-list {})
             classes (cond-> cls class (conj class))
             base-attrs (merge {:class classes} attrs)]
         [:div base-attrs
          (calendar-header {:year year :month month
                            :on-prev-month on-prev-month
                            :on-next-month on-next-month})
          (calendar-weekdays {})
          (into [:div {:class ["cal-grid"]}]
                (map (fn [day-info]
                       (calendar-day {:day day-info
                                      :today-str today-str
                                      :selected-date selected-date
                                      :on-select on-select}))
                     days))])

       :clj
       (let [classes (cond-> (calendar-classes {}) class (str " " class))
             base-attrs (merge {:class classes} attrs)]
         [:div base-attrs
          (calendar-header {:year year :month month})
          (calendar-weekdays {})
          (into [:div {:class "cal-grid"}]
                (map (fn [day-info]
                       (calendar-day {:day day-info
                                      :today-str today-str
                                      :selected-date selected-date}))
                     days))]))))
