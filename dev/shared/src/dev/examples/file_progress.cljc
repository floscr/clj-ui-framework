(ns dev.examples.file-progress
  (:require [dev.example :refer [example sx]]
            [ui.file-browser :as fb]
            [ui.file-progress :as fp]))

(def examples
  [(example :states "Upload states"
     (fn []
       [:div {:style (sx {:max-width "520px"})}
        (fp/file-progress-list {}
          (fp/file-progress-item {:name "project-proposal.pdf" :size "2.4 MB"
                                  :icon (fb/file-type-icon {:file-type :document :size :sm})
                                  :progress 100 :status :complete :on-remove identity})
          (fp/file-progress-item {:name "vacation-photo.jpg" :size "4.1 MB"
                                  :icon (fb/file-type-icon {:file-type :image :size :sm})
                                  :progress 67 :status :uploading :on-remove identity})
          (fp/file-progress-item {:name "backup.zip" :size "1.2 GB"
                                  :icon (fb/file-type-icon {:file-type :archive :size :sm})
                                  :progress 23 :status :error :on-remove identity})
          (fp/file-progress-item {:name "notes.md" :size "4.5 KB"
                                  :icon (fb/file-type-icon {:file-type :document :size :sm})
                                  :status :idle}))]))])
