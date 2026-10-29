(ns ui.page
  "The one page container, its header and its sections: the layer every app
   used to hand-roll. A page sets the reading column and the gutters once;
   everything inside fills it and is spaced with gap, never with margins."
  (:require [clojure.string :as str]
            [ui.util :as util]))

(def default-width "reading")

(defn page-class-list
  "Classes for a page container. :width is :narrow (640px, a single form or
   a dialog-like flow), :reading (720px, the default: settings, lists,
   forms), :wide (1200px, dashboards and tables) or :full (no max width)."
  [{:keys [width]}]
  (let [w (or (some-> width util/kw-name) default-width)]
    (cond-> ["page"]
      (not= w default-width) (conj (str "page-" w)))))

(defn page-classes
  "Space-joined form of page-class-list."
  [opts]
  (str/join " " (page-class-list opts)))

(defn page
  "The page container: max width by :width, fluid gutters, a vertical grid
   with 24px between regions. Renders a <main> unless :tag says otherwise.

   Props:
     :width - :narrow | :reading (default) | :wide | :full
     :tag   - element keyword, default :main
     :class - additional CSS classes
     :attrs - additional HTML attributes map

   Children are the page regions: page-header, page-section, cards, tables."
  [{:keys [width tag class attrs] :as _props} & children]
  (into [(or tag :main) (util/class-attrs (page-class-list {:width width}) class attrs)]
        children))

(defn page-header
  "The page title row: one h1 with an optional one-line description on the
   left, the page's actions on the right. The primary action of the page
   belongs here.

   Props:
     :title       - the page title (string or hiccup), rendered as the h1
     :description - optional one sentence under the title
     :class       - additional CSS classes
     :attrs       - additional HTML attributes map

   Children are the actions (buttons, a search field)."
  [{:keys [title description class attrs] :as _props} & actions]
  (let [actions (remove nil? actions)]
    (cond-> [:header (util/class-attrs ["page-header"] class attrs)
             (cond-> [:div {:class "page-header-text"}
                      [:h1 {:class "page-title"} title]]
               description (conj [:p {:class "page-description"} description]))]
      (seq actions) (conj (into [:div {:class "page-header-actions"}] actions)))))

(defn page-section
  "A titled region of a page: an h2 with an optional description, then the
   content as a 16px grid. Sections are separated by the page gap.

   Props:
     :title       - the section heading (string or hiccup), rendered as the h2
     :description - optional one sentence under the heading
     :actions     - optional hiccup rendered at the right of the heading
     :class       - additional CSS classes
     :attrs       - additional HTML attributes map

   Children are the content: a card, a settings-group, a table, fields."
  [{:keys [title description actions class attrs] :as _props} & children]
  (let [text (cond-> [:div {:class "page-section-text"}
                      [:h2 {:class "page-section-title"} title]]
               description (conj [:p {:class "page-section-description"} description]))
        head (cond-> [:div {:class "page-section-head"} text]
               actions (conj [:div {:class "page-section-actions"} actions]))]
    (into [:section (util/class-attrs ["page-section"] class attrs)
           (when title head)]
          children)))

(defn action-row-class-list
  "Classes for an action row. :align is :end (default), :start or :between."
  [{:keys [align]}]
  (let [a (or (some-> align util/kw-name) "end")]
    (cond-> ["action-row"]
      (not= a "end") (conj (str "action-row-" a)))))

(defn action-row
  "A horizontal row of actions: buttons spaced 8px apart, right-aligned by
   default so the primary action sits at the end of the reading direction.

   Props:
     :align - :end (default) | :start | :between
     :class - additional CSS classes
     :attrs - additional HTML attributes map"
  [{:keys [align class attrs] :as _props} & children]
  (into [:div (util/class-attrs (action-row-class-list {:align align}) class attrs)]
        children))
