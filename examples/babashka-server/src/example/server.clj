(ns example.server
  (:require [org.httpkit.server :as http]
            [hiccup2.core :as h]
            [ui.css.gen :as css]
            [ui.button :as button]
            [ui.alert :as alert]
            [ui.badge :as badge]
            [ui.card :as card]
            [ui.table :as table]
            [ui.progress :as progress]
            [ui.separator :as separator]
            [ui.spinner :as spinner]
            [ui.form :as form]))

;; ── CSS generated once at boot ──────────────────────────────────────

(def theme-css
  "Bundled CSS string — generated at startup from framework defaults."
  (css/build-css))

;; To customize the theme, pass overrides:
;; (def theme-css
;;   (css/build-css {:scales {:color {:accent {:hue 220}}}   ; blue accent
;;                   :tokens {:accent "var(--accent-600)"}})) ; darker shade

;; ── Page ────────────────────────────────────────────────────────────

(defn page []
  (str
    "<!DOCTYPE html>\n"
    (h/html
      [:html
       [:head
        [:meta {:charset "utf-8"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
        [:style (h/raw theme-css)]]
       [:body
        [:div {:style "max-width: 720px; margin: 0 auto; padding: 2rem;"}

         ;; Header
         [:h1 "Example App"]
         [:p {:style "color: var(--fg-1); margin-bottom: 2rem;"}
          "A babashka server using clj-ui-framework as a git dep. "
          "CSS is generated at boot — no build step, no static files."]

         (separator/separator {})

         ;; Alerts
         [:div {:style "margin: 1.5rem 0; display: flex; flex-direction: column; gap: 0.75rem;"}
          (alert/alert {:variant :success :title "Ready"}
            "Server is running with framework CSS generated at startup.")
          (alert/alert {:variant :info :title "Tip"}
            "Edit the theme overrides in server.clj and restart to see changes.")]

         ;; Buttons
         [:section {:style "margin: 2rem 0;"}
          [:h2 "Buttons"]
          [:div {:style "display: flex; gap: 0.75rem; flex-wrap: wrap; align-items: center;"}
           (button/button {:variant :primary} "Primary")
           (button/button {:variant :secondary} "Secondary")
           (button/button {:variant :ghost} "Ghost")
           (button/button {:variant :danger} "Danger")
           (button/button {:variant :primary :icon :plus})]]

         ;; Badges
         [:section {:style "margin: 2rem 0;"}
          [:h2 "Badges"]
          [:div {:style "display: flex; gap: 0.5rem; flex-wrap: wrap; align-items: center;"}
           (badge/badge {} "Default")
           (badge/badge {:variant :success} "Active")
           (badge/badge {:variant :warning} "Pending")
           (badge/badge {:variant :danger} "Expired")
           (badge/badge {:variant :outline :icon-name :star} "Featured")]]

         ;; Card
         [:section {:style "margin: 2rem 0;"}
          [:h2 "Card"]
          (card/card {}
            (card/card-header {}
              [:h3 {:style "margin: 0;"} "Project Status"]
              [:p {:style "margin: 0; color: var(--fg-2);"} "Sprint 14 overview"])
            (card/card-body {}
              [:div {:style "display: flex; flex-direction: column; gap: 1rem;"}
               [:div
                [:div {:style "display: flex; justify-content: space-between; margin-bottom: 0.25rem;"}
                 [:span "Progress"] [:span {:style "color: var(--fg-2);"} "72%"]]
                (progress/progress {:value 72 :variant :success})]
               [:div
                [:div {:style "display: flex; justify-content: space-between; margin-bottom: 0.25rem;"}
                 [:span "Bug fixes"] [:span {:style "color: var(--fg-2);"} "45%"]]
                (progress/progress {:value 45 :variant :warning})]])
            (card/card-footer {}
              (button/button {:variant :secondary :size :sm} "Details")
              (button/button {:variant :primary :size :sm} "Continue")))]

         ;; Table
         [:section {:style "margin: 2rem 0;"}
          [:h2 "Table"]
          (table/table {:headers ["Name" "Role" "Status"]
                        :rows [["Alice Johnson" "Engineering" "Active"]
                               ["Bob Chen" "Design" "Active"]
                               ["Carol Santos" "Product" "On leave"]]})]

         ;; Form
         [:section {:style "margin: 2rem 0;"}
          [:h2 "Form"]
          [:form {:style "max-width: 400px;"}
           (form/form-field {:label "Name"}
             (form/form-input {:type :text :placeholder "Your name"}))
           (form/form-field {:label "Email"}
             (form/form-input {:type :email :placeholder "you@example.com"}))
           (form/form-field {:label "Role"}
             (form/form-select {:placeholder "Select a role"
                                :options [{:value "eng" :label "Engineering"}
                                          {:value "design" :label "Design"}
                                          {:value "product" :label "Product"}]}))
           (form/form-checkbox {:label "Send me notifications"})
           (button/button {:variant :primary :attrs {:type "submit"}} "Save")]]

         ;; Footer
         (separator/separator {})
         [:p {:style "color: var(--fg-2); font-size: var(--font-sm); margin-top: 1rem;"}
          "Built with "
          [:a {:href "https://github.com/floscr/clj-ui-framework"
               :style "color: var(--accent);"}
           "clj-ui-framework"]
          " · CSS generated at boot via "
          [:code "ui.css.gen/build-css"]]]]])))

;; ── Server ──────────────────────────────────────────────────────────

(defn handler [{:keys [uri]}]
  (if (= uri "/")
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (page)}
    {:status 404
     :headers {"Content-Type" "text/plain"}
     :body "Not found"}))

(defn start! [{:keys [port] :or {port 8090}}]
  (println (str "Generating CSS from framework..."))
  (println (str "CSS: " (count theme-css) " chars"))
  (println (str "Server running at http://localhost:" port))
  (http/run-server #'handler {:port port}))
