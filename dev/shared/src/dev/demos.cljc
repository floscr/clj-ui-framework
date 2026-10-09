(ns dev.demos
  "Shared, target-agnostic component demos used by all three dev targets
   (hiccup/clj, replicant/cljs, squint). Only static, stateless demos live
   here — stateful/interactive demos and the per-target app shell stay in
   each target's own namespace.

   Cross-target normalization is handled by three tiny helpers:
     - kw-name : name, stubbed to identity for squint (keywords are strings)
     - sx      : inline style map -> string for clj, map as-is for cljs/squint
                 (squint compiles keyword keys to string keys automatically)
     - cls     : class names -> vector for replicant, string for clj/squint"
  (:require [clojure.string :as str]
            [ui.button :as button]
            [ui.alert :as alert]
            [ui.badge :as badge]
            [ui.card :as card]
            [ui.accordion :as accordion]
            [ui.table :as table]
            [ui.spinner :as spinner]
            [ui.empty-state :as empty-state]
            [ui.drop-zone :as drop-zone]
            [ui.camera :as camera]
            [ui.grid :as grid]
            [ui.processing-bar :as processing-bar]
            [ui.toast :as toast]
            [ui.skeleton :as skeleton]
            [ui.progress :as progress]
            [ui.switch :as switch]
            [ui.tooltip :as tooltip]
            [ui.breadcrumb :as breadcrumb]
            [ui.separator :as separator]
            [ui.icon :as icon]
            [ui.panels :as panels]
            [ui.popover :as popover]
            [ui.command :as command]
            [ui.toolbar :as toolbar]
            [ui.button-group :as button-group]
            [ui.tabs :as tabs]
            [ui.form :as form]
            [ui.number-field :as number-field]
            [ui.color-picker :as color-picker]
            [ui.chat :as chat]))

;; ── Cross-target helpers ────────────────────────────────────────────

#?(:squint (defn kw-name [s] s)
   :cljs   (defn kw-name [s] (name s))
   :clj    (defn kw-name [s] (name s)))

(defn sx
  "Normalize an inline style map to the target's representation.
   clj -> \"k: v; ...\" string. cljs/squint -> map unchanged (squint compiles
   keyword keys to string keys, which is what eucalypt expects)."
  [m]
  #?(:squint m
     :cljs   m
     :clj    (str/join " " (map (fn [[k v]] (str (kw-name k) ": " v ";")) m))))

(defn cls
  "Join CSS class names. Replicant requires a vector; clj/squint take a string."
  [& parts]
  #?(:squint (str/join " " parts)
     :cljs   (vec parts)
     :clj    (str/join " " parts)))

#?(:squint (defn- variant-click [v] (fn [_] (js/console.log (str "Clicked: " (kw-name v)))))
   :cljs   (defn- variant-click [v] (fn [_] (js/console.log (str "Clicked: " (kw-name v)))))
   :clj    (defn- variant-click [_v] nil))

;; ── Layout helpers ──────────────────────────────────────────────────

