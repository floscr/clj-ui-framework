(ns dev.examples.calendar
  (:require [dev.example :refer [example row]]
            [ui.calendar :as calendar]))

(def examples
  [(example :picker "Date picker"
     (fn []
       (row
        (calendar/calendar {:year 2026 :month 3 :today-str "2026-03-29"
                            :selected-date "2026-03-15"})
        (calendar/calendar {:year 2026 :month 4 :today-str "2026-03-29"}))))])
