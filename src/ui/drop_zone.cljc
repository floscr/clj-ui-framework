(ns ui.drop-zone
  "Drag & drop file upload zone.

   Without children the whole zone is a <label> wrapping a hidden file
   input (click anywhere to open the picker) showing the default empty
   state (icon + title + hint). With children, they replace the empty
   state inside a .drop-zone-content wrapper and no file input is
   rendered — compose `file-label` for a click-to-add affordance.

   In :cljs / :squint, `:on-files` is called with the selected/dropped
   files. In :clj (hiccup) there are no event handlers: the zone gets a
   `data-ui-drop-zone` attribute and the JS runtime toggles the active
   class and dispatches a `ui:drop-zone-files` CustomEvent (detail:
   {files: File[]}) on the zone element."
  (:require [clojure.string :as str]
            [ui.icon :as icon]
            [ui.util :as util]))

;; ── Class helpers ───────────────────────────────────────────────────

(defn drop-zone-class-list
  "Returns a vector of CSS class strings for the zone container."
  [{:keys [size drag-over? disabled]}]
  (let [s (or (some-> size util/kw-name) "md")]
    (cond-> ["drop-zone" (str "drop-zone-" s)]
      drag-over? (conj "drop-zone-active")
      disabled (conj "drop-zone-disabled"))))

(defn drop-zone-classes
  "Returns a space-joined class string."
  [opts]
  (str/join " " (drop-zone-class-list opts)))

;; ── Browser event handlers (:cljs / :squint only) ───────────────────

#?(:squint (defn- ->files [fs] (js/Array.from fs))
   :cljs   (defn- ->files [fs] (vec (array-seq fs)))
   :clj    (defn- ->files [fs] fs))

;; These use only host interop and are never invoked in :clj (the hiccup
;; target attaches no handlers), so they compile unconditionally.

(defn- set-active!
  "Uncontrolled mode: toggle the active class directly on the zone."
  [e active?]
  (let [cl (.-classList (.-currentTarget e))]
    (if active?
      (.add cl "drop-zone-active")
      (.remove cl "drop-zone-active"))))

(defn- drag-change! [on-drag-change e active?]
  (if on-drag-change
    (on-drag-change active?)
    (set-active! e active?)))

(defn- handle-dragover [on-drag-change]
  (fn [e]
    (.preventDefault e)
    (drag-change! on-drag-change e true)))

(defn- handle-dragleave [on-drag-change]
  (fn [e]
    (.preventDefault e)
    ;; Ignore dragleave fired when moving over child elements.
    (when-not (and (.-relatedTarget e)
                   (.contains (.-currentTarget e) (.-relatedTarget e)))
      (drag-change! on-drag-change e false))))

(defn- handle-drop [on-files on-drag-change]
  (fn [e]
    (.preventDefault e)
    (drag-change! on-drag-change e false)
    (when on-files
      (on-files (->files (.. e -dataTransfer -files))))))

(defn- handle-input-change [on-files]
  (fn [e]
    (when on-files
      (on-files (->files (.. e -target -files))))))

;; ── Components ──────────────────────────────────────────────────────

(defn file-label
  "A styled <label> wrapping a hidden file input — click-to-select
   affordance for use inside a drop-zone with custom children.

   Props:
     :accept   - file input accept string (default \"image/*\")
     :multiple - allow multiple files (default true)
     :on-files - (fn [files]) called with the selected files
                 (:cljs/:squint; in :clj the JS runtime dispatches
                 ui:drop-zone-files on the enclosing zone)
     :class    - additional CSS classes
     :attrs    - additional HTML attributes map"
  [{:keys [accept multiple on-files class attrs] :as _props} & children]
  (let [accept (or accept "image/*")
        multiple (if (nil? multiple) true multiple)]
    #?(:squint
       (into [:label (merge {:class (cond-> "drop-zone-add"
                                      class (str " " class))}
                            attrs)
              [:input {:type "file" :accept accept :multiple multiple
                       :class "sr-only"
                       :on-change (handle-input-change on-files)}]]
             children)

       :cljs
       (into [:label (merge {:class (cond-> ["drop-zone-add"]
                                      class (util/conj-classes class))}
                            attrs)
              [:input {:type "file" :accept accept :multiple multiple
                       :class ["sr-only"]
                       :on {:change (handle-input-change on-files)}}]]
             children)

       :clj
       (into [:label (merge {:class (cond-> "drop-zone-add"
                                      class (str " " class))}
                            attrs)
              [:input {:type "file" :accept accept :multiple multiple
                       :class "sr-only"}]]
             children))))

