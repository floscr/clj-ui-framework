(ns dev.examples.pagination
  (:require [dev.example :refer [example]]
            [ui.pagination :as pagination]))

(def examples
  [(example :pages "Five pages"
     (fn []
       (pagination/pagination {:current 3 :total 5
                               :href-fn (fn [p] (str "#page-" p))
                               :on-click #?(:squint (fn [p] (js/console.log (str "Page " p)))
                                            :cljs   (fn [p] (js/console.log (str "Page " p)))
                                            :clj    nil)})))])
