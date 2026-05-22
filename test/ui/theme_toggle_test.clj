(ns ui.theme-toggle-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.theme-toggle :as tt]))

(deftest theme-toggle-class-list-test
  (testing "default classes"
    (is (= ["theme-toggle"] (tt/theme-toggle-class-list {}))))

  (testing "small variant"
    (is (= ["theme-toggle" "theme-toggle-sm"] (tt/theme-toggle-class-list {:size :sm})))))

(deftest theme-toggle-component-test
  (testing "renders a div with radiogroup role"
    (let [result (tt/theme-toggle {})]
      (is (= :div (first result)))
      (is (= "radiogroup" (get-in result [1 :role])))))

  (testing "defaults to auto mode"
    (let [result (tt/theme-toggle {})
          buttons (vec (nth result 2))]
      ;; auto (second button) should be active
      (is (= "theme-toggle-btn theme-toggle-btn-active"
             (get-in (nth buttons 1) [1 :class])))))

  (testing "light mode marks first button active"
    (let [result (tt/theme-toggle {:mode "light"})
          buttons (vec (nth result 2))]
      (is (= "theme-toggle-btn theme-toggle-btn-active"
             (get-in (nth buttons 0) [1 :class])))))

  (testing "dark mode marks last button active"
    (let [result (tt/theme-toggle {:mode "dark"})
          buttons (vec (nth result 2))]
      (is (= "theme-toggle-btn theme-toggle-btn-active"
             (get-in (nth buttons 2) [1 :class])))))

  (testing "custom class is appended"
    (let [result (tt/theme-toggle {:class "my-class"})]
      (is (= "theme-toggle my-class" (get-in result [1 :class])))))

  (testing "renders three buttons with correct aria-labels"
    (let [result (tt/theme-toggle {})
          buttons (vec (nth result 2))
          labels (mapv #(get-in % [1 :aria-label]) buttons)]
      (is (= ["Light theme" "System theme" "Dark theme"] labels))))

  (testing "all buttons have role=radio"
    (let [result (tt/theme-toggle {})
          buttons (vec (nth result 2))]
      (doseq [btn buttons]
        (is (= "radio" (get-in btn [1 :role])))))))
