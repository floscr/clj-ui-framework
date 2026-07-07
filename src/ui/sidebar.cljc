(ns ui.sidebar
  (:require [clojure.string :as str]
            [ui.icon :as icon]))

;; ── Layout ──────────────────────────────────────────────────────────

(defn sidebar-layout-class-list [_opts] ["sidebar-layout"])
(defn sidebar-layout-classes [opts] (str/join " " (sidebar-layout-class-list opts)))

(defn sidebar-layout
  "Wraps sidebar + main content in a flex row.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:div (merge {:class (cond-> (sidebar-layout-classes {}) class (str " " class))} attrs)]
           children)
     :cljs
     (into [:div (merge {:class (cond-> (sidebar-layout-class-list {}) class (conj class))} attrs)]
           children)
     :clj
     (into [:div (merge {:class (cond-> (sidebar-layout-classes {}) class (str " " class))} attrs)]
           children)))

(defn sidebar-layout-main
  "Main content area next to the sidebar."
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:main (merge {:class (cond-> "sidebar-layout-main" class (str " " class))} attrs)]
           children)
     :cljs
     (into [:main (merge {:class (cond-> ["sidebar-layout-main"] class (conj class))} attrs)]
           children)
     :clj
     (into [:main (merge {:class (cond-> "sidebar-layout-main" class (str " " class))} attrs)]
           children)))

;; ── Sidebar ─────────────────────────────────────────────────────────

(defn sidebar-class-list [_opts] ["sidebar"])
(defn sidebar-classes [opts] (str/join " " (sidebar-class-list opts)))

(defn sidebar
  "Render the sidebar container.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:aside (merge {:class (cond-> (sidebar-classes {}) class (str " " class))} attrs)]
           children)
     :cljs
     (into [:aside (merge {:class (cond-> (sidebar-class-list {}) class (conj class))} attrs)]
           children)
     :clj
     (into [:aside (merge {:class (cond-> (sidebar-classes {}) class (str " " class))} attrs)]
           children)))

;; ── Sidebar Header ──────────────────────────────────────────────────

(defn sidebar-header
  "Render the sidebar header area (top of sidebar).

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:div (merge {:class (cond-> "sidebar-header" class (str " " class))} attrs)]
           children)
     :cljs
     (into [:div (merge {:class (cond-> ["sidebar-header"] class (conj class))} attrs)]
           children)
     :clj
     (into [:div (merge {:class (cond-> "sidebar-header" class (str " " class))} attrs)]
           children)))

;; ── Sidebar Brand ───────────────────────────────────────────────────

(defn sidebar-brand-class-list [_opts] ["sidebar-brand"])
(defn sidebar-brand-classes [opts] (str/join " " (sidebar-brand-class-list opts)))

