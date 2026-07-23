(ns ui.processing-bar
  "Processing bar — compact panel showing background jobs in progress.

   An accent-bordered panel with a title line and a list of in-flight
   items. Each item shows an optional thumbnail, an optional secondary
   label, and a spinner + status text.

   The bar renders unconditionally — consumers decide visibility
   (typically `(when (seq items) (processing-bar ...))`).

   Usage:
     (processing-bar {:title \"⏳ 2 scans in progress...\"}
       (processing-item {:thumb \"/thumbs/1.jpg\"
                         :label \"3 photos\"
                         :status \"Analyzing...\"})
       (processing-item {:status \"🔧 resize-images\"}))"
  (:require [ui.spinner :as spinner]))

;; ── Processing Item ─────────────────────────────────────────────────

(defn processing-item
  "Renders a single in-progress row.

   Props:
     :thumb  - thumbnail image URL (optional)
     :label  - secondary label line above the status (optional)
     :status - status text shown next to the spinner (string or hiccup)
     :class  - additional CSS classes
     :attrs  - additional HTML attributes"
  [{:keys [thumb label status class attrs]}]
  #?(:squint
     [:div (merge {:class (cond-> "processing-bar-item"
                            class (str " " class))}
                  attrs)
      (when thumb
        [:div {:class "processing-bar-thumb"}
         [:img {:src thumb :alt ""}]])
      [:div {:class "processing-bar-info"}
       (when label
         [:div {:class "processing-bar-label"} label])
       [:div {:class "processing-bar-status"}
        (spinner/spinner {:size "xs"})
        [:span status]]]]

     :cljs
     [:div (merge {:class (cond-> ["processing-bar-item"]
                            class (conj class))}
                  attrs)
      (when thumb
        [:div {:class ["processing-bar-thumb"]}
         [:img {:src thumb :alt ""}]])
      [:div {:class ["processing-bar-info"]}
       (when label
         [:div {:class ["processing-bar-label"]} label])
       [:div {:class ["processing-bar-status"]}
        (spinner/spinner {:size :xs})
        [:span status]]]]

     :clj
     [:div (merge {:class (cond-> "processing-bar-item"
                            class (str " " class))}
                  attrs)
      (when thumb
        [:div {:class "processing-bar-thumb"}
         [:img {:src thumb :alt ""}]])
      [:div {:class "processing-bar-info"}
       (when label
         [:div {:class "processing-bar-label"} label])
       [:div {:class "processing-bar-status"}
        (spinner/spinner {:size :xs})
        [:span status]]]]))

;; ── Processing Bar ──────────────────────────────────────────────────

(defn processing-bar
  "Wraps processing-item elements in an accent-bordered panel.

   Props:
     :title - header text shown above the item list (optional)
     :class - additional CSS classes
     :attrs - additional HTML attributes
   Children: processing-item elements."
  [{:keys [title class attrs]} & children]
  #?(:squint
     (into [:div (merge {:class (cond-> "processing-bar"
                                  class (str " " class))}
                        attrs)
            (when title
              [:div {:class "processing-bar-header"}
               [:span {:class "processing-bar-title"} title]])]
           [(into [:div {:class "processing-bar-list"}] children)])

     :cljs
     (into [:div (merge {:class (cond-> ["processing-bar"]
                                  class (conj class))}
                        attrs)
            (when title
              [:div {:class ["processing-bar-header"]}
               [:span {:class ["processing-bar-title"]} title]])]
           [(into [:div {:class ["processing-bar-list"]}] children)])

     :clj
     (into [:div (merge {:class (cond-> "processing-bar"
                                  class (str " " class))}
                        attrs)
            (when title
              [:div {:class "processing-bar-header"}
               [:span {:class "processing-bar-title"} title]])]
           [(into [:div {:class "processing-bar-list"}] children)])))
