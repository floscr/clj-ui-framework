(ns dev.squint
  (:require ["eucalypt" :as eu]
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
            [ui.pagination :as pagination]))

(defn toggle-theme! [_e]
  (let [el (.-documentElement js/document)
        current (.. el -dataset -theme)]
    (set! (.. el -dataset -theme)
          (if (= current "dark") "light" "dark"))))

(defn section [title & children]
  [:section {:style {"margin-bottom" "2.5rem"}}
   [:h3 {:style {"color" "var(--fg-1)" "margin-bottom" "1rem"
                  "border-bottom" "var(--border-0)" "padding-bottom" "0.5rem"}} title]
   (into [:div {:style {"display" "flex" "flex-direction" "column" "gap" "1rem"}}] children)])

;; ── Button ──────────────────────────────────────────────────────────
(def button-variants ["primary" "secondary" "ghost" "danger"])
(def button-sizes ["sm" "md" "lg"])

(defn button-demo []
  (section "Button"
    (into [:div {:style {"display" "flex" "gap" "0.75rem" "flex-wrap" "wrap" "align-items" "center"}}]
          (map (fn [v]
                 (button/button {:variant v :on-click (fn [_] (js/console.log (str "Clicked: " v)))} v))
               button-variants))
    (into [:div {:style {"display" "flex" "gap" "0.75rem" "flex-wrap" "wrap" "align-items" "center"}}]
          (map (fn [s]
                 (button/button {:variant "primary" :size s} (str "size " s)))
               button-sizes))
    (into [:div {:style {"display" "flex" "gap" "0.75rem" "flex-wrap" "wrap" "align-items" "center"}}]
          (map (fn [v]
                 (button/button {:variant v :disabled true} (str v " disabled")))
               button-variants))))

;; ── Alert ───────────────────────────────────────────────────────────
(defn alert-demo []
  (section "Alert"
    (alert/alert {:variant "success" :title "Success!"} "Your changes have been saved.")
    (alert/alert {:variant "warning" :title "Warning!"} "Please review before continuing.")
    (alert/alert {:variant "danger" :title "Error!"} "Something went wrong.")
    (alert/alert {:variant "info" :title "Info"} "This is an informational alert.")
    (alert/alert {:title "Neutral"} "A neutral alert with no variant.")))

;; ── Badge ───────────────────────────────────────────────────────────
(defn badge-demo []
  (section "Badge"
    (into [:div {:style {"display" "flex" "gap" "0.5rem" "flex-wrap" "wrap" "align-items" "center"}}]
          [(badge/badge {} "Default")
           (badge/badge {:variant "secondary"} "Secondary")
           (badge/badge {:variant "outline"} "Outline")
           (badge/badge {:variant "success"} "Success")
           (badge/badge {:variant "warning"} "Warning")
           (badge/badge {:variant "danger"} "Danger")])))

;; ── Card ────────────────────────────────────────────────────────────
(defn card-demo []
  (section "Card"
    (card/card {}
      (card/card-header {} [:h4 "Card Title"] [:p "Card description goes here."])
      (card/card-body {} [:p "This is the card content. It can contain any HTML."])
      (card/card-footer {}
        (button/button {:variant "secondary" :size "sm"} "Cancel")
        (button/button {:variant "primary" :size "sm"} "Save")))))

;; ── Accordion ───────────────────────────────────────────────────────
(defn accordion-demo []
  (section "Accordion"
    (accordion/accordion {:title "What is this framework?"} "A cross-target component library.")
    (accordion/accordion {:title "How do I use it?" :open true} "Just require the namespace and call functions.")
    (accordion/accordion {:title "Is it accessible?"} "Yes, follows ARIA best practices.")))

;; ── Table ───────────────────────────────────────────────────────────
(defn table-demo []
  (section "Table"
    (table/table {:headers ["Name" "Email" "Role" "Status"]
                  :rows [["Alice Johnson" "alice@example.com" "Admin" "Active"]
                         ["Bob Smith" "bob@example.com" "Editor" "Active"]
                         ["Carol White" "carol@example.com" "Viewer" "Pending"]]})))

