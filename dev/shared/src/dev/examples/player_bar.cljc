(ns dev.examples.player-bar
  (:require [dev.example :refer [example]]
            [ui.player-bar :as player-bar]))

(def examples
  [(example :playing "Playing, favorited"
     (fn []
       (player-bar/player-bar {:track-name "Across The Universe" :subtitle "The Beatles"
                               :playing true :progress 35 :current-time "1:23" :duration "3:48"
                               :favorited true})))

   (example :paused "Paused, shuffle and repeat on"
     (fn []
       (player-bar/player-bar {:track-name "Bohemian Rhapsody" :subtitle "Queen"
                               :playing false :progress 0 :current-time "0:00" :duration "5:55"
                               :shuffle true :repeat true})))])
