(ns ui.calendar-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.calendar :as cal]))

(deftest leap-year-test
  (testing "standard leap years"
    (is (true? (cal/leap-year? 2024)))
    (is (true? (cal/leap-year? 2000)))
    (is (true? (cal/leap-year? 1600))))
  (testing "non-leap years"
    (is (false? (cal/leap-year? 2023)))
    (is (false? (cal/leap-year? 1900)))
    (is (false? (cal/leap-year? 2100)))))

(deftest days-in-month-test
  (testing "31-day months"
    (is (= 31 (cal/days-in-month 2026 1)))
    (is (= 31 (cal/days-in-month 2026 3)))
    (is (= 31 (cal/days-in-month 2026 5)))
    (is (= 31 (cal/days-in-month 2026 7)))
    (is (= 31 (cal/days-in-month 2026 8)))
    (is (= 31 (cal/days-in-month 2026 10)))
    (is (= 31 (cal/days-in-month 2026 12))))
  (testing "30-day months"
    (is (= 30 (cal/days-in-month 2026 4)))
    (is (= 30 (cal/days-in-month 2026 6)))
    (is (= 30 (cal/days-in-month 2026 9)))
    (is (= 30 (cal/days-in-month 2026 11))))
  (testing "February"
    (is (= 28 (cal/days-in-month 2026 2)))
    (is (= 29 (cal/days-in-month 2024 2)))
    (is (= 28 (cal/days-in-month 1900 2)))
    (is (= 29 (cal/days-in-month 2000 2)))))

(deftest day-of-week-test
  (testing "known dates"
    ;; 2026-03-29 is a Sunday = 6 in our system (0=Mon)
    (is (= 6 (cal/day-of-week 2026 3 29)))
    ;; 2026-01-01 is a Thursday = 3
    (is (= 3 (cal/day-of-week 2026 1 1)))
    ;; 2024-01-01 is a Monday = 0
    (is (= 0 (cal/day-of-week 2024 1 1)))
    ;; 2023-12-25 is a Monday = 0
    (is (= 0 (cal/day-of-week 2023 12 25)))))

(deftest first-day-of-week-test
  (testing "first-day-of-week delegates to day-of-week"
    ;; March 2026 starts on Sunday = 6
    (is (= 6 (cal/first-day-of-week 2026 3)))
    ;; January 2024 starts on Monday = 0
    (is (= 0 (cal/first-day-of-week 2024 1)))))

(deftest pad2-test
  (testing "single digit padded"
    (is (= "01" (cal/pad2 1)))
    (is (= "09" (cal/pad2 9))))
  (testing "double digit not padded"
    (is (= "10" (cal/pad2 10)))
    (is (= "31" (cal/pad2 31)))))

(deftest date-str-test
  (testing "formats date as YYYY-MM-DD"
    (is (= "2026-03-29" (cal/date-str 2026 3 29)))
    (is (= "2026-01-01" (cal/date-str 2026 1 1)))))

(deftest month-name-test
  (testing "month names"
    (is (= "January" (cal/month-name 1)))
    (is (= "December" (cal/month-name 12)))
    (is (= "March" (cal/month-name 3)))))

(deftest prev-month-test
  (testing "normal"
    (is (= [2026 2] (cal/prev-month 2026 3))))
  (testing "year boundary"
    (is (= [2025 12] (cal/prev-month 2026 1)))))

(deftest next-month-test
  (testing "normal"
    (is (= [2026 4] (cal/next-month 2026 3))))
  (testing "year boundary"
    (is (= [2027 1] (cal/next-month 2026 12)))))

