(ns dev.examples.file-browser
  (:require [dev.example :refer [example sx]]
            [dev.examples.sample :as sample]
            [ui.util :as util]
            [ui.file-browser :as fb]))

(defn- menu-items [item]
  (let [folder? (= (util/kw-name (:file-type item)) "folder")]
    (cond-> [{:label "Open" :icon :folder :url "#"}]
      (not folder?) (conj {:label "Download" :icon :download :url "#"})
      true          (conj {:label "Rename" :icon :edit :url "#"})
      true          (conj {:type :separator})
      true          (conj {:label "Delete" :icon :trash :variant :danger :url "#"}))))

(def examples
  [(example :types "File type icons"
     (fn []
       (into [:div {:style (sx {:display "grid" :grid-template-columns "repeat(auto-fill, minmax(6rem, 1fr))" :gap "var(--size-3)"})}]
             (map (fn [ft]
                    [:div {:style (sx {:display "flex" :flex-direction "column" :align-items "center" :gap "var(--size-2)"
                                       :padding "var(--size-3)" :border-radius "var(--radius-control)" :border "var(--border-0)"})}
                     (fb/file-type-icon {:file-type ft})
                     [:span {:style (sx {:font-size "var(--font-xs)" :color "var(--fg-2)"})} (util/kw-name ft)]])
                  [:folder :image :video :audio :document :spreadsheet :code :archive :file]))))

   (example :grid "Grid view"
     (fn []
       [:div {}
        [:div {:class "fb-toolbar"}
         [:div {:style (sx {:font-weight "500"})} "My files"]
         (fb/file-view-toggle {:view :grid})]
        (into [:div {:class "fb-grid"}]
              (map-indexed (fn [i item]
                             (fb/file-item-grid {:item item :selected (= i 2)
                                                 :context-menu-items (menu-items item)}))
                           sample/files))]))

   (example :list "List view with sortable columns"
     (fn []
       (fb/file-table {:items sample/files
                       :sort-key :name
                       :sort-dir :asc
                       :context-menu-items-fn menu-items})))])