(defn sidebar-brand
  "Render a brand/logo block at the top of the sidebar.

   Props:
     :title    - brand title (e.g. \"Acme Inc.\")
     :subtitle - version or tagline (e.g. \"v1.0.0\")
     :icon     - single character or short text for the icon badge
     :href     - optional link URL
     :class    - additional CSS classes
     :attrs    - additional HTML attributes"
  [{:keys [title subtitle icon href class attrs] :as _props}]
  (let [icon-char (or icon (when title (subs title 0 1)))]
    #?(:squint
       (let [tag (if href :a :div)
             classes (cond-> (sidebar-brand-classes {}) class (str " " class))
             base-attrs (cond-> (merge {:class classes} attrs)
                          href (assoc :href href))]
         [tag base-attrs
          [:span {:class "sidebar-brand-icon"} icon-char]
          [:span {:class "sidebar-brand-text"}
           [:span {:class "sidebar-brand-title"} title]
           (when subtitle
             [:span {:class "sidebar-brand-subtitle"} subtitle])]])

       :cljs
       (let [tag (if href :a :div)
             cls (sidebar-brand-class-list {})
             classes (cond-> cls class (conj class))
             base-attrs (cond-> (merge {:class classes} attrs)
                          href (assoc :href href))]
         [tag base-attrs
          [:span {:class ["sidebar-brand-icon"]} icon-char]
          [:span {:class ["sidebar-brand-text"]}
           [:span {:class ["sidebar-brand-title"]} title]
           (when subtitle
             [:span {:class ["sidebar-brand-subtitle"]} subtitle])]])

       :clj
       (let [tag (if href :a :div)
             classes (cond-> (sidebar-brand-classes {}) class (str " " class))
             base-attrs (cond-> (merge {:class classes} attrs)
                          href (assoc :href href))]
         [tag base-attrs
          [:span {:class "sidebar-brand-icon"} icon-char]
          [:span {:class "sidebar-brand-text"}
           [:span {:class "sidebar-brand-title"} title]
           (when subtitle
             [:span {:class "sidebar-brand-subtitle"} subtitle])]]))))

;; ── Sidebar Search ──────────────────────────────────────────────────

(defn sidebar-search-class-list [_opts] ["sidebar-search"])
(defn sidebar-search-classes [opts] (str/join " " (sidebar-search-class-list opts)))

(defn sidebar-search
  "Render a search input in the sidebar.

   Props:
     :placeholder - input placeholder text
     :value       - current value
     :on-change   - change handler (cljs/squint only)
     :class       - additional CSS classes
     :attrs       - additional HTML attributes"
  [{:keys [placeholder value on-change class attrs] :as _props}]
  #?(:squint
     (let [classes (cond-> (sidebar-search-classes {}) class (str " " class))
           input-attrs (cond-> {:class "sidebar-search-input"
                                :type "search"
                                :placeholder (or placeholder "Search...")}
                         value     (assoc :value value)
                         on-change (assoc :on-input on-change))]
       [:div (merge {:class classes} attrs)
        [:span {:class "sidebar-search-icon" :aria-hidden "true"}]
        [:input input-attrs]])

     :cljs
     (let [cls (sidebar-search-class-list {})
           classes (cond-> cls class (conj class))
           input-attrs (cond-> {:class ["sidebar-search-input"]
                                :type "search"
                                :placeholder (or placeholder "Search...")}
                         value     (assoc :value value)
                         on-change (assoc-in [:on :input] on-change))]
       [:div (merge {:class classes} attrs)
        [:span {:class ["sidebar-search-icon"] :aria-hidden "true"}]
        [:input input-attrs]])

     :clj
     (let [classes (cond-> (sidebar-search-classes {}) class (str " " class))
           input-attrs (cond-> {:class "sidebar-search-input"
                                :type "search"
                                :placeholder (or placeholder "Search...")}
                         value (assoc :value value))]
       [:div (merge {:class classes} attrs)
        [:span {:class "sidebar-search-icon" :aria-hidden "true"}]
        [:input input-attrs]])))

;; ── Sidebar Content ─────────────────────────────────────────────────

(defn sidebar-content
  "Render the scrollable content area of the sidebar.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:nav (merge {:class (cond-> "sidebar-content" class (str " " class))} attrs)]
           children)
     :cljs
     (into [:nav (merge {:class (cond-> ["sidebar-content"] class (conj class))} attrs)]
           children)
     :clj
     (into [:nav (merge {:class (cond-> "sidebar-content" class (str " " class))} attrs)]
           children)))

;; ── Sidebar Group ───────────────────────────────────────────────────

(defn sidebar-group-class-list [_opts] ["sidebar-group"])
(defn sidebar-group-classes [opts] (str/join " " (sidebar-group-class-list opts)))

(defn sidebar-group
  "Render a nav group with a label and menu items.

   Props:
     :label - group heading text
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [label class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> (sidebar-group-classes {}) class (str " " class))]
       (into [:div (merge {:class classes} attrs)]
             (cond-> []
               label    (conj [:div {:class "sidebar-group-label"} label])
               true     (into children))))

     :cljs
     (let [cls (sidebar-group-class-list {})
           classes (cond-> cls class (conj class))]
       (into [:div (merge {:class classes} attrs)]
             (cond-> []
               label    (conj [:div {:class ["sidebar-group-label"]} label])
               true     (into children))))

     :clj
     (let [classes (cond-> (sidebar-group-classes {}) class (str " " class))]
       (into [:div (merge {:class classes} attrs)]
             (cond-> []
               label    (conj [:div {:class "sidebar-group-label"} label])
               true     (into children))))))

