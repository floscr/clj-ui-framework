(ns ui.toast-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.toast :as toast]))

(deftest toast-class-list-test
  (testing "default variant"
    (is (= ["toast" "toast-info"] (toast/toast-class-list {}))))
  (testing "explicit variants"
    (is (= ["toast" "toast-success"] (toast/toast-class-list {:variant :success})))
    (is (= ["toast" "toast-warning"] (toast/toast-class-list {:variant :warning})))
    (is (= ["toast" "toast-danger"] (toast/toast-class-list {:variant :danger})))))

(deftest toast-classes-test
  (is (= "toast toast-info" (toast/toast-classes {})))
  (is (= "toast toast-success" (toast/toast-classes {:variant :success}))))

(deftest toast-test
  (testing "renders element with content"
    (is (= [:div {:class "toast toast-success"} "Saved"]
           (toast/toast {:variant :success} "Saved"))))
  (testing "custom class and attrs"
    (let [[_ attrs] (toast/toast {:class "extra" :attrs {:id "t1"}} "x")]
      (is (= "toast toast-info extra" (:class attrs)))
      (is (= "t1" (:id attrs))))))

(deftest toast-flash-test
  (testing "minimal — message only"
    (is (= [:div {:data-ui-toast "Saved"
                  :style "display: none;"}]
           (toast/toast-flash {:message "Saved"}))))
  (testing "variant and duration"
    (is (= [:div {:data-ui-toast "✓ Done"
                  :style "display: none;"
                  :data-variant "success"
                  :data-duration "3000"}]
           (toast/toast-flash {:message "✓ Done"
                               :variant :success
                               :duration 3000})))))
