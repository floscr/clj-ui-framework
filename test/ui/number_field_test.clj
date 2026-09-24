(ns ui.number-field-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.number-field :as nf]))

(deftest number-field-class-list-test
  (testing "defaults"
    (is (= ["number-field" "number-field--default"]
           (nf/number-field-class-list {}))))
  (testing "variant"
    (is (= ["number-field" "number-field--spinner"]
           (nf/number-field-class-list {:variant :spinner})))
    (is (= ["number-field" "number-field--right"]
           (nf/number-field-class-list {:variant :right}))))
  (testing ":stacked is a legacy alias for :spinner"
    (is (= ["number-field" "number-field--spinner"]
           (nf/number-field-class-list {:variant :stacked})))
    (is (= "spinner" (nf/normalize-variant :stacked)))
    (is (= "right" (nf/normalize-variant "right")))
    (is (= "default" (nf/normalize-variant nil))))
  (testing "size other than md adds a modifier"
    (is (= ["number-field" "number-field--default" "number-field--sm"]
           (nf/number-field-class-list {:size :sm})))
    (is (= ["number-field" "number-field--default"]
           (nf/number-field-class-list {:size :md}))))
  (testing "disabled"
    (is (= ["number-field" "number-field--default" "number-field--disabled"]
           (nf/number-field-class-list {:disabled true})))))

(deftest clamp-value-test
  (testing "open bounds"
    (is (= 5 (nf/clamp-value 5 nil nil))))
  (testing "min"
    (is (= 0 (nf/clamp-value -3 0 nil)))
    (is (= 4 (nf/clamp-value 4 0 nil))))
  (testing "max"
    (is (= 10 (nf/clamp-value 12 nil 10)))
    (is (= 4 (nf/clamp-value 4 nil 10))))
  (testing "both"
    (is (= 0 (nf/clamp-value -1 0 10)))
    (is (= 10 (nf/clamp-value 11 0 10)))
    (is (= 5 (nf/clamp-value 5 0 10)))))

(deftest step-value-test
  (testing "default step of 1"
    (is (== 6 (nf/step-value 5 {} 1)))
    (is (== 4 (nf/step-value 5 {} -1))))
  (testing "explicit step"
    (is (== 15 (nf/step-value 10 {:step 5} 1)))
    (is (== 5 (nf/step-value 10 {:step 5} -1))))
  (testing "clamps to bounds"
    (is (== 10 (nf/step-value 10 {:max 10} 1)))
    (is (== 0 (nf/step-value 0 {:min 0} -1))))
  (testing "fractional steps stay clean"
    (is (== 0.3 (nf/step-value 0.2 {:step 0.1} 1))))
  (testing "nil value treated as 0"
    (is (== 1 (nf/step-value nil {} 1))))
  (testing "non-positive step falls back to 1"
    (is (== 6 (nf/step-value 5 {:step 0} 1)))))

(deftest number-field-renders-test
  (testing "returns a wrapper div with the data hook and an input"
    (let [[tag attrs & _children] (nf/number-field {:value 5 :min 0 :max 10})]
      (is (= :div tag))
      (is (= "true" (:data-ui-number-field attrs)))
      (is (re-find #"number-field" (:class attrs))))))
