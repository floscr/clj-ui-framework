(ns recipes.tasks
  (:require [clojure.string :as str]
            [ui.badge :as badge]
            [ui.button :as button]
            [ui.card :as card]
            [ui.form :as form]
            [ui.icon :as icon]
            [ui.separator :as separator]))

;; In squint, keywords are strings — name is identity
#?(:squint (defn- kw-name [s] s)
   :cljs   (defn- kw-name [s] (name s))
   :clj    (defn- kw-name [s] (name s)))

;; ── Data Model ──────────────────────────────────────────────────────

(def statuses
  [{:value "backlog"     :label "Backlog"     :icon :inbox}
   {:value "todo"        :label "Todo"        :icon :alert-circle}
   {:value "in-progress" :label "In Progress" :icon :clock}
   {:value "done"        :label "Done"        :icon :circle-check}
   {:value "canceled"    :label "Canceled"    :icon :circle-x}])

(def priorities
  [{:value "low"    :label "Low"    :icon :arrow-down}
   {:value "medium" :label "Medium" :icon :arrow-right}
   {:value "high"   :label "High"   :icon :arrow-up}])

(def labels
  [{:value "bug"           :label "Bug"}
   {:value "feature"       :label "Feature"}
   {:value "documentation" :label "Documentation"}])