;; ── Sidebar Menu ────────────────────────────────────────────────────

(defn sidebar-menu
  "Render a list of sidebar menu items.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:ul (merge {:class (cond-> "sidebar-menu" class (str " " class))} attrs)]
           (map (fn [child] [:li child]) children))
     :cljs
     (into [:ul (merge {:class (cond-> ["sidebar-menu"] class (conj class))} attrs)]
           (map (fn [child] [:li child]) children))
     :clj
     (into [:ul (merge {:class (cond-> "sidebar-menu" class (str " " class))} attrs)]
           (map (fn [child] [:li child]) children))))

;; ── Sidebar Menu Item ───────────────────────────────────────────────

(defn sidebar-menu-item-class-list
  "Returns class list for a menu item."
  [{:keys [active]}]
  (cond-> ["sidebar-menu-item"]
    active (conj "sidebar-menu-item-active")))

(defn sidebar-menu-item-classes
  "Returns space-joined class string for a menu item."
  [opts]
  (str/join " " (sidebar-menu-item-class-list opts)))

(defn sidebar-menu-item
  "Render a single sidebar menu item.

   Props:
     :href      - URL; renders as <a> when set, <button> otherwise
     :active    - boolean, highlights as active
     :icon-name - keyword icon name (e.g. :home, :settings) from ui.icon
     :badge     - optional badge text/number
     :on-click  - click handler (cljs/squint only)
     :class     - additional CSS classes
     :attrs     - additional HTML attributes"
  [{:keys [href active icon-name badge on-click class attrs] :as _props} & children]
  (let [icon-el (when icon-name
                  #?(:squint
                     [:span {:class "sidebar-menu-item-icon"}
                      (icon/icon {:icon-name icon-name :size :sm})]
                     :cljs
                     [:span {:class ["sidebar-menu-item-icon"]}
                      (icon/icon {:icon-name icon-name :size :sm})]
                     :clj
                     [:span {:class "sidebar-menu-item-icon"}
                      (icon/icon {:icon-name icon-name :size :sm})]))]
    #?(:squint
       (let [tag (if href :a :button)
             classes (cond-> (sidebar-menu-item-classes {:active active})
                       class (str " " class))
             base-attrs (cond-> (merge {:class classes} attrs)
                          href     (assoc :href href)
                          on-click (assoc :on-click on-click))]
         (into [tag base-attrs]
               (cond-> (if icon-el [icon-el] [])
                 true  (into children)
                 badge (conj [:span {:class "sidebar-menu-item-badge"} badge]))))

       :cljs
       (let [tag (if href :a :button)
             cls (sidebar-menu-item-class-list {:active active})
             classes (cond-> cls class (conj class))
             base-attrs (cond-> (merge {:class classes} attrs)
                          href     (assoc :href href)
                          on-click (assoc-in [:on :click] on-click))]
         (into [tag base-attrs]
               (cond-> (if icon-el [icon-el] [])
                 true  (into children)
                 badge (conj [:span {:class ["sidebar-menu-item-badge"]} badge]))))

       :clj
       (let [tag (if href :a :button)
             classes (cond-> (sidebar-menu-item-classes {:active active})
                       class (str " " class))
             base-attrs (cond-> (merge {:class classes} attrs)
                          href (assoc :href href))]
         (into [tag base-attrs]
               (cond-> (if icon-el [icon-el] [])
                 true  (into children)
                 badge (conj [:span {:class "sidebar-menu-item-badge"} badge])))))))

;; ── Sidebar Collapsible ─────────────────────────────────────────────

