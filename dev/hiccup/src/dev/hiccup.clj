(ns dev.hiccup
  (:require [org.httpkit.server :as http]
            [hiccup2.core :as h]
            [ui.button :as button]))

(def variants [:primary :secondary :ghost :danger])
(def sizes [:sm :md :lg])

(defn button-grid []
  [:div {:style "display: grid; grid-template-columns: repeat(4, auto); gap: 1rem; align-items: center;"}
   ;; Header row
   [:div]
   (for [size sizes]
     [:div {:style "font-weight: 600; text-align: center; color: var(--fg-1); font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.05em;"}
      (name size)])

   ;; Variant rows
   (for [variant variants]
     (list
       [:div {:style "font-weight: 600; color: var(--fg-1); font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.05em;"}
        (name variant)]
       (for [size sizes]
         [:div {:style "text-align: center;"}
          (button/button {:variant variant :size size}
                         (str (name variant) " " (name size)))])))])

(defn disabled-row []
  [:div {:style "display: flex; gap: 0.75rem; flex-wrap: wrap;"}
   (for [variant variants]
     (button/button {:variant variant :disabled true}
                    (str (name variant) " disabled")))])

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
         [:h3 {:style "color: var(--fg-1); margin-bottom: 1rem;"} "Button Grid"]
         (button-grid)
         [:h3 {:style "color: var(--fg-1); margin: 2rem 0 1rem;"} "Disabled States"]
         (disabled-row)]]])))

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
