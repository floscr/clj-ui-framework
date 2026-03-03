(ns ui.skeleton
  (:require [clojure.string :as str]))

#?(:squint (defn- kw-name [s] s)
   :cljs   (defn- kw-name [s] (name s))
   :clj    (defn- kw-name [s] (name s)))

(defn skeleton-class-list
  "Generate a vector of CSS class strings for a skeleton placeholder.
   Variants: :line (text line), :box (square), :circle, :heading."
  [{:keys [variant]}]
  (cond-> ["skeleton"]
    variant (conj (str "skeleton--" (kw-name variant)))))

(defn skeleton-classes
  "Generate CSS class string for a skeleton."
  [opts]
  (str/join " " (skeleton-class-list opts)))

(defn skeleton
  "Render a skeleton loading placeholder.

   Props:
     :variant - :line, :box, :circle, :heading
     :class   - additional CSS classes
     :attrs   - additional HTML attributes"
  [{:keys [variant class attrs] :as _props}]
  #?(:squint
     (let [classes (cond-> (skeleton-classes {:variant variant})
                     class (str " " class))
           base-attrs (merge {:class classes :role "status" :aria-label "Loading"} attrs)]
       [:div base-attrs])

     :cljs
     (let [cls (skeleton-class-list {:variant variant})
           classes (cond-> cls class (conj class))
           base-attrs (merge {:class classes :role "status" :aria-label "Loading"} attrs)]
       [:div base-attrs])

     :clj
     (let [classes (cond-> (skeleton-classes {:variant variant})
                     class (str " " class))
           base-attrs (merge {:class classes :role "status" :aria-label "Loading"} attrs)]
       [:div base-attrs])))
