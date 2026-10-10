(ns dev.examples.button
  (:require [dev.example :refer [example row log-click]]
            [ui.util :as util]
            [ui.button :as button]))

(def variants [:primary :secondary :ghost :danger :success])
(def sizes [:sm :md :lg])

(def examples
  [(example :variants "Variants"
     (fn []
       (apply row
              (map (fn [v]
                     (button/button {:variant v :on-click (log-click (util/kw-name v))} (util/kw-name v)))
                   variants))))

   (example :sizes "Sizes"
     (fn []
       (apply row
              (map (fn [s]
                     (button/button {:variant :primary :size s} (str "size " (util/kw-name s))))
                   sizes))))

   (example :disabled "Disabled"
     (fn []
       (apply row
              (map (fn [v]
                     (button/button {:variant v :disabled true} (str (util/kw-name v) " disabled")))
                   variants))))

   (example :links "Links"
     (fn []
       (row
        (button/button {:variant :primary :href "#"} "Link primary")
        (button/button {:variant :secondary :href "#"} "Link secondary")
        (button/button {:variant :link} "Link button")
        (button/button {:variant :link :href "https://example.com"} "Link with href"))))

   (example :icons "With icons"
     (fn []
       (row
        (button/button {:variant :primary :icon-left :plus} "Add item")
        (button/button {:variant :secondary :icon-right :arrow-right} "Next")
        (button/button {:variant :primary :icon-left :download :icon-right :arrow-down} "Download")
        (button/button {:variant :ghost :icon-left :edit} "Edit"))))

   (example :icon-only "Icon only"
     (fn []
       (row
        (button/button {:variant :primary :icon :plus :attrs {:aria-label "Add"}})
        (button/button {:variant :secondary :icon :search :attrs {:aria-label "Search"}})
        (button/button {:variant :ghost :icon :settings :attrs {:aria-label "Settings"}})
        (button/button {:variant :danger :icon :trash :attrs {:aria-label "Delete"}})
        (button/button {:variant :primary :icon :plus :size :sm :attrs {:aria-label "Add"}})
        (button/button {:variant :primary :icon :plus :size :lg :attrs {:aria-label "Add"}}))))

   (example :round "Round"
     (fn []
       (row
        (button/button {:variant :primary :icon :play :round true :attrs {:aria-label "Play"}})
        (button/button {:variant :secondary :icon :pause :round true :attrs {:aria-label "Pause"}})
        (button/button {:variant :ghost :icon :heart :round true :attrs {:aria-label "Like"}})
        (button/button {:variant :primary :icon :plus :size :sm :round true :attrs {:aria-label "Add"}})
        (button/button {:variant :primary :icon :plus :size :lg :round true :attrs {:aria-label "Add"}}))))

   (example :loading "Loading"
     (fn []
       (row
        (button/button {:variant :primary :loading true} "Saving…")
        (button/button {:variant :success :loading true} "Publishing…")
        (button/button {:variant :secondary :loading true :size :sm} "Loading sm")
        (button/button {:variant :secondary :loading true :size :lg} "Loading lg")
        (button/button {:variant :primary :icon-left :upload :loading true} "Uploading…")
        (button/button {:variant :primary :icon :plus :loading true :attrs {:aria-label "Adding"}}))))])