(defn drop-zone
  "Drag & drop file upload zone.

   Props:
     :accept         - file input accept string (default \"image/*\")
     :multiple       - allow multiple files (default true)
     :icon           - empty-state icon keyword (default :upload)
     :title          - empty-state title text
     :hint           - empty-state hint text
     :size           - :md (default) or :lg
     :drag-over?     - externally-controlled active state
     :disabled       - boolean, disables drop/click and dims the zone
     :on-files       - (fn [files]) files dropped or selected (:cljs/:squint)
     :on-drag-change - (fn [bool]) drag-over change (:cljs/:squint). When
                       omitted the active class is toggled directly on
                       the DOM node (uncontrolled mode).
     :class, :attrs  - additional classes / attributes"
  [{:keys [accept multiple icon title hint size drag-over? disabled
           on-files on-drag-change class attrs] :as _props}
   & children]
  (let [accept (or accept "image/*")
        multiple (if (nil? multiple) true multiple)
        icon (or icon :upload)
        disabled? (boolean disabled)
        has-children? (seq children)
        cls-opts {:size size :drag-over? drag-over? :disabled disabled?}]
    #?(:squint
       (let [classes (cond-> (drop-zone-classes cls-opts)
                       class (str " " class))
             base-attrs (merge (cond-> {:class classes}
                                 (not disabled?)
                                 (assoc :on-dragover (handle-dragover on-drag-change)
                                        :on-dragleave (handle-dragleave on-drag-change)
                                        :on-drop (handle-drop on-files on-drag-change)))
                               attrs)]
         (if has-children?
           [:div base-attrs
            (into [:div {:class "drop-zone-content"}] children)]
           [:label base-attrs
            [:input (cond-> {:type "file" :accept accept :multiple multiple
                             :class "sr-only"
                             :on-change (handle-input-change on-files)}
                      disabled? (assoc :disabled true))]
            [:div {:class "drop-zone-empty"}
             (icon/icon {:icon-name icon :size :xl :class "drop-zone-icon"})
             (when title [:p {:class "drop-zone-title"} title])
             (when hint [:p {:class "drop-zone-hint"} hint])]]))

       :cljs
       (let [cls (cond-> (drop-zone-class-list cls-opts)
                   class (util/conj-classes class))
             base-attrs (merge (cond-> {:class cls}
                                 (not disabled?)
                                 (assoc :on {:dragover (handle-dragover on-drag-change)
                                             :dragleave (handle-dragleave on-drag-change)
                                             :drop (handle-drop on-files on-drag-change)}))
                               attrs)]
         (if has-children?
           [:div base-attrs
            (into [:div {:class ["drop-zone-content"]}] children)]
           [:label base-attrs
            [:input {:type "file" :accept accept :multiple multiple
                     :disabled disabled?
                     :class ["sr-only"]
                     :on {:change (handle-input-change on-files)}}]
            [:div {:class ["drop-zone-empty"]}
             (icon/icon {:icon-name icon :size :xl :class "drop-zone-icon"})
             (when title [:p {:class ["drop-zone-title"]} title])
             (when hint [:p {:class ["drop-zone-hint"]} hint])]]))

       :clj
       (let [classes (cond-> (drop-zone-classes cls-opts)
                       class (str " " class))
             base-attrs (merge (cond-> {:class classes}
                                 (not disabled?) (assoc :data-ui-drop-zone ""))
                               attrs)]
         (if has-children?
           [:div base-attrs
            (into [:div {:class "drop-zone-content"}] children)]
           [:label base-attrs
            [:input (cond-> {:type "file" :accept accept :multiple multiple
                             :class "sr-only"}
                      disabled? (assoc :disabled true))]
            [:div {:class "drop-zone-empty"}
             (icon/icon {:icon-name icon :size :xl :class "drop-zone-icon"})
             (when title [:p {:class "drop-zone-title"} title])
             (when hint [:p {:class "drop-zone-hint"} hint])]])))))

;; ── Full-page drop (body listeners + overlay) ───────────────────────

