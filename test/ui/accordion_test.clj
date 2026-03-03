(ns ui.accordion-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.accordion :as accordion]))

(deftest accordion-class-list-test
  (testing "closed accordion"
    (is (= ["accordion"] (accordion/accordion-class-list {})))
    (is (= ["accordion"] (accordion/accordion-class-list {:open false}))))

  (testing "open accordion"
    (is (= ["accordion" "accordion--open"] (accordion/accordion-class-list {:open true})))))

(deftest accordion-classes-test
  (testing "space-joined output"
    (is (= "accordion" (accordion/accordion-classes {})))
    (is (= "accordion accordion--open" (accordion/accordion-classes {:open true})))))

(deftest accordion-component-test
  (testing "closed accordion renders trigger only"
    (let [result (accordion/accordion {:title "Question?"} "Answer.")]
      (is (= :div (first result)))
      (is (= "accordion" (get-in result [1 :class])))
      ;; trigger is present
      (is (= "accordion-trigger" (get-in result [2 1 :class])))))

  (testing "open accordion includes content"
    (let [result (accordion/accordion {:title "Q?" :open true} "A.")]
      (is (= "accordion accordion--open" (get-in result [1 :class])))
      ;; should contain accordion-content div
      (is (some #(and (vector? %) (= "accordion-content" (get-in % [1 :class])))
                (rest (rest result)))))))
