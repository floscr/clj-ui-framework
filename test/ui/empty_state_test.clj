(ns ui.empty-state-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.empty-state :as empty-state]))

(deftest empty-state-class-list-test
  (testing "base class"
    (is (= ["empty-state"] (empty-state/empty-state-class-list {})))
    (is (= "empty-state" (empty-state/empty-state-classes {})))))

(deftest empty-state-component-test
  (testing "renders a div with text children"
    (let [result (empty-state/empty-state {} "No items yet")]
      (is (= :div (first result)))
      (is (= "empty-state" (get-in result [1 :class])))
      (is (= "No items yet" (nth result 2)))))

  (testing "extra class gets appended"
    (let [result (empty-state/empty-state {:class "extra"} "Empty")]
      (is (= "empty-state extra" (get-in result [1 :class])))))

  (testing "extra attrs get merged"
    (let [result (empty-state/empty-state {:attrs {:id "empty"}} "Empty")]
      (is (= "empty" (get-in result [1 :id])))))

  (testing "icon renders before children"
    (let [result (empty-state/empty-state {:icon :inbox} "No mail")]
      (is (= :svg (first (nth result 2))))
      (is (= "No mail" (nth result 3)))))

  (testing "no icon element without :icon"
    (let [result (empty-state/empty-state {} "Empty")]
      (is (= 3 (count result)))))

  (testing "multiple children"
    (let [result (empty-state/empty-state {} [:p "Nothing here"] [:p "Add something"])]
      (is (= [:p "Nothing here"] (nth result 2)))
      (is (= [:p "Add something"] (nth result 3))))))
