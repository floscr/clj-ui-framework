(ns ui.calendar-events-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.calendar-events :as cal-events]))

(def sample-events
  [{:title "Team standup"     :date "2026-03-29" :time-start "09:00" :time-end "09:30" :color :accent}
   {:title "Lunch with Alex"  :date "2026-03-29" :time-start "12:00" :time-end "13:00" :color :success}
   {:title "Deploy v2.0"      :date "2026-03-29" :time-start "15:00" :color :danger}
   {:title "Design review"    :date "2026-03-30" :time-start "10:00" :color :warning}
   {:title "All-day planning" :date "2026-03-31" :color nil :done? true}])

(deftest event-color-class-test
  (testing "named colors"
    (is (= "cal-event-accent" (cal-events/event-color-class :accent)))
    (is (= "cal-event-danger" (cal-events/event-color-class :danger)))
    (is (= "cal-event-success" (cal-events/event-color-class :success)))
    (is (= "cal-event-warning" (cal-events/event-color-class :warning))))
  (testing "extra categorical palettes"
    (is (= "cal-event-blue" (cal-events/event-color-class :blue)))
    (is (= "cal-event-teal" (cal-events/event-color-class :teal)))
    (is (= "cal-event-pink" (cal-events/event-color-class :pink)))
    (is (= "cal-event-orange" (cal-events/event-color-class :orange))))
  (testing "string names work too (squint passes strings)"
    (is (= "cal-event-blue" (cal-events/event-color-class "blue"))))
  (testing "nil or unknown returns default"
    (is (= "cal-event-default" (cal-events/event-color-class nil)))
    (is (= "cal-event-default" (cal-events/event-color-class :unknown)))))

(deftest events-for-date-test
  (testing "returns events for a matching date"
    (let [evts (cal-events/events-for-date sample-events "2026-03-29")]
      (is (= 3 (count evts)))))
  (testing "sorted by time-start"
    (let [evts (cal-events/events-for-date sample-events "2026-03-29")]
      (is (= "09:00" (:time-start (first evts))))
      (is (= "15:00" (:time-start (last evts))))))
  (testing "returns empty for non-matching date"
    (is (empty? (cal-events/events-for-date sample-events "2026-04-01"))))
  (testing "events without time sort last"
    (let [evts (cal-events/events-for-date sample-events "2026-03-31")]
      (is (= 1 (count evts)))
      (is (= "All-day planning" (:title (first evts)))))))

(deftest format-time-test
  (testing "formats HH:MM"
    (is (= "09:00" (cal-events/format-time "09:00")))
    (is (= "14:30" (cal-events/format-time "14:30"))))
  (testing "nil returns nil"
    (is (nil? (cal-events/format-time nil)))))

(deftest event-time-display-test
  (testing "time range with start and end"
    (is (= "09:00 \u2013 09:30"
           (cal-events/event-time-display {:time-start "09:00" :time-end "09:30"}))))
  (testing "start only"
    (is (= "15:00"
           (cal-events/event-time-display {:time-start "15:00"}))))
  (testing "no time"
    (is (nil? (cal-events/event-time-display {})))))

(deftest event-pill-class-list-test
  (testing "default"
    (is (= ["cal-event-pill" "cal-event-default"]
           (cal-events/event-pill-class-list {}))))
  (testing "with color"
    (is (= ["cal-event-pill" "cal-event-accent"]
           (cal-events/event-pill-class-list {:color :accent}))))
  (testing "done event"
    (is (= ["cal-event-pill" "cal-event-default" "cal-event-done"]
           (cal-events/event-pill-class-list {:done? true}))))
  (testing "color + done"
    (is (= ["cal-event-pill" "cal-event-danger" "cal-event-done"]
           (cal-events/event-pill-class-list {:color :danger :done? true}))))
  (testing "stacked layout"
    (is (= ["cal-event-pill" "cal-event-accent" "cal-event-pill-stacked"]
           (cal-events/event-pill-class-list {:color :accent :layout :stacked}))))
  (testing "inline layout adds no class"
    (is (= ["cal-event-pill" "cal-event-default"]
           (cal-events/event-pill-class-list {:layout :inline}))))
  (testing "stacked task"
    (is (= ["cal-event-pill" "cal-event-default" "cal-event-pill-stacked" "cal-event-task"]
           (cal-events/event-pill-class-list {:layout :stacked :task? true})))))

