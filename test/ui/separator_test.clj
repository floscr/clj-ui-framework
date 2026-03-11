(ns ui.separator-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.separator :as separator]))

(deftest separator-class-list-test
  (testing "default (horizontal)"
    (is (= ["separator" "separator-horizontal"] (separator/separator-class-list {}))))

  (testing "explicit horizontal"
    (is (= ["separator" "separator-horizontal"] (separator/separator-class-list {:orientation :horizontal}))))

  (testing "vertical"
    (is (= ["separator" "separator-vertical"] (separator/separator-class-list {:orientation :vertical})))))

(deftest separator-classes-test
  (testing "returns space-joined string"
    (is (= "separator separator-horizontal" (separator/separator-classes {})))
    (is (= "separator separator-vertical" (separator/separator-classes {:orientation :vertical})))))

(deftest separator-component-test
  (testing "renders a div with role=none and data-orientation"
    (let [result (separator/separator {})]
      (is (= :div (first result)))
      (is (= "separator separator-horizontal" (get-in result [1 :class])))
      (is (= "none" (get-in result [1 :role])))
      (is (= "horizontal" (get-in result [1 :data-orientation])))))

  (testing "vertical orientation"
    (let [result (separator/separator {:orientation :vertical})]
      (is (= "separator separator-vertical" (get-in result [1 :class])))
      (is (= "vertical" (get-in result [1 :data-orientation])))))

  (testing "extra class is appended"
    (let [result (separator/separator {:class "my-sep"})]
      (is (= "separator separator-horizontal my-sep" (get-in result [1 :class])))))

  (testing "attrs are merged"
    (let [result (separator/separator {:attrs {:id "sep-1"}})]
      (is (= "sep-1" (get-in result [1 :id]))))))
