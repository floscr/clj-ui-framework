(ns ui.alert-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.alert :as alert]
            [ui.icon :as icon]))

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

  (testing "variant alert includes default icon"
    (let [result (alert/alert {:variant :success :title "Done!"} "Saved.")
          icon-span (nth result 2)]
      (is (= :span (first icon-span)))
      (is (= "alert-icon" (get-in icon-span [1 :class])))))

  (testing "neutral alert has no icon"
    (let [result (alert/alert {:title "Title"} "Body")]
      (is (not (some #(and (vector? %) (= "alert-icon" (get-in % [1 :class])))
                     (rest (rest result)))))))

  (testing "icon-name false suppresses icon"
    (let [result (alert/alert {:variant :success :icon-name false} "Body")]
      (is (not (some #(and (vector? %) (= "alert-icon" (get-in % [1 :class])))
                     (rest (rest result)))))))

  (testing "icon-name overrides default"
    (let [result (alert/alert {:variant :success :icon-name :star} "Body")
          icon-span (nth result 2)]
      (is (= "alert-icon" (get-in icon-span [1 :class])))))

  (testing "alert content is wrapped in alert-content div"
    (let [result (alert/alert {:variant :info :title "Title"} "Body")
          content-div (last result)]
      (is (= :div (first content-div)))
      (is (= "alert-content" (get-in content-div [1 :class]))))))
