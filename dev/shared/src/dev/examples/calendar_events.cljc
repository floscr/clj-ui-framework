(ns dev.examples.calendar-events
  (:require [dev.example :refer [example sx]]
            [dev.examples.sample :as sample]
            [ui.calendar-events :as cal-events]))

(def ^:private week
  [{:date "2026-03-27" :day-num 27 :day-label "Fr"}
   {:date "2026-03-28" :day-num 28 :day-label "Sa"}
   {:date "2026-03-29" :day-num 29 :day-label "Su"}
   {:date "2026-03-30" :day-num 30 :day-label "Mo"}
   {:date "2026-03-31" :day-num 31 :day-label "Tu"}
   {:date "2026-04-01" :day-num 1  :day-label "We"}
   {:date "2026-04-02" :day-num 2  :day-label "Th"}])

(def examples
  [(example :grid "Event grid"
     (fn []
       (cal-events/calendar-event-grid {:year 2026 :month 3 :today-str "2026-03-29"
                                        :selected-date "2026-03-29"
                                        :events sample/calendar-events})))

   (example :compact "Compact month"
     (fn []
       [:div {:style (sx {:max-width "360px"})}
        (cal-events/month-grid {:year 2026 :month 3 :variant :compact
                                :today-str "2026-03-29" :selected-date "2026-03-29"
                                :highlight-week "2026-03-29"
                                :events sample/calendar-events})]))

   (example :pills "Event pills, inline and stacked"
     (fn []
       (let [evts [{:title "Team standup" :time-start "09:00" :time-end "09:30" :color :accent}
                   {:title "Lunch with Anna" :time-start "12:30" :time-end "13:30" :color :warning}
                   {:title "Pay rent" :color :danger :task? true}]]
         [:div {:style (sx {:display "grid" :grid-template-columns "repeat(auto-fit, minmax(10rem, 12rem))" :gap "0.5rem 1.5rem"})}
          (into [:div {:style (sx {:display "flex" :flex-direction "column" :gap "3px"})}]
                (map (fn [e] (cal-events/event-pill {:event e})) evts))
          (into [:div {:style (sx {:display "flex" :flex-direction "column" :gap "3px"})}]
                (map (fn [e] (cal-events/event-pill {:event e :layout :stacked})) evts))])))

   (example :ticker "Day ticker"
     (fn []
       (cal-events/ticker-strip {:days week
                                 :today-str "2026-03-29"
                                 :selected "2026-03-29"
                                 :events sample/calendar-events})))

   (example :agenda "Agenda list"
     (fn []
       [:div {:style (sx {:max-width "480px"})}
        (cal-events/agenda-list {:days [{:date "2026-03-29" :label "Today"}
                                        {:date "2026-03-30" :label "Tomorrow"}
                                        {:date "2026-03-31" :label "Tue"}
                                        {:date "2026-04-01" :label "Wed"}]
                                 :today-str "2026-03-29"
                                 :events sample/calendar-events})]))

   (example :day "Day timeline"
     (fn []
       [:div {:style (sx {:max-width "480px" :height "320px" :overflow "auto"})}
        (cal-events/day-timeline {:date "2026-03-29" :today-str "2026-03-29" :now-minutes 630
                                  :events sample/calendar-events})]))])
