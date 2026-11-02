(ns dev.examples.video-player
  (:require [dev.example :refer [example sx]]
            [ui.video-player :as vp]))

(def ^:private stream
  "Mux's public Big Buck Bunny test stream: five HLS levels, 240p to 1080p."
  "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")

(defn- frame [& children]
  (into [:div {:style (sx {:max-width "880px"})}] children))

(def examples
  [(example :series "Series: up next, autoplay and a custom sleep timer"
     (fn []
       (frame
        (vp/video-player
         {:src stream :title "Episode 1 · The meadow" :start-at 62
          :next {:title "Episode 2 · The apple"}
          :buttons [{:id :sleep :icon :moon :label "Sleep timer"
                     :options [{:value 0 :label "Off"}
                               {:value 15 :label "15 minutes"}
                               {:value 30 :label "30 minutes"}
                               {:value "end" :label "End of episode"}]
                     :value 0}]}))))

   (example :plain "Plain variant: no glass, controls on a gradient"
     (fn []
       (frame
        (vp/video-player
         {:src stream :title "Episode 1 · The meadow" :start-at 62 :variant :plain
          :next {:title "Episode 2 · The apple"}}))))

   (example :minimal "Exact controls: speed, a loop toggle, fullscreen"
     (fn []
       (frame
        (vp/video-player
         {:src stream :start-at 300
          :controls [:speed :loop :fullscreen]
          :buttons [{:id :loop :type :toggle :icon :repeat :label "Loop"}]}))))])
