(ns ui.button-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.button :as button]
            [ui.icon :as icon]))

(deftest button-class-list-test
  (testing "default variant and size"
    (is (= ["btn" "btn-secondary"] (button/button-class-list {}))))

  (testing "explicit variant"
    (is (= ["btn" "btn-primary"] (button/button-class-list {:variant :primary})))
    (is (= ["btn" "btn-ghost"] (button/button-class-list {:variant :ghost})))
    (is (= ["btn" "btn-danger"] (button/button-class-list {:variant :danger})))
    (is (= ["btn" "btn-success"] (button/button-class-list {:variant :success})))
    (is (= ["btn" "btn-link"] (button/button-class-list {:variant :link}))))

  (testing "explicit size"
    (is (= ["btn" "btn-secondary" "btn-sm"] (button/button-class-list {:size :sm})))
    (is (= ["btn" "btn-secondary"] (button/button-class-list {:size :md})))
    (is (= ["btn" "btn-secondary" "btn-lg"] (button/button-class-list {:size :lg}))))

  (testing "variant + size combined"
    (is (= ["btn" "btn-primary" "btn-lg"] (button/button-class-list {:variant :primary :size :lg})))))

(deftest button-classes-test
  (testing "default variant and size"
    (is (= "btn btn-secondary" (button/button-classes {}))))

  (testing "explicit variant"
    (is (= "btn btn-primary" (button/button-classes {:variant :primary})))
    (is (= "btn btn-ghost" (button/button-classes {:variant :ghost})))
    (is (= "btn btn-danger" (button/button-classes {:variant :danger})))
    (is (= "btn btn-secondary" (button/button-classes {:variant :secondary}))))

  (testing "explicit size"
    (is (= "btn btn-secondary btn-sm" (button/button-classes {:size :sm})))
    (is (= "btn btn-secondary" (button/button-classes {:size :md})))
    (is (= "btn btn-secondary btn-lg" (button/button-classes {:size :lg}))))

  (testing "variant + size combined"
    (is (= "btn btn-primary btn-lg" (button/button-classes {:variant :primary :size :lg})))
    (is (= "btn btn-danger btn-sm" (button/button-classes {:variant :danger :size :sm}))))

  (testing "nil variant falls back to default"
    (is (= "btn btn-secondary" (button/button-classes {:variant nil}))))

  (testing "nil size falls back to default (md, no size class)"
    (is (= "btn btn-secondary" (button/button-classes {:size nil}))))

  (testing "string variants work"
    (is (= "btn btn-primary" (button/button-classes {:variant "primary"})))))

(deftest button-component-test
  (testing "basic button renders correct hiccup (clj target)"
    (let [result (button/button {:variant :primary} "Click me")]
      (is (= :button (first result)))
      (is (= "btn btn-primary" (get-in result [1 :class])))
      (is (= "Click me" (nth result 2)))))

  (testing "disabled button has disabled attr"
    (let [result (button/button {:variant :primary :disabled true} "Disabled")]
      (is (true? (get-in result [1 :disabled])))))

  (testing "non-disabled button has no disabled attr"
    (let [result (button/button {:variant :primary} "Enabled")]
      (is (nil? (get-in result [1 :disabled])))))

  (testing "extra class gets appended"
    (let [result (button/button {:variant :primary :class "extra"} "Test")]
      (is (= "btn btn-primary extra" (get-in result [1 :class])))))

  (testing "extra attrs get merged"
    (let [result (button/button {:variant :primary :attrs {:id "my-btn"}} "Test")]
      (is (= "my-btn" (get-in result [1 :id])))))

  (testing "multiple children"
    (let [result (button/button {:variant :primary} "A" "B")]
      (is (= "A" (nth result 2)))
      (is (= "B" (nth result 3)))))

  (testing "href renders as <a> tag"
    (let [result (button/button {:variant :primary :href "/about"} "About")]
      (is (= :a (first result)))
      (is (= "/about" (get-in result [1 :href])))
      (is (= "btn btn-primary" (get-in result [1 :class])))
      (is (= "About" (nth result 2)))))

  (testing "no href renders as <button> tag"
    (let [result (button/button {:variant :primary} "Click")]
      (is (= :button (first result)))))

  (testing "link variant"
    (let [result (button/button {:variant :link} "Learn more")]
      (is (= :button (first result)))
      (is (= "btn btn-link" (get-in result [1 :class])))))

  (testing "link variant with href renders as <a>"
    (let [result (button/button {:variant :link :href "https://example.com"} "Visit")]
      (is (= :a (first result)))
      (is (= "https://example.com" (get-in result [1 :href])))
      (is (= "btn btn-link" (get-in result [1 :class]))))))

