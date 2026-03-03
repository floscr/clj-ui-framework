(ns ui.accordion-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.accordion :as accordion]))

(deftest accordion-class-list-test
  (testing "always returns base class"
    (is (= ["accordion"] (accordion/accordion-class-list {})))
    (is (= ["accordion"] (accordion/accordion-class-list {:open true})))))

(deftest accordion-classes-test
  (testing "space-joined output"
    (is (= "accordion" (accordion/accordion-classes {})))
    (is (= "accordion" (accordion/accordion-classes {:open true})))))

(deftest accordion-component-test
  (testing "closed accordion uses details/summary"
    (let [result (accordion/accordion {:title "Question?"} "Answer.")]
      (is (= :details (first result)))
      (is (= "accordion" (get-in result [1 :class])))
      (is (nil? (get-in result [1 :open])))
      ;; summary trigger is present
      (is (= :summary (get-in result [2 0])))
      (is (= "accordion-trigger" (get-in result [2 1 :class])))
      ;; content div is always present
      (is (some #(and (vector? %) (= "accordion-content" (get-in % [1 :class])))
                (rest (rest result))))))

  (testing "open accordion has open attribute"
    (let [result (accordion/accordion {:title "Q?" :open true} "A.")]
      (is (= :details (first result)))
      (is (true? (get-in result [1 :open])))))

  (testing "children are inside content div"
    (let [result (accordion/accordion {:title "T"} "Child1" "Child2")]
      ;; find the content div
      (let [content-div (first (filter #(and (vector? %)
                                             (= "accordion-content" (get-in % [1 :class])))
                                       (rest (rest result))))]
        (is (some? content-div))))))
