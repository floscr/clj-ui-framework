(ns ui.button-group-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.button-group :as bg]))

(deftest button-group-class-list-test
  (testing "default variant is ghost (no boxed class)"
    (is (= ["button-group"] (bg/button-group-class-list {})))
    (is (= ["button-group"] (bg/button-group-class-list {:variant :ghost}))))

  (testing "boxed variant"
    (is (= ["button-group" "button-group-boxed"] (bg/button-group-class-list {:variant :boxed}))))

  (testing "string variant works"
    (is (= ["button-group" "button-group-boxed"] (bg/button-group-class-list {:variant "boxed"}))))

  (testing "nil variant falls back to ghost"
    (is (= ["button-group"] (bg/button-group-class-list {:variant nil})))))

(deftest button-group-classes-test
  (testing "space-joined string"
    (is (= "button-group" (bg/button-group-classes {})))
    (is (= "button-group button-group-boxed" (bg/button-group-classes {:variant :boxed})))))

(deftest button-group-item-class-list-test
  (testing "base item"
    (is (= ["button-group-item"] (bg/button-group-item-class-list {}))))

  (testing "active item"
    (is (= ["button-group-item" "is-active"] (bg/button-group-item-class-list {:active true}))))

  (testing "icon-only item"
    (is (= ["button-group-item" "button-group-item-icon"]
           (bg/button-group-item-class-list {:icon-only true}))))

  (testing "active icon-only item"
    (is (= ["button-group-item" "button-group-item-icon" "is-active"]
           (bg/button-group-item-class-list {:icon-only true :active true})))))

(deftest button-group-component-test
  (testing "container renders div with role group"
    (let [result (bg/button-group {:variant :boxed} "x")]
      (is (= :div (first result)))
      (is (= "button-group button-group-boxed" (get-in result [1 :class])))
      (is (= "group" (get-in result [1 :role])))
      (is (= "x" (nth result 2)))))

  (testing "extra class appended"
    (let [result (bg/button-group {:class "extra"} "x")]
      (is (= "button-group extra" (get-in result [1 :class])))))

  (testing "extra attrs merged"
    (let [result (bg/button-group {:attrs {:id "g1"}} "x")]
      (is (= "g1" (get-in result [1 :id]))))))

(deftest button-group-item-component-test
  (testing "text item"
    (let [result (bg/button-group-item {} "All")]
      (is (= :button (first result)))
      (is (= "button-group-item" (get-in result [1 :class])))
      (is (= "All" (nth result 2)))))

  (testing "active text item"
    (let [result (bg/button-group-item {:active true} "All")]
      (is (= "button-group-item is-active" (get-in result [1 :class])))))

  (testing "icon-only item is square"
    (let [result (bg/button-group-item {:icon :grid})]
      (is (= "button-group-item button-group-item-icon" (get-in result [1 :class])))
      (is (= :svg (first (nth result 2))))))

  (testing "icon with text is not icon-only"
    (let [result (bg/button-group-item {:icon :grid} "Grid")]
      (is (= "button-group-item" (get-in result [1 :class])))
      (is (= :svg (first (nth result 2))))
      (is (= "Grid" (nth result 3)))))

  (testing "count renders trailing badge span"
    (let [result (bg/button-group-item {:count 353} "All")]
      (is (= "All" (nth result 2)))
      (let [badge (nth result 3)]
        (is (= :span (first badge)))
        (is (= "button-group-count" (get-in badge [1 :class])))
        (is (= 353 (nth badge 2))))))

  (testing "extra class appended"
    (let [result (bg/button-group-item {:class "extra"} "All")]
      (is (= "button-group-item extra" (get-in result [1 :class]))))))

(deftest button-group-separator-test
  (testing "renders divider with role separator"
    (let [result (bg/button-group-separator {})]
      (is (= :div (first result)))
      (is (= "button-group-separator" (get-in result [1 :class])))
      (is (= "separator" (get-in result [1 :role])))))

  (testing "extra class appended"
    (let [result (bg/button-group-separator {:class "extra"})]
      (is (= "button-group-separator extra" (get-in result [1 :class]))))))
