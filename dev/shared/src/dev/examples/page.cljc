(ns dev.examples.page
  (:require [dev.example :refer [example sx]]
            [ui.util :as util]
            [ui.page :as page]
            [ui.button :as button]
            [ui.button-group :as button-group]
            [ui.settings :as settings]
            [ui.switch :as switch]
            [ui.metric :as metric]
            [ui.list-row :as list-row]))

(def ^:private frame
  {:border "var(--border-0)" :border-radius "var(--radius-panel)"
   :background "var(--bg-0)" :overflow "hidden"})

(def examples
  [(example :settings "A settings page: reading width, sections, action row"
     (fn []
       [:div {:style (sx frame)}
        (page/page {:tag :div}
          (page/page-header {:title "Settings"
                             :description "Changes apply to this account on every device."})
          (page/page-section {:title "Notifications" :description "What we email you about."}
            (settings/settings-group {}
              (settings/settings-row {:label "Mentions" :hint "When someone mentions you in a comment."}
                (switch/switch-toggle {:checked true}))
              (settings/settings-row {:label "Weekly summary" :hint "A Monday email with last week's activity."}
                (switch/switch-toggle {}))))
          (page/page-section {:title "Appearance"}
            (settings/settings-group {}
              (settings/settings-row {:label "Theme"}
                (button-group/button-group {:variant :boxed}
                  (button-group/button-group-item {:active true} "System")
                  (button-group/button-group-item {} "Light")
                  (button-group/button-group-item {} "Dark")))))
          (page/action-row {}
            (button/button {} "Cancel")
            (button/button {:variant :primary} "Save changes")))]))

   (example :dashboard "A dashboard: wide, header actions, metric row, list"
     (fn []
       [:div {:style (sx frame)}
        (page/page {:tag :div :width :wide}
          (page/page-header {:title "Overview" :description "Thu 29 Oct"}
            (button-group/button-group {:variant :boxed}
              (button-group/button-group-item {:active true} "7d")
              (button-group/button-group-item {} "30d")
              (button-group/button-group-item {} "90d"))
            (button/button {:variant :primary :icon-left :plus} "New goal"))
          (metric/metric-row {}
            (metric/metric {:label "Days in a row" :value "12" :unit "days" :delta "+3" :trend :up :hint "this week"})
            (metric/metric {:label "Best streak" :value "34" :unit "days"})
            (metric/metric {:label "Done this month" :value "18" :unit "of 22" :delta "82%" :hint "completion"}))
          (page/page-section {:title "Goals" :actions (button/button {:variant :ghost :size :sm} "See all")}
            (list-row/list-rows {}
              (list-row/list-row {:icon :check :title "Walk" :description "Mon to Fri" :meta "12 days" :href "#"})
              (list-row/list-row {:icon :moon :title "No screens after 22:00" :description "Every day" :meta "4 days" :href "#"})
              (list-row/list-row {:icon :sun :title "Morning stretch" :description "Mon, Wed, Fri" :meta "Not yet today" :href "#"}))))]))

   (example :widths "Widths"
     (fn []
       (into [:div {:style (sx {:display "flex" :flex-direction "column" :gap "0.75rem"})}]
             (map (fn [w]
                    [:div {:style (sx frame)}
                     (page/page {:tag :div :width w :attrs {:style (sx {:padding-block "var(--size-3)"})}}
                       [:div {:style (sx {:height "var(--size-8)" :border-radius "var(--radius-control)"
                                          :background "var(--accent-subtle)" :display "flex" :align-items "center"
                                          :justify-content "center" :font-size "var(--font-sm)" :color "var(--accent)"})}
                        (util/kw-name w)])])
                  [:narrow :reading :wide]))))])
