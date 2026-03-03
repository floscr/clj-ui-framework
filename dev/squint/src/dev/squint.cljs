(ns dev.squint
  (:require ["eucalypt" :as eu]
            [ui.button :as button]))

(def variants ["primary" "secondary" "ghost" "danger"])
(def sizes ["sm" "md" "lg"])

(defn toggle-theme! [_e]
  (let [el (.-documentElement js/document)
        current (.. el -dataset -theme)]
    (set! (.. el -dataset -theme)
          (if (= current "dark") "light" "dark"))))

(def label-style {"font-weight" "600"
                  "color" "var(--fg-1)"
                  "font-size" "0.75rem"
                  "text-transform" "uppercase"
                  "letter-spacing" "0.05em"})

(defn button-grid []
  (into
    [:div {:style {"display" "grid"
                   "grid-template-columns" "repeat(4, auto)"
                   "gap" "1rem"
                   "align-items" "center"}}
     [:div]
     [:div {:style (merge label-style {"text-align" "center"})} "sm"]
     [:div {:style (merge label-style {"text-align" "center"})} "md"]
     [:div {:style (merge label-style {"text-align" "center"})} "lg"]]
    (mapcat (fn [variant]
              [[:div {:style label-style} variant]
               [:div {:style {"text-align" "center"}}
                (button/button {:variant variant :size "sm"
                                :on-click (fn [_] (js/console.log (str "Clicked: " variant " sm")))}
                               (str variant " sm"))]
               [:div {:style {"text-align" "center"}}
                (button/button {:variant variant :size "md"
                                :on-click (fn [_] (js/console.log (str "Clicked: " variant " md")))}
                               (str variant " md"))]
               [:div {:style {"text-align" "center"}}
                (button/button {:variant variant :size "lg"
                                :on-click (fn [_] (js/console.log (str "Clicked: " variant " lg")))}
                               (str variant " lg"))]])
            variants)))

(defn disabled-row []
  (into
    [:div {:style {"display" "flex" "gap" "0.75rem" "flex-wrap" "wrap"}}]
    (map (fn [variant]
           (button/button {:variant variant :disabled true}
                          (str variant " disabled")))
         variants)))

(defn app []
  [:div {:style {"max-width" "800px" "margin" "0 auto"}}
   [:div {:style {"display" "flex" "justify-content" "space-between" "align-items" "center" "margin-bottom" "2rem"}}
    [:h2 {:style {"margin" "0" "color" "var(--fg-0)"}} "Squint (Eucalypt)"]
    [:button {:on-click toggle-theme!
              :style {"padding" "0.5rem 1rem" "cursor" "pointer" "border-radius" "var(--radius-md)"
                      "border" "var(--border-0)" "background" "var(--bg-1)" "color" "var(--fg-0)"}}
     "Toggle Dark Mode"]]
   [:h3 {:style {"color" "var(--fg-1)" "margin-bottom" "1rem"}} "Button Grid"]
   (button-grid)
   [:h3 {:style {"color" "var(--fg-1)" "margin" "2rem 0 1rem"}} "Disabled States"]
   (disabled-row)])

(defn init! []
  (eu/render (app) (js/document.getElementById "app")))

(defn reload! []
  (eu/render (app) (js/document.getElementById "app")))

(init!)
