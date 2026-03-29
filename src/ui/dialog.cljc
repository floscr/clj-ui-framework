(ns ui.dialog
  (:require [clojure.string :as str]))

(defn dialog-class-list
  "Generate a vector of CSS class strings for a dialog."
  [_opts]
  ["dialog"])

(defn dialog-classes
  "Generate CSS class string for a dialog."
  [opts]
  (str/join " " (dialog-class-list opts)))

;; ── Native <dialog> ─────────────────────────────────────────────────

(defn dialog
  "Render a native <dialog> element.

   Use with .showModal() for built-in backdrop and focus trapping.
   Clicking the backdrop closes the dialog automatically.

   Props:
     :open  - boolean, whether the dialog is open
     :id    - dialog id for targeting
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [open id class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> (dialog-classes {})
                     class (str " " class))
           base-attrs (merge {:class classes
                              :on-click (fn [e]
                                          (when (identical? (.-target e) (.-currentTarget e))
                                            (.close (.-currentTarget e))))}
                             (when id {:id id})
                             (when open {:open true})
                             attrs)]
       (into [:dialog base-attrs] children))

     :cljs
     (let [cls (dialog-class-list {})
           classes (cond-> cls class (conj class))
           base-attrs (merge {:class classes
                              :on {:click (fn [e]
                                            (when (identical? (.-target e) (.-currentTarget e))
                                              (.close (.-currentTarget e))))}}
                             (when id {:id id})
                             (when open {:open true})
                             attrs)]
       (into [:dialog base-attrs] children))

     :clj
     (let [classes (cond-> (dialog-classes {})
                     class (str " " class))
           base-attrs (merge {:class classes
                              :onclick "if(event.target===this)this.close()"}
                             (when id {:id id})
                             (when open {:open true})
                             attrs)]
       (into [:dialog base-attrs] children))))

;; ── Overlay-based dialog ────────────────────────────────────────────
;; For reactive frameworks (Eucalypt, etc.) where native <dialog> isn't
;; practical. Renders a backdrop + panel as plain divs, controlled by
;; show/hide in app state.

(defn dialog-overlay
  "Render a dialog backdrop overlay.

   Covers the viewport and centers its children. Clicking the backdrop
   fires :on-close. Wrap a dialog-panel inside this.

   Props:
     :on-close - callback when backdrop is clicked
     :class    - additional CSS classes
     :attrs    - additional HTML attributes"
  [{:keys [on-close class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> "dialog-overlay"
                     class (str " " class))
           base-attrs (merge {:class classes
                              :on-click (when on-close
                                          (fn [e]
                                            (when (identical? (.-target e) (.-currentTarget e))
                                              (on-close))))}
                             attrs)]
       (into [:div base-attrs] children))

     :cljs
     (let [classes (cond-> ["dialog-overlay"]
                     class (conj class))
           base-attrs (merge {:class classes
                              :on (when on-close
                                    {:click (fn [e]
                                              (when (identical? (.-target e) (.-currentTarget e))
                                                (on-close)))})}
                             attrs)]
       (into [:div base-attrs] children))

     :clj
     (let [classes (cond-> "dialog-overlay"
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       (into [:div base-attrs] children))))

(defn dialog-panel
  "Render a dialog panel (the visible box inside an overlay).

   Use inside dialog-overlay. Stops click propagation so clicks
   inside the panel don't close the overlay.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> "dialog-panel"
                     class (str " " class))
           base-attrs (merge {:class classes
                              :on-click (fn [e] (.stopPropagation e))}
                             attrs)]
       (into [:div base-attrs] children))

     :cljs
     (let [classes (cond-> ["dialog-panel"]
                     class (conj class))
           base-attrs (merge {:class classes
                              :on {:click (fn [e] (.stopPropagation e))}}
                             attrs)]
       (into [:div base-attrs] children))

     :clj
     (let [classes (cond-> "dialog-panel"
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       (into [:div base-attrs] children))))

;; ── Shared sections ─────────────────────────────────────────────────

(defn dialog-header
  "Render a dialog header section."
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:header (merge {:class (cond-> "dialog-header" class (str " " class))} attrs)] children)
     :cljs
     (into [:header (merge {:class (cond-> ["dialog-header"] class (conj class))} attrs)] children)
     :clj
     (into [:header (merge {:class (cond-> "dialog-header" class (str " " class))} attrs)] children)))

(defn dialog-body
  "Render a dialog body section."
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:div (merge {:class (cond-> "dialog-body" class (str " " class))} attrs)] children)
     :cljs
     (into [:div (merge {:class (cond-> ["dialog-body"] class (conj class))} attrs)] children)
     :clj
     (into [:div (merge {:class (cond-> "dialog-body" class (str " " class))} attrs)] children)))

(defn dialog-footer
  "Render a dialog footer section."
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:footer (merge {:class (cond-> "dialog-footer" class (str " " class))} attrs)] children)
     :cljs
     (into [:footer (merge {:class (cond-> ["dialog-footer"] class (conj class))} attrs)] children)
     :clj
     (into [:footer (merge {:class (cond-> "dialog-footer" class (str " " class))} attrs)] children)))
