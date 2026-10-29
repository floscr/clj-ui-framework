(ns ui.list-row-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.list-row :as list-row]))

(deftest list-row-class-list-test
  (is (= ["list-row"] (list-row/list-row-class-list {})))
  (is (= ["list-row" "list-row-interactive"] (list-row/list-row-class-list {:interactive true})))
  (is (= "list-row list-row-interactive list-row-active"
         (list-row/list-row-classes {:interactive true :active true}))))

(deftest list-rows-test
  (let [result (list-row/list-rows {:class "x"} [:div "r"])]
    (is (= :div (first result)))
    (is (= "list-rows x" (get-in result [1 :class])))
    (is (= "list" (get-in result [1 :role])))
    (is (= [:div "r"] (nth result 2)))))

(deftest list-row-test
  (testing "a plain div with a span title"
    (let [result (list-row/list-row {:title "Favorites"})]
      (is (= :div (first result)))
      (is (= "list-row" (get-in result [1 :class])))
      (is (= "listitem" (get-in result [1 :role])))
      (is (= [:span {:class "list-row-text"} [:span {:class "list-row-title"} "Favorites"]]
             (nth result 2)))
      (is (= 3 (count result)) "no leading or trailing without content")))
  (testing "href makes the title a link, icon leads, meta trails"
    (let [result (list-row/list-row {:icon :music :title "T" :description "D" :meta "21" :href "/p"})]
      (is (= :div (first result)) "the row itself stays a div")
      (is (= "list-row list-row-interactive" (get-in result [1 :class])))
      (let [leading (nth result 2)]
        (is (= "list-row-leading" (get-in leading [1 :class])))
        (is (= :svg (first (nth leading 2)))))
      (is (= [:a {:class "list-row-title" :href "/p"} "T"] (get-in result [3 2])))
      (is (= [:span {:class "list-row-description"} "D"] (get-in result [3 3])))
      (is (= [:span {:class "list-row-trailing"} [:span {:class "list-row-meta"} "21"]]
             (nth result 4)))))
  (testing "thumb wins over icon"
    (let [leading (nth (list-row/list-row {:icon :music :thumb "/t.jpg" :title "T"}) 2)]
      (is (= [:img {:class "list-row-thumb" :src "/t.jpg" :alt ""}] (nth leading 2)))))
  (testing "trailing children after meta, active row"
    (let [result (list-row/list-row {:title "T" :active true} [:button "x"] nil)]
      (is (= "list-row list-row-active" (get-in result [1 :class])))
      (is (= "true" (get-in result [1 :aria-current])))
      (is (= [:span {:class "list-row-trailing"} [:button "x"]] (nth result 3)))))
  (testing "on-click makes the title a button; the handler itself is dropped on clj"
    (let [result (list-row/list-row {:title "T" :on-click (fn [_] nil)})]
      (is (= "list-row list-row-interactive" (get-in result [1 :class])))
      (is (= [:button {:class "list-row-title" :type "button"} "T"] (get-in result [2 2]))))))
