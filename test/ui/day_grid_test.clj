(ns ui.day-grid-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.day-grid :as day-grid]))

(deftest day-grid-class-list-test
  (is (= ["day-grid"] (day-grid/day-grid-class-list {})))
  (is (= ["day-grid" "day-grid-teal"] (day-grid/day-grid-class-list {:color :teal})))
  (is (= ["day-grid"] (day-grid/day-grid-class-list {:color :nope})) "unknown colors fall back")
  (is (= "day-grid day-grid-teal" (day-grid/day-grid-classes {:color "teal"}))))

(deftest day-cell-classes-test
  (is (= "day-grid-cell" (day-grid/day-cell-classes {})))
  (is (= "day-grid-cell day-grid-done day-grid-today"
         (day-grid/day-cell-classes {:state :done :today true}))))

(deftest day-grid-test
  (testing "header and cells"
    (let [result (day-grid/day-grid {:days [{:date "Mon" :state :done} {:state :future}]})]
      (is (= "day-grid" (get-in result [1 :class])))
      (is (= 9 (count (drop 2 result))) "7 labels + 2 days")
      (is (= [:span {:class "day-grid-label" :aria-hidden "true"} "M"] (nth result 2)))
      (is (= [:span {:class "day-grid-cell day-grid-done" :title "Mon" :role "img" :aria-label "Mon"}]
             (nth result 9)))
      (is (= [:span {:class "day-grid-cell day-grid-future"}] (nth result 10)))))
  (testing "no header, class and attrs"
    (let [result (day-grid/day-grid {:labels false :days [{:title "x"}] :class "c" :attrs {:id "g"}})]
      (is (= "day-grid c" (get-in result [1 :class])))
      (is (= "g" (get-in result [1 :id])))
      (is (= 3 (count result))))))
