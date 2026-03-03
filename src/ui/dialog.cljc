(ns ui.dialog
  (:require [clojure.string :as str]))

(defn dialog-class-list
  "Generate a vector of CSS class strings for a dialog."
  [_opts]
  ["dialog"])

(defn dialog-classes
  "Generate CSS class string for a dialog."
  [opts]
  (str/join " " (dialog-class-list opts)))

(defn dialog
  "Render a dialog element.

   Clicking the backdrop (the area outside the dialog box) will close the
   dialog automatically when opened via .showModal().

   Props:
     :open  - boolean, whether the dialog is open
     :id    - dialog id for targeting
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [open id class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> (dialog-classes {})
                     class (str " " class))
           base-attrs (merge {:class classes
                              :on-click (fn [e]
                                          (when (identical? (.-target e) (.-currentTarget e))
                                            (.close (.-currentTarget e))))}
                             (when id {:id id})
                             (when open {:open true})
                             attrs)]
       (into [:dialog base-attrs] children))

     :cljs
     (let [cls (dialog-class-list {})
           classes (cond-> cls class (conj class))
           base-attrs (merge {:class classes
                              :on {:click (fn [e]
                                            (when (identical? (.-target e) (.-currentTarget e))
                                              (.close (.-currentTarget e))))}}
                             (when id {:id id})
                             (when open {:open true})
                             attrs)]
       (into [:dialog base-attrs] children))

     :clj
     (let [classes (cond-> (dialog-classes {})
                     class (str " " class))
           base-attrs (merge {:class classes
                              :onclick "if(event.target===this)this.close()"}
                             (when id {:id id})
                             (when open {:open true})
                             attrs)]
       (into [:dialog base-attrs] children))))

(defn dialog-header
  "Render a dialog header section."
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:header (merge {:class (cond-> "dialog-header" class (str " " class))} attrs)] children)
     :cljs
     (into [:header (merge {:class (cond-> ["dialog-header"] class (conj class))} attrs)] children)
     :clj
     (into [:header (merge {:class (cond-> "dialog-header" class (str " " class))} attrs)] children)))

(defn dialog-body
  "Render a dialog body section."
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:div (merge {:class (cond-> "dialog-body" class (str " " class))} attrs)] children)
     :cljs
     (into [:div (merge {:class (cond-> ["dialog-body"] class (conj class))} attrs)] children)
     :clj
     (into [:div (merge {:class (cond-> "dialog-body" class (str " " class))} attrs)] children)))

(defn dialog-footer
  "Render a dialog footer section."
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (into [:footer (merge {:class (cond-> "dialog-footer" class (str " " class))} attrs)] children)
     :cljs
     (into [:footer (merge {:class (cond-> ["dialog-footer"] class (conj class))} attrs)] children)
     :clj
     (into [:footer (merge {:class (cond-> "dialog-footer" class (str " " class))} attrs)] children)))
