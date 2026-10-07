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
           (cal-events/event-pill-class-list {:color :danger :done? true})))))

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
             (get-in result [1 :class]))))))

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
