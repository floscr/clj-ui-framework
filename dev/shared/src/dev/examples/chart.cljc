(ns dev.examples.chart
  (:require [dev.example :refer [example]]
            [ui.button-group :as button-group]
            [ui.card :as card]
            [ui.chart :as chart]
            [ui.metric :as metric]))

(def days
  ["Mon 5 Oct" "Tue 6 Oct" "Wed 7 Oct" "Thu 8 Oct" "Fri 9 Oct" "Sat 10 Oct" "Sun 11 Oct"
   "Mon 12 Oct" "Tue 13 Oct" "Wed 14 Oct" "Thu 15 Oct" "Fri 16 Oct" "Sat 17 Oct" "Sun 18 Oct"])

(def visitors [124 141 133 180 152 61 48 139 162 310 244 171 70 55])
(def before   [110 118 126 131 120 58 44 121 133 140 138 129 62 50])

(def points
  (vec (map-indexed (fn [i v] {:label (nth days i) :value v :compare (nth before i)})
                    visitors)))

(def weekly [540 610 590 720 680 810 790 905 870 1020 980 1151 1099])

(defn- range-picker []
  (button-group/button-group {:variant :boxed}
    (button-group/button-group-item {} "7d")
    (button-group/button-group-item {:active true} "14d")
    (button-group/button-group-item {} "30d")
    (button-group/button-group-item {} "90d")))

(def examples
  [(example :area "Time series against the period before"
     (fn []
       (card/card {}
         (chart/chart {:title "Visitors"
                       :description "2,030 in the last 14 days, +27% on the 14 before"
                       :actions (range-picker)}
           (chart/area-chart {:points points
                              :label "This period"
                              :compare-label "Period before"})))))

   (example :area-single "One series, another color, straight segments"
     (fn []
       (card/card {}
         (chart/chart {:title "Response time" :description "Median per day, in milliseconds"}
           (chart/area-chart {:color :teal
                              :curve :linear
                              :label "Median"
                              :format (fn [n] (str (chart/format-number n) " ms"))
                              :points (vec (map-indexed (fn [i v] {:label (nth days i) :value v})
                                                        [212 208 230 221 199 187 190 240 236 228 204 198 185 181]))})))))

   (example :bar-list "Ranked rows"
     (fn []
       (card/card {}
         (chart/chart {:title "Top pages"}
           (chart/bar-list
            {:rows [{:label "Home" :description "/" :value 812 :display "812" :note "40%" :href "#home"}
                    {:label "Pricing" :description "/pricing" :value 431 :note "21%" :href "#pricing" :active true}
                    {:label "Changelog" :description "/changelog" :value 289 :note "14%" :href "#changelog"}
                    {:label "Getting started" :description "/docs/getting-started" :value 204 :note "10%" :href "#docs"}
                    {:label "Careers" :description "/careers" :value 96 :note "5%" :href "#careers"}]})))))

   (example :donut "Shares of a whole"
     (fn []
       (card/card {}
         (chart/chart {:title "Where visitors come from"}
           (chart/donut
            {:center-value "2,030"
             :center-label "visitors"
             :segments [{:label "Direct" :value 1040 :display "1,040"}
                        {:label "Search" :value 520 :active true}
                        {:label "Social" :value 260}
                        {:label "Other sites" :value 150}
                        {:label "Newsletter" :value 45}
                        {:label "Campaigns" :value 15}]})))))

   (example :sparkline "Trend beside a number"
     (fn []
       (metric/metric-row {}
         (metric/metric {:label "Visitors" :value "2,030" :delta "+27%" :trend :up :hint "vs the 14 days before"
                         :chart (chart/sparkline {:values weekly :label "Visitors per week, last 13 weeks"})})
         (metric/metric {:label "Signups" :value "64" :delta "+8%" :trend :up :hint "vs before"
                         :chart (chart/sparkline {:values [3 5 4 6 4 7 5 6 8 5 7 6 9] :color :teal})})
         (metric/metric {:label "Bounce rate" :value "41" :unit "%" :delta "-3%" :trend :down :hint "vs before"
                         :chart (chart/sparkline {:values [48 47 49 46 45 46 44 45 43 44 42 42 41]
                                                  :color :orange :area false :zero false})}))))

   (example :states "Loading and empty"
     (fn []
       (card/card {}
         (chart/chart {:title "Visitors" :description "Loading the last 14 days"}
           (chart/area-chart {:loading true :points []}))
         (chart/chart {:title "Signups" :description "Today"}
           (chart/area-chart {:points [] :empty-label "No signups yet today"})))))])
