(ns dev.examples.notification
  (:require [dev.example :refer [example log-click sx row]]
            [ui.notification :as notification]
            [ui.button :as button]))

(def ^:private open {:label "Open" :on-click (log-click "Open")})

(def ^:private job
  {:description [:code "bb test --focus xi.session-test"]})

(defn- cards [& items]
  (into [:div {:style (sx {:display "flex" :flex-direction "column" :gap "1rem" :max-width "22rem"})}]
        (map notification/notification items)))

(defn- notify-button
  "A button calling window.__uiNotify.<method>(title, opts) (ui-runtime.js)."
  [label method title opts-js]
  (button/button
   #?(:squint {:variant :secondary
               :on-click (fn [_] (.call (aget js/window.__uiNotify method) nil title (js/JSON.parse opts-js)))}
      :cljs   {:variant :secondary
               :on-click (fn [_] (.call (aget js/window.__uiNotify method) nil title (js/JSON.parse opts-js)))}
      :clj    {:variant :secondary
               :attrs {:onclick (str "__uiNotify." method "(" (pr-str title) ", " opts-js ")")}})
   label))

(def examples
  [(example :states "Every type, as static cards"
     (fn []
       (cards {:title "Copied to clipboard" :type :success}
              (merge job {:type :loading :title "Running in background" :meta "0:12" :action open})
              (merge job {:type :success :title "Tests passed" :meta "212 tests · 34 s" :action open :promise true})
              (merge job {:type :error :title "Command failed" :meta "exit 1 · 1 failure" :action open :close-button true})
              {:type :warning :title "Disk almost full" :description "9.2 of 10 GB used"}
              {:type :info :title "A new version is ready" :cancel {:label "Later"} :action {:label "Reload"}}
              {:title "Event has been created" :description "Monday, January 3rd at 6:00pm"})))

   (example :rich-colors "Rich colors and invert"
     (fn []
       (cards {:type :success :title "Event has been created" :rich-colors true}
              {:type :info :title "Be at the area 10 minutes before the event time" :rich-colors true}
              {:type :warning :title "Event start time cannot be earlier than 8am" :rich-colors true}
              {:type :error :title "Event has not been created" :rich-colors true}
              {:title "Inverted" :description "The opposite theme's surface" :invert true})))

   (example :toaster "A contained toaster: hover or tap to fan out, swipe right to dismiss"
     (fn []
       [:div {:style (sx {:position "relative" :height "16rem" :border "var(--border-0)"
                          :border-radius "var(--radius-panel)" :background "var(--bg-0)"})}
        (notification/notification-toaster
         {:contained true
          :offset 16
          :close-button true
          :items [{:id "ex-1" :type :success :title "Copied to clipboard"}
                  (merge job {:id "ex-2" :type :loading :title "Running in background" :action open})
                  {:id "ex-3" :type :error :title "Export failed" :description "Disk full"}]})]))

   (example :imperative "Sonner's toast() on any page: window.__uiNotify"
     (fn []
       (row (notify-button "Default" "message" "Event has been created" "{\"description\":\"Monday, January 3rd at 6:00pm\"}")
            (notify-button "Success" "success" "Saved" "{}")
            (notify-button "Error" "error" "Something failed" "{\"closeButton\":true}")
            (notify-button "Action" "info" "Message archived" "{\"action\":{\"label\":\"Undo\"}}")
            (notify-button "Rich" "warning" "Storage is 92% full" "{\"richColors\":true}"))))])
