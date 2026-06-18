(ns ui.popover-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.popover :as popover]))

(deftest trigger-attrs-test
  (testing "links to target id and marks as dialog trigger"
    (let [a (popover/trigger-attrs "p1")]
      (is (= "p1" (:popovertarget a)))
      (is (= "dialog" (:aria-haspopup a))))))

(deftest content-class-list-test
  (testing "default side"
    (is (= ["popover-content" "popover-content--bottom"]
           (popover/content-class-list {}))))
  (testing "explicit side"
    (is (= ["popover-content" "popover-content--top"]
           (popover/content-class-list {:side :top})))))

(deftest popover-trigger-test
  (testing "renders a button wired to the target"
    (let [[tag attrs label] (popover/popover-trigger {:target "p1"} "Open")]
      (is (= :button tag))
      (is (= "p1" (:popovertarget attrs)))
      (is (= "dialog" (:aria-haspopup attrs)))
      (is (= "popover-trigger" (:class attrs)))
      (is (= "Open" label))))
  (testing "merges extra classes"
    (let [[_ attrs] (popover/popover-trigger {:target "p1" :class "btn btn-secondary"} "x")]
      (is (= "popover-trigger btn btn-secondary" (:class attrs))))))

(deftest popover-content-test
  (testing "defaults"
    (let [[tag attrs] (popover/popover-content {:id "p1"} "x")]
      (is (= :div tag))
      (is (= "p1" (:id attrs)))
      (is (= "auto" (:popover attrs)))
      (is (= "dialog" (:role attrs)))
      (is (= "bottom" (:data-popover-side attrs)))
      (is (= "center" (:data-popover-align attrs)))
      (is (= "popover-content popover-content--bottom" (:class attrs)))))
  (testing "side and align"
    (let [[_ attrs] (popover/popover-content {:id "p2" :side :top :align :start} "x")]
      (is (= "top" (:data-popover-side attrs)))
      (is (= "start" (:data-popover-align attrs)))
      (is (= "popover-content popover-content--top" (:class attrs))))))

(deftest popover-sections-test
  (testing "header, title, description"
    (is (= :header (first (popover/popover-header {} "h"))))
    (is (= "popover-title" (:class (second (popover/popover-title {} "t")))))
    (is (= "popover-description" (:class (second (popover/popover-description {} "d")))))))