(defn sidebar-collapsible
  "Render a collapsible section in the sidebar using <details>/<summary>.

   Props:
     :title - trigger text
     :open  - boolean, initially expanded
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [title open class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> "sidebar-collapsible" class (str " " class))
           base-attrs (cond-> (merge {:class classes} attrs)
                        open (assoc :open true))]
       [:details base-attrs
        [:summary
         [:span title]
         [:span {:class "sidebar-collapsible-chevron" :aria-hidden "true"}]]
        (into [:div {:class "sidebar-collapsible-content"}] children)])

     :cljs
     (let [classes (cond-> ["sidebar-collapsible"] class (conj class))
           base-attrs (cond-> (merge {:class classes} attrs)
                        open (assoc :open true))]
       [:details base-attrs
        [:summary
         [:span title]
         [:span {:class ["sidebar-collapsible-chevron"] :aria-hidden "true"}]]
        (into [:div {:class ["sidebar-collapsible-content"]}] children)])

     :clj
     (let [classes (cond-> "sidebar-collapsible" class (str " " class))
           base-attrs (cond-> (merge {:class classes} attrs)
                        open (assoc :open true))]
       [:details base-attrs
        [:summary
         [:span title]
         [:span {:class "sidebar-collapsible-chevron" :aria-hidden "true"}]]
        (into [:div {:class "sidebar-collapsible-content"}] children)])))

;; ── Sidebar Mobile Toggle ────────────────────────────────────────────

(defn sidebar-mobile-toggle-class-list [_opts] ["sidebar-mobile-toggle"])
(defn sidebar-mobile-toggle-classes [opts] (str/join " " (sidebar-mobile-toggle-class-list opts)))

(defn sidebar-mobile-toggle
  "Render a hamburger/close toggle button for mobile sidebar.
   Hidden on desktop via CSS. On click, toggles `data-sidebar-open`
   on the nearest `.sidebar-layout` ancestor.

   Props:
     :on-click - click handler (cljs/squint only)
     :class    - additional CSS classes
     :attrs    - additional HTML attributes"
  [{:keys [on-click class attrs] :as _props}]
  #?(:squint
     (let [classes (cond-> (sidebar-mobile-toggle-classes {}) class (str " " class))
           base-attrs (cond-> (merge {:class classes
                                      :type "button"
                                      :aria-label "Toggle sidebar"} attrs)
                        on-click (assoc :on-click on-click))]
       [:button base-attrs
        [:span {:class "sidebar-toggle-icon-open" :aria-hidden "true"}
         (icon/icon {:icon-name :menu :size :sm})]
        [:span {:class "sidebar-toggle-icon-close" :aria-hidden "true"}
         (icon/icon {:icon-name :x :size :sm})]])

     :cljs
     (let [cls (sidebar-mobile-toggle-class-list {})
           classes (cond-> cls class (conj class))
           base-attrs (cond-> (merge {:class classes
                                      :type "button"
                                      :aria-label "Toggle sidebar"} attrs)
                        on-click (assoc-in [:on :click] on-click))]
       [:button base-attrs
        [:span {:class ["sidebar-toggle-icon-open"] :aria-hidden "true"}
         (icon/icon {:icon-name :menu :size :sm})]
        [:span {:class ["sidebar-toggle-icon-close"] :aria-hidden "true"}
         (icon/icon {:icon-name :x :size :sm})]])

     :clj
     (let [classes (cond-> (sidebar-mobile-toggle-classes {}) class (str " " class))
           base-attrs (merge {:class classes
                              :type "button"
                              :aria-label "Toggle sidebar"
                              :onclick "this.closest('.sidebar-layout').toggleAttribute('data-sidebar-open')"} attrs)]
       [:button base-attrs
        [:span {:class "sidebar-toggle-icon-open" :aria-hidden "true"}
         (icon/icon {:icon-name :menu :size :sm})]
        [:span {:class "sidebar-toggle-icon-close" :aria-hidden "true"}
         (icon/icon {:icon-name :x :size :sm})]])))

