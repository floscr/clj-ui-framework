(ns ui.player-bar
  (:require [clojure.string :as str]
            [ui.icon :as icon]))

#?(:squint (defn- kw-name [s] s)
   :cljs   (defn- kw-name [s] (name s))
   :clj    (defn- kw-name [s] (name s)))

;; ── Class helpers ───────────────────────────────────────────────────

(defn player-bar-class-list
  "Returns a vector of CSS class strings for the player bar container."
  [_opts]
  ["player-bar"])

(defn player-bar-classes
  "Returns a space-joined class string for the player bar."
  [opts]
  (str/join " " (player-bar-class-list opts)))

;; ── Component ───────────────────────────────────────────────────────

(defn player-bar
  "Render a music player bar with transport controls, progress, and track info.

   Props:
     :track-name    - current track name (string)
     :playing       - boolean, currently playing
     :progress      - number 0-100, playback progress percentage
     :current-time  - formatted time string (e.g. \"1:23\")
     :duration      - formatted time string (e.g. \"4:56\")
     :shuffle       - boolean, shuffle mode active
     :repeat        - boolean, repeat mode active
     :favorited     - boolean, track is favorited
     :artwork       - optional hiccup for artwork slot
     :subtitle      - optional subtitle/artist text
     :on-play-pause - click handler (squint/cljs only)
     :on-next       - click handler
     :on-previous   - click handler
     :on-shuffle    - click handler
     :on-repeat     - click handler
     :on-favorite   - click handler
     :on-seek       - click handler for progress bar
     :on-volume     - click handler
     :class         - additional CSS classes
     :attrs         - additional HTML attributes"
  [{:keys [track-name playing progress current-time duration
           shuffle repeat favorited artwork subtitle
           on-play-pause on-next on-previous on-shuffle on-repeat
           on-favorite on-seek on-volume
           class attrs]}]
  (let [pct (str (or progress 0) "%")]
    #?(:squint
       (let [classes (cond-> (player-bar-classes {})
                       class (str " " class))
             base-attrs (merge {:class classes} attrs)]
         [:div base-attrs
          ;; Progress slider
          [:div.player-bar-progress
           (cond-> {}
             on-seek (assoc :on-click on-seek))
           [:div.player-bar-progress-track
            [:div.player-bar-progress-fill {:style {"width" pct}}]]]
          ;; Main row
          [:div.player-bar-row
           ;; Left: artwork + info + heart
           [:div.player-bar-left
            [:div.player-bar-artwork
             (or artwork (icon/icon {:icon-name :music :size :sm}))]
            [:div.player-bar-info
             [:p.player-bar-title track-name]
             (when subtitle
               [:p.player-bar-subtitle subtitle])]
            [:button.player-bar-icon-btn
             (cond-> {:class (when favorited "player-bar-fav-active")
                      :title (if favorited "Remove from favorites" "Add to favorites")}
               on-favorite (assoc :on-click on-favorite))
             (icon/icon {:icon-name :heart :size :sm})]]
           ;; Center: transport controls
           [:div.player-bar-center
            [:button.player-bar-icon-btn.player-bar-hide-sm
             (cond-> {:class (when shuffle "active")
                      :title "Shuffle"}
               on-shuffle (assoc :on-click on-shuffle))
             (icon/icon {:icon-name :shuffle :size :sm})]
            [:button.player-bar-icon-btn
             (cond-> {:title "Previous"}
               on-previous (assoc :on-click on-previous))
             (icon/icon {:icon-name :skip-back :size :sm})]
            [:button.player-bar-play-btn
             (cond-> {:title (if playing "Pause" "Play")}
               on-play-pause (assoc :on-click on-play-pause))
             (icon/icon {:icon-name (if playing :pause :play) :size :sm :filled true})]
            [:button.player-bar-icon-btn
             (cond-> {:title "Next"}
               on-next (assoc :on-click on-next))
             (icon/icon {:icon-name :skip-forward :size :sm})]
            [:button.player-bar-icon-btn.player-bar-hide-sm
             (cond-> {:class (when repeat "active")
                      :title "Repeat"}
               on-repeat (assoc :on-click on-repeat))
             (icon/icon {:icon-name :repeat :size :sm})]]
           ;; Right: time + volume
           [:div.player-bar-right
            (when (and current-time duration)
              [:span.player-bar-time (str current-time " / " duration)])
            [:button.player-bar-icon-btn
             (cond-> {:title "Volume"}
               on-volume (assoc :on-click on-volume))
             (icon/icon {:icon-name :volume-2 :size :sm})]]]])

       :cljs
       (let [cls (player-bar-class-list {})
             classes (cond-> cls class (conj class))
             base-attrs (merge {:class classes} attrs)]
         [:div base-attrs
          ;; Progress slider
          [:div (cond-> {:class ["player-bar-progress"]}
                  on-seek (assoc :on {:click on-seek}))
           [:div {:class ["player-bar-progress-track"]}
            [:div {:class ["player-bar-progress-fill"]
                   :style {:width pct}}]]]
          ;; Main row
          [:div {:class ["player-bar-row"]}
           ;; Left: artwork + info + heart
           [:div {:class ["player-bar-left"]}
            [:div {:class ["player-bar-artwork"]}
             (or artwork (icon/icon {:icon-name :music :size :sm}))]
            [:div {:class ["player-bar-info"]}
             [:p {:class ["player-bar-title"]} track-name]
             (when subtitle
               [:p {:class ["player-bar-subtitle"]} subtitle])]
            [:button (cond-> {:class (cond-> ["player-bar-icon-btn"]
                                      favorited (conj "player-bar-fav-active"))
                              :title (if favorited "Remove from favorites" "Add to favorites")}
                       on-favorite (assoc :on {:click on-favorite}))
             (icon/icon {:icon-name :heart :size :sm})]]
           ;; Center: transport controls
           [:div {:class ["player-bar-center"]}
            [:button (cond-> {:class (cond-> ["player-bar-icon-btn" "player-bar-hide-sm"]
                                      shuffle (conj "active"))
                              :title "Shuffle"}
                       on-shuffle (assoc :on {:click on-shuffle}))
             (icon/icon {:icon-name :shuffle :size :sm})]
            [:button (cond-> {:class ["player-bar-icon-btn"]
                              :title "Previous"}
                       on-previous (assoc :on {:click on-previous}))
             (icon/icon {:icon-name :skip-back :size :sm})]
            [:button (cond-> {:class ["player-bar-play-btn"]
                              :title (if playing "Pause" "Play")}
                       on-play-pause (assoc :on {:click on-play-pause}))
             (icon/icon {:icon-name (if playing :pause :play) :size :sm :filled true})]
            [:button (cond-> {:class ["player-bar-icon-btn"]
                              :title "Next"}
                       on-next (assoc :on {:click on-next}))
             (icon/icon {:icon-name :skip-forward :size :sm})]
            [:button (cond-> {:class (cond-> ["player-bar-icon-btn" "player-bar-hide-sm"]
                                      repeat (conj "active"))
                              :title "Repeat"}
                       on-repeat (assoc :on {:click on-repeat}))
             (icon/icon {:icon-name :repeat :size :sm})]]
           ;; Right: time + volume
           [:div {:class ["player-bar-right"]}
            (when (and current-time duration)
              [:span {:class ["player-bar-time"]} (str current-time " / " duration)])
            [:button (cond-> {:class ["player-bar-icon-btn"]
                              :title "Volume"}
                       on-volume (assoc :on {:click on-volume}))
             (icon/icon {:icon-name :volume-2 :size :sm})]]]])

       :clj
       (let [classes (cond-> (player-bar-classes {})
                       class (str " " class))
             base-attrs (merge {:class classes} attrs)]
         [:div base-attrs
          ;; Progress slider
          [:div {:class "player-bar-progress"}
           [:div {:class "player-bar-progress-track"}
            [:div {:class "player-bar-progress-fill"
                   :style (str "width: " pct)}]]]
          ;; Main row
          [:div {:class "player-bar-row"}
           ;; Left: artwork + info + heart
           [:div {:class "player-bar-left"}
            [:div {:class "player-bar-artwork"}
             (or artwork (icon/icon {:icon-name :music :size :sm}))]
            [:div {:class "player-bar-info"}
             [:p {:class "player-bar-title"} track-name]
             (when subtitle
               [:p {:class "player-bar-subtitle"} subtitle])]
            [:button {:class (str "player-bar-icon-btn"
                                  (when favorited " player-bar-fav-active"))
                      :title (if favorited "Remove from favorites" "Add to favorites")}
             (icon/icon {:icon-name :heart :size :sm})]]
           ;; Center: transport controls
           [:div {:class "player-bar-center"}
            [:button {:class (str "player-bar-icon-btn player-bar-hide-sm"
                                  (when shuffle " active"))
                      :title "Shuffle"}
             (icon/icon {:icon-name :shuffle :size :sm})]
            [:button {:class "player-bar-icon-btn"
                      :title "Previous"}
             (icon/icon {:icon-name :skip-back :size :sm})]
            [:button {:class "player-bar-play-btn"
                      :title (if playing "Pause" "Play")}
             (icon/icon {:icon-name (if playing :pause :play) :size :sm :filled true})]
            [:button {:class "player-bar-icon-btn"
                      :title "Next"}
             (icon/icon {:icon-name :skip-forward :size :sm})]
            [:button {:class (str "player-bar-icon-btn player-bar-hide-sm"
                                  (when repeat " active"))
                      :title "Repeat"}
             (icon/icon {:icon-name :repeat :size :sm})]]
           ;; Right: time + volume
           [:div {:class "player-bar-right"}
            (when (and current-time duration)
              [:span {:class "player-bar-time"} (str current-time " / " duration)])
            [:button {:class "player-bar-icon-btn"
                      :title "Volume"}
             (icon/icon {:icon-name :volume-2 :size :sm})]]]]))))
