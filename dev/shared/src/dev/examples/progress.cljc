(ns dev.examples.progress
  (:require [dev.example :refer [example column]]
            [ui.progress :as progress]))

(def examples
  [(example :tones "Values and tones"
     (fn []
       (column
        (progress/progress {:value 25 :attrs {:aria-label "Upload"}})
        (progress/progress {:value 50 :variant :success :attrs {:aria-label "Import"}})
        (progress/progress {:value 75 :variant :warning :attrs {:aria-label "Quota"}})
        (progress/progress {:value 90 :variant :danger :attrs {:aria-label "Disk"}}))))])
