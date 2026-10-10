(ns dev.examples.camera
  (:require [dev.example :refer [example sx]]
            [ui.camera :as camera]
            [ui.toast :as toast]))

(def examples
  [(example :placeholder "Placeholder (native capture input)"
     (fn []
       [:div {:style (sx {:width "280px"})}
        (camera/camera-view {})]))

   (example :viewfinder "Live viewfinder markup"
     (fn []
       [:div {:style (sx {:width "280px"})}
        (camera/camera-view
         {:active? true
          :video-id "demo-camera-video"
          :on-capture #?(:squint (fn [] (toast/show-toast! {:message "Capture clicked" :variant :info}))
                         :cljs   (fn [] (toast/show-toast! {:message "Capture clicked" :variant :info}))
                         :clj    nil)
          :on-switch #?(:squint (fn [] (toast/show-toast! {:message "Switch clicked" :variant :info}))
                        :cljs   (fn [] (toast/show-toast! {:message "Switch clicked" :variant :info}))
                        :clj    true)})]))])
