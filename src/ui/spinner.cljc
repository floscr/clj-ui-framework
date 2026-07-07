(ns ui.spinner
  (:require [clojure.string :as str]
            [ui.util :as util]))

(def default-size "md")

(defn spinner-class-list
  "Generate a vector of CSS class strings for a spinner.
   Sizes: :sm, :md (default), :lg."
  [{:keys [size]}]
  (let [s (or (some-> size util/kw-name) default-size)]
    (cond-> ["spinner"]
      (= s "sm") (conj "spinner-sm")
      (= s "lg") (conj "spinner-lg"))))

(defn spinner-classes
  "Generate CSS class string for a spinner."
  [opts]
  (str/join " " (spinner-class-list opts)))

(defn spinner
  "Render a spinner element.

   Props:
     :size  - :sm, :md, :lg
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [size class attrs] :as _props}]
  #?(:squint
     (let [classes (cond-> (spinner-classes {:size size})
                     class (str " " class))
           base-attrs (merge {:class classes :role "status" :aria-label "Loading"} attrs)]
       [:span base-attrs])

     :cljs
     (let [cls (spinner-class-list {:size size})
           classes (cond-> cls class (conj class))
           base-attrs (merge {:class classes :role "status" :aria-label "Loading"} attrs)]
       [:span base-attrs])

     :clj
     (let [classes (cond-> (spinner-classes {:size size})
                     class (str " " class))
           base-attrs (merge {:class classes :role "status" :aria-label "Loading"} attrs)]
       [:span base-attrs])))
