(ns ui.page-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.page :as page]))

(deftest page-class-list-test
  (testing "reading is the default and adds no modifier"
    (is (= ["page"] (page/page-class-list {})))
    (is (= ["page"] (page/page-class-list {:width :reading}))))
  (testing "other widths add a modifier"
    (is (= ["page" "page-narrow"] (page/page-class-list {:width :narrow})))
    (is (= ["page" "page-wide"] (page/page-class-list {:width :wide})))
    (is (= "page page-full" (page/page-classes {:width :full})))))

(deftest page-test
  (testing "renders a main with the children"
    (let [result (page/page {} [:p "a"] [:p "b"])]
      (is (= :main (first result)))
      (is (= "page" (get-in result [1 :class])))
      (is (= [[:p "a"] [:p "b"]] (subvec result 2)))))
  (testing "tag, width, class and attrs"
    (let [result (page/page {:tag :div :width :wide :class "x" :attrs {:id "p"}} "c")]
      (is (= :div (first result)))
      (is (= "page page-wide x" (get-in result [1 :class])))
      (is (= "p" (get-in result [1 :id]))))))

(deftest page-header-test
  (testing "title only"
    (let [result (page/page-header {:title "Settings"})]
      (is (= :header (first result)))
      (is (= "page-header" (get-in result [1 :class])))
      (is (= [:div {:class "page-header-text"} [:h1 {:class "page-title"} "Settings"]]
             (nth result 2)))
      (is (= 3 (count result)) "no actions container without actions")))
  (testing "description and actions"
    (let [result (page/page-header {:title "T" :description "D"} [:button "A"] nil)]
      (is (= [:p {:class "page-description"} "D"] (get-in result [2 3])))
      (is (= [:div {:class "page-header-actions"} [:button "A"]] (nth result 3))))))

(deftest page-section-test
  (testing "title, description, actions and content"
    (let [result (page/page-section {:title "Notifications" :description "What we send" :actions [:a "all"]}
                                    [:div "content"])]
      (is (= :section (first result)))
      (is (= "page-section" (get-in result [1 :class])))
      (let [head (nth result 2)]
        (is (= "page-section-head" (get-in head [1 :class])))
        (is (= [:h2 {:class "page-section-title"} "Notifications"] (get-in head [2 2])))
        (is (= [:p {:class "page-section-description"} "What we send"] (get-in head [2 3])))
        (is (= [:div {:class "page-section-actions"} [:a "all"]] (nth head 3))))
      (is (= [:div "content"] (nth result 3)))))
  (testing "no title, no head"
    (let [result (page/page-section {} [:div "c"])]
      (is (nil? (nth result 2)))
      (is (= [:div "c"] (nth result 3))))))

(deftest action-row-test
  (is (= ["action-row"] (page/action-row-class-list {})))
  (is (= ["action-row" "action-row-between"] (page/action-row-class-list {:align :between})))
  (let [result (page/action-row {:align :start} [:button "a"])]
    (is (= "action-row action-row-start" (get-in result [1 :class])))
    (is (= [:button "a"] (nth result 2)))))
