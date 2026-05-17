(ns dev.replicant
  (:require [clojure.string :as str]
            [replicant.dom :as d]
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
            [ui.lightbox :as lightbox])
  (:require-macros [ui.macros :refer [inline-file]]))

;; ── State ───────────────────────────────────────────────────────────

(defonce !page (atom :components))

;; ── Helpers ─────────────────────────────────────────────────────────

(defn section [title & children]
  (let [id (str/lower-case title)]
    [:section {:id id :style {:margin-bottom "2.5rem"}}
     [:h3 {:style {:color "var(--fg-1)" :margin-bottom "1rem"
                    :border-bottom "var(--border-0)" :padding-bottom "0.5rem"}} title]
     (into [:div {:style {:display "flex" :flex-direction "column" :gap "1rem"}}] children)]))

(defn page-header [title subtitle]
  [:div {:style {:margin-bottom "2rem"}}
   [:h2 {:style {:margin "0 0 0.25rem" :color "var(--fg-0)"}} title]
   (when subtitle
     [:p {:style {:margin "0" :color "var(--fg-2)" :font-size "var(--font-sm)"}} subtitle])])

;; ── Component Demos ─────────────────────────────────────────────────

(def button-variants [:primary :secondary :ghost :danger])
(def button-sizes [:sm :md :lg])

(defn button-demo []
  (section "Button"
    [:div {:style {:display "flex" :gap "0.75rem" :flex-wrap "wrap" :align-items "center"}}
     (for [v button-variants]
       (button/button {:variant v :on-click (fn [_] (js/console.log (str "Clicked: " (name v))))}
                      (name v)))]
    [:div {:style {:display "flex" :gap "0.75rem" :flex-wrap "wrap" :align-items "center"}}
     (for [s button-sizes]
       (button/button {:variant :primary :size s} (str "size " (name s))))]
    [:div {:style {:display "flex" :gap "0.75rem" :flex-wrap "wrap" :align-items "center"}}
     (for [v button-variants]
       (button/button {:variant v :disabled true} (str (name v) " disabled")))]
    [:div {:style {:display "flex" :gap "0.75rem" :flex-wrap "wrap" :align-items "center"}}
     (button/button {:variant :primary :href "#"} "Link primary")
     (button/button {:variant :secondary :href "#"} "Link secondary")
     (button/button {:variant :link} "Link button")
     (button/button {:variant :link :href "https://example.com"} "Link with href")]
    [:div {:style {:display "flex" :gap "0.75rem" :flex-wrap "wrap" :align-items "center"}}
     (button/button {:variant :primary :icon-left :plus} "Add item")
     (button/button {:variant :secondary :icon-right :arrow-right} "Next")
     (button/button {:variant :primary :icon-left :download :icon-right :arrow-down} "Download")
     (button/button {:variant :ghost :icon-left :edit} "Edit")]
    [:div {:style {:display "flex" :gap "0.75rem" :flex-wrap "wrap" :align-items "center"}}
     (button/button {:variant :primary :icon :plus})
     (button/button {:variant :secondary :icon :search})
     (button/button {:variant :ghost :icon :settings})
     (button/button {:variant :danger :icon :trash})
     (button/button {:variant :primary :icon :plus :size :sm})
     (button/button {:variant :primary :icon :plus :size :lg})]
    [:div {:style {:display "flex" :gap "0.75rem" :flex-wrap "wrap" :align-items "center"}}
     (button/button {:variant :primary :icon :play :round true})
     (button/button {:variant :secondary :icon :pause :round true})
     (button/button {:variant :ghost :icon :heart :round true})
     (button/button {:variant :primary :icon :plus :size :sm :round true})
     (button/button {:variant :primary :icon :plus :size :lg :round true})]))

(defn alert-demo []
  (section "Alert"
    (alert/alert {:variant :success :title "Success!"} "Your changes have been saved.")
    (alert/alert {:variant :warning :title "Warning!"} "Please review before continuing.")
    (alert/alert {:variant :danger :title "Error!"} "Something went wrong.")
    (alert/alert {:variant :info :title "Info"} "This is an informational alert.")
    (alert/alert {:title "Neutral"} "A neutral alert with no variant.")))

