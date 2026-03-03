(ns ui.switch-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.switch :as switch]))

(deftest switch-class-list-test
  (testing "default switch"
    (is (= ["switch"] (switch/switch-class-list {}))))

  (testing "disabled switch"
    (is (= ["switch" "switch--disabled"] (switch/switch-class-list {:disabled true})))))

(deftest switch-component-test
  (testing "renders a label"
    (let [result (switch/switch-toggle {:label "Notifications"})]
      (is (= :label (first result)))
      (is (= "switch" (get-in result [1 :class])))))

  (testing "checked switch has checked track class"
    (let [result (switch/switch-toggle {:checked true :label "On"})]
      ;; Find the track span
      (let [track (nth result 3)]
        (is (= "switch-track switch-track--checked" (get-in track [1 :class]))))))

  (testing "unchecked switch has no checked track class"
    (let [result (switch/switch-toggle {:checked false :label "Off"})]
      (let [track (nth result 3)]
        (is (= "switch-track" (get-in track [1 :class])))))))
