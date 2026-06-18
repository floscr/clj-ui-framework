(ns ui.toolbar
  "Toolbar — a rounded container that groups buttons (typically icon buttons)
   with optional separators. Pairs naturally with the popover: drop a toolbar
   inside a `popover-content` (with the `popover-content--flush` modifier) to
   get a floating toolbar anchored to a trigger.

   Usage:
     (toolbar {}
       (button {:variant :ghost :icon :bold})
       (button {:variant :ghost :icon :italic})
       (toolbar-separator {})
       (button {:variant :ghost :icon :link}))"
  (:require [clojure.string :as str]))

;; In squint, keywords are strings — name is identity
#?(:squint (defn- kw-name [s] s)
   :cljs   (defn- kw-name [s] (name s))
   :clj    (defn- kw-name [s] (name s)))

(defn toolbar-class-list
  "Vector of CSS classes for a toolbar container given :orientation."
  [{:keys [orientation]}]
  (let [o (or (some-> orientation kw-name) "horizontal")]
    ["toolbar" (str "toolbar-" o)]))

(defn toolbar-classes
  "Space-joined class string for a toolbar container."
  [opts]
  (str/join " " (toolbar-class-list opts)))

(defn toolbar
  "Render a toolbar container.

   Props:
     :orientation - :horizontal (default) or :vertical
     :class       - additional CSS classes
     :attrs       - additional HTML attributes"
  [{:keys [orientation class attrs] :as _props} & children]
  (let [o (or (some-> orientation kw-name) "horizontal")]
    #?(:squint
       (into [:div (merge {:class (cond-> (toolbar-classes {:orientation orientation})
                                    class (str " " class))
                           :role "toolbar"
                           :aria-orientation o}
                          attrs)]
             children)

       :cljs
       (into [:div (merge {:class (cond-> (toolbar-class-list {:orientation orientation})
                                    class (conj class))
                           :role "toolbar"
                           :aria-orientation o}
                          attrs)]
             children)

       :clj
       (into [:div (merge {:class (cond-> (toolbar-classes {:orientation orientation})
                                    class (str " " class))
                           :role "toolbar"
                           :aria-orientation o}
                          attrs)]
             children))))

(defn toolbar-separator
  "Render a divider between toolbar groups. Renders perpendicular to the
   toolbar's flow direction (vertical line in a horizontal toolbar).

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [class attrs] :as _props}]
  #?(:squint
     [:div (merge {:class (cond-> "toolbar-separator" class (str " " class))
                   :role "separator"}
                  attrs)]
     :cljs
     [:div (merge {:class (cond-> ["toolbar-separator"] class (conj class))
                   :role "separator"}
                  attrs)]
     :clj
     [:div (merge {:class (cond-> "toolbar-separator" class (str " " class))
                   :role "separator"}
                  attrs)]))
