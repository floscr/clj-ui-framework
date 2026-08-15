(ns ui.select-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.select :as select]))

(deftest normalize-options-test
  (testing "maps pass through"
    (is (= [{:value "a" :label "A"}]
           (select/normalize-options [{:value "a" :label "A"}]))))
  (testing "bare strings become value+label"
    (is (= [{:value "Foo" :label "Foo"} {:value "Bar" :label "Bar"}]
           (select/normalize-options ["Foo" "Bar"])))))

(deftest selected-label-test
  (let [opts [{:value "a" :label "Apple"} {:value "b" :label "Banana"}]]
    (testing "returns label of matching value"
      (is (= "Banana" (select/selected-label opts "b" "Pick"))))
    (testing "falls back to placeholder when no match"
      (is (= "Pick" (select/selected-label opts nil "Pick"))))
    (testing "falls back to empty string with no placeholder"
      (is (= "" (select/selected-label opts nil nil))))))

(deftest select-component-test
  (testing "renders a wrapper + trigger button"
    (let [result  (select/select {:options [{:value "a" :label "Apple"}]})
          trigger (nth result 2)]
      (is (= :div (first result)))
      (is (= "select" (get-in result [1 :class])))
      (is (= :button (first trigger)))
      (is (= "select-trigger" (get-in trigger [1 :class])))
      (is (= "listbox" (get-in trigger [1 :aria-haspopup])))
      (is (= "window.__uiSelect(this)" (get-in trigger [1 :onclick])))))

  (testing "placeholder label carries the placeholder class"
    (let [result  (select/select {:placeholder "Choose" :options [{:value "a" :label "A"}]})
          trigger (nth result 2)
          span    (nth trigger 2)]
      (is (= "select-value select-value--placeholder" (get-in span [1 :class])))
      (is (= "Choose" (nth span 2)))))

  (testing "selected value renders label without placeholder class"
    (let [result  (select/select {:value "a" :options [{:value "a" :label "Apple"}]})
          trigger (nth result 2)
          span    (nth trigger 2)]
      (is (= "select-value" (get-in span [1 :class])))
      (is (= "Apple" (nth span 2)))))

  (testing "disabled sets the button disabled"
    (let [result  (select/select {:disabled true :options []})
          trigger (nth result 2)]
      (is (true? (get-in trigger [1 :disabled])))))

  (testing "custom class is appended to the trigger"
    (let [result  (select/select {:class "wide" :options []})
          trigger (nth result 2)]
      (is (= "select-trigger wide" (get-in trigger [1 :class])))))

  (testing "name emits a hidden input carrying the value"
    (let [result (select/select {:name "role" :value "a"
                                 :options [{:value "a" :label "A"}]})
          input  (nth result 3)]
      (is (= :input (first input)))
      (is (= "hidden" (get-in input [1 :type])))
      (is (= "role" (get-in input [1 :name])))
      (is (= "a" (get-in input [1 :value]))))))
