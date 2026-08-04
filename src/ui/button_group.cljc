(ns ui.button-group
  "Button group — a cluster of related buttons rendered as a segmented
   control. Two flavors:

     :ghost  (default) container-less, tight cluster; the active item is
             filled. Good for a header toolbar (view switchers, steppers).
     :boxed  a bordered container (like `tabs-boxed`); the active segment
             is filled with a subtle shadow. Good for filters with counts.

   Items can be icon-only or text (with an optional trailing count badge).
   Insert a `button-group-separator` to visually divide sub-clusters.

   Usage:
     (button-group {:variant :boxed}
       (button-group-item {:active true} \"All\" {:count 353})
       (button-group-item {} \"Downloaded\" {:count 28})
       (button-group-item {} \"Missing\" {:count 325}))

     (button-group {}
       (button-group-item {:icon :grid :active true})
       (button-group-item {:icon :layout})
       (button-group-separator {})
       (button-group-item {:icon :settings}))"
  (:require [clojure.string :as str]
            [ui.util :as util]
            [ui.icon :as icon]))

(defn button-group-class-list
  "Vector of CSS classes for a button-group container given :variant."
  [{:keys [variant]}]
  (let [v (or (some-> variant util/kw-name) "ghost")]
    (cond-> ["button-group"]
      (= v "boxed") (conj "button-group-boxed"))))

(defn button-group-classes
  "Space-joined class string for a button-group container."
  [opts]
  (str/join " " (button-group-class-list opts)))

(defn button-group-item-class-list
  "Vector of CSS classes for a single button-group item.
     :active    - highlighted/selected item
     :icon-only - square icon button (no text)"
  [{:keys [active icon-only]}]
  (cond-> ["button-group-item"]
    icon-only (conj "button-group-item-icon")
    active    (conj "is-active")))

(defn button-group-item-classes
  "Space-joined class string for a button-group item."
  [opts]
  (str/join " " (button-group-item-class-list opts)))

(defn button-group
  "Render a button-group container.

   Props:
     :variant - :ghost (default) or :boxed
     :class   - additional CSS classes
     :attrs   - additional HTML attributes"
  [{:keys [variant class attrs] :as _props} & children]
  #?(:squint
     (into [:div (merge {:class (cond-> (button-group-classes {:variant variant})
                                  class (str " " class))
                         :role "group"}
                        attrs)]
           children)

     :cljs
     (into [:div (merge {:class (cond-> (button-group-class-list {:variant variant})
                                  class (conj class))
                         :role "group"}
                        attrs)]
           children)

     :clj
     (into [:div (merge {:class (cond-> (button-group-classes {:variant variant})
                                  class (str " " class))
                         :role "group"}
                        attrs)]
           children)))

(defn button-group-item
  "Render a single button inside a button-group.

   Props:
     :active   - boolean; highlights the item as selected
     :icon     - icon name keyword (e.g. :grid); icon-only when no children
     :count    - optional number/string rendered as a muted trailing badge
     :href     - URL string; when set, renders as <a> (a nav segment)
     :on-click - click handler (ignored in :clj target)
     :class    - additional CSS classes
     :attrs    - additional HTML attributes"
  [{:keys [active icon count href on-click class attrs] :as _props} & children]
  (let [icon-only? (and (some? icon) (empty? children))
        icon-el    (when icon (icon/icon {:icon-name icon :size :sm}))
        tag        (if href :a :button)]
    #?(:squint
       (let [classes    (cond-> (button-group-item-classes {:active active :icon-only icon-only?})
                          class (str " " class))
             base-attrs (cond-> (merge {:class classes} attrs)
                          href     (assoc :href href)
                          on-click (assoc :on-click on-click))]
         (into [tag base-attrs]
               (cond-> []
                 icon          (conj icon-el)
                 true          (into children)
                 (some? count) (conj [:span {:class "button-group-count"} count]))))

       :cljs
       (let [classes    (cond-> (button-group-item-class-list {:active active :icon-only icon-only?})
                          class (conj class))
             base-attrs (cond-> (merge {:class classes} attrs)
                          href     (assoc :href href)
                          on-click (assoc-in [:on :click] on-click))]
         (into [tag base-attrs]
               (cond-> []
                 icon          (conj icon-el)
                 true          (into children)
                 (some? count) (conj [:span {:class ["button-group-count"]} count]))))

       :clj
       (let [classes    (cond-> (button-group-item-classes {:active active :icon-only icon-only?})
                          class (str " " class))
             base-attrs (cond-> (merge {:class classes} attrs)
                          href (assoc :href href))]
         (into [tag base-attrs]
               (cond-> []
                 icon          (conj icon-el)
                 true          (into children)
                 (some? count) (conj [:span {:class "button-group-count"} count])))))))

(defn button-group-separator
  "Render a thin divider between sub-clusters of a button-group.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [class attrs] :as _props}]
  #?(:squint
     [:div (merge {:class (cond-> "button-group-separator" class (str " " class))
                   :role "separator"}
                  attrs)]
     :cljs
     [:div (merge {:class (cond-> ["button-group-separator"] class (conj class))
                   :role "separator"}
                  attrs)]
     :clj
     [:div (merge {:class (cond-> "button-group-separator" class (str " " class))
                   :role "separator"}
                  attrs)]))