(defn section [title & children]
  (let [id (-> title str/lower-case (str/replace #"\s+" "-"))]
    [:section {:id id :style (sx {:margin-bottom "2.5rem"})}
     [:h3 {:style (sx {:color "var(--fg-1)" :margin-bottom "1rem"
                       :border-bottom "var(--border-0)" :padding-bottom "0.5rem"})} title]
     (into [:div {:style (sx {:display "flex" :flex-direction "column" :gap "1rem"})}]
           children)]))

(defn page-header [title subtitle]
  [:div {:style (sx {:margin-bottom "2rem"})}
   [:h2 {:style (sx {:margin "0 0 0.25rem" :color "var(--fg-0)"})} title]
   (when subtitle
     [:p {:style (sx {:margin "0" :color "var(--fg-2)" :font-size "var(--font-sm)"})} subtitle])])

;; ── Shared data ─────────────────────────────────────────────────────

(def button-variants [:primary :secondary :ghost :danger :success])
(def button-sizes [:sm :md :lg])

;; ── Component demos (static, stateless) ─────────────────────────────

(defn button-demo []
  (let [hstack {:display "flex" :gap "0.75rem" :flex-wrap "wrap" :align-items "center"}]
    (section "Button"
      (into [:div {:style (sx hstack)}]
            (map (fn [v]
                   (button/button {:variant v :on-click (variant-click v)} (kw-name v)))
                 button-variants))
      (into [:div {:style (sx hstack)}]
            (map (fn [s]
                   (button/button {:variant :primary :size s} (str "size " (kw-name s))))
                 button-sizes))
      (into [:div {:style (sx hstack)}]
            (map (fn [v]
                   (button/button {:variant v :disabled true} (str (kw-name v) " disabled")))
                 button-variants))
      (into [:div {:style (sx hstack)}]
            [(button/button {:variant :primary :href "#"} "Link primary")
             (button/button {:variant :secondary :href "#"} "Link secondary")
             (button/button {:variant :link} "Link button")
             (button/button {:variant :link :href "https://example.com"} "Link with href")])
      (into [:div {:style (sx hstack)}]
            [(button/button {:variant :primary :icon-left :plus} "Add item")
             (button/button {:variant :secondary :icon-right :arrow-right} "Next")
             (button/button {:variant :primary :icon-left :download :icon-right :arrow-down} "Download")
             (button/button {:variant :ghost :icon-left :edit} "Edit")])
      (into [:div {:style (sx hstack)}]
            [(button/button {:variant :primary :icon :plus})
             (button/button {:variant :secondary :icon :search})
             (button/button {:variant :ghost :icon :settings})
             (button/button {:variant :danger :icon :trash})
             (button/button {:variant :primary :icon :plus :size :sm})
             (button/button {:variant :primary :icon :plus :size :lg})])
      (into [:div {:style (sx hstack)}]
            [(button/button {:variant :primary :icon :play :round true})
             (button/button {:variant :secondary :icon :pause :round true})
             (button/button {:variant :ghost :icon :heart :round true})
             (button/button {:variant :primary :icon :plus :size :sm :round true})
             (button/button {:variant :primary :icon :plus :size :lg :round true})])
      (into [:div {:style (sx hstack)}]
            [(button/button {:variant :primary :loading true} "Saving…")
             (button/button {:variant :success :loading true} "Publishing…")
             (button/button {:variant :secondary :loading true :size :sm} "Loading sm")
             (button/button {:variant :secondary :loading true :size :lg} "Loading lg")
             (button/button {:variant :primary :icon-left :upload :loading true} "Uploading…")
             (button/button {:variant :primary :icon :plus :loading true})]))))

(defn alert-demo []
  (section "Alert"
    (alert/alert {:variant :success :title "Success!"} "Your changes have been saved.")
    (alert/alert {:variant :warning :title "Warning!"} "Please review before continuing.")
    (alert/alert {:variant :danger :title "Error!"} "Something went wrong.")
    (alert/alert {:variant :info :title "Info"} "This is an informational alert.")
    (alert/alert {:title "Neutral"} "A neutral alert with no variant.")))

(defn badge-demo []
  (let [hstack {:display "flex" :gap "0.5rem" :flex-wrap "wrap" :align-items "center"}]
    (section "Badge"
      (into [:div {:style (sx hstack)}]
            [(badge/badge {} "Default")
             (badge/badge {:variant :secondary} "Secondary")
             (badge/badge {:variant :outline} "Outline")
             (badge/badge {:variant :success} "Success")
             (badge/badge {:variant :warning} "Warning")
             (badge/badge {:variant :danger} "Danger")])
      (into [:div {:style (sx hstack)}]
            [(badge/badge {:icon-name :check :variant :success} "Verified")
             (badge/badge {:icon-name :star} "Featured")
             (badge/badge {:icon-name :alert-triangle :variant :warning} "Caution")
             (badge/badge {:icon-name :clock :variant :secondary} "Pending")]))))

(defn card-demo []
  (section "Card"
    (card/card {}
      (card/card-header {} [:h4 "Card Title"] [:p "Card description goes here."])
      (card/card-body {} [:p "This is the card content. It can contain any HTML."])
      (card/card-footer {}
        (button/button {:variant :secondary :size :sm} "Cancel")
        (button/button {:variant :primary :size :sm} "Save")))

    [:h5 "Card List (full dividers)"]
    (card/card-list {}
      (card/card-list-item {} "Notifications")
      (card/card-list-item {} "Privacy")
      (card/card-list-item {} "Appearance")
      (card/card-list-item {} "Accessibility"))

    [:h5 "Card List (inset dividers)"]
    (card/card-list {:divider :inset}
      (card/card-list-item {} "Notifications")
      (card/card-list-item {} "Privacy")
      (card/card-list-item {} "Appearance")
      (card/card-list-item {} "Accessibility"))))

(defn accordion-demo []
  (section "Accordion"
    [:div {:class (cls "accordion-group")}
     (accordion/accordion {:title "What is this framework?"} "A cross-target component library.")
     (accordion/accordion {:title "How do I use it?" :open true} "Just require the namespace and call functions.")
     (accordion/accordion {:title "Is it accessible?"} "Yes, follows ARIA best practices.")]))

(defn table-demo []
  (section "Table"
    (table/table {:headers ["Name" "Email" "Role" "Status"]
                  :rows [["Alice Johnson" "alice@example.com" "Admin" "Active"]
                         ["Bob Smith" "bob@example.com" "Editor" "Active"]
                         ["Carol White" "carol@example.com" "Viewer" "Pending"]]})))

(defn spinner-demo []
  (section "Spinner"
    [:div {:style (sx {:display "flex" :gap "1.5rem" :align-items "center"})}
     (spinner/spinner {:size :xs})
     (spinner/spinner {:size :sm})
     (spinner/spinner {})
     (spinner/spinner {:size :lg})]))

(defn empty-state-demo []
  (section "Empty State"
    [:div {:style (sx {:border "var(--border-0)" :border-radius "var(--radius-md)"})}
     (empty-state/empty-state {} "No items yet")]
    [:div {:style (sx {:border "var(--border-0)" :border-radius "var(--radius-md)" :margin-top "1rem"})}
     (empty-state/empty-state {:icon :inbox}
       [:p "No photos yet — upload or take one."])]))

(defn drop-zone-demo []
  (section "Drop Zone"
    (drop-zone/drop-zone {:title "Upload photos"
                          :hint "Drag & drop or click to select"})
    (drop-zone/drop-zone {:size :lg
                          :icon :image
                          :title "Upload photos"
                          :hint "Drag & drop or click to select"})
    (drop-zone/drop-zone {}
      [:div {:style (sx {:display "flex" :gap "0.5rem" :flex-wrap "wrap"})}
       [:div {:style (sx {:width "64px" :height "64px" :border-radius "var(--radius-md)" :background "var(--bg-2)"})}]
       [:div {:style (sx {:width "64px" :height "64px" :border-radius "var(--radius-md)" :background "var(--bg-2)"})}]
       [:div {:style (sx {:width "64px" :height "64px" :border-radius "var(--radius-md)" :background "var(--bg-2)"})}]]
      (drop-zone/file-label {}
        [:span "+ Add more"]))))

(def toast-variants [:info :success :warning :danger])

#?(:squint (defn- toast-click [v]
             (fn [_] (toast/show-toast! {:message (str "Toast — " v) :variant v})))
   :cljs   (defn- toast-click [v]
             (fn [_] (toast/show-toast! {:message (str "Toast — " (kw-name v)) :variant v})))
   :clj    (defn- toast-click [_v] nil))

(defn toast-demo []
  (section "Toast"
    (into [:div {:style (sx {:display "flex" :gap "0.75rem" :flex-wrap "wrap"})}]
          (map (fn [v]
                 (button/button
                  (cond-> {:variant :secondary
                           :on-click (toast-click v)}
                    #?(:squint false :cljs false :clj true)
                    (assoc :attrs {:onclick (str "__uiToast('Toast — " (kw-name v)
                                                 "', {variant: '" (kw-name v) "'})")}))
                  (str "Show " (kw-name v))))
               toast-variants))
    [:div {:style (sx {:display "flex" :flex-direction "column" :gap "0.5rem" :align-items "flex-start"})}
     (toast/toast {:variant :info} "Static toast preview")
     (toast/toast {:variant :success} "✓ Saved")
     (toast/toast {:variant :warning} "Careful with this")
     (toast/toast {:variant :danger} "✗ Something failed")]))

(defn processing-bar-demo []
  (section "Processing Bar"
    (processing-bar/processing-bar {:title "⏳ 2 uploads processing..."}
      (processing-bar/processing-item {:thumb "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='36' height='36'%3E%3Crect fill='%234f46e5' width='36' height='36'/%3E%3C/svg%3E"
                                       :label "3 photos"
                                       :status "AI analyzing..."})
      (processing-bar/processing-item {:status "🔧 optimize-images"}))))

(defn camera-demo []
  (section "Camera"
    [:div {:style (sx {:display "flex" :gap "1rem" :flex-wrap "wrap"})}
     [:div {:style (sx {:width "280px"})}
      (camera/camera-view {})]
     [:div {:style (sx {:width "280px"})}
      (camera/camera-view
       {:active? true
        :video-id "demo-camera-video"
        :on-capture #?(:squint (fn [] (toast/show-toast! {:message "Capture clicked" :variant :info}))
                       :cljs   (fn [] (toast/show-toast! {:message "Capture clicked" :variant :info}))
                       :clj    nil)
        :on-switch #?(:squint (fn [] (toast/show-toast! {:message "Switch clicked" :variant :info}))
                      :cljs   (fn [] (toast/show-toast! {:message "Switch clicked" :variant :info}))
                      :clj    true)})]]))

