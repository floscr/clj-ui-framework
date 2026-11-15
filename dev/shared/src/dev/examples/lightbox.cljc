(ns dev.examples.lightbox
  (:require [dev.example :refer [example sx row note]]
            [dev.examples.sample :as sample]
            [ui.lightbox :as lightbox]))

(def examples
  [(example :gallery "Gallery: thumbnails open a managed lightbox"
     (fn []
       [:div
        (note "Click a photo. Swipe or use the arrows, pinch / ctrl-scroll / double-click to zoom, drag down or flick a trackpad up or down to close.")
        (apply row
               (map-indexed (fn [i {:keys [src alt]}]
                              (lightbox/image-thumbnail {:src src :alt alt :class "example-thumb"
                                                         :lightbox "example-lightbox-gallery" :index i}))
                            sample/photos))
        (lightbox/lightbox {:id "example-lightbox-gallery"
                            :open false
                            :items sample/photos
                            :thumbnails true
                            :actions [(lightbox/action {:icon-name :download :label "Download"
                                                        :href (:src (first sample/photos)) :download true})]})]))

   (example :single "One image"
     (fn []
       (let [{:keys [src alt]} (first sample/images)]
         [:div
          (row (lightbox/image-thumbnail {:src src :alt alt :class "example-thumb"
                                          :lightbox "example-lightbox-single" :index 0}))
          (lightbox/lightbox {:id "example-lightbox-single" :open false :src src :alt alt})])))

   (example :overlay "Contained preview"
     (fn []
       [:div {:style (sx {:position "relative" :height "360px" :border-radius "var(--radius-panel)" :overflow "hidden"})}
        (lightbox/lightbox {:contained true :items sample/photos :index 1})]))])
