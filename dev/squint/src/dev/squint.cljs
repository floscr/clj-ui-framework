(ns dev.squint
  (:require [clojure.string :as str]
            ["eucalypt" :as eu]
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
                               toast-demo camera-demo
                               skeleton-demo progress-demo switch-demo tooltip-demo
                               breadcrumb-demo separator-demo form-demo
                               popover-demo command-demo toolbar-demo tabs-demo]]))

;; ── State ───────────────────────────────────────────────────────────

(def !page (atom "components"))
(def !theme-mode (atom "auto"))

;; ── Helpers ─────────────────────────────────────────────────────────

(defn set-theme! [mode]
  (reset! !theme-mode mode)
  (.set js/window.__uiTheme mode)
  (render!))

(defn toggle-sidebar! [_e]
  (when-let [layout (.querySelector js/document ".sidebar-layout")]
    (.toggleAttribute layout "data-sidebar-open")))

(defn close-sidebar! [_e]
  (when-let [layout (.querySelector js/document ".sidebar-layout")]
    (.removeAttribute layout "data-sidebar-open")))

(defn toggle-floating-sidebar! [_e]
  (when-let [layout (.querySelector js/document ".sidebar-layout--floating")]
    (.toggleAttribute layout "data-sidebar-open")))

(defn close-floating-sidebar! [_e]
  (when-let [layout (.querySelector js/document ".sidebar-layout--floating")]
    (.removeAttribute layout "data-sidebar-open")))

;; ── Component Demos ─────────────────────────────────────────────────

(defn dialog-demo []
  (section "Dialog"
    [:p {:style {"color" "var(--fg-2)" "font-size" "var(--font-sm)"}} "Click button to open dialog."]
    (button/button {:variant "primary"
                    :on-click (fn [_]
                                (when-let [el (js/document.getElementById "demo-dialog")]
                                  (.showModal el)))}
      "Open dialog")
    (dialog/dialog {:id "demo-dialog"}
      (dialog/dialog-header {} [:h3 "Dialog Title"] [:p "Are you sure you want to continue?"])
      (dialog/dialog-body {} [:p "This action cannot be undone."])
      (dialog/dialog-footer {}
        (button/button {:variant "secondary" :size "sm"
                        :on-click (fn [_] (.close (js/document.getElementById "demo-dialog")))}
          "Cancel")
        (button/button {:variant "primary" :size "sm"
                        :on-click (fn [_] (.close (js/document.getElementById "demo-dialog")))}
          "Confirm")))))

(def !ctx-log (atom []))

(defn context-menu-demo []
  (let [log @!ctx-log]
    (section "Context Menu"
      [:p {:style {"color" "var(--fg-2)" "font-size" "var(--font-sm)"}}
       "Right-click (or long-press) the areas below to open context menus."]
      [:div {:style {"display" "flex" "gap" "1rem" "flex-wrap" "wrap"}}
       (context-menu/context-menu-trigger
         {:items [{:label "Edit"   :url "#edit"   :icon "edit"}
                  {:label "Copy"   :url "#copy"   :icon "copy"}
                  {:type "separator"}
                  {:label "Delete" :url "#delete" :icon "trash" :variant "danger"}]}
         [:div {:style {"padding" "2rem" "border" "var(--border-0)" "border-radius" "var(--radius-md)"
                        "cursor" "context-menu" "text-align" "center" "min-width" "12rem"}}
          "Link actions"])
       (context-menu/context-menu-trigger
         {:items [{:label "View profile"  :url "#profile" :icon "user"}
                  {:label "Send message" :url "#message" :icon "mail"}
                  {:label "Share"        :url "#share"   :icon "link"}]}
         [:div {:style {"padding" "2rem" "border" "var(--border-0)" "border-radius" "var(--radius-md)"
                        "cursor" "context-menu" "text-align" "center" "min-width" "12rem"}}
          "Different menu"])
       (context-menu/context-menu-trigger
         {:items [{:label "Bookmark"  :icon "bookmark"
                   :on-click (fn [] (swap! !ctx-log conj "Bookmarked!") (render!))}
                  {:label "Star"      :icon "star"
                   :on-click (fn [] (swap! !ctx-log conj "Starred!") (render!))}
                  {:label "Download" :icon "download"
                   :on-click (fn [] (swap! !ctx-log conj "Downloading…") (render!))}
                  {:type "separator"}
                  {:label "Report"   :icon "alert-triangle" :variant "danger"
                   :on-click (fn [] (swap! !ctx-log conj "Reported.") (render!))}]}
         [:div {:style {"padding" "2rem" "border" "var(--border-0)" "border-radius" "var(--radius-md)"
                        "cursor" "context-menu" "text-align" "center" "min-width" "12rem"
                        "border-style" "dashed"}}
          "Callbacks (no navigation)"])]
      (when (seq log)
        [:div {:style {"margin-top" "0.75rem" "display" "flex" "flex-direction" "column" "gap" "0.25rem"}}
         [:div {:style {"display" "flex" "justify-content" "space-between" "align-items" "center"}}
          [:span {:style {"font-size" "var(--font-xs)" "color" "var(--fg-2)"}} "Event log:"]
          (button/button {:variant "ghost" :size "sm"
                          :on-click (fn [_] (reset! !ctx-log []) (render!))}
            "Clear")]
         (into [:div {:style {"display" "flex" "gap" "0.5rem" "flex-wrap" "wrap"}}]
               (map (fn [msg] (badge/badge {:variant "secondary"} msg)) log))]))))

