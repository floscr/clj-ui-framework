(ns ui.color-picker-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.color-picker :as cp]))

(defn- nodes
  "Every hiccup element vector under `form`, depth first."
  [form]
  (->> (tree-seq vector? #(filter vector? (drop 1 %)) form)
       (filter #(keyword? (first %)))))

(defn- with-attr [form k]
  (filter #(and (map? (second %)) (contains? (second %) k)) (nodes form)))

(deftest normalize-formats-test
  (testing "defaults to every format"
    (is (= ["oklch" "hex" "rgb" "hsl"] (cp/normalize-formats nil)))
    (is (= ["oklch" "hex" "rgb" "hsl"] (cp/normalize-formats []))))
  (testing "keeps the given order, drops unknown names and duplicates"
    (is (= ["hsl" "oklch" "hex"] (cp/normalize-formats [:hsl :lab :oklch "hex" :hsl]))))
  (testing "nothing known left → every format"
    (is (= ["oklch" "hex" "rgb" "hsl"] (cp/normalize-formats [:lab])))))

(deftest color-picker-class-list-test
  (is (= ["color-picker"] (cp/color-picker-class-list {})))
  (is (= ["color-picker" "color-picker--disabled"] (cp/color-picker-class-list {:disabled true}))))

(deftest color-picker-renders-test
  (let [form (cp/color-picker {:value "#ff0000" :name "bg"})
        [tag attrs] form]
    (testing "wrapper carries the runtime contract"
      (is (= :div tag))
      (is (= "true" (:data-ui-color-picker attrs)))
      (is (= "#ff0000" (:data-value attrs)))
      (is (= "oklch hex rgb hsl" (:data-formats attrs)))
      (is (= "color-picker" (:class attrs))))
    (testing "hidden value input holds the value and the name"
      (let [[[_ v]] (with-attr form :data-ui-color-value)]
        (is (= "hidden" (:type v)))
        (is (= "#ff0000" (:value v)))
        (is (= "bg" (:name v)))))
    (testing "one format button per format, hue track, no opacity track"
      (is (= ["oklch" "hex" "rgb" "hsl"] (map (comp :data-ui-color-format second)
                                      (with-attr form :data-ui-color-format))))
      (is (= 1 (count (with-attr form :data-ui-color-hue))))
      (is (empty? (with-attr form :data-ui-color-alpha))))))

(deftest color-picker-options-test
  (testing "a single format hides the format switch"
    (is (empty? (with-attr (cp/color-picker {:formats [:hex]}) :data-ui-color-format))))
  (testing ":alpha adds the opacity track"
    (let [form (cp/color-picker {:alpha true})]
      (is (= "true" (:data-alpha (second form))))
      (is (= 1 (count (with-attr form :data-ui-color-alpha))))))
  (testing ":disabled marks the wrapper and disables the controls"
    (let [form (cp/color-picker {:disabled true})]
      (is (= "true" (:data-ui-color-disabled (second form))))
      (is (true? (:disabled (second (first (with-attr form :data-ui-color-text)))))))))
