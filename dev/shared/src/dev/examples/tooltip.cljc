(ns dev.examples.tooltip
  (:require [dev.example :refer [example note row sx]]
            [ui.badge :as badge]
            [ui.button :as button]
            [ui.day-grid :as day-grid]
            [ui.icon :as icon]
            [ui.list-row :as list-row]
            [ui.metric :as metric]
            [ui.progress :as progress]
            [ui.toolbar :as toolbar]
            [ui.tooltip :as tooltip]))

(defn- icon-button [icon-name label shortcut side]
  (tooltip/tooltip {:text label :shortcut shortcut :side side}
    (button/button {:variant :ghost :icon icon-name :attrs {:aria-label label}})))

(def ^:private muted
  (sx {:margin "0" :color "var(--fg-1)" :font-size "var(--font-xs)"
       :display "flex" :align-items "center" :gap "var(--size-1)"}))

(def ^:private avatar
  (str "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 32'%3E"
       "%3Crect width='32' height='32' fill='%23c2410c'/%3E"
       "%3Ctext x='16' y='20.5' font-family='system-ui' font-size='12' fill='white' text-anchor='middle'%3EMO%3C/text%3E%3C/svg%3E"))

(def ^:private thumbnail
  (str "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 160 90'%3E"
       "%3Cdefs%3E%3ClinearGradient id='g' x2='1' y2='1'%3E%3Cstop stop-color='%23f0abfc'/%3E"
       "%3Cstop offset='1' stop-color='%234338ca'/%3E%3C/linearGradient%3E%3C/defs%3E"
       "%3Crect width='160' height='90' fill='url(%23g)'/%3E%3Ccircle cx='44' cy='30' r='16' fill='%23fde68a'/%3E%3C/svg%3E"))

(defn- person-card []
  (tooltip/tooltip-card {}
    (list-row/list-row {:thumb avatar :title "Mara Okafor" :description "Design engineer, Platform"})
    [:p {:style (sx {:margin "0"})} "Owns the component library and the theme tokens. Reviews UI changes on Tuesdays."]
    [:div {:style (sx {:display "flex" :gap "var(--size-5)" :color "var(--fg-1)"})}
     [:span [:strong {:style (sx {:font-weight "500" :color "var(--fg-0)"})} "42"] " sessions"]
     [:span [:strong {:style (sx {:font-weight "500" :color "var(--fg-0)"})} "57"] " reviews"]]
    [:p {:style muted} (icon/icon {:icon-name :clock :size :sm}) "14:32 in Vienna"]))

(defn- repo-card []
  (tooltip/tooltip-card {}
    (list-row/list-row {:icon :code :title "floscr/clj-ui-framework" :description "github.com"})
    [:p {:style (sx {:margin "0"})} "Cross-target UI components for Clojure, ClojureScript and Squint."]
    [:p {:style muted} (icon/icon {:icon-name :star :size :sm}) "128 · updated 2 h ago"]))

(defn- event-card []
  (tooltip/tooltip-card {}
    (list-row/list-row {:icon :calendar :title "Design review" :description "Wed, 14:00 to 14:45"})
    [:p {:style muted} (icon/icon {:icon-name :users :size :sm}) "3 going · Room Ostbahn"]))

(defn- metric-card []
  (tooltip/tooltip-card {}
    (metric/metric {:label "p95 response, 24 h" :value "182" :unit "ms"
                    :delta "+12%" :trend :down :hint "vs yesterday" :flush true})
    [:svg {:viewBox "0 0 120 32" :preserveAspectRatio "none"
           :style (sx {:width "100%" :height "2rem" :display "block"})}
     [:polyline {:points "0,26 10,24 20,25 30,20 40,22 50,16 60,18 70,13 80,15 90,8 100,11 110,6 120,7"
                 :fill "none" :stroke "var(--accent)" :stroke-width "1.5"
                 :vector-effect "non-scaling-stroke" :stroke-linejoin "round"}]]))

(defn- deploy-card []
  (let [step (fn [icon-name label] [:p {:style muted} (icon/icon {:icon-name icon-name :size :sm}) label])]
    (tooltip/tooltip-card {}
      [:div {:style (sx {:display "flex" :justify-content "space-between"})}
       [:strong {:style (sx {:font-weight "500"})} "Deploy hetzner--xi"]
       [:span {:style (sx {:color "var(--fg-1)"})} "72%"]]
      (progress/progress {:value 72 :attrs {:aria-label "Deploy progress"}})
      (step :circle-check "Build, 38 s")
      (step :circle-check "Push closure, 12 s")
      (step :refresh "Restart service")
      (step :clock "Health check"))))

