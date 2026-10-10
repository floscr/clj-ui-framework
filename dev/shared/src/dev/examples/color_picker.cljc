(ns dev.examples.color-picker
  (:require [dev.example :refer [example sx note]]
            [ui.color-picker :as color-picker]))

(defn- cell [label picker]
  [:div {:style (sx {:width "16rem"})}
   [:h4 {:style (sx {:margin-bottom "0.75rem"})} label]
   picker])

(def examples
  [(example :pickers "Formats, alpha and disabled"
     (fn []
       [:div {}
        (note "Drag the plane, move the tracks, or type any CSS color and press Enter.")
        [:div {:style (sx {:display "flex" :flex-wrap "wrap" :gap "1.5rem" :align-items "flex-start" :margin-top "0.75rem"})}
         (cell "All formats and opacity" (color-picker/color-picker {:value "#3b82f6" :alpha true}))
         (cell "Hex only" (color-picker/color-picker {:value "#1f2937" :formats [:hex]}))
         (cell "Disabled" (color-picker/color-picker {:value "hsl(140, 60%, 45%)" :disabled true}))]]))])
