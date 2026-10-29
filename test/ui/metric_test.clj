(ns ui.metric-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.metric :as metric]))

(deftest metric-class-list-test
  (is (= ["metric"] (metric/metric-class-list {})))
  (is (= ["metric" "metric-flush"] (metric/metric-class-list {:flush true})))
  (is (= "metric metric-flush" (metric/metric-classes {:flush true}))))

(deftest metric-test
  (testing "label and value only"
    (let [result (metric/metric {:label "Orders" :value "12"})]
      (is (= :div (first result)))
      (is (= "metric" (get-in result [1 :class])))
      (is (= [:span {:class "metric-label"} "Orders"] (nth result 2)))
      (is (= [:div {:class "metric-value"} [:span {:class "metric-number"} "12"]] (nth result 3)))
      (is (= 4 (count result)) "no foot without delta or hint")))
  (testing "unit, delta with trend, hint"
    (let [result (metric/metric {:label "L" :value "1" :unit "EUR" :delta "+2%" :trend :up :hint "vs last"})]
      (is (= [:span {:class "metric-unit"} "EUR"] (get-in result [3 3])))
      (is (= [:div {:class "metric-foot"}
              [:span {:class "metric-delta metric-delta-up"} "+2%"]
              [:span {:class "metric-hint"} "vs last"]]
             (nth result 4)))))
  (testing "trend down and flat"
    (is (= "metric-delta metric-delta-down"
           (get-in (metric/metric {:label "L" :value "1" :delta "-1" :trend :down}) [4 2 1 :class])))
    (is (= "metric-delta"
           (get-in (metric/metric {:label "L" :value "1" :delta "0" :trend :flat}) [4 2 1 :class]))))
  (testing "flush, class and attrs"
    (let [result (metric/metric {:label "L" :value "1" :flush true :class "x" :attrs {:id "m"}})]
      (is (= "metric metric-flush x" (get-in result [1 :class])))
      (is (= "m" (get-in result [1 :id]))))))

(deftest metric-row-test
  (let [result (metric/metric-row {} [:div "a"] [:div "b"])]
    (is (= "metric-row" (get-in result [1 :class])))
    (is (= [[:div "a"] [:div "b"]] (subvec result 2)))))
