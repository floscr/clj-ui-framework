(ns dev.examples.tooltip
  (:require [dev.example :refer [example sx]]
            [ui.tooltip :as tooltip]
            [ui.button :as button]))

(def examples
  [(example :hover "Hover or focus"
     (fn []
       [:div {:style (sx {:display "flex" :gap "1.5rem" :padding-top "2rem"})}
        (tooltip/tooltip {:text "Save your changes"}
          (button/button {:variant :primary} "Save"))
        (tooltip/tooltip {:text "Delete this item"}
          (button/button {:variant :danger} "Delete"))
        (tooltip/tooltip {:text "View profile"}
          [:a {:href "#" :style (sx {:color "var(--accent)"})} "Profile"])]))])