(defn theme-toggle-demo []
  (section "Theme Toggle"
    [:div {:style {"display" "flex" "gap" "1.5rem" "align-items" "center" "flex-wrap" "wrap"}}
     [:div {:style {"display" "flex" "flex-direction" "column" "gap" "0.5rem" "align-items" "center"}}
      [:span {:style {"font-size" "var(--font-xs)" "color" "var(--fg-2)"}} "Default (md)"]
      (theme-toggle/theme-toggle {:mode @!theme-mode
                                  :on-change (fn [mode] (set-theme! mode))})]
     [:div {:style {"display" "flex" "flex-direction" "column" "gap" "0.5rem" "align-items" "center"}}
      [:span {:style {"font-size" "var(--font-xs)" "color" "var(--fg-2)"}} "Small"]
      (theme-toggle/theme-toggle {:mode @!theme-mode :size "sm"
                                  :on-change (fn [mode] (set-theme! mode))})]
     [:div {:style {"display" "flex" "flex-direction" "column" "gap" "0.5rem" "align-items" "center"}}
      [:span {:style {"font-size" "var(--font-xs)" "color" "var(--fg-2)"}} "Light selected"]
      (theme-toggle/theme-toggle {:mode "light"})]
     [:div {:style {"display" "flex" "flex-direction" "column" "gap" "0.5rem" "align-items" "center"}}
      [:span {:style {"font-size" "var(--font-xs)" "color" "var(--fg-2)"}} "Dark selected"]
      (theme-toggle/theme-toggle {:mode "dark"})]]))

(defn pagination-demo []
  (section "Pagination"
    (pagination/pagination {:current 3 :total 5
                            :on-click (fn [p] (js/console.log (str "Page: " p)))})))

;; ── Tag Input State ─────────────────────────────────────────────────

(def all-tags
  [{:label "React" :value "react"}
   {:label "Next.js" :value "nextjs"}
   {:label "TypeScript" :value "typescript"}
   {:label "Clojure" :value "clojure"}
   {:label "ClojureScript" :value "clojurescript"}
   {:label "Squint" :value "squint"}
   {:label "Babashka" :value "babashka"}
   {:label "Replicant" :value "replicant"}])

(def !tag-state (atom {:tags [] :input-value "" :open false :active-index 0}))

(defn tag-input-demo []
  (let [{:keys [tags input-value open active-index]} @!tag-state
        filtered (vec (tag-input/filter-tags all-tags tags input-value))]
    (section "Tag Input"
      [:div {:style {"max-width" "480px"}}
       (tag-input/tag-input
         {:tags tags
          :input-value input-value
          :open open
          :filtered-items filtered
          :active-index active-index
          :placeholder "Add frameworks..."
          :on-input (fn [v]
                      (swap! !tag-state assoc :input-value v :open true :active-index 0)
                      (render!))
          :on-focus (fn [_]
                      (swap! !tag-state assoc :open true)
                      (render!))
          :on-blur (fn [_]
                     (js/setTimeout (fn []
                                      (swap! !tag-state assoc :open false)
                                      (render!)) 150))
          :on-select (fn [item]
                       (swap! !tag-state (fn [s]
                                           (-> s
                                               (update :tags conj item)
                                               (assoc :input-value "" :active-index 0))))
                       (render!))
          :on-remove (fn [tag]
                       (swap! !tag-state update :tags
                              (fn [ts] (vec (remove #(= (:label %) (:label tag)) ts))))
                       (render!))
          :on-backspace (fn []
                          (swap! !tag-state update :tags
                                 (fn [ts] (if (seq ts) (vec (butlast ts)) ts)))
                          (render!))
          :on-clear (fn []
                      (if (seq input-value)
                        (swap! !tag-state assoc :input-value "" :active-index 0)
                        (swap! !tag-state assoc :tags [] :active-index 0))
                      (render!))
          :on-key-down (fn [key]
                         (let [{:keys [active-index]} @!tag-state
                               filtered (vec (tag-input/filter-tags all-tags (:tags @!tag-state) (:input-value @!tag-state)))]
                           (cond
                             (= key "ArrowDown")
                             (swap! !tag-state update :active-index
                                    (fn [i] (min (dec (count filtered)) (inc i))))

                             (= key "ArrowUp")
                             (swap! !tag-state update :active-index
                                    (fn [i] (max 0 (dec i))))

                             (= key "Enter")
                             (when-let [item (get filtered active-index)]
                               (swap! !tag-state (fn [s]
                                                   (-> s
                                                       (update :tags conj item)
                                                       (assoc :input-value "" :active-index 0))))))
                           (render!)))})]
      [:p {:style {"color" "var(--fg-2)" "font-size" "var(--font-sm)" "margin-top" "0.5rem"}}
       (str "Selected: " (str/join ", " (map :label tags)))])))

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
       :favorited true
       :on-play-pause (fn [_] (js/console.log "play/pause"))
       :on-next (fn [_] (js/console.log "next"))
       :on-previous (fn [_] (js/console.log "previous"))})
    [:div {:style {"margin-top" "1rem"}}
     (player-bar/player-bar
       {:track-name "Bohemian Rhapsody"
        :subtitle "Queen"
        :playing false
        :progress 0
        :current-time "0:00"
        :duration "5:55"
        :shuffle true
        :repeat true
        :favorited false
        :on-play-pause (fn [_] (js/console.log "play/pause"))})]))

