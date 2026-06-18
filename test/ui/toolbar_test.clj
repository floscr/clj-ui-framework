(ns ui.toolbar-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.toolbar :as toolbar]))

(deftest toolbar-class-list-test
  (testing "default orientation"
    (is (= ["toolbar" "toolbar-horizontal"]
           (toolbar/toolbar-class-list {}))))
  (testing "explicit orientation"
    (is (= ["toolbar" "toolbar-vertical"]
           (toolbar/toolbar-class-list {:orientation :vertical})))))

(deftest toolbar-classes-test
  (testing "space-joined string"
    (is (= "toolbar toolbar-horizontal" (toolbar/toolbar-classes {})))))

(deftest toolbar-test
  (testing "renders a div with toolbar role and orientation"
    (let [[tag attrs child] (toolbar/toolbar {} [:button "a"])]
      (is (= :div tag))
      (is (= "toolbar" (:role attrs)))
      (is (= "horizontal" (:aria-orientation attrs)))
      (is (= "toolbar toolbar-horizontal" (:class attrs)))
      (is (= [:button "a"] child))))
  (testing "vertical orientation"
    (let [[_ attrs] (toolbar/toolbar {:orientation :vertical})]
      (is (= "vertical" (:aria-orientation attrs)))
      (is (= "toolbar toolbar-vertical" (:class attrs)))))
  (testing "merges extra classes"
    (let [[_ attrs] (toolbar/toolbar {:class "extra"})]
      (is (= "toolbar toolbar-horizontal extra" (:class attrs))))))

(deftest toolbar-separator-test
  (testing "renders a separator div"
    (let [[tag attrs] (toolbar/toolbar-separator {})]
      (is (= :div tag))
      (is (= "separator" (:role attrs)))
      (is (= "toolbar-separator" (:class attrs)))))
  (testing "merges extra classes"
    (let [[_ attrs] (toolbar/toolbar-separator {:class "extra"})]
      (is (= "toolbar-separator extra" (:class attrs))))))
