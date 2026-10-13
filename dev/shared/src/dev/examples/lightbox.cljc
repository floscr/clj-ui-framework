(ns dev.examples.lightbox
  (:require [dev.example :refer [example sx row]]
            [dev.examples.sample :as sample]
            [ui.lightbox :as lightbox]))

(def examples
  [(example :thumbnails "Thumbnails (sized by the host with a class)"
     (fn []
       (apply row
              (map (fn [{:keys [src alt]}] (lightbox/image-thumbnail {:src src :alt alt :class "example-thumb"}))
                   sample/images))))

   (example :overlay "Overlay (contained preview)"
     (fn []
       [:div {:class "example-contained"
              :style (sx {:position "relative" :height "300px" :border-radius "var(--radius-panel)" :overflow "hidden"})}
        (lightbox/lightbox {:src (:src (first sample/images)) :alt "Indigo"})]))])
