(ns ui.tabs-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.tabs :as tabs]))

(deftest tabs-class-list-test
  (testing "default variant is boxed"
    (is (= ["tabs" "tabs-boxed"] (tabs/tabs-class-list {}))))
  (testing "explicit variant"
    (is (= ["tabs" "tabs-line"] (tabs/tabs-class-list {:variant :line})))))

(deftest tabs-classes-test
  (testing "space-joined string"
    (is (= "tabs tabs-boxed" (tabs/tabs-classes {})))
    (is (= "tabs tabs-line" (tabs/tabs-classes {:variant :line})))))

(def sample
  {:name "t"
   :variant :line
   :default "b"
   :tabs [{:id "a" :label "A" :content "AC"}
          {:id "b" :label "B" :content "BC"}]})

(deftest tabs-structure-test
  (let [[tag attrs list-el panels-el] (tabs/tabs sample)]
    (testing "root element"
      (is (= :div tag))
      (is (= "tabs tabs-line" (:class attrs))))
    (testing "tablist with interleaved inputs and labels"
      (let [[ltag lattrs in1 lab1 in2 lab2] list-el]
        (is (= :div ltag))
        (is (= "tablist" (:role lattrs)))
        (is (= :input (first in1)))
        (is (= "radio" (:type (second in1))))
        (is (= "t" (:name (second in1))))
        (is (= "t-a" (:id (second in1))))
        (is (= :label (first lab1)))
        (is (= "t-a" (:for (second lab1))))
        (is (= "A" (nth lab1 2)))
        (is (= "t-b" (:id (second in2))))
        (is (= "B" (nth lab2 2)))))
    (testing "panels carry content in order"
      (let [[ptag _ p1 p2] panels-el]
        (is (= :div ptag))
        (is (= "tabpanel" (:role (second p1))))
        (is (= "AC" (nth p1 2)))
        (is (= "BC" (nth p2 2)))))))

(deftest tabs-default-active-test
  (testing "the :default tab's input is checked, others are not"
    (let [[_ _ list-el] (tabs/tabs sample)
          [_ _ in1 _ in2] list-el]
      (is (nil? (:checked (second in1))))
      (is (true? (:checked (second in2))))))
  (testing "falls back to the first tab when :default is missing"
    (let [[_ _ list-el] (tabs/tabs (dissoc sample :default))
          [_ _ in1 _ in2] list-el]
      (is (true? (:checked (second in1))))
      (is (nil? (:checked (second in2))))))
  (testing "falls back to the first tab when :default is unknown"
    (let [[_ _ list-el] (tabs/tabs (assoc sample :default "nope"))
          [_ _ in1 _ in2] list-el]
      (is (true? (:checked (second in1))))
      (is (nil? (:checked (second in2)))))))

(deftest tabs-keyword-ids-test
  (testing "keyword ids and default are coerced to strings"
    (let [[_ _ list-el] (tabs/tabs {:name "k"
                                    :default :two
                                    :tabs [{:id :one :label "One" :content "1"}
                                           {:id :two :label "Two" :content "2"}]})
          [_ _ in1 lab1 in2] list-el]
      (is (= "k-one" (:id (second in1))))
      (is (= "k-one" (:for (second lab1))))
      (is (nil? (:checked (second in1))))
      (is (true? (:checked (second in2)))))))

(deftest tabs-extra-class-test
  (testing "merges extra classes and attrs onto the root"
    (let [[_ attrs] (tabs/tabs (assoc sample :class "mb-4" :attrs {:id "settings-tabs"}))]
      (is (= "tabs tabs-line mb-4" (:class attrs)))
      (is (= "settings-tabs" (:id attrs))))))
