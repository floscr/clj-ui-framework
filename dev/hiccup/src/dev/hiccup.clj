(ns dev.hiccup
  (:require [org.httpkit.server :as http]
            [hiccup2.core :as h]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [babashka.fs :as fs]
            [ui.button :as button]
            [ui.alert :as alert]
            [ui.badge :as badge]
            [ui.card :as card]
            [ui.accordion :as accordion]
            [ui.table :as table]
            [ui.dialog :as dialog]
            [ui.spinner :as spinner]
            [ui.skeleton :as skeleton]
            [ui.progress :as progress]
            [ui.switch :as switch]
            [ui.tooltip :as tooltip]
            [ui.breadcrumb :as breadcrumb]
            [ui.pagination :as pagination]
            [ui.form :as form]
            [ui.sidebar :as sidebar]
            [ui.icon :as icon]
            [ui.separator :as separator]
            [ui.calendar :as calendar]
            [ui.calendar-events :as cal-events]
            [ui.tag-input :as tag-input]
            [ui.markdown :as markdown]
            [ui.player-bar :as player-bar]
            [ui.lightbox :as lightbox]
            [ui.context-menu :as context-menu]
            [ui.drop-zone :as drop-zone]
            [ui.file-browser :as fb]
            [ui.file-progress :as fp]
            [ui.theme-toggle :as theme-toggle]
            [dev.demos :refer [section page-header button-demo alert-demo badge-demo
                               card-demo accordion-demo table-demo spinner-demo
                               empty-state-demo drop-zone-demo processing-bar-demo
                               toast-demo camera-demo grid-demo
                               skeleton-demo progress-demo switch-demo tooltip-demo
                               breadcrumb-demo separator-demo form-demo number-field-demo chat-demo
                               popover-demo command-demo toolbar-demo button-group-demo header-patterns-demo tabs-demo
                               panels-demo]]))

;; ── Query Params ────────────────────────────────────────────────────

(defn resolve-git-sha
  "Full commit SHA of the running deploy, via git. Falls back to \"unknown\"."
  []
  (try
    (let [{:keys [exit out]} (sh/sh "git" "rev-parse" "HEAD")]
      (if (zero? exit) (str/trim out) "unknown"))
    (catch Exception _ "unknown")))

(def deployed-sha (resolve-git-sha))

