(ns ui.metric
  "A number with its context: label, value, unit, change and a hint. One
   metric is a bordered tile; a metric-row lays several out as equal
   columns. The KPI row of a dashboard, the stats of a record."
  (:require [clojure.string :as str]
            [ui.util :as util]))

(defn metric-class-list
  "Classes for a metric tile. :flush drops the border and padding so the
   metric can sit inside a card or a list."
  [{:keys [flush]}]
  (cond-> ["metric"]
    flush (conj "metric-flush")))

(defn metric-classes
  "Space-joined form of metric-class-list."
  [opts]
  (str/join " " (metric-class-list opts)))

(defn metric
  "A labeled number.

   Props:
     :label - what the number is (string or hiccup), above it
     :value - the number itself, formatted (string or hiccup)
     :unit  - optional unit or suffix beside the value, muted (EUR, days, %)
     :delta - optional change (string, e.g. \"+12%\"), under the value
     :trend - :up | :down | :flat; colors the delta success, danger or muted
     :hint  - optional short text beside the delta (vs last month)
     :flush - boolean; no border or padding, for use inside another surface
     :class - additional CSS classes
     :attrs - additional HTML attributes map"
  [{:keys [label value unit delta trend hint flush class attrs] :as _props}]
  (let [t (some-> trend util/kw-name)
        delta-class (cond-> "metric-delta"
                      (= t "up")   (str " metric-delta-up")
                      (= t "down") (str " metric-delta-down"))]
    (cond-> [:div (util/class-attrs (metric-class-list {:flush flush}) class attrs)
             [:span {:class "metric-label"} label]
             (cond-> [:div {:class "metric-value"}
                      [:span {:class "metric-number"} value]]
               unit (conj [:span {:class "metric-unit"} unit]))]
      (or delta hint)
      (conj (cond-> [:div {:class "metric-foot"}]
              delta (conj [:span {:class delta-class} delta])
              hint  (conj [:span {:class "metric-hint"} hint]))))))

(defn metric-row
  "Equal-width columns of metrics that wrap on narrow screens: 16px apart,
   each at least 160px wide, all the same height.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes map

   Children are metric elements."
  [{:keys [class attrs] :as _props} & metrics]
  (into [:div (util/class-attrs ["metric-row"] class attrs)]
        metrics))
