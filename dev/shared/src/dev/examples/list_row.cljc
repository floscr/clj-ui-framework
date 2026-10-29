(ns dev.examples.list-row
  (:require [dev.example :refer [example log-click]]
            [ui.list-row :as list-row]
            [ui.button :as button]
            [ui.badge :as badge]))

(def ^:private thumb
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 40 40'%3E%3Crect width='40' height='40' fill='%23c4b5fd'/%3E%3Ccircle cx='20' cy='17' r='8' fill='%237c5cfc'/%3E%3C/svg%3E")

(def examples
  [(example :navigation "Navigation rows with icons and counts"
     (fn []
       (list-row/list-rows {}
         (list-row/list-row {:icon :music :title "Favorites" :meta "21" :href "#"})
         (list-row/list-row {:icon :music :title "Recently played" :meta "50" :href "#"})
         (list-row/list-row {:icon :music :title "Mixtapes" :meta "79" :href "#"})
         (list-row/list-row {:icon :folder :title "All music" :meta "1262" :href "#"})
         (list-row/list-row {:icon :folder :title "Night drive · Late night drive · Deep house, melancholic dance, electronic dance music"
                             :meta "100" :href "#"}))))

   (example :records "Records with a thumbnail, description and actions"
     (fn []
       (list-row/list-rows {}
         (list-row/list-row {:thumb thumb :title "Super Mario 3D World + Bowser's Fury"
                             :description "35 EUR · Good · 15 Sep 08:35"}
           (badge/badge {:variant :success :size :sm} "Published")
           (button/button {:variant :ghost :size :sm :icon :edit :attrs {:aria-label "Edit"}}))
         (list-row/list-row {:thumb thumb :title "Super Mario Bros. Wonder"
                             :description "38 EUR · Like new · 15 Sep 08:35"}
           (button/button {:variant :ghost :size :sm :icon :edit :attrs {:aria-label "Edit"}})
           (button/button {:variant :primary :size :sm} "Publish"))
         (list-row/list-row {:thumb thumb :title "Metroid Dread"
                             :description "30 EUR · Like new · 15 Sep 08:34"}
           (button/button {:variant :ghost :size :sm :icon :edit :attrs {:aria-label "Edit"}})
           (button/button {:variant :primary :size :sm} "Publish")))))

   (example :selection "Selectable rows"
     (fn []
       (list-row/list-rows {}
         (list-row/list-row {:title "Linux" :description "This device" :active true :on-click (log-click "Linux")})
         (list-row/list-row {:title "Pixel 8" :description "Online" :on-click (log-click "Pixel 8")})
         (list-row/list-row {:title "Studio" :description "Last seen yesterday" :on-click (log-click "Studio")}))))])