(deftest calendar-days-test
  (testing "generates correct number of days in complete weeks"
    (let [days (cal/calendar-days 2026 3)]
      ;; Must be divisible by 7
      (is (zero? (mod (count days) 7)))
      ;; Must contain all 31 days of March 2026
      (let [current (filter :current-month? days)]
        (is (= 31 (count current)))
        (is (= 1 (:day (first current))))
        (is (= 31 (:day (last current)))))))

  (testing "leading days from previous month"
    ;; March 2026 starts on Sunday (6), so 6 leading days from Feb
    (let [days (cal/calendar-days 2026 3)
          leading (take-while #(not (:current-month? %)) days)]
      (is (= 6 (count leading)))
      (is (= 2 (:month (first leading))))))

  (testing "trailing days from next month"
    (let [days (cal/calendar-days 2026 3)
          trailing (drop-while #(or (:current-month? %)
                                     (= 2 (:month %)))
                               days)]
      ;; All trailing days should be in April
      (is (every? #(= 4 (:month %)) trailing))))

  (testing "month starting on Monday has no leading days"
    ;; June 2026 starts on Monday
    (let [days (cal/calendar-days 2026 6)
          leading (take-while #(not (:current-month? %)) days)]
      (is (= 0 (count leading))))))

(deftest calendar-class-list-test
  (testing "default"
    (is (= ["cal"] (cal/calendar-class-list {})))))

(deftest day-cell-class-list-test
  (testing "default day"
    (is (= ["cal-day"] (cal/day-cell-class-list {:current-month? true}))))

  (testing "today"
    (is (= ["cal-day" "cal-day-today"]
           (cal/day-cell-class-list {:current-month? true :today? true}))))

  (testing "selected"
    (is (= ["cal-day" "cal-day-selected"]
           (cal/day-cell-class-list {:current-month? true :selected? true}))))

  (testing "outside month"
    (is (= ["cal-day" "cal-day-outside"]
           (cal/day-cell-class-list {:current-month? false}))))

  (testing "disabled"
    (is (= ["cal-day" "cal-day-disabled"]
           (cal/day-cell-class-list {:current-month? true :disabled? true}))))

  (testing "today + selected"
    (is (= ["cal-day" "cal-day-today" "cal-day-selected"]
           (cal/day-cell-class-list {:current-month? true :today? true :selected? true})))))

(deftest calendar-component-test
  (testing "renders correct hiccup structure (clj target)"
    (let [result (cal/calendar {:year 2026 :month 3 :today-str "2026-03-29"})]
      (is (= :div (first result)))
      (is (= "cal" (get-in result [1 :class])))))

  (testing "extra class gets appended"
    (let [result (cal/calendar {:year 2026 :month 3 :class "my-cal"})]
      (is (= "cal my-cal" (get-in result [1 :class])))))

  (testing "calendar header contains month and year"
    (let [result (cal/calendar-header {:year 2026 :month 3})]
      ;; Header is a div with nav-title containing "March 2026"
      (is (= :div (first result)))
      ;; Find the title div
      (let [children (drop 2 result)
            title-div (some #(when (and (vector? %)
                                        (= :div (first %))
                                        (= "cal-nav-title" (get-in % [1 :class])))
                              %)
                           children)]
        (is (some? title-div))
        (is (= "March 2026" (last title-div)))))))

(deftest day-of-year-test
  (is (= 1 (cal/day-of-year 2026 1 1)))
  (is (= 60 (cal/day-of-year 2024 2 29)))
  (is (= 365 (cal/day-of-year 2026 12 31)))
  (is (= 366 (cal/day-of-year 2024 12 31))))

(deftest iso-week-test
  (testing "mid-year"
    (is (= 41 (cal/iso-week 2026 10 8)))
    (is (= 40 (cal/iso-week 2026 9 28))))
  (testing "early January belongs to the previous year's last week"
    (is (= 53 (cal/iso-week 2021 1 3)))
    (is (= 52 (cal/iso-week 2023 1 1))))
  (testing "late December can be week 1 of the next year"
    (is (= 1 (cal/iso-week 2024 12 30)))
    (is (= 1 (cal/iso-week 2025 12 29))))
  (testing "53-week years"
    (is (= 53 (cal/iso-week 2020 12 31)))
    (is (= 53 (cal/iso-week 2026 12 31)))))

(deftest calendar-weekdays-test
  (testing "default short labels, no today marker"
    (let [s (pr-str (cal/calendar-weekdays {}))]
      (is (re-find #"\"Mo\"" s))
      (is (not (re-find #"is-today" s)))))
  (testing "custom labels + today column"
    (let [[_ _ & cols] (cal/calendar-weekdays {:labels cal/long-weekday-labels :today-index 3})
          thu (nth cols 3)]
      (is (= "cal-weekday is-today" (get-in thu [1 :class])))
      (is (= [:span {:class "cal-weekday-dot"}] (nth thu 2)))
      (is (= "Thu" (nth thu 3)))
      (is (= "cal-weekday" (get-in (first cols) [1 :class]))))))
