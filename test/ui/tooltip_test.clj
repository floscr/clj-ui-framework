(ns ui.tooltip-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.tooltip :as tooltip]))

(deftest tooltip-component-test
  (testing "renders a span with data-tooltip attr"
    (let [result (tooltip/tooltip {:text "Hello"} "Hover me")]
      (is (= :span (first result)))
      (is (= "tooltip" (get-in result [1 :class])))
      (is (= "Hello" (get-in result [1 :data-tooltip])))
      (is (= "Hover me" (nth result 2)))))

  (testing "extra class appended"
    (let [result (tooltip/tooltip {:text "Tip" :class "extra"} "X")]
      (is (= "tooltip extra" (get-in result [1 :class]))))))
