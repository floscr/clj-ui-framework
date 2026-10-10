(ns dev.examples.toolbar
  (:require [dev.example :refer [example row]]
            [ui.toolbar :as toolbar]
            [ui.button :as button]
            [ui.popover :as popover]))

(def ^:private trigger-cls (button/button-classes {:variant :secondary}))

(def examples
  [(example :horizontal "Horizontal with separators"
     (fn []
       (toolbar/toolbar {}
         (button/button {:variant :ghost :icon :edit :attrs {:aria-label "Edit"}})
         (button/button {:variant :ghost :icon :copy :attrs {:aria-label "Copy"}})
         (button/button {:variant :ghost :icon :link :attrs {:aria-label "Link"}})
         (toolbar/toolbar-separator {})
         (button/button {:variant :ghost :icon :star :attrs {:aria-label "Star"}})
         (button/button {:variant :ghost :icon :bookmark :attrs {:aria-label "Bookmark"}})
         (toolbar/toolbar-separator {})
         (button/button {:variant :ghost :icon :trash :attrs {:aria-label "Delete"}}))))

   (example :vertical "Vertical"
     (fn []
       (toolbar/toolbar {:orientation :vertical}
         (button/button {:variant :ghost :icon :plus :attrs {:aria-label "Zoom in"}})
         (button/button {:variant :ghost :icon :minus :attrs {:aria-label "Zoom out"}})
         (toolbar/toolbar-separator {})
         (button/button {:variant :ghost :icon :search :attrs {:aria-label "Fit"}}))))

   (example :floating "Floating in a popover"
     (fn []
       (row
        (popover/popover-trigger {:target "toolbar-popover" :class trigger-cls} "Show toolbar")
        (popover/popover-content {:id "toolbar-popover" :side :top :align :center
                                  :class "popover-content--flush"}
          (toolbar/toolbar {}
            (button/button {:variant :ghost :icon :star :attrs {:aria-label "Star"}})
            (button/button {:variant :ghost :icon :heart :attrs {:aria-label "Like"}})
            (button/button {:variant :ghost :icon :copy :attrs {:aria-label "Copy"}})
            (toolbar/toolbar-separator {})
            (button/button {:variant :ghost :icon :trash :attrs {:aria-label "Delete"}}))))))])
