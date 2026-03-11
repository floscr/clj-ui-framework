(ns ui.separator
  (:require [clojure.string :as str]))

#?(:squint (defn- kw-name [s] s)
   :cljs   (defn- kw-name [s] (name s))
   :clj    (defn- kw-name [s] (name s)))

(defn separator-class-list
  "Generate a vector of CSS class strings for a separator.
   Orientation: :horizontal (default), :vertical."
  [{:keys [orientation]}]
  (let [o (or (some-> orientation kw-name) "horizontal")]
    ["separator" (str "separator-" o)]))

(defn separator-classes
  "Generate CSS class string for a separator."
  [opts]
  (str/join " " (separator-class-list opts)))

(defn separator
  "Render a separator element. Works across all targets via reader conditionals.

   Props:
     :orientation - :horizontal (default), :vertical
     :class       - additional CSS classes
     :attrs       - additional HTML attributes map"
  [{:keys [orientation class attrs] :as _props}]
  #?(:squint
     (let [classes (cond-> (separator-classes {:orientation orientation})
                     class (str " " class))
           base-attrs (merge {:class classes
                              :role "none"
                              :data-orientation (or (some-> orientation kw-name) "horizontal")}
                             attrs)]
       [:div base-attrs])

     :cljs
     (let [cls (separator-class-list {:orientation orientation})
           classes (cond-> cls class (conj class))
           base-attrs (merge {:class classes
                              :role "none"
                              :data-orientation (or (some-> orientation kw-name) "horizontal")}
                             attrs)]
       [:div base-attrs])

     :clj
     (let [classes (cond-> (separator-classes {:orientation orientation})
                     class (str " " class))
           base-attrs (merge {:class classes
                              :role "none"
                              :data-orientation (or (some-> orientation kw-name) "horizontal")}
                             attrs)]
       [:div base-attrs])))
