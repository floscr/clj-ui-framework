(ns ui.badge-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.badge :as badge]))

(deftest badge-class-list-test
  (testing "default variant (primary)"
    (is (= ["badge"] (badge/badge-class-list {}))))

  (testing "explicit variants"
    (is (= ["badge"] (badge/badge-class-list {:variant :primary})))
    (is (= ["badge" "badge-secondary"] (badge/badge-class-list {:variant :secondary})))
    (is (= ["badge" "badge-outline"] (badge/badge-class-list {:variant :outline})))
    (is (= ["badge" "badge-success"] (badge/badge-class-list {:variant :success})))
    (is (= ["badge" "badge-warning"] (badge/badge-class-list {:variant :warning})))
    (is (= ["badge" "badge-danger"] (badge/badge-class-list {:variant :danger})))))

(deftest badge-classes-test
  (testing "space-joined output"
    (is (= "badge" (badge/badge-classes {})))
    (is (= "badge badge-danger" (badge/badge-classes {:variant :danger})))))

(deftest badge-component-test
  (testing "renders a span"
    (let [result (badge/badge {} "New")]
      (is (= :span (first result)))
      (is (= "badge" (get-in result [1 :class])))
      (is (= "New" (nth result 2)))))

  (testing "extra class gets appended"
    (let [result (badge/badge {:class "extra"} "X")]
      (is (= "badge extra" (get-in result [1 :class]))))))
