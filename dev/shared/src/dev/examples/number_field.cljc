(ns dev.examples.number-field
  (:require [dev.example :refer [example sx note]]
            [ui.number-field :as number-field]
            [ui.form :as form]))

(defn- cell [label field]
  [:div {:style (sx {:width "11rem"})}
   (form/form-field {:label label} field)])

(defn- cells [& children]
  (into [:div {:style (sx {:display "flex" :flex-wrap "wrap" :gap "1.5rem" :align-items "flex-end"})}]
        children))

(def examples
  [(example :variants "Variants"
     (fn []
       [:div {}
        (note "Scroll over a field or click a stepper to change the value.")
        (cells
         (cell "Default" (number-field/number-field {:value 5 :min 0 :max 100}))
         (cell "Buttons right" (number-field/number-field {:value 5 :min 0 :max 100 :variant :right}))
         (cell "Spinner" (number-field/number-field {:value 5 :min 0 :max 100 :variant :spinner})))]))

   (example :sizes "Sizes"
     (fn []
       (cells
        (cell "Small" (number-field/number-field {:value 2 :min 0 :max 10 :size :sm}))
        (cell "Medium" (number-field/number-field {:value 2 :min 0 :max 10 :size :md}))
        (cell "Large" (number-field/number-field {:value 2 :min 0 :max 10 :size :lg}))
        (cell "Spinner small" (number-field/number-field {:value 2 :min 0 :max 10 :size :sm :variant :spinner}))
        (cell "Spinner large" (number-field/number-field {:value 2 :min 0 :max 10 :size :lg :variant :spinner}))
        (cell "Right large" (number-field/number-field {:value 2 :min 0 :max 10 :size :lg :variant :right})))))

   (example :steps "Steps, forms and disabled"
     (fn []
       (cells
        (cell "Step 0.5" (number-field/number-field {:value 1 :min 0 :max 10 :step 0.5}))
        (cell "In form" (form/form-field {:label "Amount" :hint "Between 10 and 100."}
                          (number-field/number-field {:value 5 :min 0 :max 100 :name "amount"})))
        (cell "Disabled" (number-field/number-field {:value 5 :disabled true})))))])