(defn- grid-tile
  "Demo tile: a colored block. Square by default; give :h for a natural
   height (masonry)."
  [i & [h]]
  [:div {:style (sx (cond-> {:background "var(--bg-2)"
                             :border "var(--border-0)"
                             :border-radius "var(--radius-md)"
                             :display "flex"
                             :align-items "center"
                             :justify-content "center"
                             :color "var(--fg-2)"
                             :font-size "var(--font-sm)"}
                      h       (assoc :height (str h "px"))
                      (not h) (assoc :aspect-ratio "1")))}
   (str i)])

(def ^:private masonry-heights [120 190 90 150 220 110 170 130])

(defn grid-demo []
  (section "Grid"
    [:div {:style (sx {:display "flex" :gap "1rem" :flex-wrap "wrap" :align-items "center"})}
     (grid/layout-toggle {:value :grid})
     (grid/layout-toggle {:value :grid :boxed true})
     (grid/size-stepper {:value :m})]
    [:p {:class (cls "text-muted" "text-sm")} "Square grid (size :s)"]
    (apply grid/grid {:size :s}
           (map (fn [i] (grid-tile i)) (range 1 9)))
    [:p {:class (cls "text-muted" "text-sm")} "Masonry (size :s) — needs the JS runtime outside Firefox"]
    (apply grid/grid {:layout :masonry :size :s}
           (map-indexed (fn [i h] (grid-tile (inc i) h)) masonry-heights))
    [:p {:class (cls "text-muted" "text-sm")} "Custom :min \"180px\" override"]
    (apply grid/grid {:min "180px"}
           (map (fn [i] (grid-tile i)) (range 1 5)))))

