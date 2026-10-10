(ns dev.examples.popover
  (:require [dev.example :refer [example row sx]]
            [ui.popover :as popover]
            [ui.button :as button]
            [ui.form :as form]))

(def ^:private trigger-cls (button/button-classes {:variant :secondary}))

(def examples
  [(example :basic "With a form"
     (fn []
       (row
        (popover/popover-trigger {:target "popover-basic" :class trigger-cls} "Open popover")
        (popover/popover-content {:id "popover-basic" :side :bottom :align :center}
          (popover/popover-header {}
            (popover/popover-title {} "Dimensions")
            (popover/popover-description {} "Set the dimensions for the layer."))
          [:div {:style (sx {:display "flex" :flex-direction "column" :gap "0.5rem"})}
           (form/form-field {:label "Width"}
             (form/form-input {:type :text :value "100%"}))
           (form/form-field {:label "Height"}
             (form/form-input {:type :text :value "25px"}))]))))

   (example :placement "Sides and alignment"
     (fn []
       (row
        (popover/popover-trigger {:target "popover-start" :class trigger-cls} "Align start")
        (popover/popover-content {:id "popover-start" :side :bottom :align :start}
          (popover/popover-title {} "Aligned to start"))
        (popover/popover-trigger {:target "popover-end" :class trigger-cls} "Align end")
        (popover/popover-content {:id "popover-end" :side :bottom :align :end}
          (popover/popover-title {} "Aligned to end"))
        (popover/popover-trigger {:target "popover-top" :class trigger-cls} "Side top")
        (popover/popover-content {:id "popover-top" :side :top :align :center}
          (popover/popover-title {} "Opens above"))
        (popover/popover-trigger {:target "popover-right" :class trigger-cls} "Side right")
        (popover/popover-content {:id "popover-right" :side :right :align :center}
          (popover/popover-title {} "Opens to the right")))))])
