(ns dev.examples.tag-input
  (:require [dev.example :refer [example sx]]
            [ui.tag-input :as tag-input]))

(def examples
  [(example :open "Selected tags with suggestions"
     (fn []
       [:div {:style (sx {:max-width "480px" :min-height "220px"})}
        (tag-input/tag-input
         {:tags [{:label "Clojure" :value "clojure"}
                 {:label "Babashka" :value "babashka"}]
          :input-value ""
          :open true
          :filtered-items [{:label "React" :value "react"}
                           {:label "Squint" :value "squint"}
                           {:label "TypeScript" :value "typescript"}]
          :active-index 0
          :placeholder "Add frameworks…"})]))

   (example :empty "Empty"
     (fn []
       [:div {:style (sx {:max-width "480px"})}
        (tag-input/tag-input {:tags [] :input-value "" :placeholder "Add a label…"})]))])
