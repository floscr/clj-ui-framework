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

;; ─── Card List ─────────────────────────────────────────────────

(deftest card-list-class-list-test
  (testing "default divider is full"
    (is (= ["card-list"] (card/card-list-class-list {}))))
  (testing "explicit full divider"
    (is (= ["card-list"] (card/card-list-class-list {:divider :full}))))
  (testing "inset divider adds modifier"
    (is (= ["card-list" "card-list-inset"] (card/card-list-class-list {:divider :inset})))))

(deftest card-list-test
  (testing "renders a div with role list"
    (let [result (card/card-list {} "Item")]
      (is (= :div (first result)))
      (is (= "card-list" (get-in result [1 :class])))
      (is (= "list" (get-in result [1 :role])))
      (is (= "Item" (nth result 2)))))
  (testing "inset divider class"
    (let [result (card/card-list {:divider :inset} "Item")]
      (is (= "card-list card-list-inset" (get-in result [1 :class])))))
  (testing "extra class gets appended"
    (let [result (card/card-list {:class "extra"} "Item")]
      (is (= "card-list extra" (get-in result [1 :class]))))))

(deftest card-list-item-test
  (testing "renders a div with role listitem"
    (let [result (card/card-list-item {} "Content")]
      (is (= :div (first result)))
      (is (= "card-list-item" (get-in result [1 :class])))
      (is (= "listitem" (get-in result [1 :role])))
      (is (= "Content" (nth result 2)))))
  (testing "extra class gets appended"
    (let [result (card/card-list-item {:class "extra"} "Content")]
      (is (= "card-list-item extra" (get-in result [1 :class]))))))
