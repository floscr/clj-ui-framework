(ns ui.empty-state
  (:require [clojure.string :as str]
            [ui.icon :as icon]))

(defn empty-state-class-list
  "Returns a vector of CSS class strings for an empty state. :fill makes it
   grow to the region it is in and center vertically."
  [{:keys [fill]}]
  (cond-> ["empty-state"]
    fill (conj "empty-state-fill")))

(defn empty-state-classes
  "Returns a space-joined class string."
  [opts]
  (str/join " " (empty-state-class-list opts)))

(defn empty-state
  "Centered, muted placeholder for empty lists and views.

   Props:
     :icon  - optional icon name keyword rendered above the text (e.g. :inbox)
     :fill  - boolean; fill the region (flex: 1, at least 320px) and center
              vertically, for a view whose whole content is the empty state
     :class - additional CSS classes
     :attrs - additional HTML attributes map

   Children are the placeholder content (usually a short text)."
  [{:keys [icon fill class attrs] :as _props} & children]
  (let [icon-el (when icon
                  (icon/icon {:icon-name icon :size :xl :class "empty-state-icon"}))]
    #?(:squint
       (let [classes (cond-> (empty-state-classes {:fill fill})
                       class (str " " class))
             base-attrs (merge {:class classes} attrs)]
         (into (cond-> [:div base-attrs]
                 icon-el (conj icon-el))
               children))

       :cljs
       (let [cls (empty-state-class-list {:fill fill})
             classes (cond-> cls
                       class (conj class))
             base-attrs (merge {:class classes} attrs)]
         (into (cond-> [:div base-attrs]
                 icon-el (conj icon-el))
               children))

       :clj
       (let [classes (cond-> (empty-state-classes {:fill fill})
                       class (str " " class))
             base-attrs (merge {:class classes} attrs)]
         (into (cond-> [:div base-attrs]
                 icon-el (conj icon-el))
               children)))))
