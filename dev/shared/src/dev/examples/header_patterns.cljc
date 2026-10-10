(ns dev.examples.header-patterns
  "Composed inspiration: app header bars assembled from button, button-group,
   form-input and icon. Not a component; copy the composition into an app."
  (:require [dev.example :refer [example sx cls]]
            [ui.button :as button]
            [ui.button-group :as button-group]
            [ui.form :as form]))

(def ^:private nav {:display "flex" :align-items "center" :gap "1.5rem"
                    :min-height "3.5rem" :padding "0 1.25rem"})
(def ^:private frame {:border "var(--border-0)" :border-radius "var(--radius-panel)"
                      :background "var(--bg-1)" :overflow "hidden"})
(def ^:private row1 (assoc nav :border-bottom "var(--border-0)"))
(def ^:private spacer [:div {:style (sx {:flex "1"})}])

(def examples
  [(example :photos "Title, boxed icon group, action, primary CTA"
     (fn []
       [:div {:style (sx frame)}
        [:div {:style (sx nav)}
         [:strong {:style (sx {:color "var(--fg-0)" :font-size "var(--font-md)"})} "Photos"]
         spacer
         (button-group/button-group {:variant :boxed}
           (button-group/button-group-item {:icon :grid :active true :attrs {:aria-label "Grid"}})
           (button-group/button-group-item {:icon :layout-dashboard :attrs {:aria-label "Dashboard"}})
           (button-group/button-group-separator {})
           (button-group/button-group-item {:icon :minus :attrs {:aria-label "Smaller"}})
           (button-group/button-group-item {:icon :plus :attrs {:aria-label "Larger"}})
           (button-group/button-group-separator {})
           (button-group/button-group-item {:icon :settings :attrs {:aria-label "Settings"}}))
         (button/button {:variant :ghost :icon :camera :attrs {:aria-label "Take photo"}})
         (button/button {:variant :primary :icon-left :upload} "Upload")]]))

   (example :two-row "Two rows: title and refresh, search and filters"
     (fn []
       [:div {:style (sx frame)}
        [:div {:style (sx row1)}
         [:strong {:style (sx {:color "var(--fg-0)" :font-size "var(--font-md)"})} "Watchlist"]
         [:span {:class (cls "text-sm" "text-muted")} "353 films"]
         spacer
         (button/button {:variant :ghost :icon :refresh :attrs {:aria-label "Refresh"}})]
        [:div {:style (sx nav)}
         [:div {:style (sx {:flex "1"})}
          (form/form-input {:icon-left :search :placeholder "Search all movies…"})]
         (button-group/button-group {:variant :boxed}
           (button-group/button-group-item {:active true :count 353} "All")
           (button-group/button-group-item {:count 28} "Downloaded")
           (button-group/button-group-item {:count 325} "Missing"))]]))])
