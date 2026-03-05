(ns ui.alert
  (:require [clojure.string :as str]
            [ui.icon :as icon]))

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

(def ^:private variant-icons
  "Default icon names per alert variant."
  {"success" :circle-check
   "warning" :alert-triangle
   "danger"  :alert-circle
   "info"    :info})

(defn alert
  "Render an alert element.

   Props:
     :variant   - :success, :warning, :danger, :info (nil for neutral)
     :title     - optional title string
     :icon-name - override icon (nil uses variant default, false to suppress)
     :class     - additional CSS classes
     :attrs     - additional HTML attributes"
  [{:keys [variant title icon-name class attrs] :as _props} & children]
  (let [v     (some-> variant kw-name)
        iname (cond
                (false? icon-name) nil          ;; explicitly suppressed
                icon-name          icon-name    ;; explicit override
                v                  (get variant-icons v))]
    #?(:squint
       (let [classes (cond-> (alert-classes {:variant variant})
                       class (str " " class))
             base-attrs (merge {:class classes :role "alert"} attrs)]
         (into [:div base-attrs]
               (cond-> []
                 iname   (conj [:span {:class "alert-icon"} (icon/icon {:icon-name iname :size :sm})])
                 true    (conj (into [:div {:class "alert-content"}]
                                     (cond-> []
                                       title   (conj [:p {:class "alert-title"} title])
                                       :always (into (map (fn [c] [:p {:class "alert-body"} c]) children))))))))

       :cljs
       (let [cls (alert-class-list {:variant variant})
             classes (cond-> cls class (conj class))
             base-attrs (merge {:class classes :role "alert"} attrs)]
         (into [:div base-attrs]
               (cond-> []
                 iname   (conj [:span {:class ["alert-icon"]} (icon/icon {:icon-name iname :size :sm})])
                 true    (conj (into [:div {:class ["alert-content"]}]
                                     (cond-> []
                                       title   (conj [:p {:class ["alert-title"]} title])
                                       :always (into (map (fn [c] [:p {:class ["alert-body"]} c]) children))))))))

       :clj
       (let [classes (cond-> (alert-classes {:variant variant})
                       class (str " " class))
             base-attrs (merge {:class classes :role "alert"} attrs)]
         (into [:div base-attrs]
               (cond-> []
                 iname   (conj [:span {:class "alert-icon"} (icon/icon {:icon-name iname :size :sm})])
                 true    (conj (into [:div {:class "alert-content"}]
                                     (cond-> []
                                       title   (conj [:p {:class "alert-title"} title])
                                       :always (into (map (fn [c] [:p {:class "alert-body"} c]) children)))))))))))

