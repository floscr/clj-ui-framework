(ns ui.settings-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.settings :as settings]))

(deftest settings-group-test
  (testing "a div holding the rows"
    (let [result (settings/settings-group {:class "x" :attrs {:id "g"}} [:div "r1"] [:div "r2"])]
      (is (= :div (first result)))
      (is (= "settings-group x" (get-in result [1 :class])))
      (is (= "g" (get-in result [1 :id])))
      (is (= [[:div "r1"] [:div "r2"]] (subvec result 2))))))

(deftest settings-row-test
  (testing "label only, no control container"
    (let [result (settings/settings-row {:label "Mentions"})]
      (is (= "settings-row" (get-in result [1 :class])))
      (is (= [:div {:class "settings-row-text"} [:span {:class "settings-row-label"} "Mentions"]]
             (nth result 2)))
      (is (= 3 (count result)))))
  (testing "hint and control"
    (let [result (settings/settings-row {:label "L" :hint "H"} [:input {:type "checkbox"}] nil)]
      (is (= [:span {:class "settings-row-hint"} "H"] (get-in result [2 3])))
      (is (= [:div {:class "settings-row-control"} [:input {:type "checkbox"}]] (nth result 3))))))
