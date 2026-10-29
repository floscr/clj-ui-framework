(ns ui.list-row
  "Rows of records: a leading icon or thumbnail, a title with an optional
   description, metadata and controls at the end, several rows on one
   bordered surface. The playlist, the saved items, the inbox."
  (:require [clojure.string :as str]
            [ui.util :as util]
            [ui.icon :as icon]))

(defn list-row-class-list
  "Classes for a row. :interactive marks a row whose title is a link or a
   button (hover fill, pointer); :active marks the selected row."
  [{:keys [interactive active]}]
  (cond-> ["list-row"]
    interactive (conj "list-row-interactive")
    active      (conj "list-row-active")))

(defn list-row-classes
  "Space-joined form of list-row-class-list."
  [opts]
  (str/join " " (list-row-class-list opts)))

(defn list-rows
  "The bordered surface that holds rows and divides them with hairlines.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes map

   Children are list-row elements."
  [{:keys [class attrs] :as _props} & rows]
  (into [:div (util/class-attrs ["list-rows"] class (merge {:role "list"} attrs))]
        rows))

(defn- title-el
  "The title: an <a> with :href, a <button> with :on-click, else a <span>.
   The link or button stretches over the whole row (CSS ::after), so the
   row stays a <div> and its trailing controls remain valid, clickable
   buttons of their own."
  [title href on-click]
  (cond
    href     [:a {:class "list-row-title" :href href} title]
    on-click [:button #?(:squint {:class "list-row-title" :type "button" :on-click on-click}
                         :cljs   {:class ["list-row-title"] :type "button" :on {:click on-click}}
                         :clj    {:class "list-row-title" :type "button"})
              title]
    :else    [:span {:class "list-row-title"} title]))

(defn list-row
  "One record. The row is a <div>; with :href the title is a link and with
   :on-click a button, either stretched over the row, so the whole row
   opens the record while trailing controls stay separate buttons.

   Props:
     :title       - the record's name (string or hiccup), one line, truncated
     :description - optional second line, muted, truncated
     :icon        - optional leading icon keyword (:folder, :music)
     :thumb       - optional leading image URL (40px square) instead of :icon
     :meta        - optional trailing text, muted (a count, a date)
     :href        - URL; the row opens this link
     :on-click    - click handler; the row is a button (:cljs and :squint)
     :active      - boolean; the selected row
     :class       - additional CSS classes
     :attrs       - additional HTML attributes map

   Children are trailing controls (icon buttons, a badge, a switch)."
  [{:keys [title description icon thumb meta href on-click active class attrs] :as _props} & trailing]
  (let [trailing    (remove nil? trailing)
        interactive (boolean (or href on-click))
        base        (util/class-attrs (list-row-class-list {:interactive interactive :active active})
                                      class
                                      (merge {:role "listitem"} attrs))
        base        (cond-> base active (assoc :aria-current "true"))
        leading     (cond
                      thumb [:img {:class "list-row-thumb" :src thumb :alt ""}]
                      icon  (icon/icon {:icon-name icon :size :md})
                      :else nil)]
    (cond-> [:div base]
      leading (conj [:span {:class "list-row-leading"} leading])
      true    (conj (cond-> [:span {:class "list-row-text"}
                             (title-el title href on-click)]
                      description (conj [:span {:class "list-row-description"} description])))
      (or meta (seq trailing))
      (conj (into (cond-> [:span {:class "list-row-trailing"}]
                    meta (conj [:span {:class "list-row-meta"} meta]))
                  trailing)))))