(defn- find-by-value [coll v]
  (first (filter #(= (:value %) v) coll)))

;; ── Sample Data ─────────────────────────────────────────────────────

(def sample-tasks
  [{:id "TASK-8782" :label "documentation" :title "You can't compress the program without quantifying the open-source SSD pixel!"       :status "in-progress" :priority "medium"}
   {:id "TASK-7878" :label "documentation" :title "Try to calculate the EXE feed, maybe it will index the multi-byte pixel!"            :status "backlog"     :priority "medium"}
   {:id "TASK-7839" :label "bug"           :title "We need to bypass the neural TCP card!"                                              :status "todo"        :priority "high"}
   {:id "TASK-5562" :label "feature"       :title "The SAS interface is down, bypass the open-source pixel so we can back up the PNG bandwidth!" :status "backlog" :priority "medium"}
   {:id "TASK-8686" :label "feature"       :title "I'll parse the wireless SSL protocol, that should driver the API panel!"             :status "canceled"    :priority "medium"}
   {:id "TASK-1280" :label "bug"           :title "Use the digital TLS panel, then you can transmit the haptic system!"                 :status "done"        :priority "high"}
   {:id "TASK-7262" :label "feature"       :title "The UTF8 application is down, parse the neural bandwidth so we can back up the PNG firewall!" :status "done" :priority "high"}
   {:id "TASK-1138" :label "feature"       :title "Generating the driver won't do anything, we need to quantify the 1080p SMTP bandwidth!" :status "in-progress" :priority "medium"}
   {:id "TASK-7184" :label "feature"       :title "We need to program the back-end THX pixel!"                                         :status "todo"        :priority "low"}
   {:id "TASK-5160" :label "documentation" :title "Calculating the bus won't do anything, we need to navigate the back-end JSON protocol!" :status "in-progress" :priority "high"}
   {:id "TASK-5618" :label "documentation" :title "Generating the driver won't do anything, we need to index the online SSL application!" :status "done"       :priority "medium"}
   {:id "TASK-6699" :label "documentation" :title "I'll transmit the wireless JBOD capacitor, that should hard drive the SSD feed!"     :status "backlog"     :priority "medium"}
   {:id "TASK-2858" :label "bug"           :title "We need to override the online UDP bus!"                                             :status "backlog"     :priority "medium"}
   {:id "TASK-9864" :label "bug"           :title "I'll reboot the 1080p FTP panel, that should matrix the HEX hard drive!"             :status "done"        :priority "high"}
   {:id "TASK-8404" :label "bug"           :title "We need to generate the virtual HEX alarm!"                                         :status "in-progress" :priority "low"}
   {:id "TASK-5365" :label "documentation" :title "Backing up the pixel won't do anything, we need to transmit the primary IB array!"   :status "in-progress" :priority "low"}
   {:id "TASK-1780" :label "documentation" :title "The CSS feed is down, index the bluetooth transmitter so we can compress the CLI protocol!" :status "todo" :priority "high"}
   {:id "TASK-6938" :label "documentation" :title "Use the redundant SCSI application, then you can hack the optical alarm!"            :status "todo"        :priority "high"}
   {:id "TASK-9885" :label "bug"           :title "We need to compress the auxiliary VGA driver!"                                       :status "backlog"     :priority "high"}
   {:id "TASK-3216" :label "documentation" :title "Transmitting the transmitter won't do anything, we need to compress the virtual HDD sensor!" :status "backlog" :priority "medium"}])

;; ── Sub-components ──────────────────────────────────────────────────

(defn- status-display
  "Render a status value with its icon."
  [status-value]
  (let [{:keys [label icon]} (find-by-value statuses status-value)]
    #?(:squint
       [:span {:class "tasks-status"}
        (icon/icon {:icon-name icon :size :sm})
        (or label status-value)]

       :cljs
       [:span {:class ["tasks-status"]}
        (icon/icon {:icon-name icon :size :sm})
        (or label status-value)]

       :clj
       [:span {:class "tasks-status"}
        (icon/icon {:icon-name icon :size :sm})
        (or label status-value)])))

(defn- priority-display
  "Render a priority value with its icon."
  [priority-value]
  (let [{:keys [label icon]} (find-by-value priorities priority-value)]
    #?(:squint
       [:span {:class "tasks-priority"}
        (icon/icon {:icon-name icon :size :sm})
        (or label priority-value)]

       :cljs
       [:span {:class ["tasks-priority"]}
        (icon/icon {:icon-name icon :size :sm})
        (or label priority-value)]

       :clj
       [:span {:class "tasks-priority"}
        (icon/icon {:icon-name icon :size :sm})
        (or label priority-value)])))

(defn- label-badge
  "Render a label as a badge."
  [label-value]
  (badge/badge {:variant :outline :size :sm} label-value))

;; ── Toolbar ─────────────────────────────────────────────────────────

(defn tasks-toolbar
  "Render the filter/search toolbar above the tasks table."
  [{:keys [#?@(:squint [] :cljs [on-search] :clj [])]}]
  #?(:squint
     [:div {:class "tasks-toolbar"}
      [:div {:class "tasks-toolbar-left"}
       (form/form-input {:type "text"
                         :placeholder "Filter tasks..."
                         :class "tasks-search"
                         :attrs {:style {"max-width" "16rem"}}})]
      [:div {:class "tasks-toolbar-right"}
       (button/button {:variant "secondary" :size "sm"}
         (icon/icon {:icon-name "plus" :size "sm"})
         "Add Task")]]

     :cljs
     [:div {:class ["tasks-toolbar"]}
      [:div {:class ["tasks-toolbar-left"]}
       (form/form-input {:type :text
                         :placeholder "Filter tasks..."
                         :class "tasks-search"
                         :on-change on-search
                         :attrs {:style {:max-width "16rem"}}})]
      [:div {:class ["tasks-toolbar-right"]}
       (button/button {:variant :secondary :size :sm}
         (icon/icon {:icon-name :plus :size :sm})
         "Add Task")]]

     :clj
     [:div {:class "tasks-toolbar"}
      [:div {:class "tasks-toolbar-left"}
       (form/form-input {:type :text
                         :placeholder "Filter tasks..."
                         :class "tasks-search"
                         :attrs {:style "max-width: 16rem;"}})]
      [:div {:class "tasks-toolbar-right"}
       (button/button {:variant :secondary :size :sm}
         (icon/icon {:icon-name :plus :size :sm})
         "Add Task")]]))

;; ── Task Row ────────────────────────────────────────────────────────

(defn- task-row
  "Render a single task as a table row."
  [{:keys [id label title status priority]}]
  #?(:squint
     [:tr
      [:td {:class "tasks-cell-checkbox"}
       [:input {:type "checkbox" :class "form-checkbox" :aria-label (str "Select task " id)}]]
      [:td {:class "tasks-cell-id font-mono text-muted"} id]
      [:td {:class "tasks-cell-label"} (when label (label-badge label))]
      [:td {:class "tasks-cell-title"}
       [:span {:class "tasks-title-text"} title]]
      [:td {:class "tasks-cell-status"} (status-display status)]
      [:td {:class "tasks-cell-priority"} (priority-display priority)]
      [:td {:class "tasks-cell-actions"}
       (button/button {:variant "ghost" :size "sm" :class "btn-icon"}
         (icon/icon {:icon-name "menu" :size "sm"}))]]

     :cljs
     [:tr
      [:td {:class ["tasks-cell-checkbox"]}
       [:input {:type "checkbox" :class ["form-checkbox"] :aria-label (str "Select task " id)}]]
      [:td {:class ["tasks-cell-id" "font-mono" "text-muted"]} id]
      [:td {:class ["tasks-cell-label"]} (when label (label-badge label))]
      [:td {:class ["tasks-cell-title"]}
       [:span {:class ["tasks-title-text"]} title]]
      [:td {:class ["tasks-cell-status"]} (status-display status)]
      [:td {:class ["tasks-cell-priority"]} (priority-display priority)]
      [:td {:class ["tasks-cell-actions"]}
       (button/button {:variant :ghost :size :sm :class "btn-icon"}
         (icon/icon {:icon-name :menu :size :sm}))]]

     :clj
     [:tr
      [:td {:class "tasks-cell-checkbox"}
       [:input {:type "checkbox" :class "form-checkbox" :aria-label (str "Select task " id)}]]
      [:td {:class "tasks-cell-id font-mono text-muted"} id]
      [:td {:class "tasks-cell-label"} (when label (label-badge label))]
      [:td {:class "tasks-cell-title"}
       [:span {:class "tasks-title-text"} title]]
      [:td {:class "tasks-cell-status"} (status-display status)]
      [:td {:class "tasks-cell-priority"} (priority-display priority)]
      [:td {:class "tasks-cell-actions"}
       (button/button {:variant :ghost :size :sm :class "btn-icon"}
         (icon/icon {:icon-name :menu :size :sm}))]]))

;; ── Tasks Table ─────────────────────────────────────────────────────

(defn tasks-table
  "Render the full tasks data table."
  [{:keys [tasks]}]
  (let [task-list (or tasks sample-tasks)]
    #?(:squint
       [:div {:class "table-wrapper"}
        [:table {:class "table tasks-table"}
         [:thead
          [:tr
           [:th {:class "tasks-cell-checkbox"}
            [:input {:type "checkbox" :class "form-checkbox" :aria-label "Select all tasks"}]]
           [:th "Task"]
           [:th "Label"]
           [:th "Title"]
           [:th "Status"]
           [:th "Priority"]
           [:th {:class "tasks-cell-actions"}]]]
         (into [:tbody]
               (map task-row task-list))]]

       :cljs
       [:div {:class ["table-wrapper"]}
        [:table {:class ["table" "tasks-table"]}
         [:thead
          [:tr
           [:th {:class ["tasks-cell-checkbox"]}
            [:input {:type "checkbox" :class ["form-checkbox"] :aria-label "Select all tasks"}]]
           [:th "Task"]
           [:th "Label"]
           [:th "Title"]
           [:th "Status"]
           [:th "Priority"]
           [:th {:class ["tasks-cell-actions"]}]]]
         (into [:tbody]
               (map task-row task-list))]]

       :clj
       [:div {:class "table-wrapper"}
        [:table {:class "table tasks-table"}
         [:thead
          [:tr
           [:th {:class "tasks-cell-checkbox"}
            [:input {:type "checkbox" :class "form-checkbox" :aria-label "Select all tasks"}]]
           [:th "Task"]
           [:th "Label"]
           [:th "Title"]
           [:th "Status"]
           [:th "Priority"]
           [:th {:class "tasks-cell-actions"}]]]
         (into [:tbody]
               (map task-row task-list))]])))

;; ── Footer ──────────────────────────────────────────────────────────

(defn tasks-footer
  "Render the table footer with row count and pagination."
  [{:keys [tasks page per-page]}]
  (let [task-list  (or tasks sample-tasks)
        total      (count task-list)
        pp         (or per-page 10)
        current    (or page 1)
        total-pages (max 1 (int (Math/ceil (/ total pp))))]
    #?(:squint
       [:div {:class "tasks-footer"}
        [:span {:class "text-sm text-muted"}
         (str "0 of " total " row(s) selected.")]
        [:div {:class "tasks-footer-right"}
         [:span {:class "text-sm"}
          (str "Page " current " of " total-pages)]
         [:div {:class "tasks-pagination"}
          (button/button {:variant "secondary" :size "sm" :class "btn-icon"
                          :disabled (= current 1)}
            (icon/icon {:icon-name "chevron-left" :size "sm"}))
          (button/button {:variant "secondary" :size "sm" :class "btn-icon"
                          :disabled (= current total-pages)}
            (icon/icon {:icon-name "chevron-right" :size "sm"}))]]]

       :cljs
       [:div {:class ["tasks-footer"]}
        [:span {:class ["text-sm" "text-muted"]}
         (str "0 of " total " row(s) selected.")]
        [:div {:class ["tasks-footer-right"]}
         [:span {:class ["text-sm"]}
          (str "Page " current " of " total-pages)]
         [:div {:class ["tasks-pagination"]}
          (button/button {:variant :secondary :size :sm :class "btn-icon"
                          :disabled (= current 1)}
            (icon/icon {:icon-name :chevron-left :size :sm}))
          (button/button {:variant :secondary :size :sm :class "btn-icon"
                          :disabled (= current total-pages)}
            (icon/icon {:icon-name :chevron-right :size :sm}))]]]

       :clj
       [:div {:class "tasks-footer"}
        [:span {:class "text-sm text-muted"}
         (str "0 of " total " row(s) selected.")]
        [:div {:class "tasks-footer-right"}
         [:span {:class "text-sm"}
          (str "Page " current " of " total-pages)]
         [:div {:class "tasks-pagination"}
          (button/button {:variant :secondary :size :sm :class "btn-icon"
                          :disabled (= current 1)}
            (icon/icon {:icon-name :chevron-left :size :sm}))
          (button/button {:variant :secondary :size :sm :class "btn-icon"
                          :disabled (= current total-pages)}
            (icon/icon {:icon-name :chevron-right :size :sm}))]]])))

