(ns ui.chat-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.chat :as chat]))

(deftest chat-bubble-class-list-test
  (testing "default role is assistant"
    (is (= ["chat-bubble" "chat-bubble-assistant"]
           (chat/chat-bubble-class-list {}))))
  (testing "user role"
    (is (= ["chat-bubble" "chat-bubble-user"]
           (chat/chat-bubble-class-list {:role :user}))))
  (testing "pending adds modifier"
    (is (= ["chat-bubble" "chat-bubble-assistant" "chat-bubble-pending"]
           (chat/chat-bubble-class-list {:role :assistant :pending true})))))

(deftest chat-bubble-classes-test
  (is (= "chat-bubble chat-bubble-user"
         (chat/chat-bubble-classes {:role :user}))))

(deftest chat-bubble-test
  (testing "renders children with role classes"
    (let [[tag attrs & children] (chat/chat-bubble {:role :user} "hello")]
      (is (= :div tag))
      (is (= "chat-bubble chat-bubble-user" (:class attrs)))
      (is (= ["hello"] (vec children)))))
  (testing "extra class and attrs merge"
    (let [[_ attrs] (chat/chat-bubble {:role :assistant :class "extra" :attrs {:id "m1"}} "hi")]
      (is (= "chat-bubble chat-bubble-assistant extra" (:class attrs)))
      (is (= "m1" (:id attrs))))))

(deftest chat-log-test
  (let [[tag attrs & children] (chat/chat-log {} [:span "a"] [:span "b"])]
    (is (= :div tag))
    (is (= "chat-log" (:class attrs)))
    (is (= 2 (count children)))))

(deftest chat-thinking-test
  (let [[_ attrs & children] (chat/chat-thinking {})]
    (is (= "chat-bubble chat-bubble-assistant chat-bubble-pending" (:class attrs)))
    (is (some #(and (vector? %) (= [:span "Thinking…"] %)) children)))
  (testing "custom label"
    (let [[_ _ & children] (chat/chat-thinking {:label "Coach is thinking…"})]
      (is (some #(= [:span "Coach is thinking…"] %) children)))))

(deftest chat-input-test
  (testing "form with textarea and send button"
    (let [[tag attrs textarea footer] (chat/chat-input {:placeholder "Message…"
                                                        :input-attrs {:name "message"}})]
      (is (= :form tag))
      (is (= "chat-input" (:class attrs)))
      (is (= :textarea (first textarea)))
      (is (= "message" (:name (second textarea))))
      (is (= "Message…" (:placeholder (second textarea))))
      (is (= "chat-input-footer" (:class (second footer))))))
  (testing "hint renders in footer"
    (let [[_ _ _ footer] (chat/chat-input {:hint "Sees your profile"})
          [_ _ hint-el] footer]
      (is (= [:span {:class "chat-hint"} "Sees your profile"] hint-el))))
  (testing "form attrs merge (htmx)"
    (let [[_ attrs] (chat/chat-input {:attrs {:hx-post "/api/coach"}})]
      (is (= "/api/coach" (:hx-post attrs))))))
