(ns ui.popover
  "Popover — rich content in a floating panel, triggered by a button.

   Built on the native HTML Popover API (`popovertarget` + `popover=\"auto\"`),
   so click-to-toggle, click-outside dismiss, and Escape all work without any
   app state — including the hiccup target. A tiny JS runtime (ui-runtime.js,
   compiled from squint) anchors the panel to its trigger via :side / :align.

   Usage (all targets):
     (popover-trigger {:target \"settings\"
                       :class (button/button-classes {:variant :secondary})}
       \"Open\")
     (popover-content {:id \"settings\" :side :bottom :align :center}
       (popover-header {}
         (popover-title {} \"Dimensions\")
         (popover-description {} \"Set the dimensions for the layer.\"))
       ...)

   The trigger's :target and the content's :id must match. To attach the
   trigger behaviour to an existing element, spread (trigger-attrs id) onto it."
  (:require [ui.util :as util]))

(defn trigger-attrs
  "Return the attribute map that turns any element into a popover trigger.
   Spread onto a button: (button {:attrs (trigger-attrs \"my-popover\")} ...)."
  [target]
  {:popovertarget target
   :aria-haspopup "dialog"})

(defn content-class-list
  "Vector of CSS classes for a popover content panel given :side."
  [{:keys [side]}]
  (let [s (or (some-> side util/kw-name) "bottom")]
    ["popover-content" (str "popover-content--" s)]))

;; ── Trigger ─────────────────────────────────────────────────────────

(defn popover-trigger
  "Render a <button> that toggles a popover.

   Props:
     :target - id of the matching popover-content (required)
     :class  - additional CSS classes (e.g. button styling)
     :attrs  - additional HTML attributes"
  [{:keys [target class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> "popover-trigger" class (str " " class))
           base-attrs (merge {:class classes :popovertarget target :aria-haspopup "dialog"} attrs)]
       (into [:button base-attrs] children))

     :cljs
     (let [classes (util/conj-classes ["popover-trigger"] class)
           base-attrs (merge {:class classes :popovertarget target :aria-haspopup "dialog"} attrs)]
       (into [:button base-attrs] children))

     :clj
     (let [classes (cond-> "popover-trigger" class (str " " class))
           base-attrs (merge {:class classes :popovertarget target :aria-haspopup "dialog"} attrs)]
       (into [:button base-attrs] children))))

;; ── Content ─────────────────────────────────────────────────────────

(defn popover-content
  "Render the floating popover panel.

   Props:
     :id    - must match the trigger's :target (required)
     :side  - :top, :right, :bottom (default), :left — which side of the
              trigger the panel opens on
     :align - :start, :center (default), :end — alignment along the side
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [id side align class attrs] :as _props} & children]
  (let [side*  (or (some-> side util/kw-name) "bottom")
        align* (or (some-> align util/kw-name) "center")]
    #?(:squint
       (let [classes (cond-> (str "popover-content popover-content--" side*)
                       class (str " " class))
             base-attrs (merge {:class classes :popover "auto" :role "dialog"
                                :data-popover-side side* :data-popover-align align*}
                               (when id {:id id})
                               attrs)]
         (into [:div base-attrs] children))

       :cljs
       (let [classes (util/conj-classes ["popover-content" (str "popover-content--" side*)] class)
             base-attrs (merge {:class classes :popover "auto" :role "dialog"
                                :data-popover-side side* :data-popover-align align*}
                               (when id {:id id})
                               attrs)]
         (into [:div base-attrs] children))

       :clj
       (let [classes (cond-> (str "popover-content popover-content--" side*)
                       class (str " " class))
             base-attrs (merge {:class classes :popover "auto" :role "dialog"
                                :data-popover-side side* :data-popover-align align*}
                               (when id {:id id})
                               attrs)]
         (into [:div base-attrs] children)))))

;; ── Sections ────────────────────────────────────────────────────────

(defn popover-header
  "Render a popover header section."
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:header (merge {:class (cond-> "popover-header" class (str " " class))} attrs)] children)
     :cljs
     (into [:header (merge {:class (util/conj-classes ["popover-header"] class)} attrs)] children)
     :clj
     (into [:header (merge {:class (cond-> "popover-header" class (str " " class))} attrs)] children)))

(defn popover-title
  "Render a popover title."
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:h4 (merge {:class (cond-> "popover-title" class (str " " class))} attrs)] children)
     :cljs
     (into [:h4 (merge {:class (util/conj-classes ["popover-title"] class)} attrs)] children)
     :clj
     (into [:h4 (merge {:class (cond-> "popover-title" class (str " " class))} attrs)] children)))

(defn popover-description
  "Render a popover description."
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:p (merge {:class (cond-> "popover-description" class (str " " class))} attrs)] children)
     :cljs
     (into [:p (merge {:class (util/conj-classes ["popover-description"] class)} attrs)] children)
     :clj
     (into [:p (merge {:class (cond-> "popover-description" class (str " " class))} attrs)] children)))
