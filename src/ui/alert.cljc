(ns ui.alert
  (:require [clojure.string :as str]))

#?(:squint (defn- kw-name [s] s)
   :cljs   (defn- kw-name [s] (name s))
   :clj    (defn- kw-name [s] (name s)))

(defn alert-class-list
  "Generate a vector of CSS class strings for an alert.
   Variants: :success, :warning, :danger, :info (default: nil = neutral)."
  [{:keys [variant]}]
  (cond-> ["alert"]
    variant (conj (str "alert-" (kw-name variant)))))

(defn alert-classes
  "Generate CSS class string for an alert."
  [opts]
  (str/join " " (alert-class-list opts)))

(defn alert
  "Render an alert element.

   Props:
     :variant - :success, :warning, :danger, :info (nil for neutral)
     :title   - optional title string
     :class   - additional CSS classes
     :attrs   - additional HTML attributes"
  [{:keys [variant title class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> (alert-classes {:variant variant})
                     class (str " " class))
           base-attrs (merge {:class classes :role "alert"} attrs)]
       (into [:div base-attrs]
             (cond-> []
               title (conj [:p {:class "alert-title"} title])
               :always (into (map (fn [c] [:p {:class "alert-body"} c]) children)))))

     :cljs
     (let [cls (alert-class-list {:variant variant})
           classes (cond-> cls class (conj class))
           base-attrs (merge {:class classes :role "alert"} attrs)]
       (into [:div base-attrs]
             (cond-> []
               title (conj [:p {:class ["alert-title"]} title])
               :always (into (map (fn [c] [:p {:class ["alert-body"]} c]) children)))))

     :clj
     (let [classes (cond-> (alert-classes {:variant variant})
                     class (str " " class))
           base-attrs (merge {:class classes :role "alert"} attrs)]
       (into [:div base-attrs]
             (cond-> []
               title (conj [:p {:class "alert-title"} title])
               :always (into (map (fn [c] [:p {:class "alert-body"} c]) children)))))))
