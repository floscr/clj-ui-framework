(ns dev.examples.chip
  (:require [dev.example :refer [example row log-click]]
            [ui.chip :as chip]))

(def examples
  [(example :filters "Filter chips"
     (fn []
       (row
        (chip/chip {:active true :on-click (log-click "photos") :attrs {:aria-pressed "true"}} "Photos")
        (chip/chip {:on-click (log-click "videos") :attrs {:aria-pressed "false"}} "Videos")
        (chip/chip {:on-click (log-click "documents") :attrs {:aria-pressed "false"}} "Documents"))))

   (example :dots "Color presets"
     (fn []
       (row
        (chip/chip {:active true :dot-color "var(--accent-500)"} "Purple")
        (chip/chip {:dot-color "var(--blue-500)"} "Blue")
        (chip/chip {:dot-color "var(--teal-500)"} "Teal")
        (chip/chip {:dot-color "var(--orange-500)"} "Orange"))))])
