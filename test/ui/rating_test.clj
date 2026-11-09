(ns ui.rating-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.rating :as rating]))

(deftest rating-class-list-test
  (testing "defaults"
    (is (= ["rating" "rating--md"] (rating/rating-class-list {}))))
  (testing "size, read-only, disabled"
    (is (= ["rating" "rating--sm" "rating--read-only"]
           (rating/rating-class-list {:size :sm :read-only true})))
    (is (= ["rating" "rating--lg" "rating--disabled"]
           (rating/rating-class-list {:size "lg" :disabled true})))))

(deftest values-test
  (testing "value-str drops a whole number's decimals"
    (is (= "4" (rating/value-str 4)))
    (is (= "4" (rating/value-str 4.0)))
    (is (= "3.5" (rating/value-str 3.5)))
    (is (= "0.5" (rating/value-str 1/2)))
    (is (= "0" (rating/value-str nil))))
  (testing "steps"
    (is (= ["0.5" "1" "1.5" "2" "2.5" "3" "3.5" "4" "4.5" "5"]
           (map rating/value-str (rating/steps {}))))
    (is (= ["1" "2" "3"] (map rating/value-str (rating/steps {:max 3 :half false})))))
  (testing "format-value and value-label"
    (is (= "3½" (rating/format-value 3.5)))
    (is (= "½" (rating/format-value 0.5)))
    (is (= "4" (rating/format-value 4)))
    (is (= "Not rated" (rating/value-label 0)))
    (is (= "½ star" (rating/value-label 0.5)))
    (is (= "1 star" (rating/value-label 1)))
    (is (= "3½ stars" (rating/value-label 3.5)))))

(defn- radios [tree]
  (filter #(and (vector? %) (= :input (first %))) (tree-seq vector? seq tree)))

(deftest rating-renders-test
  (testing "interactive: a radio group with a 0 radio and one per step"
    (let [[tag attrs] (rating/rating {:value 3.5 :name "r"})
          rs (radios (rating/rating {:value 3.5 :name "r"}))]
      (is (= :span tag))
      (is (= "radiogroup" (:role attrs)))
      (is (= "3.5" (:data-value attrs)))
      (is (= 11 (count rs)))
      (is (every? #(= "r" (:name (second %))) rs))
      (is (= ["3.5"] (map (comp :value second) (filter (comp :checked second) rs))))))
  (testing "unrated checks the 0 radio"
    (is (= ["0"] (map (comp :value second)
                      (filter (comp :checked second) (radios (rating/rating {:name "r"})))))))
  (testing "read-only: an image of the value, no radios"
    (let [tree (rating/rating {:value 4 :read-only true})
          [_ attrs] tree]
      (is (= "img" (:role attrs)))
      (is (= "Rating: 4 out of 5" (:aria-label attrs)))
      (is (empty? (radios tree)))))
  (testing "a generated group name when none is given"
    (let [n1 (:name (second (first (radios (rating/rating {})))))
          n2 (:name (second (first (radios (rating/rating {})))))]
      (is (not= n1 n2)))))
