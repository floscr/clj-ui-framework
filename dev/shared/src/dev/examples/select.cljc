(ns dev.examples.select
  (:require [dev.example :refer [example sx]]
            [ui.select :as select]))

(def ^:private regions
  [{:value "eu" :label "Europe"}
   {:value "us" :label "United States"}
   {:value "ap" :label "Asia Pacific"}])

(def examples
  [(example :basic "With a value and a placeholder"
     (fn []
       [:div {:style (sx {:display "flex" :flex-direction "column" :gap "0.75rem" :max-width "20rem"})}
        (select/select {:options regions :value "eu" :name "region"})
        (select/select {:options regions :placeholder "Pick a region" :name "region-2"})
        (select/select {:options regions :value "us" :disabled true})]))])
