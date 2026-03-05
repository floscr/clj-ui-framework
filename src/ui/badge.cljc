(ns ui.badge
  (:require [clojure.string :as str]
            [ui.icon :as icon]))

#?(:squint (defn- kw-name [s] s)
   :cljs   (defn- kw-name [s] (name s))
   :clj    (defn- kw-name [s] (name s)))

(def default-variant "primary")

(defn badge-class-list
  "Generate a vector of CSS class strings for a badge.
   Variants: :primary (default), :secondary, :outline, :success, :warning, :danger."
  [{:keys [variant]}]
  (let [v (or (some-> variant kw-name) default-variant)]
    (if (= v "primary")
      ["badge"]
      ["badge" (str "badge-" v)])))

(defn badge-classes
  "Generate CSS class string for a badge."
  [opts]
  (str/join " " (badge-class-list opts)))

(defn badge
  "Render a badge element.

   Props:
     :variant   - :primary, :secondary, :outline, :success, :warning, :danger
     :icon-name - optional leading icon (e.g. :check, :star)
     :class     - additional CSS classes
     :attrs     - additional HTML attributes"
  [{:keys [variant icon-name class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> (badge-classes {:variant variant})
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       (into [:span base-attrs]
             (cond-> []
               icon-name (conj (icon/icon {:icon-name icon-name :size :sm :class "badge-icon"}))
               true      (into children))))

     :cljs
     (let [cls (badge-class-list {:variant variant})
           classes (cond-> cls class (conj class))
           base-attrs (merge {:class classes} attrs)]
       (into [:span base-attrs]
             (cond-> []
               icon-name (conj (icon/icon {:icon-name icon-name :size :sm :class "badge-icon"}))
               true      (into children))))

     :clj
     (let [classes (cond-> (badge-classes {:variant variant})
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       (into [:span base-attrs]
             (cond-> []
               icon-name (conj (icon/icon {:icon-name icon-name :size :sm :class "badge-icon"}))
               true      (into children))))))
