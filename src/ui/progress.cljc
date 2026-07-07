(ns ui.progress
  (:require [clojure.string :as str]
            [ui.util :as util]))

(defn progress-bar-class-list
  "Generate a vector of CSS class strings for the progress bar fill.
   Variants: nil (default/accent), :success, :warning, :danger."
  [{:keys [variant]}]
  (cond-> ["progress-bar"]
    variant (conj (str "progress-bar--" (util/kw-name variant)))))

(defn progress-bar-classes
  "Generate CSS class string for the progress bar fill."
  [opts]
  (str/join " " (progress-bar-class-list opts)))

(defn progress
  "Render a progress bar.

   Props:
     :value   - number 0-100
     :variant - nil, :success, :warning, :danger
     :class   - additional CSS classes
     :attrs   - additional HTML attributes"
  [{:keys [value variant class attrs] :as _props}]
  (let [pct (str (or value 0) "%")]
    #?(:squint
       (let [classes (cond-> "progress" class (str " " class))
             bar-cls (progress-bar-classes {:variant variant})]
         [:div (merge {:class classes :role "progressbar"
                       :aria-valuenow (str (or value 0))
                       :aria-valuemin "0" :aria-valuemax "100"} attrs)
          [:div {:class bar-cls :style {"width" pct}}]])

       :cljs
       (let [cls (cond-> ["progress"] class (conj class))
             bar-cls (progress-bar-class-list {:variant variant})]
         [:div (merge {:class cls :role "progressbar"
                       :aria-valuenow (str (or value 0))
                       :aria-valuemin "0" :aria-valuemax "100"} attrs)
          [:div {:class bar-cls :style {:width pct}}]])

       :clj
       (let [classes (cond-> "progress" class (str " " class))
             bar-cls (progress-bar-classes {:variant variant})]
         [:div (merge {:class classes :role "progressbar"
                       :aria-valuenow (str (or value 0))
                       :aria-valuemin "0" :aria-valuemax "100"} attrs)
          [:div {:class bar-cls :style (str "width: " pct)}]]))))
