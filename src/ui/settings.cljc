(ns ui.settings
  "Settings rows: a label and a hint on the left, the control on the right,
   several rows on one bordered surface with hairline dividers. The shape
   of every preferences screen, so apps stop drawing it themselves."
  (:require [ui.util :as util]))

(defn settings-group
  "One bordered surface holding settings-rows, divided by hairlines. One
   group per section of a settings page.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes map

   Children are settings-row elements."
  [{:keys [class attrs] :as _props} & rows]
  (into [:div (util/class-attrs ["settings-group"] class attrs)]
        rows))

(defn settings-row
  "One setting: the label (and an optional hint under it) on the left, the
   control on the right. The control is the children: a switch, a select, a
   button, a short input.

   Props:
     :label - the setting's name (string or hiccup)
     :hint  - optional one line under the label, muted
     :class - additional CSS classes
     :attrs - additional HTML attributes map"
  [{:keys [label hint class attrs] :as _props} & control]
  (let [control (remove nil? control)]
    (cond-> [:div (util/class-attrs ["settings-row"] class attrs)
             (cond-> [:div {:class "settings-row-text"}
                      [:span {:class "settings-row-label"} label]]
               hint (conj [:span {:class "settings-row-hint"} hint]))]
      (seq control) (conj (into [:div {:class "settings-row-control"}] control)))))
