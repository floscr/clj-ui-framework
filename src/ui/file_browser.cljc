(ns ui.file-browser
  "File browser components — grid/list views and file drop zone.

   Usage:
     ;; Grid view
     [:div {:class \"fb-grid\"}
      (for [item files]
        (file-item-grid {:item item
                         :context-menu-items [{:label \"Open\" :icon :folder}
                                              {:label \"Delete\" :icon :trash :variant :danger}]}))]\n
     ;; List view
     [:div {:class \"fb-list\"}
      (file-list-header {})
      (for [item files]
        (file-item-list {:item item}))]

     ;; Drop zone
     (file-dropzone {:id \"upload\"
                     :accept \"image/*,.pdf\"
                     :multiple true
                     :on-files (fn [files] ...)})"
  (:require [clojure.string :as str]
            [ui.icon :as icon]
            [ui.progress :as progress]
            [ui.context-menu :as context-menu]))

;; In squint, keywords are strings — name is identity
#?(:squint (defn- kw-name [s] s)
   :cljs   (defn- kw-name [s] (name s))
   :clj    (defn- kw-name [s] (name s)))

;; ── File type → icon mapping ────────────────────────────────────────

(def ^:private type->icon-name
  {"folder"      "folder"
   "image"       "image"
   "video"       "film"
   "audio"       "music"
   "document"    "file-text"
   "spreadsheet" "file-spreadsheet"
   "code"        "code"
   "archive"     "package"
   "file"        "file"})

(defn file-type->icon
  "Returns the icon name for a file type.
   Returns a keyword in :clj/:cljs, a string in :squint."
  [file-type]
  (let [t (kw-name (or file-type "file"))
        icon-str (or (get type->icon-name t) "file")]
    #?(:squint icon-str
       :cljs   (keyword icon-str)
       :clj    (keyword icon-str))))

;; ── File Type Icon ──────────────────────────────────────────────────

(defn file-type-icon
  "Renders a file type icon.
   Props:
     :file-type - keyword/string (:folder, :image, :video, etc.)
     :size      - icon size (:sm, :md, :lg, :xl)"
  [{:keys [file-type size]}]
  (let [icon-name (file-type->icon file-type)]
    #?(:squint
       [:div {:class "fb-item-icon"}
        (icon/icon {:icon-name icon-name :size (or size "lg")})]

       :cljs
       [:div {:class ["fb-item-icon"]}
        (icon/icon {:icon-name icon-name :size (or size :lg)})]

       :clj
       [:div {:class "fb-item-icon"}
        (icon/icon {:icon-name icon-name :size (or size :lg)})])))

;; ── View Toggle ─────────────────────────────────────────────────────

(defn file-view-toggle
  "Grid/list view toggle buttons.
   Props:
     :view          - current view (:grid or :list)
     :on-grid-click - handler for grid button
     :on-list-click - handler for list button"
  [{:keys [view on-grid-click on-list-click]}]
  (let [v (kw-name (or view "grid"))
        grid-active? (= v "grid")
        list-active? (= v "list")]
    #?(:squint
       [:div {:class "fb-view-toggle"}
        [:button {:class (str "fb-view-toggle-btn"
                              (when grid-active? " fb-view-toggle-btn-active"))
                  :on-click on-grid-click
                  :title "Grid view"}
         (icon/icon {:icon-name "grid" :size "sm"})]
        [:button {:class (str "fb-view-toggle-btn"
                              (when list-active? " fb-view-toggle-btn-active"))
                  :on-click on-list-click
                  :title "List view"}
         (icon/icon {:icon-name "list" :size "sm"})]]

       :cljs
       [:div {:class ["fb-view-toggle"]}
        [:button {:class (cond-> ["fb-view-toggle-btn"]
                           grid-active? (conj "fb-view-toggle-btn-active"))
                  :on {:click on-grid-click}
                  :title "Grid view"}
         (icon/icon {:icon-name :grid :size :sm})]
        [:button {:class (cond-> ["fb-view-toggle-btn"]
                           list-active? (conj "fb-view-toggle-btn-active"))
                  :on {:click on-list-click}
                  :title "List view"}
         (icon/icon {:icon-name :list :size :sm})]]

       :clj
       [:div {:class "fb-view-toggle"}
        [:button {:class (str "fb-view-toggle-btn"
                              (when grid-active? " fb-view-toggle-btn-active"))
                  :title "Grid view"}
         (icon/icon {:icon-name :grid :size :sm})]
        [:button {:class (str "fb-view-toggle-btn"
                              (when list-active? " fb-view-toggle-btn-active"))
                  :title "List view"}
         (icon/icon {:icon-name :list :size :sm})]])))

;; ── Grid Item ───────────────────────────────────────────────────────

(defn file-item-grid
  "Renders a file as a grid tile.
   Props:
     :item               - map with :name, :file-type, :size
     :selected           - boolean
     :class              - additional CSS classes
     :attrs              - additional HTML attributes
     :on-click           - click handler (cljs/squint)
     :context-menu-items - if provided, wraps in context-menu-trigger"
  [{:keys [item selected class attrs on-click context-menu-items]}]
  (let [{:keys [name file-type size]} item
        selected? (boolean selected)
        inner
        #?(:squint
           [:div (merge {:class (cond-> "fb-item fb-item-grid"
                                  selected? (str " fb-item-selected")
                                  class     (str " " class))
                         :on-click on-click}
                        attrs)
            (file-type-icon {:file-type file-type})
            [:div {:class "fb-item-name"} name]
            (when size
              [:div {:class "fb-item-meta"} size])]

           :cljs
           [:div (merge {:class (cond-> ["fb-item" "fb-item-grid"]
                                  selected? (conj "fb-item-selected")
                                  class     (conj class))
                         :on {:click on-click}}
                        attrs)
            (file-type-icon {:file-type file-type})
            [:div {:class ["fb-item-name"]} name]
            (when size
              [:div {:class ["fb-item-meta"]} size])]

           :clj
           [:div (merge {:class (cond-> "fb-item fb-item-grid"
                                  selected? (str " fb-item-selected")
                                  class     (str " " class))}
                        attrs)
            (file-type-icon {:file-type file-type})
            [:div {:class "fb-item-name"} name]
            (when size
              [:div {:class "fb-item-meta"} size])])]
    (if (seq context-menu-items)
      (context-menu/context-menu-trigger {:items context-menu-items} inner)
      inner)))

;; ── List Header ─────────────────────────────────────────────────────

(defn- sort-indicator
  "Renders a chevron icon indicating sort direction."
  [dir]
  (let [icon-name #?(:squint (if (= dir "asc") "chevron-up" "chevron-down")
                     :cljs   (if (= (kw-name dir) "asc") :chevron-up :chevron-down)
                     :clj    (if (= (kw-name dir) "asc") :chevron-up :chevron-down))]
    (icon/icon {:icon-name icon-name :size #?(:squint "sm" :cljs :sm :clj :sm)})))

(defn- header-cell
  "A single sortable column header cell."
  [label col-key sort-key sort-dir on-sort]
  (let [active? (= (kw-name col-key) (kw-name (or sort-key "")))
        sortable? (some? on-sort)]
    #?(:squint
       [:div {:class (cond-> "fb-list-header-cell"
                       active?   (str " fb-list-header-cell-active")
                       sortable? (str " fb-list-header-cell-sortable"))
              :on-click (when sortable? (fn [e] (on-sort col-key)))}
        [:span label]
        (when active? (sort-indicator sort-dir))]

       :cljs
       [:div {:class (cond-> ["fb-list-header-cell"]
                       active?   (conj "fb-list-header-cell-active")
                       sortable? (conj "fb-list-header-cell-sortable"))
              :on (when sortable? {:click (fn [e] (on-sort col-key))})}
        [:span label]
        (when active? (sort-indicator sort-dir))]

       :clj
       [:div {:class (cond-> "fb-list-header-cell"
                       active?   (str " fb-list-header-cell-active")
                       sortable? (str " fb-list-header-cell-sortable"))}
        [:span label]
        (when active? (sort-indicator sort-dir))])))

(defn file-list-header
  "Renders the column header row for list view.
   Props:
     :sort-key - current sort column (:name, :size, :modified, :type)
     :sort-dir - current direction (:asc or :desc)
     :on-sort  - (fn [column-key]) called when a column header is clicked
     :class    - additional CSS classes
     :attrs    - additional HTML attributes"
  [{:keys [sort-key sort-dir on-sort class attrs]}]
  #?(:squint
     [:div (merge {:class (cond-> "fb-list-header"
                            class (str " " class))}
                  attrs)
      (header-cell "Name"     "name"     sort-key sort-dir on-sort)
      (header-cell "Size"     "size"     sort-key sort-dir on-sort)
      (header-cell "Modified" "modified" sort-key sort-dir on-sort)
      (header-cell "Type"     "type"     sort-key sort-dir on-sort)]

     :cljs
     [:div (merge {:class (cond-> ["fb-list-header"]
                            class (conj class))}
                  attrs)
      (header-cell "Name"     :name     sort-key sort-dir on-sort)
      (header-cell "Size"     :size     sort-key sort-dir on-sort)
      (header-cell "Modified" :modified sort-key sort-dir on-sort)
      (header-cell "Type"     :type     sort-key sort-dir on-sort)]

     :clj
     [:div (merge {:class (cond-> "fb-list-header"
                            class (str " " class))}
                  attrs)
      (header-cell "Name"     :name     sort-key sort-dir on-sort)
      (header-cell "Size"     :size     sort-key sort-dir on-sort)
      (header-cell "Modified" :modified sort-key sort-dir on-sort)
      (header-cell "Type"     :type     sort-key sort-dir on-sort)]))

;; ── List Item ───────────────────────────────────────────────────────

(defn file-item-list
  "Renders a file as a list row.
   Props:
     :item               - map with :name, :file-type, :size, :modified
     :selected           - boolean
     :class              - additional CSS classes
     :attrs              - additional HTML attributes
     :on-click           - click handler
     :context-menu-items - if provided, wraps in context-menu-trigger"
  [{:keys [item selected class attrs on-click context-menu-items]}]
  (let [{:keys [name file-type size modified]} item
        selected? (boolean selected)
        type-label (str/capitalize (kw-name (or file-type "file")))
        inner
        #?(:squint
           [:div (merge {:class (cond-> "fb-item fb-item-list"
                                  selected? (str " fb-item-selected")
                                  class     (str " " class))
                         :on-click on-click}
                        attrs)
            [:div {:class "fb-item-name-cell"}
             (file-type-icon {:file-type file-type :size "sm"})
             [:div {:class "fb-item-name"} name]]
            [:div {:class "fb-item-meta"} (or size "\u2014")]
            [:div {:class "fb-item-meta"} (or modified "\u2014")]
            [:div {:class "fb-item-meta"} type-label]]

           :cljs
           [:div (merge {:class (cond-> ["fb-item" "fb-item-list"]
                                  selected? (conj "fb-item-selected")
                                  class     (conj class))
                         :on {:click on-click}}
                        attrs)
            [:div {:class ["fb-item-name-cell"]}
             (file-type-icon {:file-type file-type :size :sm})
             [:div {:class ["fb-item-name"]} name]]
            [:div {:class ["fb-item-meta"]} (or size "\u2014")]
            [:div {:class ["fb-item-meta"]} (or modified "\u2014")]
            [:div {:class ["fb-item-meta"]} type-label]]

           :clj
           [:div (merge {:class (cond-> "fb-item fb-item-list"
                                  selected? (str " fb-item-selected")
                                  class     (str " " class))}
                        attrs)
            [:div {:class "fb-item-name-cell"}
             (file-type-icon {:file-type file-type :size :sm})
             [:div {:class "fb-item-name"} name]]
            [:div {:class "fb-item-meta"} (or size "\u2014")]
            [:div {:class "fb-item-meta"} (or modified "\u2014")]
            [:div {:class "fb-item-meta"} type-label]])]
    (if (seq context-menu-items)
      (context-menu/context-menu-trigger {:items context-menu-items} inner)
      inner)))

;; ── Drop Zone ─────────────────────────────────────────────────────────

(defn file-dropzone
  "Renders a drag-and-drop file upload zone.

   In :clj (SSR) this renders a <label> wrapping a hidden <input type=\"file\">
   so clicking opens the native file picker without JS.

   In :cljs/:squint this adds drag/drop event handlers and a click handler
   that programmatically opens the file picker.

   Props:
     :id       - unique id for the hidden file input (required)
     :accept   - accepted file types (e.g. \"image/*,.pdf\")
     :multiple - boolean, allow multiple files
     :disabled - boolean
     :on-files - (fn [file-list]) called when files are picked/dropped
     :active   - boolean, force the drag-active visual state
     :title    - main text (default \"Drag & drop files here\")
     :subtitle - sub text (default \"or click to browse\")
     :class    - additional CSS classes
     :attrs    - additional HTML attributes"
  [{:keys [id accept multiple disabled on-files active title subtitle class attrs]}]
  (let [title-text    (or title "Drag & drop files here")
        subtitle-text (or subtitle "or click to browse")
        disabled?     (boolean disabled)
        active?       (boolean active)
        input-id      (or id "fb-dropzone-input")]
    #?(:squint
       [:div (merge {:class (cond-> "fb-dropzone"
                              active?   (str " fb-dropzone-active")
                              disabled? (str " fb-dropzone-disabled")
                              class     (str " " class))
                     :on-click (when-not disabled?
                                 (fn [e]
                                   (when-let [input (.querySelector (.-currentTarget e)
                                                                    (str "#" input-id))]
                                     (.click input))))
                     :on-drag-over (when-not disabled?
                                     (fn [e]
                                       (.preventDefault e)
                                       (.. e -currentTarget -classList (add "fb-dropzone-active"))))
                     :on-drag-leave (when-not disabled?
                                      (fn [e]
                                        (.preventDefault e)
                                        (.. e -currentTarget -classList (remove "fb-dropzone-active"))))
                     :on-drop (when-not disabled?
                                (fn [e]
                                  (.preventDefault e)
                                  (.. e -currentTarget -classList (remove "fb-dropzone-active"))
                                  (when on-files
                                    (on-files (.. e -dataTransfer -files)))))}
                    attrs)
        [:input {:id input-id :type "file" :class "fb-dropzone-input"
                 :accept accept :multiple (boolean multiple)
                 :on-change (when on-files
                              (fn [e]
                                (on-files (.. e -target -files))
                                (set! (.. e -target -value) "")))}]
        [:div {:class "fb-dropzone-content"}
         [:div {:class "fb-dropzone-icon"}
          (icon/icon {:icon-name "upload" :size "xl"})]
         [:div {:class "fb-dropzone-text"} title-text]
         [:div {:class "fb-dropzone-subtext"} subtitle-text]]]

       :cljs
       [:div (merge {:class (cond-> ["fb-dropzone"]
                              active?   (conj "fb-dropzone-active")
                              disabled? (conj "fb-dropzone-disabled")
                              class     (conj class))
                     :on (when-not disabled?
                           {:click (fn [e]
                                     (when-let [input (.querySelector (.-currentTarget e)
                                                                      (str "#" input-id))]
                                       (.click input)))
                            :dragover (fn [e]
                                        (.preventDefault e)
                                        (.. e -currentTarget -classList (add "fb-dropzone-active")))
                            :dragleave (fn [e]
                                         (.preventDefault e)
                                         (.. e -currentTarget -classList (remove "fb-dropzone-active")))
                            :drop (fn [e]
                                    (.preventDefault e)
                                    (.. e -currentTarget -classList (remove "fb-dropzone-active"))
                                    (when on-files
                                      (on-files (.. e -dataTransfer -files))))})}
                    attrs)
        [:input {:id input-id :type "file" :class ["fb-dropzone-input"]
                 :accept accept :multiple (boolean multiple)
                 :on {:change (when on-files
                                (fn [e]
                                  (on-files (.. e -target -files))
                                  (set! (.. e -target -value) "")))}}]
        [:div {:class ["fb-dropzone-content"]}
         [:div {:class ["fb-dropzone-icon"]}
          (icon/icon {:icon-name :upload :size :xl})]
         [:div {:class ["fb-dropzone-text"]} title-text]
         [:div {:class ["fb-dropzone-subtext"]} subtitle-text]]]

       :clj
       [:label (merge {:class (cond-> "fb-dropzone"
                                disabled? (str " fb-dropzone-disabled")
                                class     (str " " class))
                       :for input-id}
                      attrs)
        [:input (cond-> {:id input-id :type "file" :class "fb-dropzone-input"
                         :name input-id}
                  accept   (assoc :accept accept)
                  multiple (assoc :multiple true)
                  disabled (assoc :disabled true))]
        [:div {:class "fb-dropzone-content"}
         [:div {:class "fb-dropzone-icon"}
          (icon/icon {:icon-name :upload :size :xl})]
         [:div {:class "fb-dropzone-text"} title-text]
         [:div {:class "fb-dropzone-subtext"} subtitle-text]]])))

;; ── Drop Zone File Item ─────────────────────────────────────────────

(defn file-dropzone-item
  "Renders a file entry in the upload queue.

   Props:
     :name      - file name
     :size      - formatted size string (e.g. \"2.4 MB\")
     :file-type - keyword/string for icon (:image, :document, etc.)
     :progress  - upload progress 0–100 (nil = no progress bar)
     :status    - :idle, :uploading, :complete, :error
     :on-remove - (fn []) called when remove button is clicked
     :class     - additional CSS classes
     :attrs     - additional HTML attributes"
  [{:keys [name size file-type progress status on-remove class attrs]}]
  (let [s (kw-name (or status "idle"))
        error?    (= s "error")
        complete? (= s "complete")]
    #?(:squint
       [:div (merge {:class (cond-> "fb-dropzone-file"
                              error?    (str " fb-dropzone-file-error")
                              complete? (str " fb-dropzone-file-complete")
                              class     (str " " class))}
                    attrs)
        [:div {:class "fb-dropzone-file-icon"}
         (file-type-icon {:file-type (or file-type "file") :size "sm"})]
        [:div {:class "fb-dropzone-file-info"}
         [:div {:class "fb-dropzone-file-name"} name]
         (when size
           [:div {:class "fb-dropzone-file-size"} size])
         (when (and progress (not complete?))
           (progress/progress {:value progress
                               :variant (when error? "danger")}))]
        (when on-remove
          [:button {:class "fb-dropzone-file-remove"
                    :on-click (fn [e]
                                (.stopPropagation e)
                                (on-remove))
                    :title "Remove"}
           (icon/icon {:icon-name "x" :size "sm"})])]

       :cljs
       [:div (merge {:class (cond-> ["fb-dropzone-file"]
                              error?    (conj "fb-dropzone-file-error")
                              complete? (conj "fb-dropzone-file-complete")
                              class     (conj class))}
                    attrs)
        [:div {:class ["fb-dropzone-file-icon"]}
         (file-type-icon {:file-type (or file-type :file) :size :sm})]
        [:div {:class ["fb-dropzone-file-info"]}
         [:div {:class ["fb-dropzone-file-name"]} name]
         (when size
           [:div {:class ["fb-dropzone-file-size"]} size])
         (when (and progress (not complete?))
           (progress/progress {:value progress
                               :variant (when error? :danger)}))]
        (when on-remove
          [:button {:class ["fb-dropzone-file-remove"]
                    :on {:click (fn [e]
                                  (.stopPropagation e)
                                  (on-remove))}
                    :title "Remove"}
           (icon/icon {:icon-name :x :size :sm})])]

       :clj
       [:div (merge {:class (cond-> "fb-dropzone-file"
                              error?    (str " fb-dropzone-file-error")
                              complete? (str " fb-dropzone-file-complete")
                              class     (str " " class))}
                    attrs)
        [:div {:class "fb-dropzone-file-icon"}
         (file-type-icon {:file-type (or file-type :file) :size :sm})]
        [:div {:class "fb-dropzone-file-info"}
         [:div {:class "fb-dropzone-file-name"} name]
         (when size
           [:div {:class "fb-dropzone-file-size"} size])
         (when (and progress (not complete?))
           (progress/progress {:value progress
                               :variant (when error? :danger)}))]
        (when on-remove
          [:button {:class "fb-dropzone-file-remove"
                    :title "Remove"}
           (icon/icon {:icon-name :x :size :sm})])])))

;; ── Drop Zone File List ─────────────────────────────────────────────

(defn file-dropzone-list
  "Wraps a list of file-dropzone-item elements.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes
   Children: file-dropzone-item elements"
  [{:keys [class attrs]} & children]
  #?(:squint
     (into [:div (merge {:class (cond-> "fb-dropzone-files"
                                  class (str " " class))}
                        attrs)]
           children)

     :cljs
     (into [:div (merge {:class (cond-> ["fb-dropzone-files"]
                                  class (conj class))}
                        attrs)]
           children)

     :clj
     (into [:div (merge {:class (cond-> "fb-dropzone-files"
                                  class (str " " class))}
                        attrs)]
           children)))

;; ── Body Drop Zone (full-page drag & drop) ──────────────────────────

#?(:squint
   (defn init-body-dropzone!
     "Attaches document-level drag & drop listeners for full-page drop zones.

      Returns a cleanup function that removes all listeners.

      Props:
        :on-files         - (fn [file-list]) called when files are dropped
        :on-active-change - (fn [active?]) called when drag enters/leaves the page"
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
                            (on-files (.. e -dataTransfer -files))))]
       (.addEventListener js/document "dragenter" on-dragenter)
       (.addEventListener js/document "dragleave" on-dragleave)
       (.addEventListener js/document "dragover" on-dragover)
       (.addEventListener js/document "drop" on-drop)
       ;; Return cleanup fn
       (fn []
         (.removeEventListener js/document "dragenter" on-dragenter)
         (.removeEventListener js/document "dragleave" on-dragleave)
         (.removeEventListener js/document "dragover" on-dragover)
         (.removeEventListener js/document "drop" on-drop))))

   :cljs
   (defn init-body-dropzone!
     "Attaches document-level drag & drop listeners for full-page drop zones.

      Returns a cleanup function that removes all listeners.

      Props:
        :on-files         - (fn [file-list]) called when files are dropped
        :on-active-change - (fn [active?]) called when drag enters/leaves the page"
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
                            (on-files (.. e -dataTransfer -files))))]
       (.addEventListener js/document "dragenter" on-dragenter)
       (.addEventListener js/document "dragleave" on-dragleave)
       (.addEventListener js/document "dragover" on-dragover)
       (.addEventListener js/document "drop" on-drop)
       ;; Return cleanup fn
       (fn []
         (.removeEventListener js/document "dragenter" on-dragenter)
         (.removeEventListener js/document "dragleave" on-dragleave)
         (.removeEventListener js/document "dragover" on-dragover)
         (.removeEventListener js/document "drop" on-drop)))))

