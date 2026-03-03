(ns ui.accordion
  (:require [clojure.string :as str]))

(defn accordion-class-list
  "Generate a vector of CSS class strings for an accordion item."
  [_opts]
  ["accordion"])

(defn accordion-classes
  "Generate CSS class string for an accordion."
  [opts]
  (str/join " " (accordion-class-list opts)))

(defn accordion
  "Render an accordion (collapsible) item using native <details>/<summary>.

   Props:
     :title - trigger text
     :open  - boolean, whether initially expanded
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [title open class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> (accordion-classes {})
                     class (str " " class))
           base-attrs (cond-> (merge {:class classes} attrs)
                        open (assoc :open true))]
       [:details base-attrs
        [:summary {:class "accordion-trigger"} title]
        (into [:div {:class "accordion-content"}] children)])

     :cljs
     (let [cls (accordion-class-list {})
           classes (cond-> cls class (conj class))
           base-attrs (cond-> (merge {:class classes} attrs)
                        open (assoc :open true))]
       [:details base-attrs
        [:summary {:class ["accordion-trigger"]} title]
        (into [:div {:class ["accordion-content"]}] children)])

     :clj
     (let [classes (cond-> (accordion-classes {})
                     class (str " " class))
           base-attrs (cond-> (merge {:class classes} attrs)
                        open (assoc :open true))]
       [:details base-attrs
        [:summary {:class "accordion-trigger"} title]
        (into [:div {:class "accordion-content"}] children)])))
