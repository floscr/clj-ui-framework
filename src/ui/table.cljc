(ns ui.table
  (:require [clojure.string :as str]
            [ui.util :as util]))

(defn table-class-list
  "Generate a vector of CSS class strings for a table.
   Variants: nil (default), :striped, :bordered."
  [{:keys [variant]}]
  (cond-> ["table"]
    variant (conj (str "table--" (util/kw-name variant)))))

(defn table-classes
  "Generate CSS class string for a table."
  [opts]
  (str/join " " (table-class-list opts)))

(defn table
  "Render a table element.

   Props:
     :headers - vector of column header strings
     :rows    - vector of row vectors (each row is a vector of cell values)
     :variant - nil, :striped, :bordered
     :class   - additional CSS classes
     :attrs   - additional HTML attributes"
  [{:keys [headers rows variant class attrs] :as _props}]
  #?(:squint
     (let [classes (cond-> (table-classes {:variant variant})
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       [:div {:class "table-wrapper"}
        [:table base-attrs
         (when (seq headers)
           [:thead
            (into [:tr]
                  (map (fn [h] [:th h]) headers))])
         (into [:tbody]
               (map (fn [row]
                      (into [:tr]
                            (map (fn [cell] [:td cell]) row)))
                    rows))]])

     :cljs
     (let [cls (table-class-list {:variant variant})
           classes (cond-> cls class (conj class))
           base-attrs (merge {:class classes} attrs)]
       [:div {:class ["table-wrapper"]}
        [:table base-attrs
         (when (seq headers)
           [:thead
            (into [:tr]
                  (map (fn [h] [:th h]) headers))])
         (into [:tbody]
               (map (fn [row]
                      (into [:tr]
                            (map (fn [cell] [:td cell]) row)))
                    rows))]])

     :clj
     (let [classes (cond-> (table-classes {:variant variant})
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       [:div {:class "table-wrapper"}
        [:table base-attrs
         (when (seq headers)
           [:thead
            (into [:tr]
                  (map (fn [h] [:th h]) headers))])
         (into [:tbody]
               (map (fn [row]
                      (into [:tr]
                            (map (fn [cell] [:td cell]) row)))
                    rows))]])))
