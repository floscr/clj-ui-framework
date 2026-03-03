(ns ui.dialog-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.dialog :as dialog]))

(deftest dialog-class-list-test
  (testing "always returns dialog class"
    (is (= ["dialog"] (dialog/dialog-class-list {})))))

(deftest dialog-component-test
  (testing "renders a dialog element"
    (let [result (dialog/dialog {:id "my-dialog"} "Content")]
      (is (= :dialog (first result)))
      (is (= "dialog" (get-in result [1 :class])))
      (is (= "my-dialog" (get-in result [1 :id])))))

  (testing "open dialog has open attr"
    (let [result (dialog/dialog {:open true} "Content")]
      (is (true? (get-in result [1 :open])))))

  (testing "dialog has backdrop close handler"
    (let [result (dialog/dialog {} "Content")]
      (is (string? (get-in result [1 :onclick])))
      (is (clojure.string/includes? (get-in result [1 :onclick]) "close")))))

(deftest dialog-sections-test
  (testing "dialog-header renders header"
    (let [result (dialog/dialog-header {} [:h3 "Title"])]
      (is (= :header (first result)))
      (is (= "dialog-header" (get-in result [1 :class])))))

  (testing "dialog-body renders div"
    (let [result (dialog/dialog-body {} "Body")]
      (is (= :div (first result)))
      (is (= "dialog-body" (get-in result [1 :class])))))

  (testing "dialog-footer renders footer"
    (let [result (dialog/dialog-footer {} "Footer")]
      (is (= :footer (first result)))
      (is (= "dialog-footer" (get-in result [1 :class]))))))
