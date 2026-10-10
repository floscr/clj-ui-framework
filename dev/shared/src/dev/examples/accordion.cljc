(ns dev.examples.accordion
  (:require [dev.example :refer [example cls]]
            [ui.accordion :as accordion]))

(def examples
  [(example :group "FAQ group"
     (fn []
       [:div {:class (cls "accordion-group")}
        (accordion/accordion {:title "What is this framework?"} "A cross-target component library.")
        (accordion/accordion {:title "How do I use it?" :open true} "Require the namespace and call the functions.")
        (accordion/accordion {:title "Is it accessible?"} "Yes, it follows ARIA best practices.")]))])
