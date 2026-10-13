(ns dev.examples.dialog
  (:require [dev.example :refer [example sx]]
            [ui.dialog :as dialog]
            [ui.button :as button]))

#?(:squint (defn- dialog-action [id method]
             (fn [_] (let [el (.getElementById js/document id)] (when el (if (= method "open") (.showModal el) (.close el))))))
   :cljs   (defn- dialog-action [id method]
             (fn [_] (let [el (.getElementById js/document id)] (when el (if (= method "open") (.showModal el) (.close el))))))
   :clj    (defn- dialog-action [_id _method] nil))

(defn- handler-props
  "Click wiring for every target: :on-click on browser targets, an inline
   onclick on hiccup."
  [id method]
  #?(:squint {:on-click (dialog-action id method)}
     :cljs   {:on-click (dialog-action id method)}
     :clj    {:attrs {:onclick (str "document.getElementById('" id "')."
                                    (if (= method "open") "showModal()" "close()"))}}))

(def examples
  [(example :native "Native dialog"
     (fn []
       [:div {}
        (button/button (merge {:variant :secondary} (handler-props "example-dialog" "open"))
          "Delete project")
        (dialog/dialog {:id "example-dialog"}
          (dialog/dialog-header {} [:h3 "Delete Harbour?"] [:p "Its 12 files are removed for everyone."])
          (dialog/dialog-body {} [:p "This cannot be undone."])
          (dialog/dialog-footer {}
            (button/button (merge {:variant :secondary :size :sm} (handler-props "example-dialog" "close"))
              "Cancel")
            (button/button (merge {:variant :danger :size :sm} (handler-props "example-dialog" "close"))
              "Delete project")))]))

   (example :panel "Overlay panel (static preview)"
     (fn []
       [:div {:style (sx {:display "flex" :justify-content "center" :padding "1rem"
                          :background "color-mix(in oklch, var(--gray-950) 40%, transparent)"
                          :border-radius "var(--radius-panel)"})}
        (dialog/dialog-panel {:attrs {:style (sx {:position "static" :width "min(100%, 26rem)"})}}
          (dialog/dialog-header {} [:h3 "Rename file"])
          (dialog/dialog-body {} [:p "Pick a name your team will recognize."])
          (dialog/dialog-footer {}
            (button/button {:variant :secondary :size :sm} "Cancel")
            (button/button {:variant :primary :size :sm} "Save changes")))]))])