(defn skeleton-demo []
  (section "Skeleton"
    [:div {:style (sx {:max-width "400px"})}
     (skeleton/skeleton {:variant :heading})
     (skeleton/skeleton {:variant :line})
     (skeleton/skeleton {:variant :line})
     [:div {:style (sx {:display "flex" :gap "1rem" :margin-top "var(--size-3)"})}
      (skeleton/skeleton {:variant :circle})
      [:div {:style (sx {:flex "1"})}
       (skeleton/skeleton {:variant :line})
       (skeleton/skeleton {:variant :line})]]]))

(defn progress-demo []
  (section "Progress"
    (progress/progress {:value 25})
    (progress/progress {:value 50 :variant :success})
    (progress/progress {:value 75 :variant :warning})
    (progress/progress {:value 90 :variant :danger})))

(defn switch-demo []
  (section "Switch"
    [:div {:style (sx {:display "flex" :flex-direction "column" :gap "0.75rem"})}
     (switch/switch-toggle {:label "Notifications" :checked false})
     (switch/switch-toggle {:label "Dark mode" :checked true})
     (switch/switch-toggle {:label "Disabled off" :disabled true})
     (switch/switch-toggle {:label "Disabled on" :checked true :disabled true})]))

(defn tooltip-demo []
  (section "Tooltip"
    [:div {:style (sx {:display "flex" :gap "1.5rem" :padding-top "2rem"})}
     (tooltip/tooltip {:text "Save your changes"}
       (button/button {:variant :primary} "Save"))
     (tooltip/tooltip {:text "Delete this item"}
       (button/button {:variant :danger} "Delete"))
     (tooltip/tooltip {:text "View profile"}
       [:a {:href "#" :style (sx {:color "var(--accent)"})} "Profile"])]))

(defn popover-demo []
  (let [trigger-cls (button/button-classes {:variant :secondary})
        hstack {:display "flex" :gap "0.75rem" :flex-wrap "wrap" :align-items "center"}]
    (section "Popover"
      (into [:div {:style (sx hstack)}]
        [(popover/popover-trigger {:target "popover-basic" :class trigger-cls} "Open popover")
         (popover/popover-content {:id "popover-basic" :side :bottom :align :center}
           (popover/popover-header {}
             (popover/popover-title {} "Dimensions")
             (popover/popover-description {} "Set the dimensions for the layer."))
           [:div {:style (sx {:display "flex" :flex-direction "column" :gap "0.5rem"})}
            (form/form-field {:label "Width"}
              (form/form-input {:type :text :value "100%"}))
            (form/form-field {:label "Height"}
              (form/form-input {:type :text :value "25px"}))])])
      (into [:div {:style (sx hstack)}]
        [(popover/popover-trigger {:target "popover-start" :class trigger-cls} "Align start")
         (popover/popover-content {:id "popover-start" :side :bottom :align :start}
           (popover/popover-title {} "Aligned to start"))
         (popover/popover-trigger {:target "popover-end" :class trigger-cls} "Align end")
         (popover/popover-content {:id "popover-end" :side :bottom :align :end}
           (popover/popover-title {} "Aligned to end"))
         (popover/popover-trigger {:target "popover-top" :class trigger-cls} "Side top")
         (popover/popover-content {:id "popover-top" :side :top :align :center}
           (popover/popover-title {} "Opens above"))
         (popover/popover-trigger {:target "popover-right" :class trigger-cls} "Side right")
         (popover/popover-content {:id "popover-right" :side :right :align :center}
           (popover/popover-title {} "Opens to the right"))]))))