(defn parse-query-params
  "Parse query string from URI into a map."
  [uri]
  (if-let [q (second (str/split uri #"\?" 2))]
    (into {}
      (for [pair (str/split q #"&")
            :let [[k v] (str/split pair #"=" 2)]
            :when k]
        [k (or v "")]))
    {}))

(def theme-persistence-script
  "/* Theme persistence: sync changes to URL & parent frame */
  (function() {
    new MutationObserver(function(mutations) {
      for (var i = 0; i < mutations.length; i++) {
        if (mutations[i].attributeName === 'data-theme') {
          var t = document.documentElement.dataset.theme;
          var url = new URL(window.location);
          if (t) url.searchParams.set('theme', t);
          else url.searchParams.delete('theme');
          history.replaceState(null, '', url);
          if (window.parent !== window) {
            window.parent.postMessage({ type: 'theme-change', theme: t || '' }, '*');
          }
        }
      }
    }).observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
    document.addEventListener('click', function(e) {
      var a = e.target.closest('a[href]');
      if (!a) return;
      try {
        var url = new URL(a.href);
        if (url.hostname === location.hostname && url.port !== location.port) {
          var t = document.documentElement.dataset.theme;
          if (t) url.searchParams.set('theme', t);
          a.href = url.toString();
        }
      } catch (ex) {}
    });
  })();")

(def theme-toggle-script
  "/* Wire __uiTheme to sidebar + demo theme toggles */
  (function() {
    if (!window.__uiTheme) return;
    window.__uiTheme.init();
    function syncToggles(state) {
      document.querySelectorAll('.theme-toggle').forEach(function(toggle) {
        toggle.querySelectorAll('.theme-toggle-btn').forEach(function(btn, i) {
          var modes = ['light', 'auto', 'dark'];
          var isActive = (modes[i] === state.mode);
          btn.classList.toggle('theme-toggle-btn-active', isActive);
          btn.setAttribute('aria-checked', String(isActive));
        });
      });
    }
    window.__uiTheme.subscribe(syncToggles);
    document.addEventListener('click', function(e) {
      var btn = e.target.closest('.theme-toggle-btn');
      if (!btn) return;
      var modes = ['light', 'auto', 'dark'];
      var i = Array.from(btn.parentElement.querySelectorAll('.theme-toggle-btn')).indexOf(btn);
      if (i >= 0 && i < modes.length) window.__uiTheme.set(modes[i]);
    });
    syncToggles({ mode: window.__uiTheme.get(), effective: window.__uiTheme.effective() });
  })();")

;; ── Helpers ─────────────────────────────────────────────────────────

;; ── Component Demos ─────────────────────────────────────────────────

(defn dialog-demo []
  (section "Dialog"
    [:p {:style "color: var(--fg-2); font-size: var(--font-sm);"} "Click button to open dialog."]
    (button/button {:variant :primary
                    :attrs {:onclick "document.getElementById('demo-dialog').showModal()"}}
      "Open dialog")
    (dialog/dialog {:id "demo-dialog"}
      (dialog/dialog-header {} [:h3 "Dialog Title"] [:p "Are you sure you want to continue?"])
      (dialog/dialog-body {} [:p "This action cannot be undone."])
      (dialog/dialog-footer {}
        (button/button {:variant :secondary :size :sm
                        :attrs {:onclick "document.getElementById('demo-dialog').close()"}}
          "Cancel")
        (button/button {:variant :primary :size :sm
                        :attrs {:onclick "document.getElementById('demo-dialog').close()"}}
          "Confirm")))))

(defn context-menu-demo []
  (section "Context Menu"
    [:p {:style "color: var(--fg-2); font-size: var(--font-sm);"}
     "Right-click (or long-press) the areas below to open context menus."]
    [:div {:style "display: flex; gap: 1rem; flex-wrap: wrap;"}
     (context-menu/context-menu-trigger
       {:items [{:label "Edit"   :url "#edit"   :icon :edit}
                {:label "Copy"   :url "#copy"   :icon :copy}
                {:type :separator}
                {:label "Delete" :url "#delete" :icon :trash :variant :danger}]}
       [:div {:style "padding: 2rem; border: var(--border-0); border-radius: var(--radius-md); cursor: context-menu; text-align: center; min-width: 12rem;"}
        "Right click here"])
     (context-menu/context-menu-trigger
       {:items [{:label "View profile"  :url "#profile" :icon :user}
                {:label "Send message" :url "#message" :icon :mail}
                {:label "Share"        :url "#share"   :icon :link}]}
       [:div {:style "padding: 2rem; border: var(--border-0); border-radius: var(--radius-md); cursor: context-menu; text-align: center; min-width: 12rem;"}
        "Different menu"])]))

(defn theme-toggle-demo []
  (section "Theme Toggle"
    [:div {:style "display: flex; gap: 1.5rem; align-items: center; flex-wrap: wrap;"}
     [:div {:style "display: flex; flex-direction: column; gap: 0.5rem; align-items: center;"}
      [:span {:style "font-size: var(--font-xs); color: var(--fg-2);"} "Default (md)"]
      (theme-toggle/theme-toggle {:mode "auto"})]
     [:div {:style "display: flex; flex-direction: column; gap: 0.5rem; align-items: center;"}
      [:span {:style "font-size: var(--font-xs); color: var(--fg-2);"} "Small"]
      (theme-toggle/theme-toggle {:mode "auto" :size :sm})]
     [:div {:style "display: flex; flex-direction: column; gap: 0.5rem; align-items: center;"}
      [:span {:style "font-size: var(--font-xs); color: var(--fg-2);"} "Light selected"]
      (theme-toggle/theme-toggle {:mode "light"})]
     [:div {:style "display: flex; flex-direction: column; gap: 0.5rem; align-items: center;"}
      [:span {:style "font-size: var(--font-xs); color: var(--fg-2);"} "Dark selected"]
      (theme-toggle/theme-toggle {:mode "dark"})]]))

(defn pagination-demo []
  (section "Pagination"
    (pagination/pagination {:current 3 :total 5
                            :href-fn (fn [p] (str "#page-" p))})))

(def sample-images
  [{:src "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='400' height='300'%3E%3Crect fill='%234f46e5' width='400' height='300'/%3E%3Ctext x='200' y='150' text-anchor='middle' dominant-baseline='middle' fill='white' font-size='24' font-family='sans-serif'%3EIndigo%3C/text%3E%3C/svg%3E"
    :alt "Indigo"}
   {:src "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='400' height='300'%3E%3Crect fill='%2316a34a' width='400' height='300'/%3E%3Ctext x='200' y='150' text-anchor='middle' dominant-baseline='middle' fill='white' font-size='24' font-family='sans-serif'%3EGreen%3C/text%3E%3C/svg%3E"
    :alt "Green"}
   {:src "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='400' height='300'%3E%3Crect fill='%23dc2626' width='400' height='300'/%3E%3Ctext x='200' y='150' text-anchor='middle' dominant-baseline='middle' fill='white' font-size='24' font-family='sans-serif'%3ERed%3C/text%3E%3C/svg%3E"
    :alt "Red"}
   {:src "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='400' height='300'%3E%3Crect fill='%23ea580c' width='400' height='300'/%3E%3Ctext x='200' y='150' text-anchor='middle' dominant-baseline='middle' fill='white' font-size='24' font-family='sans-serif'%3EOrange%3C/text%3E%3C/svg%3E"
    :alt "Orange"}])

(defn lightbox-demo []
  (section "Lightbox"
    [:p {:style "color: var(--fg-2); font-size: var(--font-sm);"}
     "Click a thumbnail to open the fullscreen lightbox overlay (requires JavaScript)."]
    [:div {:style "display: flex; gap: 0.75rem; flex-wrap: wrap;"}
     (for [{:keys [src alt]} sample-images]
       (lightbox/image-thumbnail {:src src :alt alt}))]
    [:p {:style "color: var(--fg-2); font-size: var(--font-sm); margin-top: 0.5rem;"}
     "Static lightbox overlay preview:"]
    [:div {:style "position: relative; height: 300px; border-radius: var(--radius-md); overflow: hidden;"}
     (lightbox/lightbox {:src (:src (first sample-images)) :alt "Indigo"})]))

(defn tag-input-demo []
  (section "Tag Input"
    [:div {:style "max-width: 480px;"}
     (tag-input/tag-input
       {:tags [{:label "Clojure" :value "clojure"}
               {:label "Babashka" :value "babashka"}]
        :input-value ""
        :open true
        :filtered-items [{:label "React" :value "react"}
                         {:label "Next.js" :value "nextjs"}
                         {:label "TypeScript" :value "typescript"}]
        :active-index 0
        :placeholder "Add frameworks..."})]))

(def sample-calendar-events
  [{:title "Team standup"     :date "2026-03-29" :time-start "09:00" :time-end "09:30" :color :accent}
   {:title "Lunch with Alex"  :date "2026-03-29" :time-start "12:00" :time-end "13:00" :color :success}
   {:title "Deploy v2.0"      :date "2026-03-29" :time-start "15:00" :color :danger}
   {:title "Design review"    :date "2026-03-30" :time-start "10:00" :color :warning}
   {:title "All-day planning" :date "2026-03-31" :color nil :done? true}
   {:title "Sprint retro"     :date "2026-04-01" :time-start "14:00" :time-end "15:00" :color :accent}
   {:title "1:1 with manager" :date "2026-04-02" :time-start "11:00" :color :success}
   {:title "Release party"    :date "2026-04-03" :time-start "17:00" :color :danger}])

(defn calendar-demo []
  (section "Calendar"
    [:h5 "Date Picker"]
    [:div {:style "display: flex; gap: 1.5rem; flex-wrap: wrap;"}
     (calendar/calendar {:year 2026 :month 3 :today-str "2026-03-29"
                          :selected-date "2026-03-29"})
     (calendar/calendar {:year 2026 :month 4 :today-str "2026-03-29"})]

    [:h5 "Event Grid"]
    (cal-events/calendar-event-grid {:year 2026 :month 3 :today-str "2026-03-29"
                                      :selected-date "2026-03-29"
                                      :events sample-calendar-events})

    [:h5 "Day Ticker"]
    (cal-events/ticker-strip {:days [{:date "2026-03-27" :day-num 27 :day-label "Fr"}
                                      {:date "2026-03-28" :day-num 28 :day-label "Sa"}
                                      {:date "2026-03-29" :day-num 29 :day-label "Su"}
                                      {:date "2026-03-30" :day-num 30 :day-label "Mo"}
                                      {:date "2026-03-31" :day-num 31 :day-label "Tu"}
                                      {:date "2026-04-01" :day-num 1  :day-label "We"}
                                      {:date "2026-04-02" :day-num 2  :day-label "Th"}
                                      {:date "2026-04-03" :day-num 3  :day-label "Fr"}]
                               :today-str "2026-03-29"
                               :selected "2026-03-29"
                               :events sample-calendar-events})

    [:h5 "Agenda List"]
    (cal-events/agenda-list {:days [{:date "2026-03-29" :label "Today"}
                                     {:date "2026-03-30" :label "Tomorrow"}
                                     {:date "2026-03-31" :label "Tue"}
                                     {:date "2026-04-01" :label "Wed"}
                                     {:date "2026-04-02" :label "Thu"}
                                     {:date "2026-04-03" :label "Fri"}]
                              :today-str "2026-03-29"
                              :events sample-calendar-events})))

;; ── Pages ───────────────────────────────────────────────────────────

(defn player-bar-demo []
  (section "Player Bar"
    (player-bar/player-bar
      {:track-name "Across The Universe"
       :subtitle "The Beatles"
       :playing true
       :progress 35
       :current-time "1:23"
       :duration "3:48"
       :shuffle false
       :repeat false
       :favorited true})
    [:div {:style "margin-top: 1rem;"}
     (player-bar/player-bar
       {:track-name "Bohemian Rhapsody"
        :subtitle "Queen"
        :playing false
        :progress 0
        :current-time "0:00"
        :duration "5:55"
        :shuffle true
        :repeat true
        :favorited false})]))

(defn components-page []
  [:div
   (page-header "Components" "All UI components at a glance.")
   (button-demo)
   (player-bar-demo)
   (alert-demo)
   (badge-demo)
   (card-demo)
   (accordion-demo)
   (table-demo)
   (dialog-demo)
   (context-menu-demo)
   (popover-demo)
   (command-demo)
   (toolbar-demo)
   (button-group-demo)
   (header-patterns-demo)
   (tabs-demo)
   (spinner-demo)
   (empty-state-demo)
   (drop-zone-demo)
   (processing-bar-demo)
   (toast-demo)
   (camera-demo)
   (grid-demo)
   (skeleton-demo)
   (progress-demo)
   (theme-toggle-demo)
   (switch-demo)
   (tooltip-demo)
   (breadcrumb-demo)
   (pagination-demo)
   (separator-demo)
   (panels-demo)
   (form-demo)
   (number-field-demo)
   (chat-demo)
   (tag-input-demo)
   (lightbox-demo)
   [:style (h/raw ".lightbox-overlay { position: absolute !important; }")]])

(def sample-files
  [{:name "Documents"          :file-type :folder      :modified "May 10, 2026"}
   {:name "Photos"              :file-type :folder      :modified "May 8, 2026"}
   {:name "project-proposal.pdf" :file-type :document   :size "2.4 MB"   :modified "May 12, 2026"}
   {:name "vacation-photo.jpg"  :file-type :image       :size "4.1 MB"   :modified "May 11, 2026"}
   {:name "presentation.pptx"  :file-type :document    :size "12.8 MB"  :modified "May 9, 2026"}
   {:name "budget-2026.xlsx"   :file-type :spreadsheet :size "156 KB"   :modified "May 7, 2026"}
   {:name "intro-video.mp4"    :file-type :video       :size "245 MB"   :modified "May 5, 2026"}
   {:name "podcast-ep12.mp3"   :file-type :audio       :size "48 MB"    :modified "May 3, 2026"}
   {:name "app.clj"            :file-type :code        :size "8.2 KB"   :modified "May 14, 2026"}
   {:name "backup.zip"         :file-type :archive     :size "1.2 GB"   :modified "Apr 28, 2026"}
   {:name "README.md"          :file-type :document    :size "4.5 KB"   :modified "May 15, 2026"}
   {:name "screenshot.png"     :file-type :image       :size "890 KB"   :modified "May 13, 2026"}])

(defn file-context-menu-items [item]
  (let [is-folder? (= (name (:file-type item)) "folder")]
    (cond-> [{:label "Open"   :icon :folder  :url "#"}]
      (not is-folder?) (conj {:label "Download" :icon :download :url "#"})
      true (conj {:label "Rename" :icon :edit :url "#"})
      true (conj {:label "Share" :icon :link :url "#"})
      true (conj {:type :separator})
      true (conj {:label "Delete" :icon :trash :variant :danger :url "#"}))))

(defn file-browser-page []
  [:div
   (page-header "File Browser" "Grid and list views for file management with type-specific icons and context menus.")

   (section "File Type Icons"
     [:p {:style "color: var(--fg-2); font-size: var(--font-sm); margin-bottom: 0.5rem;"}
      "Each file type gets a distinctive icon and color."]
     [:div {:style "display: grid; grid-template-columns: repeat(auto-fill, minmax(6rem, 1fr)); gap: var(--size-4);"}
      (for [ft [:folder :image :video :audio :document :spreadsheet :code :archive :file]]
        [:div {:style "display: flex; flex-direction: column; align-items: center; gap: var(--size-2); padding: var(--size-3); border-radius: var(--radius-md); border: var(--border-0);"}
         (fb/file-type-icon {:file-type ft})
         [:span {:style "font-size: var(--font-xs); color: var(--fg-2);"} (name ft)]])])

   (section "Grid View"
     [:div {:class "fb-toolbar"}
      [:div {:style "font-weight: 500;"} "My Files"]
      (fb/file-view-toggle {:view :grid})]
     [:div {:class "fb-grid"}
      (for [item sample-files]
        (fb/file-item-grid {:item item
                            :context-menu-items (file-context-menu-items item)}))])

   (section "List View"
     [:div {:class "fb-toolbar"}
      [:div {:style "font-weight: 500;"} "My Files"]
      (fb/file-view-toggle {:view :list})]
     (fb/file-table {:items sample-files
                     :sort-key :name
                     :sort-dir :asc
                     :context-menu-items-fn file-context-menu-items}))

   (section "Selected Items"
     [:p {:style "color: var(--fg-2); font-size: var(--font-sm);"}
      "Items can show a selected state."]
     [:div {:class "fb-grid"}
      (for [[i item] (map-indexed vector (take 4 sample-files))]
        (fb/file-item-grid {:item item
                            :selected (#{0 2} i)}))])

   (section "Custom Columns"
     [:p {:style "color: var(--fg-2); font-size: var(--font-sm); margin-bottom: 0.5rem;"}
      "The file table supports custom column definitions. Mix built-in helpers with your own columns."]
     (fb/file-table {:items sample-files
                     :columns [(fb/col-name {:label "File"})
                               (fb/col-size)
                               {:key   :owner
                                :label "Owner"
                                :width "120px"
                                :render (fn [item]
                                          (if (= (name (:file-type item)) "folder")
                                            "Team"
                                            "Alice"))}
                               {:key   :status
                                :label "Status"
                                :width "100px"
                                :render (fn [_item] "Synced")}]
                     :sort-key :name
                     :sort-dir :asc}))

   (section "Drop Zone"
     [:p {:style "color: var(--fg-2); font-size: var(--font-sm); margin-bottom: 0.5rem;"}
      "Drag & drop file upload area. Click to browse or drag files onto the zone."]
     (drop-zone/drop-zone {:accept "image/*,.pdf,.doc,.docx"
                           :multiple true
                           :title "Drop files here or click to browse"
                           :hint "Images and documents up to 10 MB"})
     (fp/file-progress-list {}
       (fp/file-progress-item {:name "project-proposal.pdf"
                               :size "2.4 MB"
                               :icon (fb/file-type-icon {:file-type :document :size :sm})
                               :progress 100
                               :status :complete
                               :on-remove identity})
       (fp/file-progress-item {:name "vacation-photo.jpg"
                               :size "4.1 MB"
                               :icon (fb/file-type-icon {:file-type :image :size :sm})
                               :progress 67
                               :status :uploading
                               :on-remove identity})
       (fp/file-progress-item {:name "backup.zip"
                               :size "1.2 GB"
                               :icon (fb/file-type-icon {:file-type :archive :size :sm})
                               :progress 23
                               :status :error
                               :on-remove identity})))

   (section "Drop Zone — Disabled"
     (drop-zone/drop-zone {:disabled true
                           :title "Uploads disabled"
                           :hint "You don't have permission to upload"}))

   (section "Full-Page Drop Zone Overlay"
     [:p {:style "color: var(--fg-2); font-size: var(--font-sm); margin-bottom: 0.5rem;"}
      "In interactive targets (Replicant, Squint), dragging files anywhere on the page shows this overlay. Below is a static preview."]
     [:div {:style "position: relative; height: 300px; border: var(--border-0); border-radius: var(--radius-md); overflow: hidden;"}
      (drop-zone/drop-zone-overlay {})])
   [:style ".drop-zone-overlay { position: absolute !important; }"]])

(def icon-categories
  [["Navigation"
    [:home :menu :x
     :chevron-down :chevron-up :chevron-left :chevron-right
     :arrow-down :arrow-up :arrow-left :arrow-right
     :external-link]]
   ["Actions"
    [:search :plus :minus :check :edit :trash
     :download :upload :copy :filter :link :refresh]]
   ["Objects"
    [:file :folder :image :mail :bell :calendar :clock
     :bookmark :star :heart :inbox :layers :package]]
   ["UI & System"
    [:settings :user :users :log-out :log-in :eye :eye-off
     :lock :grid :list :layout-dashboard :monitor :moon :sun]]
   ["Status"
    [:alert-triangle :alert-circle :info :circle-check :circle-x]]
   ["Media"
    [:play :pause :skip-back :skip-forward :shuffle :repeat :volume-2 :music]]
   ["Dev & Technical"
    [:code :terminal :database :globe :shield :zap :book-open :map-pin]]])

(defn calendar-page []
  [:div
   (page-header "Calendar" "Date picker, event grid, ticker strip, and agenda list.")
   (into [:div {:class "md-docs"}]
         (markdown/markdown->hiccup (slurp "src/ui/calendar.md")))
   (calendar-demo)])

(defn icons-page []
  [:div
   (page-header "Icons" (str (count icon/icon-names) " icons based on Lucide. All render as inline SVG with stroke=\"currentColor\"."))
   ;; Sizes
   (section "Sizes"
     [:div {:style "display: flex; gap: 1.5rem; align-items: end;"}
      (for [[s label] [[:sm "sm"] [:md "md (default)"] [:lg "lg"] [:xl "xl"]]]
        [:div {:style "display: flex; flex-direction: column; align-items: center; gap: var(--size-2);"}
         (icon/icon {:icon-name :star :size s})
         [:span {:style "font-size: var(--font-xs); color: var(--fg-2);"} label]])])
   ;; Filled variants
   (section "Filled Variants"
     [:p {:style "color: var(--fg-1); margin-bottom: var(--size-4); font-size: var(--font-sm);"}
      "Media icons support a " [:code ":filled true"] " prop for solid rendering."]
     [:div {:style "display: grid; grid-template-columns: repeat(auto-fill, minmax(8rem, 1fr)); gap: var(--size-4);"}
      (for [n [:play :pause :skip-back :skip-forward :repeat :volume-2]]
        [:div {:style "display: flex; flex-direction: column; align-items: center; gap: var(--size-3); padding: var(--size-3); border-radius: var(--radius-md); border: var(--border-0);"}
         [:div {:style "display: flex; gap: var(--size-4); align-items: center;"}
          (icon/icon {:icon-name n})
          (icon/icon {:icon-name n :filled true})]
         [:span {:style "font-size: var(--font-xs); color: var(--fg-2); text-align: center;"} (name n)]])])
   ;; Categories
   (for [[cat-name icons] icon-categories]
     (section cat-name
       [:div {:style "display: grid; grid-template-columns: repeat(auto-fill, minmax(5rem, 1fr)); gap: var(--size-4);"}
        (for [n icons]
          [:div {:style "display: flex; flex-direction: column; align-items: center; gap: var(--size-2); padding: var(--size-3); border-radius: var(--radius-md); border: var(--border-0);"}
           (icon/icon {:icon-name n})
           [:span {:style "font-size: var(--font-xs); color: var(--fg-2); text-align: center; word-break: break-all;"} (name n)]])]))])

(defn sidebar-page []
  [:div
   (page-header "Sidebar" "A composable sidebar with brand, search, grouped navigation, collapsible sections, and user footer.")
   (section "Example"
     (sidebar/sidebar-layout {:attrs {:style "border: var(--border-0); border-radius: var(--radius-lg); overflow: hidden; height: 500px;"}}
       (sidebar/sidebar {:attrs {:style "height: 100%; position: static;"}}
         (sidebar/sidebar-header {}
           (sidebar/sidebar-brand {:title "Acme Inc." :subtitle "Enterprise" :icon "A"})
           (sidebar/sidebar-search {:placeholder "Search..."}))
         (sidebar/sidebar-content {}
           (sidebar/sidebar-group {:label "Getting Started"}
             (sidebar/sidebar-menu {}
               (sidebar/sidebar-menu-item {:href "#" :icon-name :download} "Installation")
               (sidebar/sidebar-menu-item {:href "#" :icon-name :folder :active true} "Project Structure")))
           (sidebar/sidebar-group {:label "Building"}
             (sidebar/sidebar-menu {}
               (sidebar/sidebar-menu-item {:href "#" :icon-name :globe} "Routing")
               (sidebar/sidebar-menu-item {:href "#" :icon-name :database :badge "New"} "Data Fetching")
               (sidebar/sidebar-menu-item {:href "#" :icon-name :layers} "Rendering")
               (sidebar/sidebar-menu-item {:href "#" :icon-name :zap} "Caching")
               (sidebar/sidebar-menu-item {:href "#" :icon-name :eye} "Styling")))
           (sidebar/sidebar-group {:label "API Reference"}
             (sidebar/sidebar-collapsible {:title "Components" :open true}
               (sidebar/sidebar-menu {}
                 (sidebar/sidebar-menu-item {:href "#"} "Button")
                 (sidebar/sidebar-menu-item {:href "#"} "Card")
                 (sidebar/sidebar-menu-item {:href "#"} "Dialog")))
             (sidebar/sidebar-collapsible {:title "Functions"}
               (sidebar/sidebar-menu {}
                 (sidebar/sidebar-menu-item {:href "#"} "fetch")
                 (sidebar/sidebar-menu-item {:href "#"} "redirect")))))
         (sidebar/sidebar-footer {}
           (sidebar/sidebar-user {:user-name "Alice Johnson" :email "alice@example.com"})))
       (sidebar/sidebar-overlay {})
       (sidebar/sidebar-layout-main {}
         [:div {:style "padding: 2rem;"}
          [:div {:style "display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1rem;"}
           (sidebar/sidebar-mobile-toggle {})
           [:h3 {:style "margin: 0; color: var(--fg-0);"} "Dashboard"]]
          [:div {:style "display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem;"}
           [:div {:style "aspect-ratio: 16/9; background: var(--bg-1); border-radius: var(--radius-lg); border: var(--border-0);"}]
           [:div {:style "aspect-ratio: 16/9; background: var(--bg-1); border-radius: var(--radius-lg); border: var(--border-0);"}]
           [:div {:style "aspect-ratio: 16/9; background: var(--bg-1); border-radius: var(--radius-lg); border: var(--border-0);"}]]
          [:div {:style "margin-top: 1rem; min-height: 120px; background: var(--bg-1); border-radius: var(--radius-lg); border: var(--border-0);"}]])))
   (section "Floating Sidebar (iOS)"
     [:p {:style "color: var(--fg-2); font-size: var(--font-sm); margin: 0;"}
      "The " [:code "sidebar-layout--floating"] " modifier forces the off-canvas drawer at any size, "
      "positioned relative to its container. Tap the hamburger to open, tap the dimmed backdrop to close."]
     [:div {:style "display: flex; justify-content: center; padding: var(--size-4) 0;"}
      ;; iPhone-sized frame (375 × 812)
      [:div {:style (str "width: 375px; height: 812px; max-width: 100%; flex-shrink: 0; "
                         "border: 10px solid var(--fg-0); border-radius: 2.75rem; overflow: hidden; "
                         "background: var(--bg-0); box-shadow: var(--shadow-3);")}
       (sidebar/sidebar-layout {:class "sidebar-layout--floating" :attrs {:style "height: 100%;"}}
         (sidebar/sidebar {:attrs {:style "width: 17rem;"}}
           (sidebar/sidebar-header {}
             (sidebar/sidebar-brand {:title "Pocket" :subtitle "Personal" :icon "P"}))
           (sidebar/sidebar-content {}
             (sidebar/sidebar-group {:label "Library"}
               (sidebar/sidebar-menu {}
                 (sidebar/sidebar-menu-item {:href "#" :icon-name :home :active true} "Home")
                 (sidebar/sidebar-menu-item {:href "#" :icon-name :search} "Search")
                 (sidebar/sidebar-menu-item {:href "#" :icon-name :star :badge "12"} "Favorites")
                 (sidebar/sidebar-menu-item {:href "#" :icon-name :clock} "Recent")))
             (sidebar/sidebar-group {:label "Account"}
               (sidebar/sidebar-menu {}
                 (sidebar/sidebar-menu-item {:href "#" :icon-name :bell} "Notifications")
                 (sidebar/sidebar-menu-item {:href "#" :icon-name :settings} "Settings"))))
           (sidebar/sidebar-footer {}
             (sidebar/sidebar-user {:user-name "Jamie Rivera" :email "jamie@pocket.app"})))
         (sidebar/sidebar-overlay {})
         (sidebar/sidebar-layout-main {}
           [:div {:style "display: flex; flex-direction: column; height: 100%;"}
            ;; Top bar
            [:div {:style (str "display: flex; align-items: center; gap: var(--size-3); "
                               "padding: var(--size-3) var(--size-4); border-bottom: var(--border-0);")}
             (sidebar/sidebar-mobile-toggle {})
             [:h3 {:style "margin: 0; color: var(--fg-0); font-size: var(--font-md);"} "Home"]]
            ;; Content
            [:div {:style "flex: 1; overflow-y: auto; padding: var(--size-4); display: flex; flex-direction: column; gap: var(--size-3);"}
             [:div {:style "height: 140px; background: var(--bg-1); border-radius: var(--radius-lg); border: var(--border-0);"}]
             [:div {:style "display: grid; grid-template-columns: 1fr 1fr; gap: var(--size-3);"}
              [:div {:style "aspect-ratio: 1; background: var(--bg-1); border-radius: var(--radius-lg); border: var(--border-0);"}]
              [:div {:style "aspect-ratio: 1; background: var(--bg-1); border-radius: var(--radius-lg); border: var(--border-0);"}]]
             [:div {:style "height: 80px; background: var(--bg-1); border-radius: var(--radius-lg); border: var(--border-0);"}]
             [:div {:style "height: 80px; background: var(--bg-1); border-radius: var(--radius-lg); border: var(--border-0);"}]]]))]])])

;; ── Navigation Data ─────────────────────────────────────────────────

(def component-nav
  [{:title "General"
    :items [{:label "Button" :anchor "button"}
            {:label "Badge" :anchor "badge"}
            {:label "Card" :anchor "card"}
            {:label "Player Bar" :anchor "player-bar"}]}
   {:title "Forms"
    :items [{:label "Form" :anchor "form"}
            {:label "Chat" :anchor "chat"}
            {:label "Tag Input" :anchor "tag-input"}
            {:label "Switch" :anchor "switch"}
            {:label "Theme Toggle" :anchor "theme-toggle"}]}
   {:title "Data Display"
    :items [{:label "Table" :anchor "table"}
            {:label "Accordion" :anchor "accordion"}
            {:label "Progress" :anchor "progress"}]}
   {:title "Feedback"
    :items [{:label "Alert" :anchor "alert"}
            {:label "Dialog" :anchor "dialog"}
            {:label "Context Menu" :anchor "context-menu"}
            {:label "Command" :anchor "command"}
            {:label "Spinner" :anchor "spinner"}
            {:label "Empty State" :anchor "empty-state"}
            {:label "Drop Zone" :anchor "drop-zone"}
            {:label "Processing Bar" :anchor "processing-bar"}
            {:label "Toast" :anchor "toast"}
            {:label "Camera" :anchor "camera"}
            {:label "Grid" :anchor "grid"}
            {:label "Skeleton" :anchor "skeleton"}
            {:label "Tooltip" :anchor "tooltip"}]}
   {:title "Layout"
    :items [{:label "Separator" :anchor "separator"}
            {:label "Toolbar" :anchor "toolbar"}
            {:label "Button Group" :anchor "button-group"}
            {:label "Header Patterns" :anchor "header-patterns"}]}
   {:title "Overlay"
    :items [{:label "Lightbox" :anchor "lightbox"}]}
   {:title "Navigation"
    :items [{:label "Breadcrumb" :anchor "breadcrumb"}
            {:label "Pagination" :anchor "pagination"}
            {:label "Tabs" :anchor "tabs"}]}])

(def nav-items
  [{:id :components :label "Components"  :icon-name :package    :href "/"}
   {:id :calendar   :label "Calendar"    :icon-name :calendar   :href "/calendar"}
   {:id :icons      :label "Icons"       :icon-name :image      :href "/icons"}
   {:id :sidebar    :label "Sidebar"     :icon-name :layout-dashboard :href "/sidebar"}
   {:id :files      :label "File Browser" :icon-name :folder           :href "/files"}])

(defn resolve-page [uri]
  (case uri
    "/"          :components
    "/calendar"  :calendar
    "/icons"     :icons
    "/sidebar"   :sidebar
    "/files"     :files
    nil))

;; ── App Shell ───────────────────────────────────────────────────────

(defn req-hostname
  "Extract the hostname from a request Host header, dropping any :port.
   Falls back to localhost. Keeps the current host so target-switcher
   links work over LAN/Tailscale, not just on localhost."
  [host]
  (if (and host (re-find #":\d+$" host))
    (str/replace host #":\d+$" "")
    (or host "localhost")))

(defn make-targets
  "Target-switcher links. Accessed via an explicit :port (dev servers) →
   sibling ports on the same host. Accessed portless (production behind
   a reverse proxy) → the SPA builds served under /replicant/ and
   /squint/."
  [own-port host]
  (if (and host (not (re-find #":\d+$" host)))
    [{:label "Hiccup"    :href "/" :active true}
     {:label "Replicant" :href "/replicant/"}
     {:label "Squint"    :href "/squint/"}]
    (let [base (- own-port 3)
          hostname (req-hostname host)]
      [{:label "Hiccup"    :href (str "//" hostname ":" (+ base 3)) :active true}
       {:label "Replicant" :href (str "//" hostname ":" (+ base 1))}
       {:label "Squint"    :href (str "//" hostname ":" (+ base 2))}])))

(defn app-sidebar [active-page own-port host]
  (sidebar/sidebar {}
    (sidebar/sidebar-header {}
      (sidebar/sidebar-brand {:title "Clojure UI Framework" :subtitle "Hiccup" :icon "U"}))
    (sidebar/sidebar-content {}
      (sidebar/sidebar-group {:label "Pages"}
        (apply sidebar/sidebar-menu {}
          (for [{:keys [id label icon-name href]} nav-items]
            (sidebar/sidebar-menu-item
              {:href href :icon-name icon-name :active (= id active-page)}
              label))))
      (sidebar/sidebar-separator)
      (sidebar/sidebar-group {:label "Components"}
        (for [{:keys [title items]} component-nav]
          (sidebar/sidebar-collapsible {:title title :open true}
            (apply sidebar/sidebar-menu {}
              (for [{:keys [label anchor]} items]
                (sidebar/sidebar-menu-item {:href (str "/#" anchor)} label))))))
      (sidebar/sidebar-separator)
      (sidebar/sidebar-group {:label "Targets"}
        (apply sidebar/sidebar-menu {}
          (for [{:keys [label href active]} (make-targets own-port host)]
            (sidebar/sidebar-menu-item
              {:href href
               :icon-name :monitor
               :active active}
              label))))
      (sidebar/sidebar-separator)
      (sidebar/sidebar-group {:label "Theme"}
        [:div {:style "padding: 0.5rem 0.75rem;"}
         (theme-toggle/theme-toggle {:mode "auto"
                                     :attrs {:id "sidebar-theme-toggle"}})]))
    (sidebar/sidebar-footer {}
      (sidebar/sidebar-user {:user-name "Dev Mode" :email (str "hiccup · port " own-port) :avatar "bb"}))))

(def live-reload-script
  "/* Live reload: poll /dev/changes, reload on version bump */
  (function() {
    var lastV = null;
    setInterval(function() {
      fetch('/dev/changes').then(function(r) { return r.text(); }).then(function(v) {
        if (lastV !== null && v !== lastV) location.reload();
        lastV = v;
      }).catch(function() {});
    }, 500);
  })();")

(defn render-page [uri port live-reload? host]
  (let [params     (parse-query-params uri)
        theme      (get params "theme")
        path       (first (str/split uri #"\?" 2))
        active-page (resolve-page path)]
    (str
      "<!DOCTYPE html>\n"
      (h/html
        [:html (when (#{"dark" "light"} theme) {:data-theme theme})
         [:head
          [:meta {:charset "utf-8"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
          [:link {:rel "stylesheet" :href "/theme.css"}]
          [:style (h/raw "html, body { margin: 0; padding: 0; }")]
          [:script (h/raw theme-persistence-script)]
          (when live-reload? [:script (h/raw live-reload-script)])]
         [:body
          [:script {:src "/ui-runtime.js"}]
          [:script (h/raw theme-toggle-script)]
          (when live-reload? [:script {:src "/theme-adapter.js" :defer true}])
          (when live-reload? [:script {:src "/css-live-reload.js" :defer true}])
          (sidebar/sidebar-layout {}
            (app-sidebar active-page port host)
            (sidebar/sidebar-overlay {})
            (sidebar/sidebar-layout-main {}
              [:div {:style "--body-padding-inline: 2rem; padding: 2rem; max-width: 960px;"}
               [:div {:style "display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1rem;"}
                (sidebar/sidebar-mobile-toggle {})
                [:a.font-mono.text-xs.text-faint
                 {:href (str "https://github.com/floscr/clj-ui-framework/commit/" deployed-sha)
                  :target "_blank" :rel "noopener"
                  :title "Deployed commit"
                  :style "margin-left: auto; text-decoration: none;"}
                 deployed-sha]]
               (case active-page
                 :components (components-page)
                 :calendar   (calendar-page)
                 :icons      (icons-page)
                 :sidebar    (sidebar-page)
                 :files      (file-browser-page)
                 [:div (page-header "Not Found" "This page doesn't exist.")])]))]]))))

;; ── Live Reload ─────────────────────────────────────────────────────

(defonce !version (atom 0))
(defonce !last-mtimes (atom {}))

(def watch-dirs ["src" "dev/hiccup/src"])
(def watch-exts #{".clj" ".cljc" ".css" ".edn"})

(defn source-mtimes
  "Collect last-modified timestamps for all source files."
  []
  (into {}
    (for [dir  watch-dirs
          :let [root (io/file dir)]
          :when (.isDirectory root)
          f    (file-seq root)
          :when (and (.isFile f)
                     (some #(str/ends-with? (.getName f) %) watch-exts))]
      [(.getPath f) (.lastModified f)])))

(defn reload-namespaces!
  "Reload dev.hiccup and all transitive deps (all ui.* namespaces)."
  []
  (try
    (require 'dev.hiccup :reload-all)
    (catch Exception e
      (println "⚠ Reload error:" (.getMessage e)))))

(defn start-watcher!
  "Start a background thread that polls source files for changes."
  []
  (reset! !last-mtimes (source-mtimes))
  (future
    (loop []
      (Thread/sleep 500)
      (try
        (let [current (source-mtimes)]
          (when (not= current @!last-mtimes)
            (let [changed (into []
                            (filter #(not= (get current %) (get @!last-mtimes %)))
                            (keys current))
                  new-files (into []
                              (filter #(not (contains? @!last-mtimes %)))
                              (keys current))]
              (reset! !last-mtimes current)
              (println (str "♻ Reloading (" (count (concat changed new-files)) " file(s) changed)"))
              (reload-namespaces!)
              (swap! !version inc))))
        (catch Exception e
          (println "⚠ Watcher error:" (.getMessage e))))
      (recur))))

;; ── Server ──────────────────────────────────────────────────────────

(defonce !port (atom 3003))
(defonce !live-reload (atom true))

(def ^:private content-types
  {"html" "text/html; charset=utf-8"
   "css" "text/css"
   "js" "application/javascript"
   "mjs" "application/javascript"
   "json" "application/json"
   "map" "application/json"
   "svg" "image/svg+xml"
   "png" "image/png"
   "md" "text/markdown"
   "woff2" "font/woff2"})

(defn- serve-file [^java.io.File f]
  (let [ext (last (str/split (.getName f) #"\."))]
    {:status 200
     :headers {"Content-Type" (get content-types ext "application/octet-stream")}
     :body f}))

(defn- spa-response
  "Serve a static SPA build mounted at /<prefix>/. `rel` is the path
   under the mount; searched across `roots` in order (later roots let
   generated assets like theme.css come from the dev public dir)."
  [roots rel]
  (let [rel (if (str/blank? rel) "index.html" rel)]
    (when-not (str/includes? rel "..")
      (some (fn [root]
              (let [f (io/file root rel)]
                (when (.isFile f) (serve-file f))))
            roots))))

(def ^:private spa-mounts
  {"replicant" ["dev/replicant/prod" "dev/replicant/public"]
   "squint" ["dev/squint/dist" "dev/squint/public"]})

(defn handler [{:keys [uri headers]}]
  (let [port @!port
        host (get headers "host")
        path (first (str/split uri #"\?" 2))
        spa (when-let [[_ mount rel] (re-matches #"/(replicant|squint)(?:/(.*))?" path)]
              (if (nil? rel)
                ;; no trailing slash — relative asset URLs need one
                {:status 301 :headers {"Location" (str path "/")}}
                (spa-response (spa-mounts mount) rel)))]
    (cond
      (= path "/dev/changes")
      {:status 200
       :headers {"Content-Type" "text/plain"
                 "Cache-Control" "no-cache"}
       :body (str @!version)}

      (= path "/theme.css")
      {:status 200
       :headers {"Content-Type" "text/css"}
       :body (slurp "dist/theme.css")}

      (= path "/ui-runtime.js")
      {:status 200
       :headers {"Content-Type" "application/javascript"}
       :body (slurp "dist/ui-runtime.js")}

      (= path "/theme-adapter.js")
      {:status 200
       :headers {"Content-Type" "application/javascript"}
       :body (slurp "dev/theme-adapter.js")}

      (= path "/css-live-reload.js")
      {:status 200
       :headers {"Content-Type" "application/javascript"}
       :body (slurp "dev/css-live-reload.js")}

      spa spa

      (resolve-page path)
      {:status 200
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :body (render-page uri port @!live-reload host)}

      :else
      {:status 404
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :body (render-page uri port @!live-reload host)})))

(defn start! [{:keys [port live-reload?] :or {port 3003 live-reload? true}}]
  (reset! !port port)
  (reset! !live-reload live-reload?)
  (when live-reload? (start-watcher!))
  (println (str "Hiccup server running at http://localhost:" port
                (if live-reload? " (live reload enabled)" " (production)")))
  (http/run-server #'handler {:port port}))