;; ── Dialog ──────────────────────────────────────────────────────────
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

;; ── Spinner ─────────────────────────────────────────────────────────
(defn spinner-demo []
  (section "Spinner"
    [:div {:style {"display" "flex" "gap" "1.5rem" "align-items" "center"}}
     (spinner/spinner {:size "sm"})
     (spinner/spinner {})
     (spinner/spinner {:size "lg"})]))

;; ── Skeleton ────────────────────────────────────────────────────────
(defn skeleton-demo []
  (section "Skeleton"
    [:div {:style {"max-width" "400px"}}
     (skeleton/skeleton {:variant "heading"})
     (skeleton/skeleton {:variant "line"})
     (skeleton/skeleton {:variant "line"})
     [:div {:style {"display" "flex" "gap" "1rem" "margin-top" "var(--size-3)"}}
      (skeleton/skeleton {:variant "circle"})
      [:div {:style {"flex" "1"}}
       (skeleton/skeleton {:variant "line"})
       (skeleton/skeleton {:variant "line"})]]]))

;; ── Progress ────────────────────────────────────────────────────────
(defn progress-demo []
  (section "Progress"
    (progress/progress {:value 25})
    (progress/progress {:value 50 :variant "success"})
    (progress/progress {:value 75 :variant "warning"})
    (progress/progress {:value 90 :variant "danger"})))

;; ── Switch ──────────────────────────────────────────────────────────
(defn switch-demo []
  (section "Switch"
    [:div {:style {"display" "flex" "flex-direction" "column" "gap" "0.75rem"}}
     (switch/switch-toggle {:label "Notifications" :checked false})
     (switch/switch-toggle {:label "Dark mode" :checked true})
     (switch/switch-toggle {:label "Disabled off" :disabled true})
     (switch/switch-toggle {:label "Disabled on" :checked true :disabled true})]))

;; ── Tooltip ─────────────────────────────────────────────────────────
(defn tooltip-demo []
  (section "Tooltip"
    [:div {:style {"display" "flex" "gap" "1.5rem" "padding-top" "2rem"}}
     (tooltip/tooltip {:text "Save your changes"}
       (button/button {:variant "primary"} "Save"))
     (tooltip/tooltip {:text "Delete this item"}
       (button/button {:variant "danger"} "Delete"))
     (tooltip/tooltip {:text "View profile"}
       [:a {:href "#" :style {"color" "var(--accent)"}} "Profile"])]))

;; ── Breadcrumb ──────────────────────────────────────────────────────
(defn breadcrumb-demo []
  (section "Breadcrumb"
    (breadcrumb/breadcrumb
      {:items [{:label "Home" :href "#"}
               {:label "Projects" :href "#"}
               {:label "Oat Docs" :href "#"}
               {:label "Components"}]})))

;; ── Pagination ──────────────────────────────────────────────────────
(defn pagination-demo []
  (section "Pagination"
    (pagination/pagination {:current 3 :total 5
                            :on-click (fn [p] (js/console.log (str "Page: " p)))})))

;; ── App ─────────────────────────────────────────────────────────────
(defn app []
  [:div {:style {"max-width" "800px" "margin" "0 auto"}}
   [:div {:style {"display" "flex" "justify-content" "space-between" "align-items" "center" "margin-bottom" "2rem"}}
    [:h2 {:style {"margin" "0" "color" "var(--fg-0)"}} "Squint (Eucalypt)"]
    [:button {:on-click toggle-theme!
              :style {"padding" "0.5rem 1rem" "cursor" "pointer" "border-radius" "var(--radius-md)"
                      "border" "var(--border-0)" "background" "var(--bg-1)" "color" "var(--fg-0)"}}
     "Toggle Dark Mode"]]
   (button-demo)
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
   (pagination-demo)])

(defn init! []
  (eu/render (app) (js/document.getElementById "app")))

(defn reload! []
  (eu/render (app) (js/document.getElementById "app")))

(init!)