(defn command-demo []
  (let [trigger-cls (button/button-classes {:variant :secondary})]
    (section "Command"
      [:p {:style (sx {:color "var(--fg-2)" :font-size "var(--font-sm)" :margin-bottom "0.75rem"})}
       "Press " [:kbd {:class "command-shortcut"} "⌘K"] " or click the button. Type to filter, ↑↓ to navigate, ↵ to select."]
      [:div {}
       (command/command-trigger {:target "cmdk" :class trigger-cls}
         (icon/icon {:icon-name :search :size :sm})
         [:span "Search commands…"]
         [:kbd {:class "command-shortcut"} "⌘K"])
       (command/command-dialog {:id "cmdk" :hotkey "mod+k"
                                :placeholder "Type a command or search…"}
         (command/command-group {:heading "Suggestions"}
           (command/command-item {:icon :calendar :shortcut "⌘P"} "Calendar")
           (command/command-item {:icon :search   :shortcut "⌘F"} "Search Files")
           (command/command-item {:icon :clock} "Recent"))
         (command/command-group {:heading "Settings"}
           (command/command-item {:icon :user     :shortcut "⌘P"} "Profile")
           (command/command-item {:icon :mail     :shortcut "⌘B"} "Mail")
           (command/command-item {:icon :settings :shortcut "⌘S"} "Settings")))])))

(defn toolbar-demo []
  (let [trigger-cls (button/button-classes {:variant :secondary})
        hstack {:display "flex" :gap "0.75rem" :flex-wrap "wrap" :align-items "center"}]
    (section "Toolbar"
      ;; Horizontal rounded toolbar with grouped icon buttons
      [:div {}
       (toolbar/toolbar {}
         (button/button {:variant :ghost :icon :edit})
         (button/button {:variant :ghost :icon :copy})
         (button/button {:variant :ghost :icon :link})
         (toolbar/toolbar-separator {})
         (button/button {:variant :ghost :icon :star})
         (button/button {:variant :ghost :icon :bookmark})
         (toolbar/toolbar-separator {})
         (button/button {:variant :ghost :icon :trash}))]
      ;; Vertical toolbar
      [:div {}
       (toolbar/toolbar {:orientation :vertical}
         (button/button {:variant :ghost :icon :plus})
         (button/button {:variant :ghost :icon :minus})
         (toolbar/toolbar-separator {})
         (button/button {:variant :ghost :icon :search}))]
      ;; Toolbar triggered by a popover, anchored to its trigger
      (into [:div {:style (sx hstack)}]
        [(popover/popover-trigger {:target "toolbar-popover" :class trigger-cls} "Show toolbar")
         (popover/popover-content {:id "toolbar-popover" :side :top :align :center
                                   :class "popover-content--flush"}
           (toolbar/toolbar {}
             (button/button {:variant :ghost :icon :star})
             (button/button {:variant :ghost :icon :heart})
             (button/button {:variant :ghost :icon :copy})
             (toolbar/toolbar-separator {})
             (button/button {:variant :ghost :icon :trash})))]))))

(defn tabs-demo []
  (let [para (fn [txt] [:p {:class (cls "text-sm" "text-muted")
                           :style (sx {:margin "0"})} txt])]
    (section "Tabs"
      ;; Boxed (default) — segmented control
      (tabs/tabs
        {:variant :boxed
         :default "account"
         :tabs [{:id "account"  :label "Account"  :content (para "Make changes to your account here.")}
                {:id "password" :label "Password" :content (para "Change your password here.")}
                {:id "settings" :label "Settings" :content (para "Manage your preferences.")}]})
      ;; Line — underlined tabs
      (tabs/tabs
        {:variant :line
         :default "overview"
         :tabs [{:id "overview"  :label "Overview"  :content (para "A high-level summary of the project.")}
                {:id "analytics" :label "Analytics" :content (para "Traffic and engagement metrics.")}
                {:id "reports"   :label "Reports"   :content (para "Downloadable reports and exports.")}]}))))

