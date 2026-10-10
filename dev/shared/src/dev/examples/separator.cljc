(ns dev.examples.separator
  (:require [dev.example :refer [example sx]]
            [ui.separator :as separator]))

(def examples
  [(example :horizontal "Horizontal"
     (fn []
       [:div {:style (sx {:max-width "24rem"})}
        [:div {:style (sx {:display "flex" :flex-direction "column" :gap "0.375rem"})}
         [:div {:style (sx {:font-weight "500" :line-height "1"})} "Clojure UI"]
         [:div {:style (sx {:color "var(--fg-2)" :font-size "var(--font-sm)"})} "A cross-target component library"]]
        [:div {:style (sx {:margin "1rem 0"})}
         (separator/separator {})]
        [:p {:style (sx {:font-size "var(--font-sm)"})} "Build once, render everywhere: Hiccup, Replicant and Squint."]]))

   (example :vertical "Vertical"
     (fn []
       [:div {:style (sx {:display "flex" :align-items "center" :gap "1rem" :height "1.25rem"})}
        [:span {:style (sx {:font-size "var(--font-sm)"})} "Blog"]
        (separator/separator {:orientation :vertical})
        [:span {:style (sx {:font-size "var(--font-sm)"})} "Docs"]
        (separator/separator {:orientation :vertical})
        [:span {:style (sx {:font-size "var(--font-sm)"})} "Source"]]))])
