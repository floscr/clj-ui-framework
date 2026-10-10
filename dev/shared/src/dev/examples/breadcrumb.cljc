(ns dev.examples.breadcrumb
  (:require [dev.example :refer [example]]
            [ui.breadcrumb :as breadcrumb]))

(def examples
  [(example :basic "Four levels"
     (fn []
       (breadcrumb/breadcrumb
        {:items [{:label "Home" :href "#"}
                 {:label "Projects" :href "#"}
                 {:label "Harbour" :href "#"}
                 {:label "Components"}]})))])