(deftest button-loading-test
  (testing "loading adds btn-loading class"
    (is (= ["btn" "btn-secondary" "btn-loading"]
           (button/button-class-list {:loading true})))
    (is (= ["btn" "btn-primary" "btn-sm" "btn-loading"]
           (button/button-class-list {:variant :primary :size :sm :loading true}))))

  (testing "loading button is disabled"
    (let [result (button/button {:variant :primary :loading true} "Saving")]
      (is (true? (get-in result [1 :disabled])))))

  (testing "loading button renders spinner before children"
    (let [result (button/button {:variant :primary :loading true} "Saving")]
      (is (= :span (first (nth result 2))))
      (is (= "spinner spinner-sm" (get-in (nth result 2) [1 :class])))
      (is (= "Saving" (nth result 3)))))

  (testing "sm loading button uses xs spinner"
    (let [result (button/button {:variant :primary :size :sm :loading true} "Saving")]
      (is (= "spinner spinner-xs" (get-in (nth result 2) [1 :class])))))

  (testing "loading replaces icon-left with spinner"
    (let [result (button/button {:variant :primary :icon-left :plus :loading true} "Add")]
      (is (= :span (first (nth result 2))))
      (is (= "Add" (nth result 3)))))

  (testing "loading replaces icon-only icon with spinner"
    (let [result (button/button {:variant :primary :icon :plus :loading true})]
      (is (= :span (first (nth result 2))))
      (is (= "spinner spinner-sm" (get-in (nth result 2) [1 :class]))))))

(deftest button-icon-class-list-test
  (testing "icon-only adds btn-icon class"
    (is (= ["btn" "btn-secondary" "btn-icon"] (button/button-class-list {:icon :plus})))
    (is (= ["btn" "btn-primary" "btn-icon"] (button/button-class-list {:variant :primary :icon :plus}))))

  (testing "icon-only with size"
    (is (= ["btn" "btn-primary" "btn-sm" "btn-icon"]
           (button/button-class-list {:variant :primary :size :sm :icon :plus})))))

(deftest button-round-test
  (testing "round class added when icon and round"
    (is (= ["btn" "btn-secondary" "btn-icon" "btn-icon-round"]
           (button/button-class-list {:icon :plus :round true}))))

  (testing "round class not added without icon"
    (is (= ["btn" "btn-secondary"]
           (button/button-class-list {:round true}))))

  (testing "round class with variant and size"
    (is (= ["btn" "btn-primary" "btn-sm" "btn-icon" "btn-icon-round"]
           (button/button-class-list {:variant :primary :size :sm :icon :plus :round true})))))

(deftest button-icon-component-test
  (testing "icon-only button renders icon child"
    (let [result (button/button {:variant :primary :icon :plus})]
      (is (= :button (first result)))
      (is (= "btn btn-primary btn-icon" (get-in result [1 :class])))
      ;; third element should be the SVG icon
      (is (= :svg (first (nth result 2))))))

  (testing "icon-left renders icon before text"
    (let [result (button/button {:variant :primary :icon-left :plus} "Add")]
      (is (= :button (first result)))
      (is (= :svg (first (nth result 2))))   ;; icon is first child
      (is (= "Add" (nth result 3)))))         ;; text is second child

  (testing "icon-right renders icon after text"
    (let [result (button/button {:variant :primary :icon-right :arrow-right} "Next")]
      (is (= :button (first result)))
      (is (= "Next" (nth result 2)))          ;; text is first child
      (is (= :svg (first (nth result 3))))))  ;; icon is second child

  (testing "icon-left and icon-right together"
    (let [result (button/button {:variant :primary :icon-left :plus :icon-right :arrow-right} "Add")]
      (is (= :svg (first (nth result 2))))    ;; left icon
      (is (= "Add" (nth result 3)))           ;; text
      (is (= :svg (first (nth result 4)))))) ;; right icon
  )
