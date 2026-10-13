(ns dev.examples.timeline
  (:require [dev.example :refer [example note sx]]
            [ui.timeline :as timeline]))

(def examples
  [(example :static "Tracks, segments and transport"
     (fn []
       [:div {}
        (note "Interactive on Squint; other targets render the same markup statically.")
        [:div {:style (sx {:margin-top "0.75rem"})}
         (timeline/timeline
          {:duration 12.5
           :current 4.2
           :tracks [{:id :clip :label "Clip" :segments [{:id :c1 :start 0.8 :end 11.4 :label "video.mp4"}]}
                    {:id :zoom :label "Zoom" :segments [{:id :z1 :start 2.0 :end 5.5}
                                                        {:id :z2 :start 7.0 :end 9.0}]}]})]]))])
