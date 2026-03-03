(ns ui.button
  (:require [clojure.string :as str]))

;; In squint, keywords are strings — name is identity
#?(:squint (defn- kw-name [s] s)
   :cljs   (defn- kw-name [s] (name s))
   :clj    (defn- kw-name [s] (name s)))

(def default-variant "secondary")
(def default-size "md")

(defn button-class-list
  "Generate a vector of CSS class strings for a button given variant and size.
   Returns e.g. [\"btn\" \"btn-primary\" \"btn-lg\"]."
  [{:keys [variant size]}]
  (let [v (or (some-> variant kw-name) default-variant)
        s (or (some-> size kw-name) default-size)]
    (cond-> ["btn" (str "btn-" v)]
      (not= s "md") (conj (str "btn-" s)))))

(defn button-classes
  "Generate CSS class string for a button. Returns a space-joined string."
  [opts]
  (str/join " " (button-class-list opts)))

(defn button
  "Render a button element. Works across all targets via reader conditionals.
   When :href is provided, renders as <a> instead of <button>.

   Props:
     :variant  - :primary, :secondary, :ghost, :danger, :link
     :size     - :sm, :md, :lg
     :href     - URL string; when set, renders as <a> tag
     :on-click - click handler (ignored in :clj target)
     :disabled - boolean
     :class    - additional CSS classes (string or vector)
     :attrs    - additional HTML attributes map"
  [{:keys [variant size href on-click disabled class attrs] :as _props} & children]
  #?(:squint
     (let [tag    (if href :a :button)
           classes (cond-> (button-classes {:variant variant :size size})
                     class (str " " class))
           base-attrs (cond-> (merge {:class classes} attrs)
                        href     (assoc :href href)
                        disabled (assoc :disabled true))]
       (into [tag (cond-> base-attrs
                    on-click (assoc :on-click on-click))]
             children))

     :cljs
     (let [tag    (if href :a :button)
           cls    (button-class-list {:variant variant :size size})
           classes (cond-> cls
                     class (conj class))
           base-attrs (cond-> (merge {:class classes} attrs)
                        href     (assoc :href href)
                        disabled (assoc :disabled true))]
       (into [tag (cond-> base-attrs
                    on-click (assoc-in [:on :click] on-click))]
             children))

     :clj
     (let [tag    (if href :a :button)
           classes (cond-> (button-classes {:variant variant :size size})
                     class (str " " class))
           base-attrs (cond-> (merge {:class classes} attrs)
                        href     (assoc :href href)
                        disabled (assoc :disabled true))]
       (into [tag base-attrs] children))))
