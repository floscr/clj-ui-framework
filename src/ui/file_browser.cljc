(ns ui.file-browser
  "File browser components — grid and list views for file/folder display.

   Usage:
     ;; Grid view
     [:div {:class \"fb-grid\"}
      (for [item files]
        (file-item-grid {:item item
                         :context-menu-items [{:label \"Open\" :icon :folder}
                                              {:label \"Delete\" :icon :trash :variant :danger}]}))]

     ;; List view
     [:div {:class \"fb-list\"}
      (file-list-header {})
      (for [item files]
        (file-item-list {:item item}))]"
  (:require [clojure.string :as str]
            [ui.icon :as icon]
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
