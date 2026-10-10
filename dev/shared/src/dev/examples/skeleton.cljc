(ns dev.examples.skeleton
  (:require [dev.example :refer [example sx]]
            [ui.skeleton :as skeleton]))

(def examples
  [(example :card "Loading card"
     (fn []
       [:div {:style (sx {:max-width "400px"})}
        (skeleton/skeleton {:variant :heading})
        (skeleton/skeleton {:variant :line})
        (skeleton/skeleton {:variant :line})
        [:div {:style (sx {:display "flex" :gap "1rem" :margin-top "var(--size-3)"})}
         (skeleton/skeleton {:variant :circle})
         [:div {:style (sx {:flex "1"})}
          (skeleton/skeleton {:variant :line})
          (skeleton/skeleton {:variant :line})]]]))])
