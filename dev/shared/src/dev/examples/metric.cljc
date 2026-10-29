(ns dev.examples.metric
  (:require [dev.example :refer [example]]
            [ui.metric :as metric]
            [ui.card :as card]))

(def examples
  [(example :row "A metric row"
     (fn []
       (metric/metric-row {}
         (metric/metric {:label "Revenue" :value "48.2k" :unit "EUR" :delta "+12%" :trend :up :hint "vs last month"})
         (metric/metric {:label "Orders" :value "1,204" :delta "-3%" :trend :down :hint "vs last month"})
         (metric/metric {:label "Refunds" :value "18" :delta "0" :hint "same as last month"})
         (metric/metric {:label "Open tickets" :value "7"}))))

   (example :flush "Flush, inside a card"
     (fn []
       (card/card {}
         (card/card-header {} [:h3 "Walk"] [:p "Mon to Fri · 7 days done"])
         (metric/metric-row {}
           (metric/metric {:flush true :label "Current streak" :value "0" :unit "days"})
           (metric/metric {:flush true :label "Best streak" :value "4" :unit "days"})
           (metric/metric {:flush true :label "This month" :value "12" :unit "of 22"})))))])
