(ns dev.examples.alert
  (:require [dev.example :refer [example column]]
            [ui.alert :as alert]))

(def examples
  [(example :tones "Tones"
     (fn []
       (column
        (alert/alert {:variant :success :title "Changes saved"} "Your profile is up to date.")
        (alert/alert {:variant :warning :title "Review before continuing"} "Two fields still need a value.")
        (alert/alert {:variant :danger :title "Could not save"} "Check your connection and try again.")
        (alert/alert {:variant :info :title "Keyboard shortcuts"} "Press ? to see every shortcut.")
        (alert/alert {:title "Neutral"} "A neutral alert with no tone."))))])
