(ns dev.examples.badge
  (:require [dev.example :refer [example row]]
            [ui.badge :as badge]))

(def examples
  [(example :tones "Tones"
     (fn []
       (row
        (badge/badge {} "Default")
        (badge/badge {:variant :secondary} "Secondary")
        (badge/badge {:variant :outline} "Outline")
        (badge/badge {:variant :success} "Success")
        (badge/badge {:variant :warning} "Warning")
        (badge/badge {:variant :danger} "Danger"))))

   (example :icons "With icons"
     (fn []
       (row
        (badge/badge {:icon-name :check :variant :success} "Verified")
        (badge/badge {:icon-name :star} "Featured")
        (badge/badge {:icon-name :alert-triangle :variant :warning} "Caution")
        (badge/badge {:icon-name :clock :variant :secondary} "Pending"))))])
