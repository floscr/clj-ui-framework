(ns dev.examples.time-picker
  (:require [dev.example :refer [example sx note]]
            [ui.time-picker :as time-picker]
            [ui.form :as form]))

(defn- cell [label field]
  [:div {:style (sx {:width "18rem"})}
   (form/form-field {:label label} field)])

(defn- cells [& children]
  (into [:div {:style (sx {:display "flex" :flex-wrap "wrap" :gap "1.5rem" :align-items "flex-end"})}]
        children))

(def examples
  [(example :range "Start and end"
     (fn []
       [:div {}
        (note "Focus a time and pick a slot, or type 1430, 2:30pm or 9 and press Enter. The end list counts the length; moving the start keeps it.")
        (cells
         (cell "Meeting" (time-picker/time-picker {:start "14:30" :end "15:45"}))
         (cell "Start only" (time-picker/time-picker {:start "09:00" :range true}))
         (cell "No time yet" (time-picker/time-picker {:range true :placeholder "All day"})))]))

   (example :single "One time"
     (fn []
       (cells
        (cell "Remind me at" (time-picker/time-picker {:value "08:15" :name "remind-at"}))
        (cell "Half-hour steps" (time-picker/time-picker {:value "18:30" :step 30}))
        (cell "Not clearable" (time-picker/time-picker {:value "12:00" :clearable false})))))

   (example :states "Forms and disabled"
     (fn []
       (cells
        (cell "In a form" (form/form-field {:label "Opening hours" :hint "Submitted as 08:00-17:00."}
                            (time-picker/time-picker {:value "08:00-17:00" :name "hours"})))
        (cell "Disabled" (time-picker/time-picker {:start "10:00" :end "11:00" :disabled true})))))])
