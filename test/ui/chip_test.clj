(ns ui.chip-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.chip :as chip]))

(deftest chip-class-list-test
  (testing "default chip"
    (is (= ["chip"] (chip/chip-class-list {}))))
  (testing "active chip"
    (is (= ["chip" "chip-active"] (chip/chip-class-list {:active true})))))

(deftest chip-classes-test
  (testing "space-joined"
    (is (= "chip" (chip/chip-classes {})))
    (is (= "chip chip-active" (chip/chip-classes {:active true})))))

(deftest chip-component-test
  (testing "basic chip renders button"
    (let [[tag attrs & children] (chip/chip {} "Label")]
      (is (= :button tag))
      (is (= "chip" (:class attrs)))
      (is (= ["Label"] children))))

  (testing "active chip"
    (let [[tag attrs] (chip/chip {:active true} "Active")]
      (is (= :button tag))
      (is (= "chip chip-active" (:class attrs)))))

  (testing "chip with dot-color"
    (let [[_tag _attrs dot label] (chip/chip {:dot-color "#ff0000"} "Red")]
      (is (= :span (first dot)))
      (is (= "chip-dot" (:class (second dot))))
      (is (= "Red" label))))

  (testing "custom class"
    (let [[_tag attrs] (chip/chip {:class "extra"} "X")]
      (is (= "chip extra" (:class attrs))))))
