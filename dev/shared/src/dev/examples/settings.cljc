(ns dev.examples.settings
  (:require [dev.example :refer [example]]
            [ui.settings :as settings]
            [ui.switch :as switch]
            [ui.select :as select]
            [ui.button :as button]
            [ui.button-group :as button-group]))

(def ^:private zones
  [{:value "Europe/Vienna" :label "Vienna (UTC+1)"}
   {:value "America/New_York" :label "New York (UTC-5)"}
   {:value "Asia/Tokyo" :label "Tokyo (UTC+9)"}])

(def examples
  [(example :switches "Switch rows with hints"
     (fn []
       (settings/settings-group {}
         (settings/settings-row {:label "Mentions" :hint "When someone mentions you in a comment."}
           (switch/switch-toggle {:checked true}))
         (settings/settings-row {:label "Weekly summary" :hint "A Monday email with last week's activity."}
           (switch/switch-toggle {}))
         (settings/settings-row {:label "Product updates"}
           (switch/switch-toggle {:checked true})))))

   (example :controls "A select, a segmented choice and an action"
     (fn []
       (settings/settings-group {}
         (settings/settings-row {:label "Time zone" :hint "Used for reminders and the daily summary."}
           (select/select {:options zones :value "Europe/Vienna" :name "tz"}))
         (settings/settings-row {:label "Photos" :hint "Follows the system theme"}
           (button-group/button-group {:variant :boxed}
             (button-group/button-group-item {:active true} "Auto")
             (button-group/button-group-item {} "Light")
             (button-group/button-group-item {} "Dark")))
         (settings/settings-row {:label "Export data" :hint "A zip with your photos and metadata."}
           (button/button {:icon-left :download} "Export")))))

   (example :danger "A destructive action as a row"
     (fn []
       (settings/settings-group {}
         (settings/settings-row {:label "Delete account"
                                 :hint "Removes your profile and projects for everyone. This cannot be undone."}
           (button/button {:variant :danger} "Delete account")))))])
