(ns ui.accordion
  (:require [clojure.string :as str]))

(defn accordion-class-list
  "Generate a vector of CSS class strings for an accordion item."
  [{:keys [open]}]
  (cond-> ["accordion"]
    open (conj "accordion--open")))

(defn accordion-classes
  "Generate CSS class string for an accordion."
  [opts]
  (str/join " " (accordion-class-list opts)))

(defn accordion
  "Render an accordion (collapsible) item.

   Props:
     :title - trigger text
     :open  - boolean, whether expanded
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [title open class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> (accordion-classes {:open open})
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       (into [:div base-attrs
              [:div {:class "accordion-trigger"} title]]
             (when open
               [[:div {:class "accordion-content"}
                 (into [:div] children)]])))

     :cljs
     (let [cls (accordion-class-list {:open open})
           classes (cond-> cls class (conj class))
           base-attrs (merge {:class classes} attrs)]
       (into [:div base-attrs
              [:div {:class ["accordion-trigger"]} title]]
             (when open
               [[:div {:class ["accordion-content"]}
                 (into [:div] children)]])))

     :clj
     (let [classes (cond-> (accordion-classes {:open open})
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       (into [:div base-attrs
              [:div {:class "accordion-trigger"} title]]
             (when open
               [[:div {:class "accordion-content"}
                 (into [:div] children)]])))))
