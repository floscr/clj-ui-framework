(ns ui.alert-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.alert :as alert]))

(deftest alert-class-list-test
  (testing "neutral (no variant)"
    (is (= ["alert"] (alert/alert-class-list {}))))

  (testing "explicit variants"
    (is (= ["alert" "alert-success"] (alert/alert-class-list {:variant :success})))
    (is (= ["alert" "alert-warning"] (alert/alert-class-list {:variant :warning})))
    (is (= ["alert" "alert-danger"] (alert/alert-class-list {:variant :danger})))
    (is (= ["alert" "alert-info"] (alert/alert-class-list {:variant :info})))))

(deftest alert-classes-test
  (testing "space-joined output"
    (is (= "alert" (alert/alert-classes {})))
    (is (= "alert alert-success" (alert/alert-classes {:variant :success})))))

(deftest alert-component-test
  (testing "basic alert renders correct hiccup"
    (let [result (alert/alert {:variant :success :title "Done!"} "Saved.")]
      (is (= :div (first result)))
      (is (= "alert alert-success" (get-in result [1 :class])))
      (is (= "alert" (get-in result [1 :role])))))

  (testing "alert with title includes title paragraph"
    (let [result (alert/alert {:title "Title"} "Body")]
      (is (some #(and (vector? %) (= "alert-title" (get-in % [1 :class])))
                (rest (rest result))))))

  (testing "alert without title has no title paragraph"
    (let [result (alert/alert {} "Body")]
      (is (not (some #(and (vector? %) (= "alert-title" (get-in % [1 :class])))
                     (rest (rest result))))))))