(def !cal-state (atom {:year 2026 :month 3 :selected-date nil}))

(def !lightbox-state (atom {:src nil}))

(def sample-calendar-events
  [{:title "Team standup"     :date "2026-03-29" :time-start "09:00" :time-end "09:30" :color "accent"}
   {:title "Lunch with Alex"  :date "2026-03-29" :time-start "12:00" :time-end "13:00" :color "success"}
   {:title "Deploy v2.0"      :date "2026-03-29" :time-start "15:00" :color "danger"}
   {:title "Design review"    :date "2026-03-30" :time-start "10:00" :color "warning"}
   {:title "All-day planning" :date "2026-03-31" :color nil :done? true}
   {:title "Sprint retro"     :date "2026-04-01" :time-start "14:00" :time-end "15:00" :color "accent"}
   {:title "1:1 with manager" :date "2026-04-02" :time-start "11:00" :color "success"}
   {:title "Release party"    :date "2026-04-03" :time-start "17:00" :color "danger"}])

(defn calendar-demo []
  (let [{:keys [year month selected-date]} @!cal-state
        today-str "2026-03-29"]
    (section "Calendar"
      [:h5 "Date Picker (interactive)"]
      [:div {:style {"display" "flex" "gap" "1.5rem" "flex-wrap" "wrap"}}
       (calendar/calendar {:year year :month month
                           :today-str today-str
                           :selected-date selected-date
                           :on-select (fn [d]
                                        (swap! !cal-state assoc :selected-date d)
                                        (render!))
                           :on-prev-month (fn [_]
                                            (let [[ny nm] (calendar/prev-month year month)]
                                              (swap! !cal-state assoc :year ny :month nm)
                                              (render!)))
                           :on-next-month (fn [_]
                                            (let [[ny nm] (calendar/next-month year month)]
                                              (swap! !cal-state assoc :year ny :month nm)
                                              (render!)))})]
      (when selected-date
        [:p {:style {"color" "var(--fg-1)" "font-size" "var(--font-sm)"}}
         (str "Selected: " selected-date)])

      [:h5 "Event Grid"]
      (cal-events/calendar-event-grid {:year year :month month
                                        :today-str today-str
                                        :selected-date selected-date
                                        :events sample-calendar-events
                                        :on-select (fn [d]
                                                     (swap! !cal-state assoc :selected-date d)
                                                     (render!))
                                        :on-prev-month (fn [_]
                                                         (let [[ny nm] (calendar/prev-month year month)]
                                                           (swap! !cal-state assoc :year ny :month nm)
                                                           (render!)))
                                        :on-next-month (fn [_]
                                                         (let [[ny nm] (calendar/next-month year month)]
                                                           (swap! !cal-state assoc :year ny :month nm)
                                                           (render!)))
                                        :on-event-click (fn [evt] (js/console.log "Event clicked:" (:title evt)))})

      [:h5 "Day Ticker"]
      (cal-events/ticker-strip {:days [{:date "2026-03-27" :day-num 27 :day-label "Fr"}
                                        {:date "2026-03-28" :day-num 28 :day-label "Sa"}
                                        {:date "2026-03-29" :day-num 29 :day-label "Su"}
                                        {:date "2026-03-30" :day-num 30 :day-label "Mo"}
                                        {:date "2026-03-31" :day-num 31 :day-label "Tu"}
                                        {:date "2026-04-01" :day-num 1  :day-label "We"}
                                        {:date "2026-04-02" :day-num 2  :day-label "Th"}
                                        {:date "2026-04-03" :day-num 3  :day-label "Fr"}]
                                 :today-str today-str
                                 :selected (or selected-date today-str)
                                 :events sample-calendar-events
                                 :on-select (fn [d]
                                              (swap! !cal-state assoc :selected-date d)
                                              (render!))})

      [:h5 "Agenda List"]
      (cal-events/agenda-list {:days [{:date "2026-03-29" :label "Today"}
                                       {:date "2026-03-30" :label "Tomorrow"}
                                       {:date "2026-03-31" :label "Tue"}
                                       {:date "2026-04-01" :label "Wed"}
                                       {:date "2026-04-02" :label "Thu"}
                                       {:date "2026-04-03" :label "Fri"}]
                                :events sample-calendar-events
                                :on-event-click (fn [evt] (js/console.log "Agenda event:" (:title evt)))}))))

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
  (let [{:keys [src]} @!lightbox-state]
    (section "Lightbox"
      [:p {:style {"color" "var(--fg-2)" "font-size" "var(--font-sm)"}}
       "Click a thumbnail to open the fullscreen lightbox overlay."]
      (into [:div {:style {"display" "flex" "gap" "0.75rem" "flex-wrap" "wrap"}}]
            (map (fn [{img-src :src img-alt :alt}]
                   (lightbox/image-thumbnail
                     {:src img-src
                      :alt img-alt
                      :on-click (fn []
                                  (swap! !lightbox-state assoc :src img-src)
                                  (render!))}))
                 sample-images))
      (lightbox/lightbox {:src src
                          :on-close (fn []
                                      (swap! !lightbox-state assoc :src nil)
                                      (render!))}))))

