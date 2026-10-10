(ns dev.examples.grid
  (:require [dev.example :refer [example sx row]]
            [ui.grid :as grid]))

(defn- tile
  "Demo tile: a colored block. Square by default; give :h for a natural
   height (masonry)."
  [i & [h]]
  [:div {:style (sx (cond-> {:background "var(--bg-2)"
                             :border "var(--border-0)"
                             :border-radius "var(--radius-control)"
                             :display "flex"
                             :align-items "center"
                             :justify-content "center"
                             :color "var(--fg-2)"
                             :font-size "var(--font-sm)"}
                      h       (assoc :height (str h "px"))
                      (not h) (assoc :aspect-ratio "1")))}
   (str i)])

(def ^:private masonry-heights [120 190 90 150 220 110 170 130])

(def examples
  [(example :controls "Toolbar controls"
     (fn []
       (row
        (grid/layout-toggle {:value :grid})
        (grid/layout-toggle {:value :grid :boxed true})
        (grid/size-stepper {:value :m}))))

   (example :square "Square grid, size :s"
     (fn []
       (apply grid/grid {:size :s}
              (map (fn [i] (tile i)) (range 1 9)))))

   (example :masonry "Masonry, size :s"
     (fn []
       (apply grid/grid {:layout :masonry :size :s}
              (map-indexed (fn [i h] (tile (inc i) h)) masonry-heights))))

   (example :min "Custom :min 180px"
     (fn []
       (apply grid/grid {:min "180px"}
              (map (fn [i] (tile i)) (range 1 5)))))])
