(ns ui.icon-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.icon :as icon]))

(deftest icon-class-list-test
  (testing "default size (md)"
    (is (= ["icon"] (icon/icon-class-list {})))
    (is (= ["icon"] (icon/icon-class-list {:size :md}))))
  (testing "small"
    (is (= ["icon" "icon-sm"] (icon/icon-class-list {:size :sm}))))
  (testing "large"
    (is (= ["icon" "icon-lg"] (icon/icon-class-list {:size :lg}))))
  (testing "xl"
    (is (= ["icon" "icon-xl"] (icon/icon-class-list {:size :xl})))))

(deftest icon-classes-test
  (testing "default"
    (is (= "icon" (icon/icon-classes {}))))
  (testing "with size"
    (is (= "icon icon-sm" (icon/icon-classes {:size :sm})))
    (is (= "icon icon-lg" (icon/icon-classes {:size :lg})))))

(deftest icon-component-test
  (testing "renders SVG for known icon"
    (let [result (icon/icon {:icon-name :home})]
      (is (= :svg (first result)))
      (is (= "icon" (get-in result [1 :class])))
      (is (= "0 0 24 24" (get-in result [1 :viewBox])))
      (is (= "none" (get-in result [1 :fill])))
      (is (= "currentColor" (get-in result [1 :stroke])))
      (is (= "true" (get-in result [1 :aria-hidden])))))

  (testing "returns nil for unknown icon"
    (is (nil? (icon/icon {:icon-name :nonexistent-icon-xyz}))))

  (testing "applies size class"
    (let [result (icon/icon {:icon-name :search :size :lg})]
      (is (= "icon icon-lg" (get-in result [1 :class])))))

  (testing "appends custom class"
    (let [result (icon/icon {:icon-name :search :class "custom"})]
      (is (= "icon custom" (get-in result [1 :class])))))

  (testing "merges extra attrs"
    (let [result (icon/icon {:icon-name :check :attrs {:id "my-icon"}})]
      (is (= "my-icon" (get-in result [1 :id])))))

  (testing "home icon has correct number of children"
    (let [result (icon/icon {:icon-name :home})]
      ;; :svg + attrs + 2 path elements = 4 items
      (is (= 4 (count result)))))

  (testing "simple icon has correct path data"
    (let [result (icon/icon {:icon-name :check})
          path (nth result 2)]
      (is (= :path (first path)))
      (is (= "M20 6 9 17l-5-5" (get-in path [1 :d]))))))

(deftest icon-names-test
  (testing "icon-names contains expected icons"
    (is (contains? icon/icon-names :home))
    (is (contains? icon/icon-names :search))
    (is (contains? icon/icon-names :settings))
    (is (contains? icon/icon-names :user))
    (is (contains? icon/icon-names :mail))
    (is (contains? icon/icon-names :chevron-down))
    (is (contains? icon/icon-names :code))
    (is (contains? icon/icon-names :globe)))

  (testing "icon-names has reasonable count"
    (is (>= (count icon/icon-names) 40))))

(deftest all-icons-render-test
  (testing "every icon in the set renders successfully"
    (doseq [n icon/icon-names]
      (let [result (icon/icon {:icon-name n})]
        (is (some? result) (str "Icon " n " should render"))
        (is (= :svg (first result)) (str "Icon " n " should be an SVG"))))))
