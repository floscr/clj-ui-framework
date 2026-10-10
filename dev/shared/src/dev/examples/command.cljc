(ns dev.examples.command
  (:require [dev.example :refer [example note cls]]
            [ui.command :as command]
            [ui.button :as button]
            [ui.icon :as icon]))

(def ^:private trigger-cls (button/button-classes {:variant :secondary}))

(def examples
  [(example :palette "Palette with a hotkey"
     (fn []
       [:div {}
        (note "Press " [:kbd {:class (cls "command-shortcut")} "⌘K"]
              " or click the button. Type to filter, arrows to navigate, Enter to select.")
        [:div {:style "margin-top: 0.75rem;"}
         (command/command-trigger {:target "cmdk" :class trigger-cls}
           (icon/icon {:icon-name :search :size :sm})
           [:span "Search commands…"]
           [:kbd {:class (cls "command-shortcut")} "⌘K"])
         (command/command-dialog {:id "cmdk" :hotkey "mod+k"
                                  :placeholder "Type a command or search…"}
           (command/command-group {:heading "Suggestions"}
             (command/command-item {:icon :calendar :shortcut "⌘P"} "Calendar")
             (command/command-item {:icon :search   :shortcut "⌘F"} "Search files")
             (command/command-item {:icon :clock} "Recent"))
           (command/command-group {:heading "Settings"}
             (command/command-item {:icon :user     :shortcut "⌘P"} "Profile")
             (command/command-item {:icon :mail     :shortcut "⌘B"} "Mail")
             (command/command-item {:icon :settings :shortcut "⌘S"} "Settings")))]]))])
