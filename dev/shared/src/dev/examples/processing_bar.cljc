(ns dev.examples.processing-bar
  (:require [dev.example :refer [example]]
            [ui.processing-bar :as processing-bar]))

(def examples
  [(example :jobs "Two jobs"
     (fn []
       (processing-bar/processing-bar {:title "2 uploads processing"}
         (processing-bar/processing-item {:thumb "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='36' height='36'%3E%3Crect fill='%234f46e5' width='36' height='36'/%3E%3C/svg%3E"
                                          :label "3 photos"
                                          :status "Analyzing"})
         (processing-bar/processing-item {:status "Optimizing images"}))))])