(defn badge-demo []
  (section "Badge"
    [:div {:style {:display "flex" :gap "0.5rem" :flex-wrap "wrap" :align-items "center"}}
     (badge/badge {} "Default")
     (badge/badge {:variant :secondary} "Secondary")
     (badge/badge {:variant :outline} "Outline")
     (badge/badge {:variant :success} "Success")
     (badge/badge {:variant :warning} "Warning")
     (badge/badge {:variant :danger} "Danger")]
    [:div {:style {:display "flex" :gap "0.5rem" :flex-wrap "wrap" :align-items "center"}}
     (badge/badge {:icon-name :check :variant :success} "Verified")
     (badge/badge {:icon-name :star} "Featured")
     (badge/badge {:icon-name :alert-triangle :variant :warning} "Caution")
     (badge/badge {:icon-name :clock :variant :secondary} "Pending")]))

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
    [:div {:class ["accordion-group"]}
     (accordion/accordion {:title "What is this framework?"} "A cross-target component library.")
     (accordion/accordion {:title "How do I use it?" :open true} "Just require the namespace and call functions.")
     (accordion/accordion {:title "Is it accessible?"} "Yes, follows ARIA best practices.")]))

(defn table-demo []
  (section "Table"
    (table/table {:headers ["Name" "Email" "Role" "Status"]
                  :rows [["Alice Johnson" "alice@example.com" "Admin" "Active"]
                         ["Bob Smith" "bob@example.com" "Editor" "Active"]
                         ["Carol White" "carol@example.com" "Viewer" "Pending"]]})))

(defn dialog-demo []
  (section "Dialog"
    [:p {:style {:color "var(--fg-2)" :font-size "var(--font-sm)"}} "Click button to open dialog."]
    (button/button {:variant :primary
                    :on-click (fn [_]
                                (when-let [el (.getElementById js/document "demo-dialog")]
                                  (.showModal el)))}
      "Open dialog")
    (dialog/dialog {:id "demo-dialog"}
      (dialog/dialog-header {} [:h3 "Dialog Title"] [:p "Are you sure you want to continue?"])
      (dialog/dialog-body {} [:p "This action cannot be undone."])
      (dialog/dialog-footer {}
        (button/button {:variant :secondary :size :sm
                        :on-click (fn [_] (.close (.getElementById js/document "demo-dialog")))}
          "Cancel")
        (button/button {:variant :primary :size :sm
                        :on-click (fn [_] (.close (.getElementById js/document "demo-dialog")))}
          "Confirm")))))

(defn spinner-demo []
  (section "Spinner"
    [:div {:style {:display "flex" :gap "1.5rem" :align-items "center"}}
     (spinner/spinner {:size :sm})
     (spinner/spinner {})
     (spinner/spinner {:size :lg})]))

