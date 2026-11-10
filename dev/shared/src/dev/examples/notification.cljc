(ns dev.examples.notification
  (:require [dev.example :refer [example log-click sx]]
            [ui.notification :as notification]
            [ui.button :as button]))

(defn- open-button []
  (button/button {:size :sm :on-click (log-click "Open")} "Open"))

(defn- copied []
  (notification/notification {:status :success :title "Copied to clipboard"}))

(defn- running []
  (notification/notification {:status :loading
                              :title "Running in background"
                              :description [:code "bb test --focus xi.session-test"]
                              :meta "0:12"
                              :on-dismiss (log-click "Dismiss")
                              :dismiss-attrs {:title "Dismiss"}}
    (open-button)))

(defn- deck
  "Room above the stack for its tucked edges and for fanning out."
  [min-height stack]
  [:div {:style (sx {:display "flex" :align-items "flex-end" :min-height min-height
                     :padding-top "1.5rem"})}
   stack])

(def examples
  [(example :states "Every state of a background command"
     (fn []
       [:div {:style (sx {:display "flex" :flex-direction "column" :gap "1rem" :max-width "22rem"})}
        (copied)
        (running)
        (notification/notification {:status :success
                                    :title "Tests passed"
                                    :description [:code "bb test --focus xi.session-test"]
                                    :meta "212 tests · 34 s"}
          (open-button))
        (notification/notification {:status :danger
                                    :title "Command failed"
                                    :description [:code "bb test --focus xi.session-test"]
                                    :meta "exit 1 · 1 failure"
                                    :on-dismiss (log-click "Dismiss")
                                    :dismiss-attrs {:title "Dismiss"}}
          (open-button))
        (notification/notification {:status :warning
                                    :title "Disk almost full"
                                    :description "9.2 of 10 GB used"})
        (notification/notification {:icon :terminal
                                    :title "Started in a terminal"
                                    :description [:code "bun run dev"]})]))

   (example :stack "Stacked: the newest in front, the rest tucked behind (hover to fan out)"
     (fn []
       (deck "6rem"
         (notification/notification-stack {}
           (copied)
           (running)
           (notification/notification {:status :success :title "Export finished"})))))

   (example :expanded "Fanned out with :expanded, as after a tap, with Close all"
     (fn []
       (deck "16rem"
         (notification/notification-stack {:expanded true
                                           :on-dismiss-all (log-click "Close all")
                                           :dismiss-all-attrs {:title "Close all notifications"}}
           (copied)
           (running)
           (notification/notification {:status :success :title "Export finished"})))))])