;; ── Pages ───────────────────────────────────────────────────────────

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
   (tabs-demo)
   (spinner-demo)
   (empty-state-demo)
   (drop-zone-demo)
   (processing-bar-demo)
   (toast-demo)
   (camera-demo)
   (skeleton-demo)
   (progress-demo)
   (theme-toggle-demo)
   (switch-demo)
   (tooltip-demo)
   (breadcrumb-demo)
   (pagination-demo)
   (separator-demo)
   (form-demo)
   (tag-input-demo)
   (lightbox-demo)])

(def icon-categories
  [["Navigation"
    ["home" "menu" "x"
     "chevron-down" "chevron-up" "chevron-left" "chevron-right"
     "arrow-down" "arrow-up" "arrow-left" "arrow-right"
     "external-link"]]
   ["Actions"
    ["search" "plus" "minus" "check" "edit" "trash"
     "download" "upload" "copy" "filter" "link" "refresh"]]
   ["Objects"
    ["file" "folder" "image" "mail" "bell" "calendar" "clock"
     "bookmark" "star" "heart" "inbox" "layers" "package"]]
   ["UI & System"
    ["settings" "user" "users" "log-out" "log-in" "eye" "eye-off"
     "lock" "grid" "list" "layout-dashboard" "monitor" "moon" "sun"]]
   ["Status"
    ["alert-triangle" "alert-circle" "info" "circle-check" "circle-x"]]
   ["Media"
    ["play" "pause" "skip-back" "skip-forward" "shuffle" "repeat" "volume-2" "music"]]
   ["Dev & Technical"
    ["code" "terminal" "database" "globe" "shield" "zap" "book-open" "map-pin"]]])

(defn- icon-card [n]
  [:div {:style {"display" "flex" "flex-direction" "column" "align-items" "center"
                 "gap" "var(--size-2)" "padding" "var(--size-3)"
                 "border-radius" "var(--radius-md)" "border" "var(--border-0)"}}
   (icon/icon {:icon-name n})
   [:span {:style {"font-size" "var(--font-xs)" "color" "var(--fg-2)"
                   "text-align" "center" "word-break" "break-all"}} n]])

(defn- icon-category-section [entry]
  (let [cat-name (first entry)
        icons (second entry)]
    (section cat-name
      (into [:div {:style {"display" "grid" "grid-template-columns" "repeat(auto-fill, minmax(5rem, 1fr))" "gap" "var(--size-4)"}}]
            (map icon-card icons)))))

(def !calendar-docs (atom nil))

(defn load-calendar-docs! []
  (when-not @!calendar-docs
    (-> (js/fetch "/calendar.md")
        (.then (fn [r] (.text r)))
        (.then (fn [text]
                 (reset! !calendar-docs text)
                 (render!))))))

(defn calendar-page []
  (load-calendar-docs!)
  [:div
   (page-header "Calendar" "Date picker, event grid, ticker strip, and agenda list.")
   (when-let [md @!calendar-docs]
     (into [:div {:class "md-docs"}]
           (markdown/markdown->hiccup md)))
   (calendar-demo)])

(defn- filled-icon-card [n]
  [:div {:style {"display" "flex" "flex-direction" "column" "align-items" "center"
                 "gap" "var(--size-3)" "padding" "var(--size-3)"
                 "border-radius" "var(--radius-md)" "border" "var(--border-0)"}}
   [:div {:style {"display" "flex" "gap" "var(--size-4)" "align-items" "center"}}
    (icon/icon {:icon-name n})
    (icon/icon {:icon-name n :filled true})]
   [:span {:style {"font-size" "var(--font-xs)" "color" "var(--fg-2)"
                   "text-align" "center"}} n]])

