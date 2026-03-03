(ns ui.breadcrumb
  (:require [clojure.string :as str]))

(defn breadcrumb
  "Render a breadcrumb navigation.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes
   Items: vector of {:label \"text\" :href \"/path\"}, last item is active."
  [{:keys [items class attrs] :as _props}]
  (let [n (count items)]
    #?(:squint
       (let [classes (cond-> "breadcrumb" class (str " " class))
             base-attrs (merge {:class classes} attrs)]
         (into [:nav {:aria-label "Breadcrumb"}
                (into [:ol base-attrs]
                      (map-indexed
                        (fn [i item]
                          (let [active (= i (dec n))]
                            [:li {:class (cond-> "breadcrumb-item"
                                           active (str " breadcrumb-item--active"))}
                             (if active
                               (:label item)
                               [:a {:href (:href item)} (:label item)])]))
                        items))]
               []))

       :cljs
       (let [cls (cond-> ["breadcrumb"] class (conj class))
             base-attrs (merge {:class cls} attrs)]
         [:nav {:aria-label "Breadcrumb"}
          (into [:ol base-attrs]
                (map-indexed
                  (fn [i item]
                    (let [active (= i (dec n))]
                      [:li {:class (cond-> ["breadcrumb-item"]
                                     active (conj "breadcrumb-item--active"))}
                       (if active
                         (:label item)
                         [:a {:href (:href item)} (:label item)])]))
                  items))])

       :clj
       (let [classes (cond-> "breadcrumb" class (str " " class))
             base-attrs (merge {:class classes} attrs)]
         [:nav {:aria-label "Breadcrumb"}
          (into [:ol base-attrs]
                (map-indexed
                  (fn [i item]
                    (let [active (= i (dec n))]
                      [:li {:class (cond-> "breadcrumb-item"
                                     active (str " breadcrumb-item--active"))}
                       (if active
                         (:label item)
                         [:a {:href (:href item)} (:label item)])]))
                  items))]))))
