(ns ui.button
  (:require [clojure.string :as str]
            [ui.util :as util]
            [ui.icon :as icon]
            [ui.spinner :as spinner]))

(def default-variant "secondary")
(def default-size "md")

(defn button-class-list
  "Generate a vector of CSS class strings for a button given variant, size, and icon mode.
   Returns e.g. [\"btn\" \"btn-primary\" \"btn-lg\"].
   When :icon is provided (icon-only mode), adds \"btn-icon\".
   When :round is true and :icon is set, adds \"btn-icon-round\".
   When :loading is true, adds \"btn-loading\"."
  [{:keys [variant size icon round loading]}]
  (let [v (or (some-> variant util/kw-name) default-variant)
        s (or (some-> size util/kw-name) default-size)]
    (cond-> ["btn" (str "btn-" v)]
      (not= s "md")    (conj (str "btn-" s))
      icon              (conj "btn-icon")
      (and icon round)  (conj "btn-icon-round")
      loading           (conj "btn-loading"))))

(defn button-classes
  "Generate CSS class string for a button. Returns a space-joined string."
  [opts]
  (str/join " " (button-class-list opts)))

(defn button
  "Render a button element. Works across all targets via reader conditionals.
   When :href is provided, renders as <a> instead of <button>.

   Props:
     :variant    - :primary, :secondary, :ghost, :danger, :success, :link
     :size       - :sm, :md, :lg
     :href       - URL string; when set, renders as <a> tag
     :on-click   - click handler (ignored in :clj target)
     :disabled   - boolean
     :loading    - boolean; shows a spinner (replacing :icon/:icon-left) and disables the button
     :icon-left  - icon name keyword for left icon (e.g. :plus)
     :icon-right - icon name keyword for right icon (e.g. :arrow-right)
     :icon       - icon name keyword for icon-only button (no text children)
     :class      - additional CSS classes (string or vector)
     :attrs      - additional HTML attributes map"
  [{:keys [variant size href on-click disabled loading icon-left icon-right icon round class attrs] :as _props} & children]
  (let [icon-size (case (util/kw-name (or size default-size))
                    "sm" :sm
                    "lg" :md
                    #?(:squint "sm" :cljs :sm :clj :sm))
        disabled (or disabled loading)
        spinner-el (when loading
                     (spinner/spinner {:size (if (= "sm" (util/kw-name (or size default-size))) :xs :sm)}))]
    #?(:squint
       (let [tag    (if href :a :button)
             classes (cond-> (button-classes {:variant variant :size size :icon icon :round round :loading loading})
                       class (str " " class))
             base-attrs (cond-> (merge {:class classes} attrs)
                          href     (assoc :href href)
                          disabled (assoc :disabled true))
             base-attrs (cond-> base-attrs
                          on-click (assoc :on-click on-click))]
         (if icon
           [tag base-attrs (if loading spinner-el (icon/icon {:icon-name icon :size icon-size}))]
           (into [tag base-attrs]
                 (cond-> []
                   loading    (conj spinner-el)
                   (and icon-left (not loading)) (conj (icon/icon {:icon-name icon-left :size icon-size}))
                   true       (into children)
                   icon-right (conj (icon/icon {:icon-name icon-right :size icon-size}))))))

       :cljs
       (let [tag    (if href :a :button)
             cls    (button-class-list {:variant variant :size size :icon icon :round round :loading loading})
             classes (cond-> cls
                       class (conj class))
             base-attrs (cond-> (merge {:class classes} attrs)
                          href     (assoc :href href)
                          disabled (assoc :disabled true))
             base-attrs (cond-> base-attrs
                          on-click (assoc-in [:on :click] on-click))]
         (if icon
           [tag base-attrs (if loading spinner-el (icon/icon {:icon-name icon :size icon-size}))]
           (into [tag base-attrs]
                 (cond-> []
                   loading    (conj spinner-el)
                   (and icon-left (not loading)) (conj (icon/icon {:icon-name icon-left :size icon-size}))
                   true       (into children)
                   icon-right (conj (icon/icon {:icon-name icon-right :size icon-size}))))))

       :clj
       (let [tag    (if href :a :button)
             classes (cond-> (button-classes {:variant variant :size size :icon icon :round round :loading loading})
                       class (str " " class))
             base-attrs (cond-> (merge {:class classes} attrs)
                          href     (assoc :href href)
                          disabled (assoc :disabled true))]
         (if icon
           [tag base-attrs (if loading spinner-el (icon/icon {:icon-name icon :size icon-size}))]
           (into [tag base-attrs]
                 (cond-> []
                   loading    (conj spinner-el)
                   (and icon-left (not loading)) (conj (icon/icon {:icon-name icon-left :size icon-size}))
                   true       (into children)
                   icon-right (conj (icon/icon {:icon-name icon-right :size icon-size})))))))))

