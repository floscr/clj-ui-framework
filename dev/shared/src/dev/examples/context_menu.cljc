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
           (target "Person actions" true)))]]))
   (example :confirm "Confirm destructive items"
     (fn []
       [:div {}
        (note "By default a :confirm item arms on the first click and runs on the second. :confirm-style :panel drills into a confirm page instead.")
        [:div {:style (sx {:margin-top "0.75rem"})}
         (row
          (context-menu/context-menu-trigger
           {:items [{:label "Rename" :url "#rename" :icon :edit}
                    {:type :separator}
                    {:label "Delete session" :url "#delete" :icon :trash
                     :variant :danger :confirm true}]}
           (target "Tap again (default)"))
          (context-menu/context-menu-trigger
           {:items [{:label "Rename" :url "#rename" :icon :edit}
                    {:type :separator}
                    {:label "Delete session" :url "#delete" :icon :trash
                     :variant :danger :confirm "The conversation and its file records are removed. This can't be undone."
                     :confirm-style :panel :confirm-title "Credit Card Format"}]}
           (target "Panel" true)))]]))])