(defn file-dropzone-overlay
  "Full-screen overlay shown when files are dragged over the page.

   Renders a fixed overlay covering the viewport with a drop zone prompt.
   The consumer controls visibility (typically via on-active-change from
   init-body-dropzone!).

   Props:
     :title    - main text (default \"Drop files to upload\")
     :subtitle - sub text (default \"Release to add your files\")
     :class    - additional CSS classes
     :attrs    - additional HTML attributes"
  [{:keys [title subtitle class attrs]}]
  (let [title-text    (or title "Drop files to upload")
        subtitle-text (or subtitle "Release to add your files")]
    #?(:squint
       [:div (merge {:class (cond-> "fb-dropzone-overlay"
                              class (str " " class))}
                    attrs)
        [:div {:class "fb-dropzone-overlay-backdrop"}]
        [:div {:class "fb-dropzone-overlay-content"}
         [:div {:class "fb-dropzone-icon"}
          (icon/icon {:icon-name "upload" :size "xl"})]
         [:div {:class "fb-dropzone-text"} title-text]
         [:div {:class "fb-dropzone-subtext"} subtitle-text]]]

       :cljs
       [:div (merge {:class (cond-> ["fb-dropzone-overlay"]
                              class (conj class))}
                    attrs)
        [:div {:class ["fb-dropzone-overlay-backdrop"]}]
        [:div {:class ["fb-dropzone-overlay-content"]}
         [:div {:class ["fb-dropzone-icon"]}
          (icon/icon {:icon-name :upload :size :xl})]
         [:div {:class ["fb-dropzone-text"]} title-text]
         [:div {:class ["fb-dropzone-subtext"]} subtitle-text]]]

       :clj
       [:div (merge {:class (cond-> "fb-dropzone-overlay"
                              class (str " " class))}
                    attrs)
        [:div {:class "fb-dropzone-overlay-backdrop"}]
        [:div {:class "fb-dropzone-overlay-content"}
         [:div {:class "fb-dropzone-icon"}
          (icon/icon {:icon-name :upload :size :xl})]
         [:div {:class "fb-dropzone-text"} title-text]
         [:div {:class "fb-dropzone-subtext"} subtitle-text]]])))