(defn button-group-demo []
  (let [row {:display "flex" :gap "1rem" :flex-wrap "wrap" :align-items "center"}]
    (section "Button group"
      ;; Boxed flavor — filter segments with counts (like tabs-boxed)
      (into [:div {:style (sx row)}]
        [(button-group/button-group {:variant :boxed}
           (button-group/button-group-item {:active true :count 353} "All")
           (button-group/button-group-item {:count 28} "Downloaded")
           (button-group/button-group-item {:count 325} "Missing"))])
      ;; Boxed flavor — icon toolbar with separators (view / stepper / settings)
      (into [:div {:style (sx row)}]
        [(button-group/button-group {:variant :boxed}
           (button-group/button-group-item {:icon :grid :active true})
           (button-group/button-group-item {:icon :layout-dashboard})
           (button-group/button-group-separator {})
           (button-group/button-group-item {:icon :minus})
           (button-group/button-group-item {:icon :plus})
           (button-group/button-group-separator {})
           (button-group/button-group-item {:icon :settings}))])
      ;; Ghost flavor — container-less, tight cluster; active item filled
      (into [:div {:style (sx row)}]
        [(button-group/button-group {}
           (button-group/button-group-item {:icon :grid :active true})
           (button-group/button-group-item {:icon :layout-dashboard})
           (button-group/button-group-separator {})
           (button-group/button-group-item {:icon :settings}))]))))

(defn header-patterns-demo []
  ;; Composed inspiration — clean app header bars assembled from existing
  ;; pieces (button, button-group, form-input, icon). Not a component; copy
  ;; the composition into an app and adapt. Design language: transparent nav
  ;; with a hairline bottom border, one accent primary action, background-only
  ;; search, and bordered button groups for related multi-button clusters.
  (let [nav   {:display "flex" :align-items "center" :gap "1.5rem"
               :min-height "3.5rem" :padding "0 1.25rem"}
        frame {:border "var(--border-0)" :border-radius "var(--radius-md)"
               :background "var(--bg-0)" :overflow "hidden"}
        row1  (assoc nav :border-bottom "var(--border-0)")
        spacer [:div {:style (sx {:flex "1"})}]]
    (section "Header patterns"
      ;; 1 · Photos-style — title, boxed icon group, standalone action, primary CTA
      [:div {:style (sx frame)}
       (into [:div {:style (sx nav)}]
         [[:strong {:style (sx {:color "var(--fg-0)" :font-size "var(--font-md)"})} "Photos"]
          spacer
          (button-group/button-group {:variant :boxed}
            (button-group/button-group-item {:icon :grid :active true})
            (button-group/button-group-item {:icon :layout-dashboard})
            (button-group/button-group-separator {})
            (button-group/button-group-item {:icon :minus})
            (button-group/button-group-item {:icon :plus})
            (button-group/button-group-separator {})
            (button-group/button-group-item {:icon :settings}))
          (button/button {:variant :ghost :icon :camera})
          (button/button {:variant :primary :icon-left :upload} "Upload")])]
      ;; 2 · Two-row — tabs/title + refresh; second row search + filter group with counts
      [:div {:style (sx frame)}
       (into [:div {:style (sx row1)}]
         [[:strong {:style (sx {:color "var(--fg-0)" :font-size "var(--font-md)"})} "Watchlist"]
          [:span {:class (cls "text-sm" "text-muted")} "353 films"]
          spacer
          (button/button {:variant :ghost :icon :refresh})])
       (into [:div {:style (sx nav)}]
         [[:div {:style (sx {:flex "1"})}
           (form/form-input {:icon-left :search :placeholder "Search all movies…"})]
          (button-group/button-group {:variant :boxed}
            (button-group/button-group-item {:active true :count 353} "All")
            (button-group/button-group-item {:count 28} "Downloaded")
            (button-group/button-group-item {:count 325} "Missing"))])])))

(defn breadcrumb-demo []
  (section "Breadcrumb"
    (breadcrumb/breadcrumb
      {:items [{:label "Home" :href "#"}
               {:label "Projects" :href "#"}
               {:label "Oat Docs" :href "#"}
               {:label "Components"}]})))

(defn separator-demo []
  (section "Separator"
    [:div {:style (sx {:max-width "24rem"})}
     [:div {:style (sx {:display "flex" :flex-direction "column" :gap "0.375rem"})}
      [:div {:style (sx {:font-weight "500" :line-height "1"})} "Clojure UI"]
      [:div {:style (sx {:color "var(--fg-2)" :font-size "var(--font-sm)"})} "A cross-target component library"]]
     [:div {:style (sx {:margin "1rem 0"})}
      (separator/separator {})]
     [:p {:style (sx {:font-size "var(--font-sm)"})} "Build once, render everywhere — Hiccup, Replicant, and Squint."]]
    [:div {:style (sx {:display "flex" :align-items "center" :gap "1rem" :height "1.25rem"})}
     [:span {:style (sx {:font-size "var(--font-sm)"})} "Blog"]
     (separator/separator {:orientation :vertical})
     [:span {:style (sx {:font-size "var(--font-sm)"})} "Docs"]
     (separator/separator {:orientation :vertical})
     [:span {:style (sx {:font-size "var(--font-sm)"})} "Source"]]))