(defn icons-page []
  [:div
   (page-header "Icons" (str (count icon/icon-names) " icons based on Lucide. All render as inline SVG with stroke=\"currentColor\"."))
   (section "Sizes"
     (into [:div {:style {"display" "flex" "gap" "1.5rem" "align-items" "end"}}]
           (map (fn [pair]
                  (let [s (first pair)
                        label (second pair)]
                    [:div {:style {"display" "flex" "flex-direction" "column" "align-items" "center" "gap" "var(--size-2)"}}
                     (icon/icon {:icon-name "star" :size s})
                     [:span {:style {"font-size" "var(--font-xs)" "color" "var(--fg-2)"}} label]]))
                [["sm" "sm"] ["md" "md (default)"] ["lg" "lg"] ["xl" "xl"]])))
   (section "Filled Variants"
     [:p {:style {"color" "var(--fg-1)" "margin-bottom" "var(--size-4)" "font-size" "var(--font-sm)"}}
      "Media icons support a " [:code ":filled true"] " prop for solid rendering."]
     (into [:div {:style {"display" "grid" "grid-template-columns" "repeat(auto-fill, minmax(8rem, 1fr))" "gap" "var(--size-4)"}}]
           (map filled-icon-card ["play" "pause" "skip-back" "skip-forward" "repeat" "volume-2"])))
   (into [:div] (map icon-category-section icon-categories))])

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
               (sidebar/sidebar-menu-item {:href "#" :icon-name "download"} "Installation")
               (sidebar/sidebar-menu-item {:href "#" :icon-name "folder" :active true} "Project Structure")))
           (sidebar/sidebar-group {:label "Building"}
             (sidebar/sidebar-menu {}
               (sidebar/sidebar-menu-item {:href "#" :icon-name "globe"} "Routing")
               (sidebar/sidebar-menu-item {:href "#" :icon-name "database" :badge "New"} "Data Fetching")
               (sidebar/sidebar-menu-item {:href "#" :icon-name "layers"} "Rendering")
               (sidebar/sidebar-menu-item {:href "#" :icon-name "zap"} "Caching")
               (sidebar/sidebar-menu-item {:href "#" :icon-name "eye"} "Styling")))
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
       (sidebar/sidebar-layout-main {}
         [:div {:style {"padding" "2rem"}}
          [:h3 {:style {"margin" "0 0 1rem" "color" "var(--fg-0)"}} "Dashboard"]
          [:div {:style {"display" "grid" "grid-template-columns" "repeat(3, 1fr)" "gap" "1rem"}}
           [:div {:style {"aspect-ratio" "16/9" "background" "var(--bg-1)" "border-radius" "var(--radius-lg)" "border" "var(--border-0)"}}]
           [:div {:style {"aspect-ratio" "16/9" "background" "var(--bg-1)" "border-radius" "var(--radius-lg)" "border" "var(--border-0)"}}]
           [:div {:style {"aspect-ratio" "16/9" "background" "var(--bg-1)" "border-radius" "var(--radius-lg)" "border" "var(--border-0)"}}]]
          [:div {:style {"margin-top" "1rem" "min-height" "120px" "background" "var(--bg-1)" "border-radius" "var(--radius-lg)" "border" "var(--border-0)"}}]])))
   (section "Floating Sidebar (iOS)"
     [:p {:style {"color" "var(--fg-2)" "font-size" "var(--font-sm)" "margin" "0"}}
      "The " [:code "sidebar-layout--floating"] " modifier forces the off-canvas drawer at any size, "
      "positioned relative to its container. Tap the hamburger to open, tap the dimmed backdrop to close."]
     [:div {:style {"display" "flex" "justify-content" "center" "padding" "var(--size-4) 0"}}
      ;; iPhone-sized frame (375 × 812)
      [:div {:style {"width" "375px" "height" "812px" "max-width" "100%" "flex-shrink" "0"
                     "border" "10px solid var(--fg-0)" "border-radius" "2.75rem" "overflow" "hidden"
                     "background" "var(--bg-0)" "box-shadow" "var(--shadow-3)"}}
       (sidebar/sidebar-layout {:class "sidebar-layout--floating" :attrs {:style {"height" "100%"}}}
         (sidebar/sidebar {:attrs {:style {"width" "17rem"}}}
           (sidebar/sidebar-header {}
             (sidebar/sidebar-brand {:title "Pocket" :subtitle "Personal" :icon "P"}))
           (sidebar/sidebar-content {}
             (sidebar/sidebar-group {:label "Library"}
               (sidebar/sidebar-menu {}
                 (sidebar/sidebar-menu-item {:href "#" :icon-name "home" :active true} "Home")
                 (sidebar/sidebar-menu-item {:href "#" :icon-name "search"} "Search")
                 (sidebar/sidebar-menu-item {:href "#" :icon-name "star" :badge "12"} "Favorites")
                 (sidebar/sidebar-menu-item {:href "#" :icon-name "clock"} "Recent")))
             (sidebar/sidebar-group {:label "Account"}
               (sidebar/sidebar-menu {}
                 (sidebar/sidebar-menu-item {:href "#" :icon-name "bell"} "Notifications")
                 (sidebar/sidebar-menu-item {:href "#" :icon-name "settings"} "Settings"))))
           (sidebar/sidebar-footer {}
             (sidebar/sidebar-user {:user-name "Jamie Rivera" :email "jamie@pocket.app"})))
         (sidebar/sidebar-overlay {:on-click close-floating-sidebar!})
         (sidebar/sidebar-layout-main {}
           [:div {:style {"display" "flex" "flex-direction" "column" "height" "100%"}}
            ;; Top bar
            [:div {:style {"display" "flex" "align-items" "center" "gap" "var(--size-3)"
                           "padding" "var(--size-3) var(--size-4)" "border-bottom" "var(--border-0)"}}
             (sidebar/sidebar-mobile-toggle {:on-click toggle-floating-sidebar!})
             [:h3 {:style {"margin" "0" "color" "var(--fg-0)" "font-size" "var(--font-md)"}} "Home"]]
            ;; Content
            [:div {:style {"flex" "1" "overflow-y" "auto" "padding" "var(--size-4)" "display" "flex" "flex-direction" "column" "gap" "var(--size-3)"}}
             [:div {:style {"height" "140px" "background" "var(--bg-1)" "border-radius" "var(--radius-lg)" "border" "var(--border-0)"}}]
             [:div {:style {"display" "grid" "grid-template-columns" "1fr 1fr" "gap" "var(--size-3)"}}
              [:div {:style {"aspect-ratio" "1" "background" "var(--bg-1)" "border-radius" "var(--radius-lg)" "border" "var(--border-0)"}}]
              [:div {:style {"aspect-ratio" "1" "background" "var(--bg-1)" "border-radius" "var(--radius-lg)" "border" "var(--border-0)"}}]]
             [:div {:style {"height" "80px" "background" "var(--bg-1)" "border-radius" "var(--radius-lg)" "border" "var(--border-0)"}}]
             [:div {:style {"height" "80px" "background" "var(--bg-1)" "border-radius" "var(--radius-lg)" "border" "var(--border-0)"}}]]]))]])])

