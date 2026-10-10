(ns dev.examples.empty-state
  (:require [dev.example :refer [example sx]]
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
          [:p "No photos yet. Upload or take one."])]))])