#?(:squint
   (defn init-body-drop-zone!
     "Attaches document-level drag & drop listeners for full-page drop
      zones. Returns a cleanup function that removes all listeners.

      Props:
        :on-files         - (fn [files]) called with the dropped files
        :on-active-change - (fn [active?]) drag enters/leaves the page"
     [{:keys [on-files on-active-change]}]
     (let [counter (atom 0)
           on-dragenter (fn [e]
                          (.preventDefault e)
                          (swap! counter inc)
                          (when (= @counter 1)
                            (when on-active-change (on-active-change true))))
           on-dragleave (fn [e]
                          (.preventDefault e)
                          (swap! counter dec)
                          (when (<= @counter 0)
                            (reset! counter 0)
                            (when on-active-change (on-active-change false))))
           on-dragover  (fn [e] (.preventDefault e))
           on-drop      (fn [e]
                          (.preventDefault e)
                          (reset! counter 0)
                          (when on-active-change (on-active-change false))
                          (when on-files
                            (on-files (->files (.. e -dataTransfer -files)))))]
       (.addEventListener js/document "dragenter" on-dragenter)
       (.addEventListener js/document "dragleave" on-dragleave)
       (.addEventListener js/document "dragover" on-dragover)
       (.addEventListener js/document "drop" on-drop)
       (fn []
         (.removeEventListener js/document "dragenter" on-dragenter)
         (.removeEventListener js/document "dragleave" on-dragleave)
         (.removeEventListener js/document "dragover" on-dragover)
         (.removeEventListener js/document "drop" on-drop))))

   :cljs
   (defn init-body-drop-zone!
     "Attaches document-level drag & drop listeners for full-page drop
      zones. Returns a cleanup function that removes all listeners.

      Props:
        :on-files         - (fn [files]) called with the dropped files
        :on-active-change - (fn [active?]) drag enters/leaves the page"
     [{:keys [on-files on-active-change]}]
     (let [counter (atom 0)
           on-dragenter (fn [e]
                          (.preventDefault e)
                          (swap! counter inc)
                          (when (= @counter 1)
                            (when on-active-change (on-active-change true))))
           on-dragleave (fn [e]
                          (.preventDefault e)
                          (swap! counter dec)
                          (when (<= @counter 0)
                            (reset! counter 0)
                            (when on-active-change (on-active-change false))))
           on-dragover  (fn [e] (.preventDefault e))
           on-drop      (fn [e]
                          (.preventDefault e)
                          (reset! counter 0)
                          (when on-active-change (on-active-change false))
                          (when on-files
                            (on-files (->files (.. e -dataTransfer -files)))))]
       (.addEventListener js/document "dragenter" on-dragenter)
       (.addEventListener js/document "dragleave" on-dragleave)
       (.addEventListener js/document "dragover" on-dragover)
       (.addEventListener js/document "drop" on-drop)
       (fn []
         (.removeEventListener js/document "dragenter" on-dragenter)
         (.removeEventListener js/document "dragleave" on-dragleave)
         (.removeEventListener js/document "dragover" on-dragover)
         (.removeEventListener js/document "drop" on-drop)))))

(defn drop-zone-overlay
  "Full-screen overlay shown while files are dragged over the page.

   The consumer controls visibility (typically via :on-active-change
   from init-body-drop-zone!).

   Props:
     :title - main text (default \"Drop files to upload\")
     :hint  - sub text (default \"Release to add your files\")
     :class, :attrs - additional classes / attributes"
  [{:keys [title hint class attrs] :as _props}]
  (let [title-text (or title "Drop files to upload")
        hint-text (or hint "Release to add your files")
        icon-el (icon/icon {:icon-name :upload :size :xl :class "drop-zone-icon"})]
    #?(:squint
       [:div (merge {:class (cond-> "drop-zone-overlay"
                              class (str " " class))}
                    attrs)
        [:div {:class "drop-zone-overlay-backdrop"}]
        [:div {:class "drop-zone-overlay-content"}
         icon-el
         [:div {:class "drop-zone-title"} title-text]
         [:div {:class "drop-zone-hint"} hint-text]]]

       :cljs
       [:div (merge {:class (cond-> ["drop-zone-overlay"]
                              class (util/conj-classes class))}
                    attrs)
        [:div {:class ["drop-zone-overlay-backdrop"]}]
        [:div {:class ["drop-zone-overlay-content"]}
         icon-el
         [:div {:class ["drop-zone-title"]} title-text]
         [:div {:class ["drop-zone-hint"]} hint-text]]]

       :clj
       [:div (merge {:class (cond-> "drop-zone-overlay"
                              class (str " " class))}
                    attrs)
        [:div {:class "drop-zone-overlay-backdrop"}]
        [:div {:class "drop-zone-overlay-content"}
         icon-el
         [:div {:class "drop-zone-title"} title-text]
         [:div {:class "drop-zone-hint"} hint-text]]])))
