(ns dev.examples.switch
  (:require [dev.example :refer [example column]]
            [ui.switch :as switch]))

(def examples
  [(example :states "States"
     (fn []
       (column
        (switch/switch-toggle {:label "Notifications" :checked false})
        (switch/switch-toggle {:label "Dark mode" :checked true})
        (switch/switch-toggle {:label "Disabled off" :disabled true})
        (switch/switch-toggle {:label "Disabled on" :checked true :disabled true}))))])
