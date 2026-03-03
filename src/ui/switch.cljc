(ns ui.switch
  (:require [clojure.string :as str]))

(defn switch-class-list
  "Generate a vector of CSS class strings for a switch."
  [{:keys [disabled]}]
  (cond-> ["switch"]
    disabled (conj "switch--disabled")))

(defn switch-classes
  "Generate CSS class string for a switch."
  [opts]
  (str/join " " (switch-class-list opts)))

(defn switch-toggle
  "Render a toggle switch element.

   Props:
     :checked   - boolean, whether the switch is on
     :disabled  - boolean
     :on-change - change handler (ignored in :clj target)
     :label     - label text
     :class     - additional CSS classes
     :attrs     - additional HTML attributes"
  [{:keys [checked disabled on-change label class attrs] :as _props}]
  #?(:squint
     (let [classes (cond-> (switch-classes {:disabled disabled})
                     class (str " " class))
           track-cls (cond-> "switch-track"
                       checked (str " switch-track--checked"))]
       [:label (merge {:class classes} attrs)
        [:input {:class "switch-input" :type "checkbox"
                 :checked (boolean checked)
                 :disabled (boolean disabled)
                 :on-change on-change}]
        [:span {:class track-cls}
         [:span {:class "switch-thumb"}]]
        (when label [:span label])])

     :cljs
     (let [cls (switch-class-list {:disabled disabled})
           classes (cond-> cls class (conj class))
           track-cls (cond-> ["switch-track"]
                       checked (conj "switch-track--checked"))]
       [:label (merge {:class classes} attrs)
        [:input (cond-> {:class ["switch-input"] :type "checkbox"
                         :checked (boolean checked)
                         :disabled (boolean disabled)}
                  on-change (assoc-in [:on :change] on-change))]
        [:span {:class track-cls}
         [:span {:class ["switch-thumb"]}]]
        (when label [:span label])])

     :clj
     (let [classes (cond-> (switch-classes {:disabled disabled})
                     class (str " " class))
           track-cls (cond-> "switch-track"
                       checked (str " switch-track--checked"))]
       [:label (merge {:class classes} attrs)
        [:input {:class "switch-input" :type "checkbox"
                 :checked (boolean checked)
                 :disabled (boolean disabled)}]
        [:span {:class track-cls}
         [:span {:class "switch-thumb"}]]
        (when label [:span label])])))