;; ── Navigation ──────────────────────────────────────────────────────

(def component-nav
  [{:title "General"
    :items [{:label "Button" :anchor "button"}
            {:label "Badge" :anchor "badge"}
            {:label "Card" :anchor "card"}
            {:label "Player Bar" :anchor "player-bar"}]}
   {:title "Forms"
    :items [{:label "Form" :anchor "form"}
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
            {:label "Skeleton" :anchor "skeleton"}
            {:label "Tooltip" :anchor "tooltip"}]}
   {:title "Layout"
    :items [{:label "Separator" :anchor "separator"}]}
   {:title "Overlay"
    :items [{:label "Lightbox" :anchor "lightbox"}]}
   {:title "Navigation"
    :items [{:label "Breadcrumb" :anchor "breadcrumb"}
            {:label "Pagination" :anchor "pagination"}
            {:label "Tabs" :anchor "tabs"}]}])

(def sample-files
  [{:name "Documents"          :file-type "folder"      :modified "May 10, 2026"}
   {:name "Photos"              :file-type "folder"      :modified "May 8, 2026"}
   {:name "project-proposal.pdf" :file-type "document"   :size "2.4 MB"   :modified "May 12, 2026"}
   {:name "vacation-photo.jpg"  :file-type "image"       :size "4.1 MB"   :modified "May 11, 2026"}
   {:name "presentation.pptx"  :file-type "document"    :size "12.8 MB"  :modified "May 9, 2026"}
   {:name "budget-2026.xlsx"   :file-type "spreadsheet" :size "156 KB"   :modified "May 7, 2026"}
   {:name "intro-video.mp4"    :file-type "video"       :size "245 MB"   :modified "May 5, 2026"}
   {:name "podcast-ep12.mp3"   :file-type "audio"       :size "48 MB"    :modified "May 3, 2026"}
   {:name "app.clj"            :file-type "code"        :size "8.2 KB"   :modified "May 14, 2026"}
   {:name "backup.zip"         :file-type "archive"     :size "1.2 GB"   :modified "Apr 28, 2026"}
   {:name "README.md"          :file-type "document"    :size "4.5 KB"   :modified "May 15, 2026"}
   {:name "screenshot.png"     :file-type "image"       :size "890 KB"   :modified "May 13, 2026"}])

(defn file-context-menu-items [item]
  (let [is-folder? (= (:file-type item) "folder")]
    (cond-> [{:label "Open"   :icon "folder"
              :on-click (fn [] (js/console.log (str "Open: " (:name item))) (render!))}]
      (not is-folder?) (conj {:label "Download" :icon "download"
                               :on-click (fn [] (js/console.log (str "Download: " (:name item))) (render!))})
      true (conj {:label "Rename" :icon "edit"
                  :on-click (fn [] (js/console.log (str "Rename: " (:name item))) (render!))})
      true (conj {:label "Share" :icon "link"
                  :on-click (fn [] (js/console.log (str "Share: " (:name item))) (render!))})
      true (conj {:type "separator"})
      true (conj {:label "Delete" :icon "trash" :variant "danger"
                  :on-click (fn [] (js/console.log (str "Delete: " (:name item))) (render!))}))))

(def !fb-view (atom "grid"))
(def !fb-sort (atom {:key "name" :dir "asc"}))
(def !fb-dropped-files (atom []))
(def !fb-body-drag-active (atom false))

(defn- format-file-size [bytes]
  (cond
    (>= bytes 1073741824) (str (.toFixed (/ bytes 1073741824) 1) " GB")
    (>= bytes 1048576)    (str (.toFixed (/ bytes 1048576) 1) " MB")
    (>= bytes 1024)       (str (.toFixed (/ bytes 1024) 1) " KB")
    :else                 (str bytes " B")))

(defn- ext->file-type [filename]
  (let [ext (some-> filename (.split ".") last .toLowerCase)]
    (case ext
      ("jpg" "jpeg" "png" "gif" "svg" "webp") "image"
      ("mp4" "mov" "avi" "mkv" "webm") "video"
      ("mp3" "wav" "flac" "aac" "ogg") "audio"
      ("pdf" "doc" "docx" "txt" "rtf") "document"
      ("xls" "xlsx" "csv") "spreadsheet"
      ("js" "clj" "cljs" "py" "rb" "rs" "go" "ts") "code"
      ("zip" "tar" "gz" "rar" "7z") "archive"
      "file")))

