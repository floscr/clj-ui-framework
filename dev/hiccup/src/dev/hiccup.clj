(ns dev.hiccup
  (:require [org.httpkit.server :as http]
            [hiccup2.core :as h]
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
            [ui.icon :as icon]))

(defn section [title & children]
  [:section {:style "margin-bottom: 2.5rem;"}
   [:h3 {:style "color: var(--fg-1); margin-bottom: 1rem; border-bottom: var(--border-0); padding-bottom: 0.5rem;"} title]
   (into [:div {:style "display: flex; flex-direction: column; gap: 1rem;"}] children)])

;; ── Button ──────────────────────────────────────────────────────────
(def button-variants [:primary :secondary :ghost :danger])
(def button-sizes [:sm :md :lg])

(defn button-demo []
  (section "Button"
    [:div {:style "display: flex; gap: 0.75rem; flex-wrap: wrap; align-items: center;"}
     (for [v button-variants]
       (button/button {:variant v} (name v)))]
    [:div {:style "display: flex; gap: 0.75rem; flex-wrap: wrap; align-items: center;"}
     (for [s button-sizes]
       (button/button {:variant :primary :size s} (str "size " (name s))))]
    [:div {:style "display: flex; gap: 0.75rem; flex-wrap: wrap; align-items: center;"}
     (for [v button-variants]
       (button/button {:variant v :disabled true} (str (name v) " disabled")))]
    [:div {:style "display: flex; gap: 0.75rem; flex-wrap: wrap; align-items: center;"}
     (button/button {:variant :primary :href "#"} "Link primary")
     (button/button {:variant :secondary :href "#"} "Link secondary")
     (button/button {:variant :link} "Link button")
     (button/button {:variant :link :href "https://example.com"} "Link with href")]))

;; ── Alert ───────────────────────────────────────────────────────────
(defn alert-demo []
  (section "Alert"
    (alert/alert {:variant :success :title "Success!"} "Your changes have been saved.")
    (alert/alert {:variant :warning :title "Warning!"} "Please review before continuing.")
    (alert/alert {:variant :danger :title "Error!"} "Something went wrong.")
    (alert/alert {:variant :info :title "Info"} "This is an informational alert.")
    (alert/alert {:title "Neutral"} "A neutral alert with no variant.")))

;; ── Badge ───────────────────────────────────────────────────────────
(defn badge-demo []
  (section "Badge"
    [:div {:style "display: flex; gap: 0.5rem; flex-wrap: wrap; align-items: center;"}
     (badge/badge {} "Default")
     (badge/badge {:variant :secondary} "Secondary")
     (badge/badge {:variant :outline} "Outline")
     (badge/badge {:variant :success} "Success")
     (badge/badge {:variant :warning} "Warning")
     (badge/badge {:variant :danger} "Danger")]))

;; ── Card ────────────────────────────────────────────────────────────
(defn card-demo []
  (section "Card"
    (card/card {}
      (card/card-header {} [:h4 "Card Title"] [:p "Card description goes here."])
      (card/card-body {} [:p "This is the card content. It can contain any HTML."])
      (card/card-footer {}
        (button/button {:variant :secondary :size :sm} "Cancel")
        (button/button {:variant :primary :size :sm} "Save")))))

;; ── Accordion ───────────────────────────────────────────────────────
(defn accordion-demo []
  (section "Accordion"
    [:div {:class "accordion-group"}
     (accordion/accordion {:title "What is this framework?"} "A cross-target component library.")
     (accordion/accordion {:title "How do I use it?" :open true} "Just require the namespace and call functions.")
     (accordion/accordion {:title "Is it accessible?"} "Yes, follows ARIA best practices.")]))

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

;; ── Spinner ─────────────────────────────────────────────────────────
(defn spinner-demo []
  (section "Spinner"
    [:div {:style "display: flex; gap: 1.5rem; align-items: center;"}
     (spinner/spinner {:size :sm})
     (spinner/spinner {})
     (spinner/spinner {:size :lg})]))

;; ── Skeleton ────────────────────────────────────────────────────────
(defn skeleton-demo []
  (section "Skeleton"
    [:div {:style "max-width: 400px;"}
     (skeleton/skeleton {:variant :heading})
     (skeleton/skeleton {:variant :line})
     (skeleton/skeleton {:variant :line})
     [:div {:style "display: flex; gap: 1rem; margin-top: var(--size-3);"}
      (skeleton/skeleton {:variant :circle})
      [:div {:style "flex: 1;"}
       (skeleton/skeleton {:variant :line})
       (skeleton/skeleton {:variant :line})]]]))

;; ── Progress ────────────────────────────────────────────────────────
(defn progress-demo []
  (section "Progress"
    (progress/progress {:value 25})
    (progress/progress {:value 50 :variant :success})
    (progress/progress {:value 75 :variant :warning})
    (progress/progress {:value 90 :variant :danger})))

;; ── Switch ──────────────────────────────────────────────────────────
(defn switch-demo []
  (section "Switch"
    [:div {:style "display: flex; flex-direction: column; gap: 0.75rem;"}
     (switch/switch-toggle {:label "Notifications" :checked false})
     (switch/switch-toggle {:label "Dark mode" :checked true})
     (switch/switch-toggle {:label "Disabled off" :disabled true})
     (switch/switch-toggle {:label "Disabled on" :checked true :disabled true})]))