(defn- panels-pane [& children]
  (into [:div {:style (sx {:padding "1rem" :font-size "var(--font-sm)"
                           :color "var(--fg-2)" :height "100%"
                           :box-sizing "border-box"})}]
        children))

(defn panels-demo []
  (section "Panels"
    [:div {:style (sx {:height "260px" :border "var(--border-0)"
                       :border-radius "var(--radius-md)" :overflow "hidden"})}
     (panels/group {:orientation :horizontal}
       (panels/panel {:size 220 :min-size 140 :max-size 360 :collapsible true}
         (panels-pane "Sidebar — drag the seam, arrow keys resize, Enter collapses, double-click resets."))
       (panels/separator {:label "Resize sidebar"})
       (panels/panel {:pin true}
         (panels-pane "Main content (pinned fill — stays anchored while the sidebar folds).")))]
    [:div {:style (sx {:height "200px" :border "var(--border-0)"
                       :border-radius "var(--radius-md)" :overflow "hidden"})}
     (panels/group {:orientation :vertical}
       (panels/panel {}
         (panels-pane "Fill area."))
       (panels/separator {:label "Resize bottom panel"})
       (panels/panel {:size 60 :min-size 40 :max-size 120}
         (panels-pane "Bottom panel.")))]
    [:div {:style (sx {:height "180px" :border "var(--border-0)"
                       :border-radius "var(--radius-md)" :overflow "hidden"})}
     (panels/group {:orientation :horizontal}
       (panels/panel {}
         (panels-pane "Fill area."))
       (panels/panel {:size "30%" :min-size 120}
         (panels-pane "Bare edge panel (30%) — drag its left edge, no separator needed.")))]))

(defn form-demo []
  (section "Form"
    [:form {:style (sx {:max-width "480px"})}
     (form/form-field {:label "Name"}
       (form/form-input {:type :text :placeholder "Enter your name"}))
     (form/form-field {:label "Email"}
       (form/form-input {:type :email :placeholder "you@example.com"}))
     (form/form-field {:label "Password" :hint "At least 8 characters"}
       (form/form-input {:type :password :placeholder "Password"}))
     (form/form-field {:label "Select"}
       (form/form-select {:placeholder "Select an option"
                          :options [{:value "a" :label "Option A"}
                                    {:value "b" :label "Option B"}
                                    {:value "c" :label "Option C"}]}))
     (form/form-field {:label "Message"}
       (form/form-textarea {:placeholder "Your message..."}))
     (form/form-field {:label "Auto-growing (3 lines max)"}
       (form/form-textarea-auto {:placeholder "Grows as you type..." :max-rows 3}))
     (form/form-field {:label "Disabled"}
       (form/form-input {:type :text :placeholder "Disabled" :disabled true}))
     (form/form-field {:label "File"}
       (form/form-file {}))
     (form/form-field {:label "Date and time"}
       (form/form-input {:type :datetime-local}))
     (form/form-field {:label "Date"}
       (form/form-input {:type :date}))
     (form/form-field {:label "Search (icon left)"}
       (form/form-input {:type :text :placeholder "Search..." :icon-left :search}))
     (form/form-field {:label "URL (both icons)"}
       (form/form-input {:type :text :placeholder "example.com" :icon-left :globe :icon-right :check}))
     (form/form-checkbox {:label "I agree to the terms"})
     (form/form-radio-group {:label "Preference"
                             :radio-name "pref"
                             :options [{:value "a" :label "Option A"}
                                       {:value "b" :label "Option B"}
                                       {:value "c" :label "Option C"}]})
     (form/form-field {:label "Volume"}
       (form/form-range {:min 0 :max 100 :value 50}))
     (button/button {:variant :primary :attrs {:type "submit"}} "Submit")]
    [:div {:style (sx {:max-width "480px" :margin-top "1.5rem"})}
     [:h4 {:style (sx {:margin-bottom "0.75rem"})} "Input group"]
     (form/form-group {}
       (form/form-group-addon {} "https://")
       (form/form-input {:placeholder "subdomain"})
       (button/button {:variant :primary :size :sm} "Go"))]
    [:div {:style (sx {:max-width "480px" :margin-top "1.5rem"})}
     [:h4 {:style (sx {:margin-bottom "0.75rem"})} "Search bar preset"]
     (form/search-bar {:placeholder "Search…"})]
    [:div {:style (sx {:max-width "480px" :margin-top "1.5rem"})}
     [:h4 {:style (sx {:margin-bottom "0.75rem"})} "Validation error"]
     (form/form-field {:label "Email" :error "Please enter a valid email address."}
       (form/form-input {:type :email :error true :value "invalid-email"}))]))

