(ns ui.spinner-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.spinner :as spinner]))

(deftest spinner-class-list-test
  (testing "default size (md)"
    (is (= ["spinner"] (spinner/spinner-class-list {})))
    (is (= ["spinner"] (spinner/spinner-class-list {:size :md}))))

  (testing "small size"
    (is (= ["spinner" "spinner-sm"] (spinner/spinner-class-list {:size :sm}))))

  (testing "large size"
    (is (= ["spinner" "spinner-lg"] (spinner/spinner-class-list {:size :lg})))))

(deftest spinner-component-test
  (testing "renders a span"
    (let [result (spinner/spinner {})]
      (is (= :span (first result)))
      (is (= "spinner" (get-in result [1 :class])))
      (is (= "status" (get-in result [1 :role]))))))
