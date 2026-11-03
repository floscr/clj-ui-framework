(ns ui.tooltip-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.tooltip :as tooltip]))

(deftest tooltip-data-attrs-test
  (testing "plain tooltip needs no data attributes"
    (is (= {} (tooltip/tooltip-data-attrs {}))))

  (testing "options become the attributes the runtime reads"
    (is (= {:data-tooltip-kind "card"
            :data-tooltip-delay "300"
            :data-tooltip-side "bottom"
            :data-tooltip-tone "danger"
            :data-tooltip-theme "light"}
           (tooltip/tooltip-data-attrs {:content [:div] :delay 300 :side :bottom
                                        :tone :danger :theme :light}))))

  (testing "a zero delay is kept"
    (is (= "0" (:data-tooltip-delay (tooltip/tooltip-data-attrs {:delay 0}))))))

(deftest tooltip-label-test
  (testing "text only"
    (is (= [:span {:class "tooltip-label"} [:span "Copy"]]
           (tooltip/tooltip-label "Copy" nil))))

  (testing "shortcut keys become keycaps"
    (is (= [:span {:class "tooltip-keys"}
            [:kbd {:class "tooltip-key"} "⌘"]
            [:kbd {:class "tooltip-key"} "C"]]
           (last (tooltip/tooltip-label "Copy" ["⌘" "C"]))))))

(deftest tooltip-component-test
  (testing "wraps the trigger and carries the label in a template"
    (let [result (tooltip/tooltip {:text "Hello"} "Hover me")]
      (is (= :span (first result)))
      (is (= "tooltip" (get-in result [1 :class])))
      (is (= "Hover me" (nth result 2)))
      (is (= [:template {:class "tooltip-content"}
              [:span {:class "tooltip-label"} [:span "Hello"]]]
             (last result)))))

  (testing "rich content replaces the label and marks the kind"
    (let [result (tooltip/tooltip {:content [:div "Card"] :delay 600} "@mara")]
      (is (= "card" (get-in result [1 :data-tooltip-kind])))
      (is (= "600" (get-in result [1 :data-tooltip-delay])))
      (is (= [:div "Card"] (get-in result [3 2])))))

  (testing "extra class and attrs merge"
    (let [result (tooltip/tooltip {:text "Tip" :class "extra" :attrs {:tabindex 0}} "X")]
      (is (= "tooltip extra" (get-in result [1 :class])))
      (is (= 0 (get-in result [1 :tabindex]))))))

(deftest tooltip-card-test
  (is (= [:div {:class "tooltip-card"} "a" "b"]
         (tooltip/tooltip-card {} "a" "b"))))
