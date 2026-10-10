(ns dev.examples.panels
  (:require [dev.example :refer [example sx]]
            [ui.panels :as panels]))

(defn- pane [& children]
  (into [:div {:style (sx {:padding "1rem" :font-size "var(--font-sm)"
                           :color "var(--fg-2)" :height "100%"
                           :box-sizing "border-box"})}]
        children))

(defn- frame [height & children]
  (into [:div {:style (sx {:height height :border "var(--border-0)"
                           :border-radius "var(--radius-panel)" :overflow "hidden"})}]
        children))

(def examples
  [(example :sidebar "Collapsible sidebar with a pinned fill"
     (fn []
       (frame "260px"
         (panels/group {:orientation :horizontal}
           (panels/panel {:size 220 :min-size 140 :max-size 360 :collapsible true}
             (pane "Sidebar. Drag the seam, arrow keys resize, Enter collapses, double-click resets."))
           (panels/separator {:label "Resize sidebar"})
           (panels/panel {:pin true}
             (pane "Main content (pinned fill stays anchored while the sidebar folds)."))))))

   (example :vertical "Vertical"
     (fn []
       (frame "200px"
         (panels/group {:orientation :vertical}
           (panels/panel {}
             (pane "Fill area."))
           (panels/separator {:label "Resize bottom panel"})
           (panels/panel {:size 60 :min-size 40 :max-size 120}
             (pane "Bottom panel."))))))

   (example :bare-edge "Bare edge, no separator"
     (fn []
       (frame "180px"
         (panels/group {:orientation :horizontal}
           (panels/panel {}
             (pane "Fill area."))
           (panels/panel {:size "30%" :min-size 120}
             (pane "Bare edge panel (30%). Drag its left edge."))))))])
