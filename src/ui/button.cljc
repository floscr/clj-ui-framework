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
   Returns e.g. [\"btn\" \"btn--primary\" \"btn--lg\"]."
  [{:keys [variant size]}]
  (let [v (or (some-> variant kw-name) default-variant)
        s (or (some-> size kw-name) default-size)]
    (cond-> ["btn" (str "btn--" v)]
      (not= s "md") (conj (str "btn--" s)))))

(defn button-classes
  "Generate CSS class string for a button. Returns a space-joined string."
  [opts]
  (str/join " " (button-class-list opts)))

(defn button
  "Render a button element. Works across all targets via reader conditionals.

   Props:
     :variant  - :primary, :secondary, :ghost, :danger
     :size     - :sm, :md, :lg
     :on-click - click handler (ignored in :clj target)
     :disabled - boolean
     :class    - additional CSS classes (string or vector)
     :attrs    - additional HTML attributes map"
  [{:keys [variant size on-click disabled class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> (button-classes {:variant variant :size size})
                     class (str " " class))
           base-attrs (merge {:class classes}
                             (when disabled {:disabled true})
                             attrs)]
       (into [:button (cond-> base-attrs
                        on-click (assoc :on-click on-click))]
             children))

     :cljs
     (let [cls (button-class-list {:variant variant :size size})
           classes (cond-> cls
                     class (conj class))
           base-attrs (merge {:class classes}
                             (when disabled {:disabled true})
                             attrs)]
       (into [:button (cond-> base-attrs
                        on-click (assoc-in [:on :click] on-click))]
             children))

     :clj
     (let [classes (cond-> (button-classes {:variant variant :size size})
                     class (str " " class))
           base-attrs (merge {:class classes}
                             (when disabled {:disabled true})
                             attrs)]
       (into [:button base-attrs] children))))