(defn- streak-card []
  (tooltip/tooltip-card {}
    [:div {:style (sx {:display "flex" :justify-content "space-between"})}
     [:strong {:style (sx {:font-weight "500"})} "Morning run"]
     [:span {:style (sx {:color "var(--fg-1)"})} "12 day streak"]]
    (day-grid/day-grid
     {:color :teal
      :days (into (mapv (fn [state] {:state state})
                        [:done :done :missed :done :done :rest :rest
                         :done :done :done :done :done :rest])
                  [{:state :done :today true}])})))

(defn- image-card []
  (tooltip/tooltip-card {}
    [:img {:src thumbnail :alt "" :style (sx {:width "100%" :border-radius "var(--radius-md)" :display "block"})}]
    [:div {:style (sx {:display "flex" :justify-content "space-between"})}
     [:strong {:style (sx {:font-weight "500"})} "hero.webp"]
     [:span {:style (sx {:color "var(--fg-1)"})} "184 KB"]]
    [:p {:style muted} "1600 × 900 · added by Mara"]))

(def ^:private inline-trigger
  (sx {:color "var(--accent)" :text-decoration "underline"
       :text-decoration-color "var(--accent-subtle)" :text-underline-offset "3px"}))

(def examples
  [(example :toolbar "First waits, the next ones open instantly"
     (fn []
       [:div
        (note "Rest on one button for the delay, then slide along the toolbar: the tooltip jumps without animating.")
        [:div {:style (sx {:padding-top "2.5rem"})}
         (toolbar/toolbar {}
           (icon-button :edit "Rename" ["F2"] nil)
           (icon-button :copy "Copy link" ["⌘" "⇧" "C"] nil)
           (icon-button :link "Insert link" ["⌘" "K"] nil)
           (toolbar/toolbar-separator {})
           (icon-button :download "Download" nil nil)
           (icon-button :trash "Moves the file to the trash for everyone in this room" nil :bottom))]]))

   (example :delay "Delays"
     (fn []
       [:div
        (note "Each tooltip can wait its own time; data-tooltip-delay on an ancestor sets it for a group.")
        (row
         (tooltip/tooltip {:text "Opens at once" :delay 0}
           (button/button {:variant :secondary} "No delay"))
         (tooltip/tooltip {:text "Opens after 250 ms" :delay 250}
           (button/button {:variant :secondary} "250 ms"))
         (tooltip/tooltip {:text "Opens after the default 500 ms"}
           (button/button {:variant :secondary} "Default"))
         (tooltip/tooltip {:text "Opens after a second" :delay 1000}
           (button/button {:variant :secondary} "1 s")))]))

   (example :sides "Sides"
     (fn []
       (row
        (tooltip/tooltip {:text "Above" :side :top} (button/button {:variant :secondary} "Top"))
        (tooltip/tooltip {:text "Below" :side :bottom} (button/button {:variant :secondary} "Bottom"))
        (tooltip/tooltip {:text "To the left" :side :left} (button/button {:variant :secondary} "Left"))
        (tooltip/tooltip {:text "To the right" :side :right} (button/button {:variant :secondary} "Right")))))

   (example :rich "Rich content"
     (fn []
       [:div {:style (sx {:display "flex" :flex-direction "column" :gap "var(--size-4)"})}
        [:p {:style (sx {:margin "0" :font-size "var(--font-md)" :line-height "1.8"})}
         "Ask "
         (tooltip/tooltip {:content (person-card) :delay 600 :attrs {:tabindex 0 :style inline-trigger}} "@mara")
         " to review the "
         (tooltip/tooltip {:content (repo-card) :delay 700}
           [:a {:href "#" :style inline-trigger} "clj-ui-framework"])
         " change before the "
         (tooltip/tooltip {:content (event-card) :delay 300 :attrs {:tabindex 0 :style inline-trigger}} "design review")
         "."]
        (row
         (tooltip/tooltip {:content (metric-card) :delay 250 :attrs {:tabindex 0}}
           (badge/badge {:variant :secondary :icon-name :gauge} "p95 182 ms"))
         (tooltip/tooltip {:content (deploy-card) :delay 150 :attrs {:tabindex 0}}
           (badge/badge {:variant :success :icon-name :upload} "Deploying 72%"))
         (tooltip/tooltip {:content (streak-card) :delay 400 :attrs {:tabindex 0}}
           (badge/badge {:variant :secondary :icon-name :zap} "12 day streak"))
         (tooltip/tooltip {:content (image-card) :side :bottom :attrs {:tabindex 0}}
           (badge/badge {:variant :secondary :icon-name :image} "hero.webp")))]))

   (example :tone "Error tone"
     (fn []
       (row
        (tooltip/tooltip {:text "Use at least 8 characters" :tone :danger :delay 0 :attrs {:tabindex 0}}
          [:span {:style (sx {:color "var(--danger)" :display "inline-flex"})}
           (icon/icon {:icon-name :circle-x :size :sm})]))))])
