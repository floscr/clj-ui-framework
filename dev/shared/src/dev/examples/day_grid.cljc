(ns dev.examples.day-grid
  (:require [dev.example :refer [example]]
            [ui.day-grid :as day-grid]
            [ui.metric :as metric]
            [ui.card :as card]))

(def ^:private weeks
  (vec (map-indexed (fn [i state]
                      {:date (str "Day " (inc i)) :state state :today (= i 24)})
                    [:rest :rest :rest :rest :rest :rest :rest
                     :rest :done :done :done :excused :rest :rest
                     :done :missed :missed :done :done :rest :rest
                     :missed :done :missed :done :future :future :future])))

(def examples
  [(example :default "Default"
     (fn [] (day-grid/day-grid {:days weeks})))

   (example :card "Under the streak numbers"
     (fn []
       (card/card {}
         (card/card-header {} [:h3 "Walk"] [:p "Mon to Fri · 8 days done"])
         (metric/metric-row {}
           (metric/metric {:flush true :label "Current streak" :value "1" :unit "day"})
           (metric/metric {:flush true :label "Best streak" :value "4" :unit "days"}))
         (day-grid/day-grid {:color :teal :days weeks}))))])
