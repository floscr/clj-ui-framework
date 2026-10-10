(ns dev.examples.card
  (:require [dev.example :refer [example]]
            [ui.card :as card]
            [ui.button :as button]))

(def examples
  [(example :basic "Header, body and footer"
     (fn []
       (card/card {}
         (card/card-header {} [:h4 "Card title"] [:p "Card description goes here."])
         (card/card-body {} [:p "This is the card content. It can contain any hiccup."])
         (card/card-footer {}
           (button/button {:variant :secondary :size :sm} "Cancel")
           (button/button {:variant :primary :size :sm} "Save changes")))))

   (example :list-full "Card list, full dividers"
     (fn []
       (card/card-list {}
         (card/card-list-item {} "Notifications")
         (card/card-list-item {} "Privacy")
         (card/card-list-item {} "Appearance")
         (card/card-list-item {} "Accessibility"))))

   (example :list-inset "Card list, inset dividers"
     (fn []
       (card/card-list {:divider :inset}
         (card/card-list-item {} "Notifications")
         (card/card-list-item {} "Privacy")
         (card/card-list-item {} "Appearance")
         (card/card-list-item {} "Accessibility"))))])