;; ── Tooltip ─────────────────────────────────────────────────────────
(defn tooltip-demo []
  (section "Tooltip"
    [:div {:style "display: flex; gap: 1.5rem; padding-top: 2rem;"}
     (tooltip/tooltip {:text "Save your changes"}
       (button/button {:variant :primary} "Save"))
     (tooltip/tooltip {:text "Delete this item"}
       (button/button {:variant :danger} "Delete"))
     (tooltip/tooltip {:text "View profile"}
       [:a {:href "#" :style "color: var(--accent);"} "Profile"])]))

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
                            :href-fn (fn [p] (str "#page-" p))})))

;; ── Form ────────────────────────────────────────────────────────
(defn form-demo []
  (section "Form"
    [:form {:style "max-width: 480px;"}
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
     (form/form-field {:label "Disabled"}
       (form/form-input {:type :text :placeholder "Disabled" :disabled true}))
     (form/form-field {:label "File"}
       (form/form-file {}))
     (form/form-field {:label "Date and time"}
       (form/form-input {:type :datetime-local}))
     (form/form-field {:label "Date"}
       (form/form-input {:type :date}))
     (form/form-checkbox {:label "I agree to the terms"})
     (form/form-radio-group {:label "Preference"
                              :radio-name "pref"
                              :options [{:value "a" :label "Option A"}
                                        {:value "b" :label "Option B"}
                                        {:value "c" :label "Option C"}]})
     (form/form-field {:label "Volume"}
       (form/form-range {:min 0 :max 100 :value 50}))
     (button/button {:variant :primary :attrs {:type "submit"}} "Submit")]
    ;; Input group
    [:div {:style "max-width: 480px; margin-top: 1.5rem;"}
     [:h4 {:style "margin-bottom: 0.75rem;"} "Input group"]
     (form/form-group {}
       (form/form-group-addon {} "https://")
       (form/form-input {:placeholder "subdomain"})
       (button/button {:variant :primary :size :sm} "Go"))]
    ;; Validation error
    [:div {:style "max-width: 480px; margin-top: 1.5rem;"}
     [:h4 {:style "margin-bottom: 0.75rem;"} "Validation error"]
     (form/form-field {:label "Email" :error "Please enter a valid email address."}
       (form/form-input {:type :email :error true :value "invalid-email"}))]))

;; ── Icon ─────────────────────────────────────────────────────────
(defn icon-demo []
  (section "Icon"
    [:div {:style "display: flex; gap: 1.5rem; flex-wrap: wrap; align-items: center;"}
     (for [n [:home :search :settings :user :mail :bell :calendar :star
              :file :folder :code :terminal :globe :shield :zap :heart
              :plus :minus :check :edit :trash :download :upload :copy
              :eye :lock :bookmark :inbox :database :map-pin]]
       [:div {:style "display: flex; flex-direction: column; align-items: center; gap: 0.25rem;"}
        (icon/icon {:icon-name n})
        [:span {:style "font-size: var(--font-xs); color: var(--fg-2);"} (name n)]])]
    [:div {:style "display: flex; gap: 1rem; align-items: center; margin-top: 0.5rem;"}
     [:span {:style "font-size: var(--font-xs); color: var(--fg-2);"} "Sizes:"]
     (icon/icon {:icon-name :star :size :sm})
     (icon/icon {:icon-name :star})
     (icon/icon {:icon-name :star :size :lg})
     (icon/icon {:icon-name :star :size :xl})]))

;; ── Sidebar ─────────────────────────────────────────────────────────
(defn sidebar-demo []
  (section "Sidebar"
    (sidebar/sidebar-layout {}
      (sidebar/sidebar {}
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
        [:div {:style "padding: 2rem;"}
         [:h2 {:style "margin: 0 0 1rem; color: var(--fg-0);"} "Dashboard"]
         [:div {:style "display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem;"}
          [:div {:style "aspect-ratio: 16/9; background: var(--bg-1); border-radius: var(--radius-lg); border: var(--border-0);"}]
          [:div {:style "aspect-ratio: 16/9; background: var(--bg-1); border-radius: var(--radius-lg); border: var(--border-0);"}]
          [:div {:style "aspect-ratio: 16/9; background: var(--bg-1); border-radius: var(--radius-lg); border: var(--border-0);"}]]
         [:div {:style "margin-top: 1rem; min-height: 200px; background: var(--bg-1); border-radius: var(--radius-lg); border: var(--border-0);"}]]))))

;; ── Page ────────────────────────────────────────────────────────────
(defn page []
  (str
    "<!DOCTYPE html>\n"
    (h/html
      [:html
       [:head
        [:meta {:charset "utf-8"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
        [:link {:rel "stylesheet" :href "/theme.css"}]
        [:style (h/raw "body { padding: 2rem; }")]]
       [:body
        [:div {:style "max-width: 800px; margin: 0 auto;"}
         [:div {:style "display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem;"}
          [:h2 {:style "margin: 0; color: var(--fg-0);"} "Hiccup (Backend)"]
          [:button {:onclick "document.documentElement.dataset.theme = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark'"
                    :style "padding: 0.5rem 1rem; cursor: pointer; border-radius: var(--radius-md); border: var(--border-0); background: var(--bg-1); color: var(--fg-0);"}
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
         (pagination-demo)
         (form-demo)
         (icon-demo)]
        (sidebar-demo)]])))

(defn handler [{:keys [uri]}]
  (case uri
    "/" {:status 200
        :headers {"Content-Type" "text/html; charset=utf-8"}
        :body (page)}
    "/theme.css" {:status 200
                  :headers {"Content-Type" "text/css"}
                  :body (slurp "dist/theme.css")}
    {:status 404
     :headers {"Content-Type" "text/plain"}
     :body "Not found"}))

(defn start! [{:keys [port] :or {port 3003}}]
  (println (str "Hiccup server running at http://localhost:" port))
  (http/run-server handler {:port port}))