(defn- nf-cell [label field]
  [:div {:style (sx {:width "11rem"})}
   (form/form-field {:label label} field)])

(defn number-field-demo []
  (section "Number Field"
    [:p {:style (sx {:margin "0" :color "var(--fg-2)" :font-size "var(--font-sm)"})}
     "Scroll over a field or click a stepper to change the value."]
    [:div {:style (sx {:display "flex" :flex-wrap "wrap" :gap "1.5rem" :align-items "flex-end"})}
     (nf-cell "Default" (number-field/number-field {:value 5 :min 0 :max 100}))
     (nf-cell "Buttons right" (number-field/number-field {:value 5 :min 0 :max 100 :variant :right}))
     (nf-cell "Spinner" (number-field/number-field {:value 5 :min 0 :max 100 :variant :spinner}))]
    [:div {:style (sx {:display "flex" :flex-wrap "wrap" :gap "1.5rem" :align-items "flex-end"})}
     (nf-cell "Small" (number-field/number-field {:value 2 :min 0 :max 10 :size :sm}))
     (nf-cell "Medium" (number-field/number-field {:value 2 :min 0 :max 10 :size :md}))
     (nf-cell "Large" (number-field/number-field {:value 2 :min 0 :max 10 :size :lg}))]
    [:div {:style (sx {:display "flex" :flex-wrap "wrap" :gap "1.5rem" :align-items "flex-end"})}
     (nf-cell "Spinner small" (number-field/number-field {:value 2 :min 0 :max 10 :size :sm :variant :spinner}))
     (nf-cell "Spinner large" (number-field/number-field {:value 2 :min 0 :max 10 :size :lg :variant :spinner}))
     (nf-cell "Right large" (number-field/number-field {:value 2 :min 0 :max 10 :size :lg :variant :right}))]
    [:div {:style (sx {:display "flex" :flex-wrap "wrap" :gap "1.5rem" :align-items "flex-end"})}
     (nf-cell "Step 0.5" (number-field/number-field {:value 1 :min 0 :max 10 :step 0.5}))
     (nf-cell "In form" (form/form-field {:label "Amount" :hint "Between 10 and 100."}
                          (number-field/number-field {:value 5 :min 0 :max 100 :name "amount"})))
     (nf-cell "Disabled" (number-field/number-field {:value 5 :disabled true}))]))

(defn- cp-cell [label picker]
  [:div {:style (sx {:width "16rem"})}
   [:h4 {:style (sx {:margin-bottom "0.75rem"})} label]
   picker])

(defn color-picker-demo []
  (section "Color Picker"
    [:p {:style (sx {:margin "0" :color "var(--fg-2)" :font-size "var(--font-sm)"})}
     "Drag the plane, move the tracks, or type any CSS color and press Enter."]
    [:div {:style (sx {:display "flex" :flex-wrap "wrap" :gap "1.5rem" :align-items "flex-start"})}
     (cp-cell "All formats + opacity" (color-picker/color-picker {:value "#3b82f6" :alpha true}))
     (cp-cell "Hex only" (color-picker/color-picker {:value "#1f2937" :formats [:hex]}))
     (cp-cell "Disabled" (color-picker/color-picker {:value "hsl(140, 60%, 45%)" :disabled true}))]))

(defn chat-demo []
  (section "Chat"
    [:div {:style (sx {:max-width "480px"})}
     (chat/chat-toolbar {}
       (button/button {:variant :ghost :size :sm :icon-left :trash} "Clear chat"))
     (chat/chat-log {}
       (chat/chat-bubble {:role :user} "How was my training week?")
       (chat/chat-bubble {:role :assistant}
         [:p "Solid week — 2 strength sessions and a run. "
          [:strong "One more leg day"] " would round it out."])
       (chat/chat-bubble {:role :user} "Plan me a leg day for Friday.")
       (chat/chat-thinking {:label "Coach is thinking…"}))
     [:div {:style (sx {:margin-top "1rem"})}
      (chat/chat-input {:placeholder "Message your coach…"
                        :hint "Coach sees your profile + recent activity."
                        :send-icon :zap
                        :input-attrs {:name "message"}})]]))
