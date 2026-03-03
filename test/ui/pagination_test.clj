(ns ui.pagination-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.pagination :as pagination]))

(deftest pagination-item-class-list-test
  (testing "default item"
    (is (= ["pagination-item"] (pagination/pagination-item-class-list {}))))

  (testing "active item"
    (is (= ["pagination-item" "pagination-item--active"]
           (pagination/pagination-item-class-list {:active true}))))

  (testing "disabled item"
    (is (= ["pagination-item" "pagination-item--disabled"]
           (pagination/pagination-item-class-list {:disabled true}))))

  (testing "active + disabled"
    (is (= ["pagination-item" "pagination-item--active" "pagination-item--disabled"]
           (pagination/pagination-item-class-list {:active true :disabled true})))))

(deftest pagination-component-test
  (testing "renders nav with pagination list"
    (let [result (pagination/pagination {:current 2 :total 5})]
      (is (= :nav (first result)))
      (is (= "Pagination" (get-in result [1 :aria-label])))
      (let [ol (nth result 2)]
        (is (= :ol (first ol)))
        (is (= "pagination" (get-in ol [1 :class])))
        ;; 5 page items + prev + next = 7
        (is (= 7 (count (drop 2 ol))))))))
