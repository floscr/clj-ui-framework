(ns ui.card
  (:require [clojure.string :as str]
            [ui.util :as util]))

(defn card-class-list
  "Generate a vector of CSS class strings for a card."
  [_opts]
  ["card"])

(defn card-classes
  "Generate CSS class string for a card."
  [opts]
  (str/join " " (card-class-list opts)))

(defn card
  "Render a card element.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> (card-classes {})
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       (into [:article base-attrs] children))

     :cljs
     (let [cls (card-class-list {})
           classes (cond-> cls class (conj class))
           base-attrs (merge {:class classes} attrs)]
       (into [:article base-attrs] children))

     :clj
     (let [classes (cond-> (card-classes {})
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       (into [:article base-attrs] children))))

(defn card-header
  "Render a card header section."
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:header (merge {:class (cond-> "card-header" class (str " " class))} attrs)] children)
     :cljs
     (into [:header (merge {:class (cond-> ["card-header"] class (conj class))} attrs)] children)
     :clj
     (into [:header (merge {:class (cond-> "card-header" class (str " " class))} attrs)] children)))

(defn card-body
  "Render a card body section."
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:div (merge {:class (cond-> "card-body" class (str " " class))} attrs)] children)
     :cljs
     (into [:div (merge {:class (cond-> ["card-body"] class (conj class))} attrs)] children)
     :clj
     (into [:div (merge {:class (cond-> "card-body" class (str " " class))} attrs)] children)))

(defn card-footer
  "Render a card footer section."
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:footer (merge {:class (cond-> "card-footer" class (str " " class))} attrs)] children)
     :cljs
     (into [:footer (merge {:class (cond-> ["card-footer"] class (conj class))} attrs)] children)
     :clj
     (into [:footer (merge {:class (cond-> "card-footer" class (str " " class))} attrs)] children)))

;; ─── Card List ─────────────────────────────────────────────────────

(defn card-list-class-list
  "Generate a vector of CSS class strings for a card-list.
   Options:
     :divider - :full (default) or :inset (borders have horizontal spacing from edges)"
  [{:keys [divider]}]
  (let [d (or (some-> divider util/kw-name) "full")]
    (cond-> ["card-list"]
      (= d "inset") (conj "card-list-inset"))))

(defn card-list-classes
  "Generate CSS class string for a card-list."
  [opts]
  (str/join " " (card-list-class-list opts)))

(defn card-list
  "Render a card-list container — a card background with items separated by borders.

   Props:
     :divider - :full (default, borders go edge-to-edge) or :inset (borders inset from edges)
     :class   - additional CSS classes
     :attrs   - additional HTML attributes"
  [{:keys [divider class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> (card-list-classes {:divider divider})
                     class (str " " class))
           base-attrs (merge {:class classes :role "list"} attrs)]
       (into [:div base-attrs] children))

     :cljs
     (let [cls (card-list-class-list {:divider divider})
           classes (cond-> cls class (conj class))
           base-attrs (merge {:class classes :role "list"} attrs)]
       (into [:div base-attrs] children))

     :clj
     (let [classes (cond-> (card-list-classes {:divider divider})
                     class (str " " class))
           base-attrs (merge {:class classes :role "list"} attrs)]
       (into [:div base-attrs] children))))

(defn card-list-item
  "Render an item inside a card-list.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:div (merge {:class (cond-> "card-list-item" class (str " " class))
                         :role "listitem"} attrs)] children)
     :cljs
     (into [:div (merge {:class (cond-> ["card-list-item"] class (conj class))
                         :role "listitem"} attrs)] children)
     :clj
     (into [:div (merge {:class (cond-> "card-list-item" class (str " " class))
                         :role "listitem"} attrs)] children)))