(deftest ticker-day-class-list-test
  (testing "default"
    (is (= ["cal-ticker-day"]
           (cal-events/ticker-day-class-list {}))))
  (testing "today"
    (is (= ["cal-ticker-day" "cal-ticker-today"]
           (cal-events/ticker-day-class-list {:today? true}))))
  (testing "selected"
    (is (= ["cal-ticker-day" "cal-ticker-selected"]
           (cal-events/ticker-day-class-list {:selected? true}))))
  (testing "today + selected"
    (is (= ["cal-ticker-day" "cal-ticker-today" "cal-ticker-selected"]
           (cal-events/ticker-day-class-list {:today? true :selected? true})))))

(deftest agenda-event-class-list-test
  (testing "default"
    (is (= ["cal-agenda-event"]
           (cal-events/agenda-event-class-list {}))))
  (testing "done"
    (is (= ["cal-agenda-event" "cal-agenda-event-done"]
           (cal-events/agenda-event-class-list {:done? true})))))

(deftest event-pill-component-test
  (testing "renders event pill (clj target)"
    (let [evt {:title "Test" :color :accent :time-start "10:00"}
          result (cal-events/event-pill {:event evt})]
      (is (= :div (first result)))
      (is (= "cal-event-pill cal-event-accent"
             (get-in result [1 :class])))))
  (testing "stacked layout"
    (let [result (cal-events/event-pill {:event {:title "Test" :color :accent}
                                         :layout :stacked})]
      (is (= "cal-event-pill cal-event-accent cal-event-pill-stacked"
             (get-in result [1 :class]))))))

(deftest event-day-cell-pill-layout-test
  (testing "passes :pill-layout through to its pills"
    (let [day {:day 29 :date-str "2026-03-29" :current-month? true}
          result (cal-events/event-day-cell {:day day
                                             :events [{:title "A" :date "2026-03-29"}]
                                             :pill-layout :stacked})
          pill (-> result (nth 3) (nth 2))]
      (is (= "cal-event-pill cal-event-default cal-event-pill-stacked"
             (get-in pill [1 :class]))))))

(deftest agenda-event-row-component-test
  (testing "renders agenda event row (clj target)"
    (let [evt {:title "Meeting" :color :danger :time-start "14:00" :time-end "15:00"}
          result (cal-events/agenda-event-row {:event evt})]
      (is (= :div (first result)))
      (is (= "cal-agenda-event"
             (get-in result [1 :class]))))))

(deftest agenda-day-group-component-test
  (testing "renders day group with events"
    (let [result (cal-events/agenda-day-group {:date "2026-03-29"
                                                :label "Today"
                                                :events sample-events})]
      (is (some? result))
      (is (= :div (first result)))))
  (testing "returns nil for date with no events"
    (let [result (cal-events/agenda-day-group {:date "2026-04-01"
                                                :label "Wed"
                                                :events sample-events})]
      (is (nil? result)))))

(deftest agenda-day-group-class-list-test
  (testing "plain day"
    (is (= ["cal-agenda-day-group"] (cal-events/agenda-day-group-class-list {}))))
  (testing "today"
    (is (= ["cal-agenda-day-group" "cal-agenda-day-today"]
           (cal-events/agenda-day-group-class-list {:today? true})))))

