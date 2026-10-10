(ns dev.examples.button-group
  (:require [dev.example :refer [example]]
            [ui.button-group :as button-group]))

(def examples
  [(example :boxed-counts "Boxed with counts"
     (fn []
       (button-group/button-group {:variant :boxed}
         (button-group/button-group-item {:active true :count 353} "All")
         (button-group/button-group-item {:count 28} "Downloaded")
         (button-group/button-group-item {:count 325} "Missing"))))

   (example :boxed-icons "Boxed icon toolbar"
     (fn []
       (button-group/button-group {:variant :boxed}
         (button-group/button-group-item {:icon :grid :active true :attrs {:aria-label "Grid"}})
         (button-group/button-group-item {:icon :layout-dashboard :attrs {:aria-label "Dashboard"}})
         (button-group/button-group-separator {})
         (button-group/button-group-item {:icon :minus :attrs {:aria-label "Smaller"}})
         (button-group/button-group-item {:icon :plus :attrs {:aria-label "Larger"}})
         (button-group/button-group-separator {})
         (button-group/button-group-item {:icon :settings :attrs {:aria-label "Settings"}}))))

   (example :ghost "Ghost cluster"
     (fn []
       (button-group/button-group {}
         (button-group/button-group-item {:icon :grid :active true :attrs {:aria-label "Grid"}})
         (button-group/button-group-item {:icon :layout-dashboard :attrs {:aria-label "Dashboard"}})
         (button-group/button-group-separator {})
         (button-group/button-group-item {:icon :settings :attrs {:aria-label "Settings"}}))))])
