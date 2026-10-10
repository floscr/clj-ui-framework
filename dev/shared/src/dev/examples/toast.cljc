(ns dev.examples.toast
  (:require [dev.example :refer [example row sx]]
            [ui.util :as util]
            [ui.toast :as toast]
            [ui.button :as button]))

(def variants [:info :success :warning :danger])

#?(:squint (defn- toast-click [v]
             (fn [_] (toast/show-toast! {:message (str "Toast: " v) :variant v})))
   :cljs   (defn- toast-click [v]
             (fn [_] (toast/show-toast! {:message (str "Toast: " (util/kw-name v)) :variant v})))
   :clj    (defn- toast-click [_v] nil))

(def examples
  [(example :show "Show a toast"
     (fn []
       (apply row
              (map (fn [v]
                     (button/button
                      (cond-> {:variant :secondary
                               :on-click (toast-click v)}
                        #?(:squint false :cljs false :clj true)
                        (assoc :attrs {:onclick (str "__uiToast('Toast: " (util/kw-name v)
                                                     "', {variant: '" (util/kw-name v) "'})")}))
                      (str "Show " (util/kw-name v))))
                   variants))))

   (example :static "Static previews"
     (fn []
       [:div {:style (sx {:display "flex" :flex-direction "column" :gap "0.5rem" :align-items "flex-start"})}
        (toast/toast {:variant :info} "Static toast preview")
        (toast/toast {:variant :success} "Saved")
        (toast/toast {:variant :warning} "Careful with this")
        (toast/toast {:variant :danger} "Something failed")]))])
