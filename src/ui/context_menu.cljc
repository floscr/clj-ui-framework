(ns ui.context-menu
  "Context menu component — right-click menu for wrapped elements.

   Usage (all targets):
     (context-menu-trigger
       {:items [{:label \"Edit\"   :url \"/edit/123\" :icon :edit}
                {:type :separator}
                {:label \"Delete\" :url \"/del/123\" :icon :trash :variant :danger
                 :confirm true}]}
       [:div \"Right click me\"])

   :confirm can be true (default message) or a custom string:
     {:label \"Remove\" :url \"/rm/1\" :confirm \"Delete this permanently?\"}
   Hiccup: items are serialized as a data attribute; a tiny JS runtime
   (ui-runtime.js, compiled from squint) handles DOM creation.

   Squint/Replicant: items are passed directly to the same JS runtime.
   Items may additionally have :on-click callbacks."
  (:require [clojure.string :as str]
            [ui.util :as util]
            [ui.icon :as icon]))

;; ── Icon path extraction ────────────────────────────────────────────
;; Extracts SVG path d-strings from the icon registry so the JS runtime
;; can render icons without importing the full icon module.

(defn icon-paths-for
  "Return the SVG child elements for the given icon name, or nil.

   Each element is [tag-string [[attr-name attr-value] ...]] with every
   value stringified, so the JS runtime can build it via createElementNS
   without any platform-specific map handling. Emits ALL element kinds
   (path/circle/rect/line/polyline/polygon), not just <path>, so composite
   icons (e.g. :clock, :circle-check, :copy) render fully."
  [icon-name]
  (when icon-name
    (let [k #?(:squint icon-name
               :cljs   (if (keyword? icon-name) icon-name (keyword icon-name))
               :clj    (if (keyword? icon-name) icon-name (keyword icon-name)))
          elements (get icon/icon-paths k)]
      (when elements
        (vec (map (fn [[tag attrs]]
                    [(util/kw-name tag)
                     (vec (map (fn [[ak av]] [(util/kw-name ak) (str av)]) attrs))])
                  elements))))))

;; ── Item normalization ──────────────────────────────────────────────
;; Converts an item map into a flat structure suitable for the JS runtime.

(defn normalize-item
  "Normalize a menu item for the JS runtime.
   Adds :icon-paths from :icon, converts keyword values to strings."
  [item]
  (if (and (:type item) (= (util/kw-name (:type item)) "separator"))
    {:type "separator"}
    (let [icon-name (:icon item)]
      (cond-> {:label (:label item)}
        (:url item)      (assoc :url (:url item))
        (:variant item)  (assoc :variant (util/kw-name (:variant item)))
        (:on-click item) (assoc :on-click (:on-click item))
        (:confirm item)  (assoc :confirm (let [c (:confirm item)]
                                            (if (string? c) c true)))
        icon-name        (assoc :icon-paths (icon-paths-for icon-name))))))

;; ── JSON serialization (hiccup/clj only) ────────────────────────────

#?(:clj
   (do
     (defn- escape-json-str [s]
       (-> (str s)
           (str/replace "\\" "\\\\")
           (str/replace "\"" "\\\"")
           (str/replace "\n" "\\n")
           (str/replace "\r" "\\r")
           (str/replace "\t" "\\t")))

     (defn- json-val [v]
       (cond
         (nil? v)     "null"
         (string? v)  (str "\"" (escape-json-str v) "\"")
         (number? v)  (str v)
         (boolean? v) (str v)
         (vector? v)  (str "[" (str/join "," (map json-val v)) "]")
         (map? v)     (str "{"
                           (str/join ","
                             (for [[k v] v :when (some? v)]
                               (str "\"" (escape-json-str (if (keyword? k) (name k) (str k)))
                                    "\":" (json-val v))))
                           "}")
         :else        (str "\"" (escape-json-str (str v)) "\"")))

     (defn items->json
       "Serialize a vector of normalized items to a JSON string."
       [items]
       (json-val (vec items)))))

;; ── Context Menu Trigger ────────────────────────────────────────────

(defn context-menu-trigger
  "Wrap children with a right-click context menu trigger.

   Props:
     :items - vector of menu item maps:
              {:label \"Edit\" :url \"/edit/123\" :icon :edit}
              {:type :separator}
              {:label \"Delete\" :url \"/del/123\" :icon :trash :variant :danger}
              Squint/Replicant items may have :on-click instead of :url.
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [items class attrs] :as _props} & children]
  (let [normalized (mapv normalize-item items)]
    #?(:squint
       ;; Squint: event handler passes items directly (already JS objects).
       (let [classes   (cond-> "context-menu-trigger"
                         class (str " " class))
             base-attrs (merge {:class classes
                                :on-contextmenu
                                (fn [e]
                                  (.preventDefault e)
                                  (let [f (aget js/window "__uiContextMenu")]
                                    (when f (f e normalized))))}
                               attrs)]
         (into [:div base-attrs] children))

       :cljs
       ;; Replicant: event handler passes items to the JS runtime.
       (let [classes   (cond-> ["context-menu-trigger"]
                         class (conj class))
             items-js  (clj->js normalized)
             base-attrs (merge {:class classes
                                :on {:contextmenu
                                     (fn [e]
                                       (.preventDefault e)
                                       (when-let [f (aget js/window "__uiContextMenu")]
                                         (f e items-js)))}}
                               attrs)]
         (into [:div base-attrs] children))

       :clj
       ;; Hiccup: serialize items to data attribute, inline JS calls the runtime.
       (let [json-str  (items->json normalized)
             classes   (cond-> "context-menu-trigger"
                         class (str " " class))
             base-attrs (merge {:class classes
                                :data-context-menu json-str
                                :oncontextmenu "event.preventDefault();window.__uiContextMenu(event)"}
                               attrs)]
         (into [:div base-attrs] children)))))
