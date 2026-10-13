(ns dev.examples.context-menu
  (:require [dev.example :refer [example row note sx]]
            [ui.context-menu :as context-menu]))

(defn- target [label & [dashed?]]
  [:div {:style (sx (cond-> {:padding "2rem" :border "var(--border-0)" :border-radius "var(--radius-panel)"
                             :cursor "context-menu" :text-align "center" :min-width "12rem"
                             :background "var(--bg-0)"}
                      dashed? (assoc :border-style "dashed")))}
   label])

(def examples
  [(example :menus "Right-click or long-press"
     (fn []
       [:div {}
        (note "Right-click the areas below, or long-press them on touch.")
        [:div {:style (sx {:margin-top "0.75rem"})}
         (row
          (context-menu/context-menu-trigger
           {:items [{:label "Edit"   :url "#edit"   :icon :edit}
                    {:label "Copy"   :url "#copy"   :icon :copy}
                    {:type :separator}
                    {:label "Delete" :url "#delete" :icon :trash :variant :danger :confirm true}]}
           (target "File actions"))
          (context-menu/context-menu-trigger
           {:items [{:label "View profile" :url "#profile" :icon :user}
                    {:label "Send message" :url "#message" :icon :mail}
                    {:label "Share"        :url "#share"   :icon :link}]}
           (target "Person actions" true)))]]))])
