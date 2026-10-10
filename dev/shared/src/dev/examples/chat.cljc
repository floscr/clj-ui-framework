(ns dev.examples.chat
  (:require [dev.example :refer [example sx]]
            [ui.chat :as chat]
            [ui.button :as button]))

(def examples
  [(example :conversation "Conversation with composer"
     (fn []
       [:div {:style (sx {:max-width "480px"})}
        (chat/chat-toolbar {}
          (button/button {:variant :ghost :size :sm :icon-left :trash} "Clear chat"))
        (chat/chat-log {}
          (chat/chat-bubble {:role :user} "How was my training week?")
          (chat/chat-bubble {:role :assistant}
            [:p "Solid week: 2 strength sessions and a run. "
             [:strong "One more leg day"] " would round it out."])
          (chat/chat-bubble {:role :user} "Plan me a leg day for Friday.")
          (chat/chat-thinking {:label "Coach is thinking…"}))
        [:div {:style (sx {:margin-top "1rem"})}
         (chat/chat-input {:placeholder "Message your coach…"
                           :hint "Coach sees your profile and recent activity."
                           :send-icon :zap
                           :input-attrs {:name "message"}})]]))])
