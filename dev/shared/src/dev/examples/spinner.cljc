(ns dev.examples.spinner
  (:require [dev.example :refer [example sx]]
            [ui.spinner :as spinner]))

(def examples
  [(example :sizes "Sizes"
     (fn []
       [:div {:style (sx {:display "flex" :gap "1.5rem" :align-items "center"})}
        (spinner/spinner {:size :xs})
        (spinner/spinner {:size :sm})
        (spinner/spinner {})
        (spinner/spinner {:size :lg})]))])
