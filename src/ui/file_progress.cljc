(ns ui.file-progress
  "File progress list — shows files with upload/processing status.

   A reusable component for displaying files with progress indicators,
   status states, and remove actions. Used by file-browser drop zones
   but can be used independently anywhere file progress is shown.

   Usage:
     (file-progress-list {}
       (file-progress-item {:name \"photo.jpg\"
                            :size \"2.4 MB\"
                            :icon (icon/icon {:icon-name :image :size :sm})
                            :progress 67
                            :status :uploading
                            :on-remove (fn [] ...)})
       (file-progress-item {:name \"doc.pdf\"
                            :size \"1.1 MB\"
                            :icon (icon/icon {:icon-name :file :size :sm})
                            :status :complete}))"
  (:require [ui.icon :as icon]
            [ui.progress :as progress]
            [ui.util :as util]))

;; ── File Progress Item ──────────────────────────────────────────────

(defn file-progress-item
  "Renders a file entry with progress/status in a continuous list.

   Props:
     :name      - file name
     :size      - formatted size string (e.g. \"2.4 MB\")
     :icon      - rendered hiccup for the icon slot (e.g. from file-type-icon)
     :progress  - upload progress 0–100 (nil = no progress bar)
     :status    - :idle, :uploading, :complete, :error
     :on-remove - (fn []) called when remove button is clicked
     :class     - additional CSS classes
     :attrs     - additional HTML attributes"
  [{:keys [name size icon progress status on-remove class attrs]}]
  (let [s (util/kw-name (or status "idle"))
        error?    (= s "error")
        complete? (= s "complete")]
    #?(:squint
       [:div (merge {:class (cond-> "fp-item"
                              error?    (str " fp-item-error")
                              complete? (str " fp-item-complete")
                              class     (str " " class))}
                    attrs)
        (when icon
          [:div {:class "fp-item-icon"} icon])
        [:div {:class "fp-item-info"}
         [:div {:class "fp-item-name"} name]
         (when size
           [:div {:class "fp-item-size"} size])
         (when (and progress (not complete?))
           (progress/progress {:value progress
                               :variant (when error? "danger")}))]
        (when on-remove
          [:button {:class "fp-item-remove"
                    :on-click (fn [e]
                                (.stopPropagation e)
                                (on-remove))
                    :title "Remove"}
           (icon/icon {:icon-name "x" :size "sm"})])]

       :cljs
       [:div (merge {:class (cond-> ["fp-item"]
                              error?    (conj "fp-item-error")
                              complete? (conj "fp-item-complete")
                              class     (conj class))}
                    attrs)
        (when icon
          [:div {:class ["fp-item-icon"]} icon])
        [:div {:class ["fp-item-info"]}
         [:div {:class ["fp-item-name"]} name]
         (when size
           [:div {:class ["fp-item-size"]} size])
         (when (and progress (not complete?))
           (progress/progress {:value progress
                               :variant (when error? :danger)}))]
        (when on-remove
          [:button {:class ["fp-item-remove"]
                    :on {:click (fn [e]
                                  (.stopPropagation e)
                                  (on-remove))}
                    :title "Remove"}
           (icon/icon {:icon-name :x :size :sm})])]

       :clj
       [:div (merge {:class (cond-> "fp-item"
                              error?    (str " fp-item-error")
                              complete? (str " fp-item-complete")
                              class     (str " " class))}
                    attrs)
        (when icon
          [:div {:class "fp-item-icon"} icon])
        [:div {:class "fp-item-info"}
         [:div {:class "fp-item-name"} name]
         (when size
           [:div {:class "fp-item-size"} size])
         (when (and progress (not complete?))
           (progress/progress {:value progress
                               :variant (when error? :danger)}))]
        (when on-remove
          [:button {:class "fp-item-remove"
                    :title "Remove"}
           (icon/icon {:icon-name :x :size :sm})])])))

;; ── File Progress List ──────────────────────────────────────────────

(defn file-progress-list
  "Wraps file-progress-item elements in a continuous bordered list.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes
   Children: file-progress-item elements"
  [{:keys [class attrs]} & children]
  #?(:squint
     (into [:div (merge {:class (cond-> "fp-list"
                                  class (str " " class))}
                        attrs)]
           children)

     :cljs
     (into [:div (merge {:class (cond-> ["fp-list"]
                                  class (conj class))}
                        attrs)]
           children)

     :clj
     (into [:div (merge {:class (cond-> "fp-list"
                                  class (str " " class))}
                        attrs)]
           children)))
