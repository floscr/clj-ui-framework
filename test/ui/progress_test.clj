(ns ui.progress-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.progress :as progress]))

(deftest progress-bar-class-list-test
  (testing "default (no variant)"
    (is (= ["progress-bar"] (progress/progress-bar-class-list {}))))

  (testing "explicit variants"
    (is (= ["progress-bar" "progress-bar--success"] (progress/progress-bar-class-list {:variant :success})))
    (is (= ["progress-bar" "progress-bar--warning"] (progress/progress-bar-class-list {:variant :warning})))
    (is (= ["progress-bar" "progress-bar--danger"] (progress/progress-bar-class-list {:variant :danger})))))

(deftest progress-component-test
  (testing "renders progress with correct structure"
    (let [result (progress/progress {:value 60})]
      (is (= :div (first result)))
      (is (= "progress" (get-in result [1 :class])))
      (is (= "progressbar" (get-in result [1 :role])))
      ;; inner bar
      (let [bar (nth result 2)]
        (is (= :div (first bar)))
        (is (= "progress-bar" (get-in bar [1 :class])))
        (is (= "width: 60%" (get-in bar [1 :style])))))))
