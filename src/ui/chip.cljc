(ns ui.chip
  (:require [clojure.string :as str]))

#?(:squint (defn- kw-name [s] s)
   :cljs   (defn- kw-name [s] (name s))
   :clj    (defn- kw-name [s] (name s)))

(defn chip-class-list
  "Returns a vector of CSS class strings for a chip."
  [{:keys [active]}]
  (cond-> ["chip"]
    active (conj "chip-active")))

(defn chip-classes
  "Returns a space-joined class string."
  [opts]
  (str/join " " (chip-class-list opts)))

(defn chip
  "A small selectable button for tags, filters, presets.
   Props: :active, :dot-color, :on-click, :class, :attrs"
  [{:keys [active dot-color on-click class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> (chip-classes {:active active})
                     class (str " " class))
           base-attrs (cond-> (merge {:class classes} attrs)
                        on-click (assoc :on-click on-click))
           dot (when dot-color
                 [:span {:class "chip-dot" :style {"background" dot-color}}])]
       (cond-> [:button base-attrs]
         dot (conj dot)
         true (into children)))

     :cljs
     (let [cls (chip-class-list {:active active})
           classes (cond-> cls class (conj class))
           base-attrs (cond-> (merge {:class classes} attrs)
                        on-click (assoc :on {:click on-click}))
           dot (when dot-color
                 [:span {:class ["chip-dot"] :style {:background dot-color}}])]
       (cond-> [:button base-attrs]
         dot (conj dot)
         true (into children)))

     :clj
     (let [classes (cond-> (chip-classes {:active active})
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)
           dot (when dot-color
                 [:span {:class "chip-dot"
                         :style (str "background:" dot-color)}])]
       (cond-> [:button base-attrs]
         dot (conj dot)
         true (into children)))))
