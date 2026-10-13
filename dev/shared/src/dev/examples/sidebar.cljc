(ns dev.examples.sidebar
  (:require [dev.example :refer [example sx]]
            [ui.sidebar :as sidebar]))

(defn- block [style]
  [:div {:style (sx (merge {:background "var(--bg-1)" :border-radius "var(--radius-panel)" :border "var(--border-0)"} style))}])

(def examples
  [(example :app-shell "App shell"
     (fn []
       (sidebar/sidebar-layout {:attrs {:style (sx {:border "var(--border-0)" :border-radius "var(--radius-surface)"
                                                    :overflow "hidden" :height "520px" :min-height "0"})}}
         (sidebar/sidebar {:attrs {:style (sx {:height "100%" :position "static"})}}
           (sidebar/sidebar-header {}
             (sidebar/sidebar-brand {:title "Acme Inc." :subtitle "Enterprise" :icon "A"})
             (sidebar/sidebar-search {:placeholder "Search…"}))
           (sidebar/sidebar-content {}
             (sidebar/sidebar-group {:label "Getting started"}
               (sidebar/sidebar-menu {}
                 (sidebar/sidebar-menu-item {:href "#" :icon-name :download} "Installation")
                 (sidebar/sidebar-menu-item {:href "#" :icon-name :folder :active true} "Project structure")))
             (sidebar/sidebar-group {:label "Building"}
               (sidebar/sidebar-menu {}
                 (sidebar/sidebar-menu-item {:href "#" :icon-name :globe} "Routing")
                 (sidebar/sidebar-menu-item {:href "#" :icon-name :database :badge "New"} "Data fetching")
                 (sidebar/sidebar-menu-item {:href "#" :icon-name :layers} "Rendering")))
             (sidebar/sidebar-group {:label "API reference"}
               (sidebar/sidebar-collapsible {:title "Components" :open true}
                 (sidebar/sidebar-menu {}
                   (sidebar/sidebar-menu-item {:href "#"} "Button")
                   (sidebar/sidebar-menu-item {:href "#"} "Card")))))
           (sidebar/sidebar-footer {}
             (sidebar/sidebar-user {:user-name "Alice Johnson" :email "alice@example.com"})))
         (sidebar/sidebar-overlay {})
         (sidebar/sidebar-layout-main {}
           [:div {:style (sx {:padding "1.5rem" :display "flex" :flex-direction "column" :gap "1rem"})}
            [:div {:style (sx {:display "flex" :align-items "center" :gap "0.75rem"})}
             (sidebar/sidebar-mobile-toggle {})
             [:h3 {:style (sx {:margin "0"})} "Dashboard"]]
            [:div {:style (sx {:display "grid" :grid-template-columns "repeat(auto-fit, minmax(8rem, 1fr))" :gap "1rem"})}
             (block {:aspect-ratio "16/9"}) (block {:aspect-ratio "16/9"}) (block {:aspect-ratio "16/9"})]
            (block {:min-height "120px"})]))))])