(defn- handle-dropped-files! [dropped]
  (let [files (for [f dropped]
                {:name (.-name f)
                 :size (format-file-size (.-size f))
                 :file-type (ext->file-type (.-name f))
                 :progress (rand-int 100)
                 :status "uploading"})
        existing-names (set (map :name @!fb-dropped-files))]
    (swap! !fb-dropped-files into (remove #(.has existing-names (:name %)) files)))
  (render!))

(defn- remove-dropped-file! [filename]
  (swap! !fb-dropped-files (fn [files] (vec (remove #(= (:name %) filename) files))))
  (render!))

(defn- toggle-sort! [col-key]
  (swap! !fb-sort (fn [{:keys [key dir]}]
                    (if (= key col-key)
                      {:key key :dir (if (= dir "asc") "desc" "asc")}
                      {:key col-key :dir "asc"})))
  (render!))

(defn- sorted-files [files {:keys [key dir]}]
  (let [cmp-fn (fn [a b]
                 (let [va (get a key)
                       vb (get b key)
                       fa (= (:file-type a) "folder")
                       fb-flag (= (:file-type b) "folder")]
                   (cond
                     (and fa (not fb-flag)) -1
                     (and fb-flag (not fa)) 1
                     :else (compare (or va "") (or vb "")))))
        sorted (sort cmp-fn files)]
    (if (= dir "desc") (reverse sorted) sorted)))

(defn file-browser-page []
  (let [view @!fb-view
        sort-state @!fb-sort
        files (sorted-files sample-files sort-state)]
    [:div
     (page-header "File Browser" "Grid and list views for file management with type-specific icons and context menus.")

     (section "File Type Icons"
       [:p {:style {"color" "var(--fg-2)" "font-size" "var(--font-sm)" "margin-bottom" "0.5rem"}}
        "Each file type gets a distinctive icon and color."]
       (into [:div {:style {"display" "grid" "grid-template-columns" "repeat(auto-fill, minmax(6rem, 1fr))" "gap" "var(--size-4)"}}]
             (map (fn [ft]
                    [:div {:style {"display" "flex" "flex-direction" "column" "align-items" "center" "gap" "var(--size-2)"
                                   "padding" "var(--size-3)" "border-radius" "var(--radius-md)" "border" "var(--border-0)"}}
                     (fb/file-type-icon {:file-type ft})
                     [:span {:style {"font-size" "var(--font-xs)" "color" "var(--fg-2)"}} ft]])
                  ["folder" "image" "video" "audio" "document" "spreadsheet" "code" "archive" "file"])))

     (section "Interactive File Browser"
       [:div {:class "fb-toolbar"}
        [:div {:style {"font-weight" "500"}} "My Files"]
        (fb/file-view-toggle {:view view
                              :on-grid-click (fn [_] (reset! !fb-view "grid") (render!))
                              :on-list-click (fn [_] (reset! !fb-view "list") (render!))})]
       (if (= view "grid")
         (into [:div {:class "fb-grid"}]
               (map (fn [item]
                      (fb/file-item-grid {:item item
                                          :context-menu-items (file-context-menu-items item)}))
                    files))
         (fb/file-table {:items    files
                         :sort-key (:key sort-state)
                         :sort-dir (:dir sort-state)
                         :on-sort  toggle-sort!
                         :on-row-click (fn [item] (js/console.log (str "Clicked: " (:name item))))
                         :context-menu-items-fn file-context-menu-items})))

     (section "Selected Items"
       [:p {:style {"color" "var(--fg-2)" "font-size" "var(--font-sm)"}}
        "Items can show a selected state."]
       (into [:div {:class "fb-grid"}]
             (map-indexed (fn [i item]
                            (fb/file-item-grid {:item item
                                                :selected (or (= i 0) (= i 2))}))
                          (take 4 sample-files))))

     (section "Custom Columns"
       [:p {:style {"color" "var(--fg-2)" "font-size" "var(--font-sm)" "margin-bottom" "0.5rem"}}
        "The file table supports custom column definitions. Mix built-in helpers with your own columns."]
       (fb/file-table {:items    files
                       :columns  [(fb/col-name {:label "File"})
                                  (fb/col-size)
                                  {:key   "owner"
                                   :label "Owner"
                                   :width "120px"
                                   :render (fn [item]
                                             (if (= (:file-type item) "folder")
                                               "Team"
                                               "Alice"))}
                                  {:key   "status"
                                   :label "Status"
                                   :width "100px"
                                   :render (fn [_item] "Synced")}]
                       :sort-key (:key sort-state)
                       :sort-dir (:dir sort-state)
                       :on-sort  toggle-sort!}))

     (section "Drop Zone"
       [:p {:style {"color" "var(--fg-2)" "font-size" "var(--font-sm)" "margin-bottom" "0.5rem"}}
        "Drag & drop file upload area. Drop files or click to browse."]
       (drop-zone/drop-zone {:accept "image/*,.pdf,.doc,.docx"
                             :multiple true
                             :title "Drop files here or click to browse"
                             :hint "Images and documents up to 10 MB"
                             :on-files handle-dropped-files!})
       (when (seq @!fb-dropped-files)
         (apply fp/file-progress-list {}
           (map (fn [f]
                  (fp/file-progress-item {:name (:name f)
                                          :size (:size f)
                                          :icon (fb/file-type-icon {:file-type (:file-type f) :size "sm"})
                                          :progress (:progress f)
                                          :status (:status f)
                                          :on-remove (fn [] (remove-dropped-file! (:name f)))}))
                @!fb-dropped-files))))

     (section "Drop Zone \u2014 Disabled"
       (drop-zone/drop-zone {:disabled true
                             :title "Uploads disabled"
                             :hint "You don't have permission to upload"}))

     (section "Full-Page Drop Zone"
       [:p {:style {"color" "var(--fg-2)" "font-size" "var(--font-sm)" "margin-bottom" "0.5rem"}}
        "Drag any file over the page to see the full-screen overlay. Files dropped anywhere are added to the queue above."])

     (when @!fb-body-drag-active
       (drop-zone/drop-zone-overlay {}))]))

(def nav-items
  [{:id "components" :label "Components"  :icon-name "package"}
   {:id "calendar"   :label "Calendar"    :icon-name "calendar"}
   {:id "icons"      :label "Icons"       :icon-name "image"}
   {:id "sidebar"    :label "Sidebar"     :icon-name "layout-dashboard"}
   {:id "files"      :label "File Browser" :icon-name "folder"}])

(defn navigate! [page-id]
  (fn [_e]
    (reset! !page page-id)
    (render!)))

(defn navigate-to-section! [anchor]
  (fn [_e]
    (when (not= @!page "components")
      (reset! !page "components")
      (render!))
    (js/setTimeout
      (fn []
        (when-let [el (js/document.getElementById anchor)]
          (.scrollIntoView el {"behavior" "smooth" "block" "start"})))
      50)))

;; ── App Shell ───────────────────────────────────────────────────────

(defn own-port []
  (let [p (js/parseInt (.-port js/window.location) 10)]
    (if (js/isNaN p) 3002 p)))

(defn make-targets []
  (let [port (own-port)
        base (- port 2)]
    [{:label "Hiccup"    :port (+ base 3)}
     {:label "Replicant" :port (+ base 1)}
     {:label "Squint"    :port (+ base 2) :active true}]))

(defn app-sidebar [active-page]
  (sidebar/sidebar {}
    (sidebar/sidebar-header {}
      (sidebar/sidebar-brand {:title "Clojure UI Framework" :subtitle "Squint" :icon "U"}))
    (sidebar/sidebar-content {}
      (sidebar/sidebar-group {:label "Pages"}
        (into (sidebar/sidebar-menu {})
              (map (fn [{:keys [id label icon-name]}]
                     (sidebar/sidebar-menu-item
                       {:icon-name icon-name :active (= id active-page)
                        :on-click (navigate! id)}
                       label))
                   nav-items)))
      (sidebar/sidebar-separator)
      (into (sidebar/sidebar-group {:label "Components"})
            (map (fn [{:keys [title items]}]
                   (sidebar/sidebar-collapsible {:title title :open true}
                     (into (sidebar/sidebar-menu {})
                           (map (fn [{:keys [label anchor]}]
                                  (sidebar/sidebar-menu-item {:on-click (navigate-to-section! anchor)} label))
                                items))))
                 component-nav))
      (sidebar/sidebar-separator)
      (sidebar/sidebar-group {:label "Targets"}
        (into (sidebar/sidebar-menu {})
              (map (fn [{:keys [label port active]}]
                     (sidebar/sidebar-menu-item
                       {:href (str "//" (.-hostname js/window.location) ":" port)
                        :icon-name "monitor"
                        :active active}
                       label))
                   (make-targets))))
      (sidebar/sidebar-separator)
      (sidebar/sidebar-group {:label "Theme"}
        [:div {:style {"padding" "0.5rem 0.75rem"}}
         (theme-toggle/theme-toggle {:mode @!theme-mode
                                     :on-change (fn [mode] (set-theme! mode))})]))
    (sidebar/sidebar-footer {}
      (sidebar/sidebar-user {:user-name "Dev Mode" :email (str "squint · port " (own-port)) :avatar "sq"}))))

(defn app []
  (let [active-page @!page]
    (sidebar/sidebar-layout {}
      (app-sidebar active-page)
      (sidebar/sidebar-overlay {:on-click close-sidebar!})
      (sidebar/sidebar-layout-main {}
        [:div {:style {"padding" "2rem" "max-width" "960px"}}
         [:div {:style {"display" "flex" "align-items" "center" "gap" "0.75rem" "margin-bottom" "1rem"}}
          (sidebar/sidebar-mobile-toggle {:on-click toggle-sidebar!})]
         (case active-page
           "components" (components-page)
           "calendar"   (calendar-page)
           "icons"      (icons-page)
           "sidebar"    (sidebar-page)
           "files"      (file-browser-page)
           (components-page))]))))

;; ── Init ────────────────────────────────────────────────────────────

(defn render! []
  (eu/render (app) (js/document.getElementById "app")))

(defn init! []
  (drop-zone/init-body-drop-zone! {:on-files handle-dropped-files!
                                   :on-active-change (fn [active?]
                                                       (reset! !fb-body-drag-active active?)
                                                       (render!))})
  ;; Init theme runtime and sync atom
  (when js/window.__uiTheme
    (.init js/window.__uiTheme)
    (reset! !theme-mode (.get js/window.__uiTheme))
    (.subscribe js/window.__uiTheme
      (fn [state] (reset! !theme-mode (.-mode state)) (render!))))
  (render!))

(defn reload! []
  (render!))

(init!)
