(ns dev.replicant
  (:require [replicant.dom :as d]
            [ui.button :as button]))

(def variants [:primary :secondary :ghost :danger])
(def sizes [:sm :md :lg])

(defn button-grid []
  [:div {:style {:display "grid"
                 :grid-template-columns "repeat(4, auto)"
                 :gap "1rem"
                 :align-items "center"}}
   [:div]
   (for [size sizes]
     [:div {:style {:font-weight "600" :text-align "center" :color "var(--fg-1)"
                    :font-size "0.75rem" :text-transform "uppercase" :letter-spacing "0.05em"}}
      (name size)])
   (for [variant variants]
     (list
       [:div {:style {:font-weight "600" :color "var(--fg-1)"
                      :font-size "0.75rem" :text-transform "uppercase" :letter-spacing "0.05em"}}
        (name variant)]
       (for [size sizes]
         [:div {:style {:text-align "center"}}
          (button/button {:variant variant :size size
                          :on-click (fn [_] (js/console.log (str "Clicked: " (name variant) " " (name size))))}
                         (str (name variant) " " (name size)))])))])

(defn disabled-row []
  [:div {:style {:display "flex" :gap "0.75rem" :flex-wrap "wrap"}}
   (for [variant variants]
     (button/button {:variant variant :disabled true}
                    (str (name variant) " disabled")))])

(defn toggle-theme! [_e]
  (let [el (.-documentElement js/document)
        current (.. el -dataset -theme)]
    (set! (.. el -dataset -theme)
          (if (= current "dark") "light" "dark"))))

(defn app []
  [:div {:style {:max-width "800px" :margin "0 auto"}}
   [:div {:style {:display "flex" :justify-content "space-between" :align-items "center" :margin-bottom "2rem"}}
    [:h2 {:style {:margin "0" :color "var(--fg-0)"}} "Replicant (CLJS)"]
    [:button {:on {:click toggle-theme!}
              :style {:padding "0.5rem 1rem" :cursor "pointer" :border-radius "var(--radius-md)"
                      :border "var(--border-0)" :background "var(--bg-1)" :color "var(--fg-0)"}}
     "Toggle Dark Mode"]]
   [:h3 {:style {:color "var(--fg-1)" :margin-bottom "1rem"}} "Button Grid"]
   (button-grid)
   [:h3 {:style {:color "var(--fg-1)" :margin "2rem 0 1rem"}} "Disabled States"]
   (disabled-row)])

(defn ^:export init! []
  (d/set-dispatch! (fn [_ _]))
  (d/render (.getElementById js/document "app") (app)))

(defn ^:export reload! []
  (d/render (.getElementById js/document "app") (app)))