;; ── Sidebar Overlay ─────────────────────────────────────────────────

(defn sidebar-overlay
  "Render the backdrop overlay for mobile sidebar.
   Clicking it closes the sidebar. Place inside sidebar-layout,
   as a sibling of the sidebar.

   Props:
     :on-click - click handler (cljs/squint only)
     :class    - additional CSS classes
     :attrs    - additional HTML attributes"
  [{:keys [on-click class attrs] :as _props}]
  #?(:squint
     (let [classes (cond-> "sidebar-overlay" class (str " " class))
           base-attrs (cond-> (merge {:class classes :aria-hidden "true"} attrs)
                        on-click (assoc :on-click on-click))]
       [:div base-attrs])

     :cljs
     (let [classes (cond-> ["sidebar-overlay"] class (conj class))
           base-attrs (cond-> (merge {:class classes :aria-hidden "true"} attrs)
                        on-click (assoc-in [:on :click] on-click))]
       [:div base-attrs])

     :clj
     (let [classes (cond-> "sidebar-overlay" class (str " " class))
           base-attrs (merge {:class classes
                              :aria-hidden "true"
                              :onclick "this.closest('.sidebar-layout').removeAttribute('data-sidebar-open')"} attrs)]
       [:div base-attrs])))

;; ── Sidebar Separator ───────────────────────────────────────────────

(defn sidebar-separator
  "Render a horizontal divider in the sidebar."
  ([] (sidebar-separator {}))
  ([{:keys [class attrs] :as _props}]
   #?(:squint [:hr (merge {:class (cond-> "sidebar-separator" class (str " " class))} attrs)]
      :cljs   [:hr (merge {:class (cond-> ["sidebar-separator"] class (conj class))} attrs)]
      :clj    [:hr (merge {:class (cond-> "sidebar-separator" class (str " " class))} attrs)])))

;; ── Sidebar Footer ──────────────────────────────────────────────────

(defn sidebar-footer
  "Render the sidebar footer area (bottom of sidebar).

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:div (merge {:class (cond-> "sidebar-footer" class (str " " class))} attrs)]
           children)
     :cljs
     (into [:div (merge {:class (cond-> ["sidebar-footer"] class (conj class))} attrs)]
           children)
     :clj
     (into [:div (merge {:class (cond-> "sidebar-footer" class (str " " class))} attrs)]
           children)))

;; ── Sidebar User ────────────────────────────────────────────────────

(defn sidebar-user
  "Render a user info block (typically in the footer).

   Props:
     :name    - user display name
     :email   - user email
     :avatar  - one or two characters for the avatar circle
     :class   - additional CSS classes
     :attrs   - additional HTML attributes"
  [{:keys [#?@(:squint [user-name] :cljs [user-name] :clj [user-name])
           email avatar class attrs] :as _props}]
  (let [initials (or avatar (when user-name (subs user-name 0 2)))]
    #?(:squint
       [:div (merge {:class (cond-> "sidebar-user" class (str " " class))} attrs)
        [:span {:class "sidebar-user-avatar"} initials]
        [:span {:class "sidebar-user-info"}
         [:span {:class "sidebar-user-name"} user-name]
         (when email
           [:span {:class "sidebar-user-email"} email])]]

       :cljs
       [:div (merge {:class (cond-> ["sidebar-user"] class (conj class))} attrs)
        [:span {:class ["sidebar-user-avatar"]} initials]
        [:span {:class ["sidebar-user-info"]}
         [:span {:class ["sidebar-user-name"]} user-name]
         (when email
           [:span {:class ["sidebar-user-email"]} email])]]

       :clj
       [:div (merge {:class (cond-> "sidebar-user" class (str " " class))} attrs)
        [:span {:class "sidebar-user-avatar"} initials]
        [:span {:class "sidebar-user-info"}
         [:span {:class "sidebar-user-name"} user-name]
         (when email
           [:span {:class "sidebar-user-email"} email])]])))
