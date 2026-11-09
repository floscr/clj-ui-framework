(ns dev.examples.rating
  (:require [dev.example :refer [example sx note]]
            [ui.button :as button]
            [ui.rating :as rating]))

(defn- cell [label & children]
  (into [:div {:style (sx {:display "flex" :flex-direction "column" :gap "0.5rem"})}
         [:span {:style (sx {:font-size "var(--font-xs)" :color "var(--fg-2)"})} label]]
        children))

(defn- cells [& children]
  (into [:div {:style (sx {:display "flex" :flex-wrap "wrap" :gap "2rem" :align-items "flex-start"})}]
        children))

(def examples
  [(example :states "States"
     (fn []
       [:div {}
        (note "Point at the left half of a star for a half. Choose the current value again, or press Backspace, to clear.")
        (cells
         (cell "Not rated" (rating/rating {:label "Not rated"}))
         (cell "Rated 3½" (rating/rating {:value 3.5 :label "Rated 3½"}))
         (cell "Rated 5" (rating/rating {:value 5 :label "Rated 5"}))
         (cell "Disabled" (rating/rating {:value 2 :disabled true :label "Disabled"})))]))

   (example :in-a-row "In a row of actions"
     (fn []
       [:div {:style (sx {:display "flex" :flex-wrap "wrap" :gap "0.5rem" :align-items "center"})}
        (button/button {:variant :primary :icon-left :play} "Stream now")
        (rating/rating {:value 4 :label "Your rating"})
        (button/button {:variant :secondary :icon-left :bookmark} "Watchlist")
        (button/button {:variant :secondary :icon-left :play} "Trailer")]))

   (example :sizes "Sizes and read-only"
     (fn []
       (cells
        (cell "Small, read-only" (rating/rating {:value 4.5 :read-only true :size :sm}))
        (cell "Medium, read-only" (rating/rating {:value 3 :read-only true}))
        (cell "Large" (rating/rating {:value 2.5 :size :lg :label "Large"})))))

   (example :scales "Whole stars and other scales"
     (fn []
       (cells
        (cell "Whole stars" (rating/rating {:value 3 :half false :label "Whole stars"}))
        (cell "Ten stars" (rating/rating {:value 7 :max 10 :half false :size :sm :label "Ten stars"})))))])
