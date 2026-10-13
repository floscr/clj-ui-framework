(ns dev.examples.icon
  (:require [dev.example :refer [example sx]]
            [ui.util :as util]
            [ui.icon :as icon]))

(defn- cell [& children]
  (into [:div {:style (sx {:display "flex" :flex-direction "column" :align-items "center" :gap "var(--size-2)"
                           :padding "var(--size-3)" :border-radius "var(--radius-control)" :border "var(--border-0)"})}]
        children))

(defn- caption [s]
  [:span {:style (sx {:font-size "var(--font-xs)" :color "var(--fg-2)" :text-align "center" :word-break "break-all"})} s])

(def examples
  [(example :sizes "Sizes"
     (fn []
       [:div {:style (sx {:display "flex" :gap "1.5rem" :align-items "flex-end"})}
        (cell (icon/icon {:icon-name :star :size :sm}) (caption "sm 16"))
        (cell (icon/icon {:icon-name :star}) (caption "md 20"))
        (cell (icon/icon {:icon-name :star :size :lg}) (caption "lg 24"))
        (cell (icon/icon {:icon-name :star :size :xl}) (caption "xl 32"))]))

   (example :filled "Filled variants"
     (fn []
       (into [:div {:style (sx {:display "grid" :grid-template-columns "repeat(auto-fill, minmax(7rem, 1fr))" :gap "var(--size-3)"})}]
             (map (fn [n]
                    (cell [:div {:style (sx {:display "flex" :gap "var(--size-3)"})}
                           (icon/icon {:icon-name n})
                           (icon/icon {:icon-name n :filled true})]
                          (caption (util/kw-name n))))
                  [:play :pause :skip-back :skip-forward :repeat :volume-2]))))

   (example :all "All icons"
     (fn []
       (into [:div {:style (sx {:display "grid" :grid-template-columns "repeat(auto-fill, minmax(5.5rem, 1fr))" :gap "var(--size-2)"})}]
             (map (fn [n] (cell (icon/icon {:icon-name n}) (caption (util/kw-name n))))
                  (sort-by util/kw-name icon/icon-names)))))])
