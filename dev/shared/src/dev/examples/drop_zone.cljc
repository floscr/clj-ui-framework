(ns dev.examples.drop-zone
  (:require [dev.example :refer [example sx]]
            [ui.drop-zone :as drop-zone]))

(defn- thumb []
  [:div {:style (sx {:width "64px" :height "64px" :border-radius "var(--radius-control)" :background "var(--bg-2)"})}])

(def examples
  [(example :default "Default"
     (fn []
       (drop-zone/drop-zone {:title "Upload photos"
                             :hint "Drag and drop or click to select"})))

   (example :large "Large with icon"
     (fn []
       (drop-zone/drop-zone {:size :lg
                             :icon :image
                             :title "Upload photos"
                             :hint "Drag and drop or click to select"})))

   (example :custom "Custom content with a file label"
     (fn []
       (drop-zone/drop-zone {}
         [:div {:style (sx {:display "flex" :gap "0.5rem" :flex-wrap "wrap"})}
          (thumb) (thumb) (thumb)]
         (drop-zone/file-label {}
           [:span "+ Add more"]))))])