;; ── Full Page ───────────────────────────────────────────────────────

(defn tasks-page
  "Render the complete tasks list recipe page.
   Composes: card, badge, button, form-input, icon, table."
  [{:keys [tasks] :as opts}]
  (let [task-list (or tasks sample-tasks)]
    #?(:squint
       [:div {:class "tasks-page"}
        (card/card {}
          (card/card-header {}
            [:h3 {:class "tasks-heading"} "Welcome back!"]
            [:p {:class "text-sm text-muted"} "Here's a list of your tasks for this month."])
          (card/card-body {}
            (tasks-toolbar {})
            (tasks-table {:tasks task-list})
            (tasks-footer {:tasks task-list})))]

       :cljs
       [:div {:class ["tasks-page"]}
        (card/card {}
          (card/card-header {}
            [:h3 {:class ["tasks-heading"]} "Welcome back!"]
            [:p {:class ["text-sm" "text-muted"]} "Here's a list of your tasks for this month."])
          (card/card-body {}
            (tasks-toolbar {})
            (tasks-table {:tasks task-list})
            (tasks-footer {:tasks task-list})))]

       :clj
       [:div {:class "tasks-page"}
        (card/card {}
          (card/card-header {}
            [:h3 {:class "tasks-heading"} "Welcome back!"]
            [:p {:class "text-sm text-muted"} "Here's a list of your tasks for this month."])
          (card/card-body {}
            (tasks-toolbar {})
            (tasks-table {:tasks task-list})
            (tasks-footer {:tasks task-list})))])))