(deftest agenda-day-group-today-test
  (testing "empty today still renders, with the empty card"
    (let [result (cal-events/agenda-day-group {:date "2026-04-01" :label "Today"
                                                :events sample-events
                                                :today-str "2026-04-01"})
          s (pr-str result)]
      (is (some? result))
      (is (re-find #"cal-agenda-day-today" s))
      (is (re-find #"cal-agenda-day-empty" s))
      (is (re-find #"Nothing scheduled" s))
      (is (re-find #"A clear day" s))))
  (testing "today with events is highlighted and has no empty card"
    (let [s (pr-str (cal-events/agenda-day-group {:date "2026-03-29" :label "Today"
                                                   :events sample-events
                                                   :today-str "2026-03-29"}))]
      (is (re-find #"cal-agenda-day-today" s))
      (is (not (re-find #"cal-agenda-day-empty" s)))))
  (testing "other empty days are still dropped"
    (is (nil? (cal-events/agenda-day-group {:date "2026-04-01" :label "Wed"
                                             :events sample-events
                                             :today-str "2026-03-29"})))))

(deftest agenda-day-empty-test
  (testing "defaults, no Add button without :on-add"
    (let [s (pr-str (cal-events/agenda-day-empty {:date "2026-04-01"}))]
      (is (re-find #"Nothing scheduled" s))
      (is (not (re-find #"cal-agenda-day-empty-add" s)))))
  (testing "custom text + Add button"
    (let [s (pr-str (cal-events/agenda-day-empty {:date "2026-04-01" :title "Free" :hint "Go outside"
                                                   :on-add identity}))]
      (is (re-find #"Free" s))
      (is (re-find #"Go outside" s))
      (is (re-find #"cal-agenda-day-empty-add" s)))))

(deftest event-day-cell-head-test
  (let [day {:day 1 :month 10 :year 2026 :date-str "2026-10-01" :current-month? true}]
    (testing "head holds the number in a label; no week/month by default"
      (let [[_ _ head] (cal-events/event-day-cell {:day day :events []})]
        (is (= [:div {:class "cal-day-head"}
                nil
                [:span {:class "cal-day-label"} nil [:span {:class "cal-day-number"} "1"]]]
               head))))
    (testing "week number, month label and extra class"
      (let [[_ attrs head] (cal-events/event-day-cell {:day day :events [] :class "x"
                                                       :week-number "W40" :month-label "Oct"})]
        (is (re-find #" x$" (:class attrs)))
        (is (= [:span {:class "cal-day-week"} "W40"] (nth head 2)))
        (is (= [:span {:class "cal-day-month"} "Oct"] (get-in head [3 2])))))
    (testing "overflow renders a button"
      (let [evts (for [i (range 5)] {:title (str i) :date "2026-10-01"})
            [_ _ _ body] (cal-events/event-day-cell {:day day :events evts :max-visible 4})]
        (is (= [:button {:class "cal-event-more" :type "button"} "+1 more"] (last body)))))))

(deftest month-grid-test
  (let [evts [{:title "A" :date "2026-10-12"}]]
    (testing "full variant: long heads with today lit, week numbers on Mondays, month labels on the 1st"
      (let [[_ attrs heads grid] (cal-events/month-grid {:year 2026 :month 10 :events evts
                                                         :today-str "2026-10-08" :week-numbers? true
                                                         :class "host"})
            cells (drop 2 grid)
            s (pr-str grid)]
        (is (= "cal-month cal-month-full host" (:class attrs)))
        (is (= "cal-weekday is-today" (get-in (vec heads) [5 1 :class])))
        (is (= "cal-grid cal-grid-events" (get-in grid [1 :class])))
        (is (= 35 (count cells)))
        (is (= ["W40" "W41" "W42" "W43" "W44"] (vec (re-seq #"W\d+" s))))
        (is (= ["Oct" "Nov"] (vec (re-seq #"Oct|Nov" s))))
        (is (re-find #"cal-event-pill-stacked" s))))
    (testing "compact variant: short heads, dots, no week numbers or month labels"
      (let [[_ attrs heads grid] (cal-events/month-grid {:year 2026 :month 10 :events evts
                                                         :today-str "2026-10-08" :variant :compact
                                                         :week-numbers? true})
            s (pr-str [heads grid])]
        (is (= "cal-month cal-month-compact" (:class attrs)))
        (is (= "cal-grid cal-grid-events cal-grid-dots" (get-in grid [1 :class])))
        (is (re-find #"cal-event-day-dots" s))
        (is (re-find #"cal-day-dot " s))
        (is (not (re-find #"is-today|cal-day-week|cal-day-month" s)))))))

(deftest month-grid-highlight-week-test
  (let [active (fn [grid] (->> (drop 2 grid)
                               (filter #(re-find #"cal-week-active" (get-in % [1 :class])))
                               (map #(get-in % [1 :data-date]))))]
    (testing "marks the seven cells of the highlighted date's row"
      (let [[_ _ _ grid] (cal-events/month-grid {:year 2026 :month 10 :events []
                                                 :variant :compact :highlight-week "2026-10-13"})]
        (is (= ["2026-10-12" "2026-10-13" "2026-10-14" "2026-10-15"
                "2026-10-16" "2026-10-17" "2026-10-18"]
               (active grid)))))
    (testing "no band for an off-grid date or without the prop"
      (is (empty? (active (nth (cal-events/month-grid {:year 2026 :month 10 :events []
                                                       :highlight-week "2026-12-01"}) 3))))
      (is (empty? (active (nth (cal-events/month-grid {:year 2026 :month 10 :events []}) 3)))))))
