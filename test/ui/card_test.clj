(ns ui.card-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.card :as card]))

(deftest card-class-list-test
  (testing "always returns card class"
    (is (= ["card"] (card/card-class-list {})))))

(deftest card-component-test
  (testing "renders an article"
    (let [result (card/card {} "Content")]
      (is (= :article (first result)))
      (is (= "card" (get-in result [1 :class])))
      (is (= "Content" (nth result 2)))))

  (testing "extra class gets appended"
    (let [result (card/card {:class "extra"} "X")]
      (is (= "card extra" (get-in result [1 :class]))))))

(deftest card-header-test
  (testing "renders header"
    (let [result (card/card-header {} [:h3 "Title"])]
      (is (= :header (first result)))
      (is (= "card-header" (get-in result [1 :class]))))))

(deftest card-body-test
  (testing "renders body"
    (let [result (card/card-body {} "Content")]
      (is (= :div (first result)))
      (is (= "card-body" (get-in result [1 :class]))))))

(deftest card-footer-test
  (testing "renders footer"
    (let [result (card/card-footer {} "Actions")]
      (is (= :footer (first result)))
      (is (= "card-footer" (get-in result [1 :class]))))))
