(ns ui.skeleton-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.skeleton :as skeleton]))

(deftest skeleton-class-list-test
  (testing "default (no variant)"
    (is (= ["skeleton"] (skeleton/skeleton-class-list {}))))

  (testing "explicit variants"
    (is (= ["skeleton" "skeleton--line"] (skeleton/skeleton-class-list {:variant :line})))
    (is (= ["skeleton" "skeleton--box"] (skeleton/skeleton-class-list {:variant :box})))
    (is (= ["skeleton" "skeleton--circle"] (skeleton/skeleton-class-list {:variant :circle})))
    (is (= ["skeleton" "skeleton--heading"] (skeleton/skeleton-class-list {:variant :heading})))))

(deftest skeleton-component-test
  (testing "renders a div with role=status"
    (let [result (skeleton/skeleton {:variant :line})]
      (is (= :div (first result)))
      (is (= "skeleton skeleton--line" (get-in result [1 :class])))
      (is (= "status" (get-in result [1 :role]))))))