(defn skeleton-demo []
  (section "Skeleton"
    [:div {:style {:max-width "400px"}}
     (skeleton/skeleton {:variant :heading})
     (skeleton/skeleton {:variant :line})
     (skeleton/skeleton {:variant :line})
     [:div {:style {:display "flex" :gap "1rem" :margin-top "var(--size-3)"}}
      (skeleton/skeleton {:variant :circle})
      [:div {:style {:flex "1"}}
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
    [:div {:style {:display "flex" :flex-direction "column" :gap "0.75rem"}}
     (switch/switch-toggle {:label "Notifications" :checked false})
     (switch/switch-toggle {:label "Dark mode" :checked true})
     (switch/switch-toggle {:label "Disabled off" :disabled true})
     (switch/switch-toggle {:label "Disabled on" :checked true :disabled true})]))

(defn tooltip-demo []
  (section "Tooltip"
    [:div {:style {:display "flex" :gap "1.5rem" :padding-top "2rem"}}
     (tooltip/tooltip {:text "Save your changes"}
       (button/button {:variant :primary} "Save"))
     (tooltip/tooltip {:text "Delete this item"}
       (button/button {:variant :danger} "Delete"))
     (tooltip/tooltip {:text "View profile"}
       [:a {:href "#" :style {:color "var(--accent)"}} "Profile"])]))

(defn breadcrumb-demo []
  (section "Breadcrumb"
    (breadcrumb/breadcrumb
      {:items [{:label "Home" :href "#"}
               {:label "Projects" :href "#"}
               {:label "Oat Docs" :href "#"}
               {:label "Components"}]})))

(defn pagination-demo []
  (section "Pagination"
    (pagination/pagination {:current 3 :total 5
                            :on-click (fn [p] (js/console.log (str "Page: " p)))})))

(defn separator-demo []
  (section "Separator"
    ;; Basic horizontal
    [:div {:style {:max-width "24rem"}}
     [:div {:style {:display "flex" :flex-direction "column" :gap "0.375rem"}}
      [:div {:style {:font-weight "500" :line-height "1"}} "Clojure UI"]
      [:div {:style {:color "var(--fg-2)" :font-size "var(--font-sm)"}} "A cross-target component library"]]
     [:div {:style {:margin "1rem 0"}}
      (separator/separator {})]
     [:p {:style {:font-size "var(--font-sm)"}} "Build once, render everywhere — Hiccup, Replicant, and Squint."]]
    ;; Vertical separator
    [:div {:style {:display "flex" :align-items "center" :gap "1rem" :height "1.25rem"}}
     [:span {:style {:font-size "var(--font-sm)"}} "Blog"]
     (separator/separator {:orientation :vertical})
     [:span {:style {:font-size "var(--font-sm)"}} "Docs"]
     (separator/separator {:orientation :vertical})
     [:span {:style {:font-size "var(--font-sm)"}} "Source"]]))

(defn form-demo []
  (section "Form"
    [:form {:style {:max-width "480px"}}
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
    [:div {:style {:max-width "480px" :margin-top "1.5rem"}}
     [:h4 {:style {:margin-bottom "0.75rem"}} "Input group"]
     (form/form-group {}
       (form/form-group-addon {} "https://")
       (form/form-input {:placeholder "subdomain"})
       (button/button {:variant :primary :size :sm} "Go"))]
    [:div {:style {:max-width "480px" :margin-top "1.5rem"}}
     [:h4 {:style {:margin-bottom "0.75rem"}} "Validation error"]
     (form/form-field {:label "Email" :error "Please enter a valid email address."}
       (form/form-input {:type :email :error true :value "invalid-email"}))]))

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

(defonce !tag-state (atom {:tags [] :input-value "" :open false :active-index 0}))

(defn tag-input-demo []
  (let [{:keys [tags input-value open active-index]} @!tag-state
        filtered (vec (tag-input/filter-tags all-tags tags input-value))]
    (section "Tag Input"
      [:div {:style {:max-width "480px"}}
       (tag-input/tag-input
         {:tags tags
          :input-value input-value
          :open open
          :filtered-items filtered
          :active-index active-index
          :placeholder "Add frameworks..."
          :on-input (fn [v]
                      (swap! !tag-state assoc :input-value v :open true :active-index 0))
          :on-focus (fn [_]
                      (swap! !tag-state assoc :open true))
          :on-blur (fn [_]
                     ;; Delay to allow click on dropdown item
                     (js/setTimeout #(swap! !tag-state assoc :open false) 150))
          :on-select (fn [item]
                       (swap! !tag-state (fn [s]
                                           (-> s
                                               (update :tags conj item)
                                               (assoc :input-value "" :active-index 0)))))
          :on-remove (fn [tag]
                       (swap! !tag-state update :tags
                              (fn [ts] (vec (remove #(= (:label %) (:label tag)) ts)))))
          :on-backspace (fn []
                          (swap! !tag-state update :tags
                                 (fn [ts] (if (seq ts) (vec (butlast ts)) ts))))
          :on-clear (fn []
                      (if (seq input-value)
                        (swap! !tag-state assoc :input-value "" :active-index 0)
                        (swap! !tag-state assoc :tags [] :active-index 0)))
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
                                                       (assoc :input-value "" :active-index 0))))))))})]
      [:p {:style {:color "var(--fg-2)" :font-size "var(--font-sm)" :margin-top "0.5rem"}}
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
    [:div {:style {:margin-top "1rem"}}
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

(defonce !cal-state (atom {:year 2026 :month 3 :selected-date nil}))

(defonce !lightbox-state (atom {:src nil}))

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
  (let [{:keys [year month selected-date]} @!cal-state
        today-str "2026-03-29"]
    (section "Calendar"
      [:h5 "Date Picker (interactive)"]
      [:div {:style {:display "flex" :gap "1.5rem" :flex-wrap "wrap"}}
       (calendar/calendar {:year year :month month
                           :today-str today-str
                           :selected-date selected-date
                           :on-select (fn [d] (swap! !cal-state assoc :selected-date d))
                           :on-prev-month (fn [_]
                                            (let [[ny nm] (calendar/prev-month year month)]
                                              (swap! !cal-state assoc :year ny :month nm)))
                           :on-next-month (fn [_]
                                            (let [[ny nm] (calendar/next-month year month)]
                                              (swap! !cal-state assoc :year ny :month nm)))})]
      (when selected-date
        [:p {:style {:color "var(--fg-1)" :font-size "var(--font-sm)"}}
         (str "Selected: " selected-date)])

      [:h5 "Event Grid"]
      (cal-events/calendar-event-grid {:year year :month month
                                        :today-str today-str
                                        :selected-date selected-date
                                        :events sample-calendar-events
                                        :on-select (fn [d] (swap! !cal-state assoc :selected-date d))
                                        :on-prev-month (fn [_]
                                                          (let [[ny nm] (calendar/prev-month year month)]
                                                            (swap! !cal-state assoc :year ny :month nm)))
                                        :on-next-month (fn [_]
                                                          (let [[ny nm] (calendar/next-month year month)]
                                                            (swap! !cal-state assoc :year ny :month nm)))
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
                                 :on-select (fn [d] (swap! !cal-state assoc :selected-date d))})

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
      [:p {:style {:color "var(--fg-2)" :font-size "var(--font-sm)"}}
       "Click a thumbnail to open the fullscreen lightbox overlay."]
      [:div {:style {:display "flex" :gap "0.75rem" :flex-wrap "wrap"}}
       (for [{img-src :src img-alt :alt} sample-images]
         (lightbox/image-thumbnail
           {:src img-src
            :alt img-alt
            :on-click (fn [] (swap! !lightbox-state assoc :src img-src))}))]
      (lightbox/lightbox {:src src
                          :on-close (fn [] (swap! !lightbox-state assoc :src nil))}))))

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
   (spinner-demo)
   (skeleton-demo)
   (progress-demo)
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

(def calendar-docs-md (inline-file "../../src/ui/calendar.md"))

(defn calendar-page []
  [:div
   (page-header "Calendar" "Date picker, event grid, ticker strip, and agenda list.")
   (into [:div {:class ["md-docs"]}]
         (markdown/markdown->hiccup calendar-docs-md))
   (calendar-demo)])

(defn icons-page []
  [:div
   (page-header "Icons" (str (count icon/icon-names) " icons based on Lucide. All render as inline SVG with stroke=\"currentColor\"."))
   (section "Sizes"
     [:div {:style {:display "flex" :gap "1.5rem" :align-items "end"}}
      (for [[s label] [[:sm "sm"] [:md "md (default)"] [:lg "lg"] [:xl "xl"]]]
        [:div {:style {:display "flex" :flex-direction "column" :align-items "center" :gap "var(--size-2)"}}
         (icon/icon {:icon-name :star :size s})
         [:span {:style {:font-size "var(--font-xs)" :color "var(--fg-2)"}} label]])])
   (section "Filled Variants"
     [:p {:style {:color "var(--fg-1)" :margin-bottom "var(--size-4)" :font-size "var(--font-sm)"}}
      "Media icons support a " [:code ":filled true"] " prop for solid rendering."]
     [:div {:style {:display "grid" :grid-template-columns "repeat(auto-fill, minmax(8rem, 1fr))" :gap "var(--size-4)"}}
      (for [n [:play :pause :skip-back :skip-forward :repeat :volume-2]]
        [:div {:style {:display "flex" :flex-direction "column" :align-items "center"
                       :gap "var(--size-3)" :padding "var(--size-3)"
                       :border-radius "var(--radius-md)" :border "var(--border-0)"}}
         [:div {:style {:display "flex" :gap "var(--size-4)" :align-items "center"}}
          (icon/icon {:icon-name n})
          (icon/icon {:icon-name n :filled true})]
         [:span {:style {:font-size "var(--font-xs)" :color "var(--fg-2)"
                         :text-align "center"}} (name n)]])])
   (for [[cat-name icons] icon-categories]
     (section cat-name
       [:div {:style {:display "grid" :grid-template-columns "repeat(auto-fill, minmax(5rem, 1fr))" :gap "var(--size-4)"}}
        (for [n icons]
          [:div {:style {:display "flex" :flex-direction "column" :align-items "center"
                         :gap "var(--size-2)" :padding "var(--size-3)"
                         :border-radius "var(--radius-md)" :border "var(--border-0)"}}
           (icon/icon {:icon-name n})
           [:span {:style {:font-size "var(--font-xs)" :color "var(--fg-2)"
                           :text-align "center" :word-break "break-all"}} (name n)]])]))])

(defn sidebar-page []
  [:div
   (page-header "Sidebar" "A composable sidebar with brand, search, grouped navigation, collapsible sections, and user footer.")
   (section "Example"
     (sidebar/sidebar-layout {:attrs {:style {:border "var(--border-0)" :border-radius "var(--radius-lg)"
                                              :overflow "hidden" :height "500px"}}}
       (sidebar/sidebar {:attrs {:style {:height "100%" :position "static"}}}
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
       (sidebar/sidebar-layout-main {}
         [:div {:style {:padding "2rem"}}
          [:h3 {:style {:margin "0 0 1rem" :color "var(--fg-0)"}} "Dashboard"]
          [:div {:style {:display "grid" :grid-template-columns "repeat(3, 1fr)" :gap "1rem"}}
           [:div {:style {:aspect-ratio "16/9" :background "var(--bg-1)" :border-radius "var(--radius-lg)" :border "var(--border-0)"}}]
           [:div {:style {:aspect-ratio "16/9" :background "var(--bg-1)" :border-radius "var(--radius-lg)" :border "var(--border-0)"}}]
           [:div {:style {:aspect-ratio "16/9" :background "var(--bg-1)" :border-radius "var(--radius-lg)" :border "var(--border-0)"}}]]
          [:div {:style {:margin-top "1rem" :min-height "120px" :background "var(--bg-1)" :border-radius "var(--radius-lg)" :border "var(--border-0)"}}]])))])

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
            {:label "Switch" :anchor "switch"}]}
   {:title "Data Display"
    :items [{:label "Table" :anchor "table"}
            {:label "Accordion" :anchor "accordion"}
            {:label "Progress" :anchor "progress"}]}
   {:title "Feedback"
    :items [{:label "Alert" :anchor "alert"}
            {:label "Dialog" :anchor "dialog"}
            {:label "Spinner" :anchor "spinner"}
            {:label "Skeleton" :anchor "skeleton"}
            {:label "Tooltip" :anchor "tooltip"}]}
   {:title "Layout"
    :items [{:label "Separator" :anchor "separator"}]}
   {:title "Overlay"
    :items [{:label "Lightbox" :anchor "lightbox"}]}
   {:title "Navigation"
    :items [{:label "Breadcrumb" :anchor "breadcrumb"}
            {:label "Pagination" :anchor "pagination"}]}])

(def nav-items
  [{:id :components :label "Components"  :icon-name :package}
   {:id :calendar   :label "Calendar"    :icon-name :calendar}
   {:id :icons      :label "Icons"       :icon-name :image}
   {:id :sidebar    :label "Sidebar"     :icon-name :layout-dashboard}])

(defn navigate! [page-id]
  (fn [_e]
    (reset! !page page-id)))

(defn navigate-to-section! [anchor]
  (fn [_e]
    (when (not= @!page :components)
      (reset! !page :components))
    (js/setTimeout
      (fn []
        (when-let [el (.getElementById js/document anchor)]
          (.scrollIntoView el #js {:behavior "smooth" :block "start"})))
      50)))

(defn toggle-theme! [_e]
  (let [el (.-documentElement js/document)
        current (.. el -dataset -theme)]
    (set! (.. el -dataset -noTransitions) "")
    (set! (.. el -dataset -theme)
          (if (= current "dark") "light" "dark"))
    (js/requestAnimationFrame
      #(js-delete (.-dataset el) "noTransitions"))))

(defn toggle-sidebar! [_e]
  (when-let [layout (.querySelector js/document ".sidebar-layout")]
    (.toggleAttribute layout "data-sidebar-open")))

(defn close-sidebar! [_e]
  (when-let [layout (.querySelector js/document ".sidebar-layout")]
    (.removeAttribute layout "data-sidebar-open")))

;; ── App Shell ───────────────────────────────────────────────────────

(defn own-port []
  (let [p (js/parseInt (.-port js/window.location) 10)]
    (if (js/isNaN p) 3001 p)))

(defn make-targets []
  (let [port (own-port)
        base (- port 1)]
    [{:label "Hiccup"    :port (+ base 3)}
     {:label "Replicant" :port (+ base 1) :active true}
     {:label "Squint"    :port (+ base 2)}]))

(defn app-sidebar [active-page]
  (sidebar/sidebar {}
    (sidebar/sidebar-header {}
      (sidebar/sidebar-brand {:title "Clojure UI Framework" :subtitle "Replicant" :icon "U"}))
    (sidebar/sidebar-content {}
      (sidebar/sidebar-group {:label "Pages"}
        (apply sidebar/sidebar-menu {}
          (for [{:keys [id label icon-name]} nav-items]
            (sidebar/sidebar-menu-item
              {:icon-name icon-name :active (= id active-page)
               :on-click (navigate! id)}
              label))))
      (sidebar/sidebar-separator)
      (sidebar/sidebar-group {:label "Components"}
        (for [{:keys [title items]} component-nav]
          (sidebar/sidebar-collapsible {:title title :open true}
            (apply sidebar/sidebar-menu {}
              (for [{:keys [label anchor]} items]
                (sidebar/sidebar-menu-item {:on-click (navigate-to-section! anchor)} label))))))
      (sidebar/sidebar-separator)
      (sidebar/sidebar-group {:label "Targets"}
        (apply sidebar/sidebar-menu {}
          (for [{:keys [label port active]} (make-targets)]
            (sidebar/sidebar-menu-item
              {:href (str "http://localhost:" port)
               :icon-name :monitor
               :active active}
              label))))
      (sidebar/sidebar-separator)
      (sidebar/sidebar-group {:label "Theme"}
        (sidebar/sidebar-menu {}
          (sidebar/sidebar-menu-item
            {:icon-name :sun :on-click toggle-theme!}
            "Toggle Dark Mode"))))
    (sidebar/sidebar-footer {}
      (sidebar/sidebar-user {:user-name "Dev Mode" :email (str "replicant · port " (own-port)) :avatar "cl"}))))

(defn app []
  (let [active-page @!page]
    (sidebar/sidebar-layout {}
      (app-sidebar active-page)
      (sidebar/sidebar-overlay {:on-click close-sidebar!})
      (sidebar/sidebar-layout-main {}
        [:div {:style {:padding "2rem" :max-width "960px"}}
         [:div {:style {:display "flex" :align-items "center" :gap "0.75rem" :margin-bottom "1rem"}}
          (sidebar/sidebar-mobile-toggle {:on-click toggle-sidebar!})]
         (case active-page
           :components (components-page)
           :calendar   (calendar-page)
           :icons      (icons-page)
           :sidebar    (sidebar-page)
           (components-page))]))))

;; ── Init ────────────────────────────────────────────────────────────

(defn render! []
  (d/render (.getElementById js/document "app") (app)))

(defn ^:export init! []
  (d/set-dispatch! (fn [_ _]))
  (add-watch !page :render (fn [_ _ _ _] (render!)))
  (add-watch !tag-state :render (fn [_ _ _ _] (render!)))
  (add-watch !cal-state :render (fn [_ _ _ _] (render!)))
  (add-watch !lightbox-state :render (fn [_ _ _ _] (render!)))
  (render!))

(defn ^:export reload! []
  (render!))
