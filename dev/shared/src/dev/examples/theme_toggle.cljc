(ns dev.examples.theme-toggle
  (:require [dev.example :refer [example sx]]
            [ui.theme-toggle :as theme-toggle]))

(defn- labeled [label toggle]
  [:div {:style (sx {:display "flex" :flex-direction "column" :gap "0.5rem" :align-items "center"})}
   [:span {:style (sx {:font-size "var(--font-xs)" :color "var(--fg-2)"})} label]
   toggle])

(def examples
  [(example :modes "Sizes and modes"
     (fn []
       [:div {:style (sx {:display "flex" :gap "1.5rem" :align-items "flex-end" :flex-wrap "wrap"})}
        (labeled "Default" (theme-toggle/theme-toggle {:mode "auto"}))
        (labeled "Small" (theme-toggle/theme-toggle {:mode "auto" :size :sm}))
        (labeled "Light selected" (theme-toggle/theme-toggle {:mode "light"}))
        (labeled "Dark selected" (theme-toggle/theme-toggle {:mode "dark"}))]))])
