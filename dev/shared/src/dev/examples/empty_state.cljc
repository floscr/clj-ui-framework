(ns dev.examples.empty-state
  (:require [dev.example :refer [example sx]]
            [ui.button :as button]
            [ui.empty-state :as empty-state]))

(def examples
  [(example :text "Text only"
     (fn []
       [:div {:style (sx {:border "var(--border-0)" :border-radius "var(--radius-panel)"})}
        (empty-state/empty-state {} "No items yet")]))

   (example :icon "With icon"
     (fn []
       [:div {:style (sx {:border "var(--border-0)" :border-radius "var(--radius-panel)"})}
        (empty-state/empty-state {:icon :inbox}
          [:p "No photos yet. Upload or take one."])]))

   (example :fill "Filling the view, with a title and an action"
     (fn []
       [:div {:style (sx {:border "var(--border-0)" :border-radius "var(--radius-panel)"
                          :display "flex" :flex-direction "column" :height "24rem"})}
        (empty-state/empty-state {:icon :users :fill true}
          [:p {:class "empty-state-title"} "No devices online"]
          [:p "Open this page on another device on the same Wi-Fi."]
          (button/button {:icon-left :refresh} "Check again"))]))])
