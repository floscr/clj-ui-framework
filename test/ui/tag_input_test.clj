(ns ui.tag-input-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.tag-input :as tag-input]))

(deftest tag-input-class-list-test
  (testing "default classes"
    (is (= ["tag-input"] (tag-input/tag-input-class-list {}))))
  (testing "focused"
    (is (= ["tag-input" "tag-input-focused"] (tag-input/tag-input-class-list {:focused true}))))
  (testing "disabled"
    (is (= ["tag-input" "tag-input-disabled"] (tag-input/tag-input-class-list {:disabled true}))))
  (testing "focused + disabled"
    (is (= ["tag-input" "tag-input-focused" "tag-input-disabled"]
           (tag-input/tag-input-class-list {:focused true :disabled true})))))

(deftest tag-pill-class-list-test
  (testing "default"
    (is (= ["tag-pill"] (tag-input/tag-pill-class-list {})))))

(deftest tag-input-classes-test
  (testing "joined string"
    (is (= "tag-input" (tag-input/tag-input-classes {})))
    (is (= "tag-input tag-input-focused" (tag-input/tag-input-classes {:focused true})))))

(deftest filter-tags-test
  (let [all-tags [{:label "React" :value "react"}
                  {:label "Next.js" :value "nextjs"}
                  {:label "TypeScript" :value "typescript"}
                  {:label "Clojure" :value "clojure"}]]
    (testing "no input returns all unselected"
      (is (= 4 (count (tag-input/filter-tags all-tags [] "")))))
    (testing "filters by input value"
      (is (= [{:label "React" :value "react"}]
             (vec (tag-input/filter-tags all-tags [] "react")))))
    (testing "case insensitive"
      (is (= [{:label "TypeScript" :value "typescript"}]
             (vec (tag-input/filter-tags all-tags [] "TYPE")))))
    (testing "excludes already selected"
      (is (= [{:label "Next.js" :value "nextjs"}
              {:label "TypeScript" :value "typescript"}
              {:label "Clojure" :value "clojure"}]
             (vec (tag-input/filter-tags all-tags [{:label "React" :value "react"}] "")))))
    (testing "filter + exclusion combined"
      (is (= []
             (vec (tag-input/filter-tags all-tags [{:label "React" :value "react"}] "react")))))))

(deftest tag-pill-test
  (testing "renders pill with label"
    (let [result (tag-input/tag-pill {:label "React"})]
      (is (= :span (first result)))
      (is (some #(= "tag-pill" %) (flatten [(get (second result) :class)])))))
  (testing "no remove button when on-remove is nil"
    (let [result (tag-input/tag-pill {:label "Test"})]
      ;; Should not have a :button child
      (is (not (some #(and (vector? %) (= :button (first %)))
                     (drop 2 result)))))))

(deftest tag-input-component-test
  (testing "renders with no tags"
    (let [result (tag-input/tag-input {:tags []
                                        :input-value ""
                                        :open false
                                        :filtered-items []})]
      (is (= :div (first result)))
      (is (some #(= "tag-input" %) (flatten [(get (second result) :class)])))))
  (testing "renders with tags"
    (let [result (tag-input/tag-input {:tags [{:label "A" :value "a"}
                                               {:label "B" :value "b"}]
                                        :input-value ""
                                        :open false
                                        :filtered-items []})]
      (is (= :div (first result))))))

(deftest tag-dropdown-test
  (testing "returns nil when not open"
    (is (nil? (tag-input/tag-dropdown {:open false :items []}))))
  (testing "renders empty message when no items"
    (let [result (tag-input/tag-dropdown {:open true :items []})]
      (is (some? result))))
  (testing "renders items"
    (let [result (tag-input/tag-dropdown {:open true
                                           :items [{:label "A" :value "a"}
                                                   {:label "B" :value "b"}]
                                           :active-index 0})]
      (is (= :div (first result))))))
