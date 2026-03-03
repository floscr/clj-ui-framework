(ns ui.tooltip
  (:require [clojure.string :as str]))

(defn tooltip
  "Render a tooltip wrapper. Shows tooltip text on hover.

   Props:
     :text  - tooltip text to display
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [text class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> "tooltip" class (str " " class))
           base-attrs (merge {:class classes :data-tooltip text} attrs)]
       (into [:span base-attrs] children))

     :cljs
     (let [cls (cond-> ["tooltip"] class (conj class))
           base-attrs (merge {:class cls :data-tooltip text} attrs)]
       (into [:span base-attrs] children))

     :clj
     (let [classes (cond-> "tooltip" class (str " " class))
           base-attrs (merge {:class classes :data-tooltip text} attrs)]
       (into [:span base-attrs] children))))
