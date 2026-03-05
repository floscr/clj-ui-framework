(ns ui.sidebar-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.sidebar :as sidebar]))

;; ── Class generation ────────────────────────────────────────────────

(deftest sidebar-layout-class-list-test
  (testing "returns sidebar-layout class"
    (is (= ["sidebar-layout"] (sidebar/sidebar-layout-class-list {})))))

(deftest sidebar-class-list-test
  (testing "returns sidebar class"
    (is (= ["sidebar"] (sidebar/sidebar-class-list {})))))

(deftest sidebar-brand-class-list-test
  (testing "returns sidebar-brand class"
    (is (= ["sidebar-brand"] (sidebar/sidebar-brand-class-list {})))))

(deftest sidebar-search-class-list-test
  (testing "returns sidebar-search class"
    (is (= ["sidebar-search"] (sidebar/sidebar-search-class-list {})))))

(deftest sidebar-group-class-list-test
  (testing "returns sidebar-group class"
    (is (= ["sidebar-group"] (sidebar/sidebar-group-class-list {})))))

(deftest sidebar-menu-item-class-list-test
  (testing "default item"
    (is (= ["sidebar-menu-item"] (sidebar/sidebar-menu-item-class-list {}))))
  (testing "active item"
    (is (= ["sidebar-menu-item" "sidebar-menu-item-active"]
           (sidebar/sidebar-menu-item-class-list {:active true}))))
  (testing "inactive item"
    (is (= ["sidebar-menu-item"]
           (sidebar/sidebar-menu-item-class-list {:active false})))))

(deftest sidebar-menu-item-classes-test
  (testing "default"
    (is (= "sidebar-menu-item" (sidebar/sidebar-menu-item-classes {}))))
  (testing "active"
    (is (= "sidebar-menu-item sidebar-menu-item-active"
           (sidebar/sidebar-menu-item-classes {:active true})))))

;; ── Component rendering (clj target) ───────────────────────────────

(deftest sidebar-layout-test
  (testing "renders div with sidebar-layout class"
    (let [result (sidebar/sidebar-layout {} [:div "Content"])]
      (is (= :div (first result)))
      (is (= "sidebar-layout" (get-in result [1 :class])))))
  (testing "extra class appended"
    (let [result (sidebar/sidebar-layout {:class "custom"} [:div])]
      (is (= "sidebar-layout custom" (get-in result [1 :class]))))))

(deftest sidebar-test
  (testing "renders aside tag"
    (let [result (sidebar/sidebar {} [:div "Nav"])]
      (is (= :aside (first result)))
      (is (= "sidebar" (get-in result [1 :class]))))))

(deftest sidebar-header-test
  (testing "renders header div"
    (let [result (sidebar/sidebar-header {} [:span "Brand"])]
      (is (= :div (first result)))
      (is (= "sidebar-header" (get-in result [1 :class]))))))

(deftest sidebar-brand-test
  (testing "renders div by default"
    (let [result (sidebar/sidebar-brand {:title "Acme" :subtitle "v1.0"})]
      (is (= :div (first result)))
      (is (= "sidebar-brand" (get-in result [1 :class])))))
  (testing "renders <a> with href"
    (let [result (sidebar/sidebar-brand {:title "Acme" :href "/"})]
      (is (= :a (first result)))
      (is (= "/" (get-in result [1 :href])))))
  (testing "icon defaults to first char of title"
    (let [result (sidebar/sidebar-brand {:title "Acme"})]
      ;; The icon span is at index 2
      (is (= "A" (nth (nth result 2) 2))))))

(deftest sidebar-search-test
  (testing "renders search input"
    (let [result (sidebar/sidebar-search {:placeholder "Find..."})]
      (is (= :div (first result)))
      (is (= "sidebar-search" (get-in result [1 :class])))
      ;; input is the third child (after class div and icon span)
      (let [input (nth result 3)]
        (is (= :input (first input)))
        (is (= "Find..." (get-in input [1 :placeholder])))))))

(deftest sidebar-content-test
  (testing "renders nav tag"
    (let [result (sidebar/sidebar-content {} [:div "Groups"])]
      (is (= :nav (first result)))
      (is (= "sidebar-content" (get-in result [1 :class]))))))

(deftest sidebar-group-test
  (testing "renders label when provided"
    (let [result (sidebar/sidebar-group {:label "Section"} [:ul "Items"])]
      (is (= :div (first result)))
      (is (= "sidebar-group" (get-in result [1 :class])))
      ;; Label is the second element
      (let [label (nth result 2)]
        (is (= "sidebar-group-label" (get-in label [1 :class])))
        (is (= "Section" (nth label 2))))))
  (testing "no label when omitted"
    (let [result (sidebar/sidebar-group {} [:ul "Items"])]
      ;; Should only have attrs + children, no label div
      (is (= [:ul "Items"] (nth result 2))))))

(deftest sidebar-menu-test
  (testing "wraps children in <li> tags"
    (let [result (sidebar/sidebar-menu {} [:a "Link1"] [:a "Link2"])]
      (is (= :ul (first result)))
      (is (= "sidebar-menu" (get-in result [1 :class])))
      (is (= [:li [:a "Link1"]] (nth result 2)))
      (is (= [:li [:a "Link2"]] (nth result 3))))))

(deftest sidebar-menu-item-test
  (testing "renders <a> with href"
    (let [result (sidebar/sidebar-menu-item {:href "/about"} "About")]
      (is (= :a (first result)))
      (is (= "/about" (get-in result [1 :href])))
      (is (= "sidebar-menu-item" (get-in result [1 :class])))))
  (testing "renders <button> without href"
    (let [result (sidebar/sidebar-menu-item {} "Click")]
      (is (= :button (first result)))))
  (testing "active class"
    (let [result (sidebar/sidebar-menu-item {:active true :href "#"} "Active")]
      (is (= "sidebar-menu-item sidebar-menu-item-active" (get-in result [1 :class])))))
  (testing "badge"
    (let [result (sidebar/sidebar-menu-item {:href "#" :badge "3"} "Messages")]
      (is (= "Messages" (nth result 2)))
      (let [badge (nth result 3)]
        (is (= "sidebar-menu-item-badge" (get-in badge [1 :class])))
        (is (= "3" (nth badge 2)))))))

(deftest sidebar-collapsible-test
  (testing "renders <details> with summary"
    (let [result (sidebar/sidebar-collapsible {:title "Section" :open true} [:ul "Items"])]
      (is (= :details (first result)))
      (is (= "sidebar-collapsible" (get-in result [1 :class])))
      (is (true? (get-in result [1 :open])))
      ;; summary
      (let [summary (nth result 2)]
        (is (= :summary (first summary)))
        (is (= "Section" (nth (nth summary 1) 1)))))))

(deftest sidebar-separator-test
  (testing "renders <hr>"
    (let [result (sidebar/sidebar-separator)]
      (is (= :hr (first result)))
      (is (= "sidebar-separator" (get-in result [1 :class]))))))

(deftest sidebar-footer-test
  (testing "renders footer div"
    (let [result (sidebar/sidebar-footer {} [:span "User"])]
      (is (= :div (first result)))
      (is (= "sidebar-footer" (get-in result [1 :class]))))))

(deftest sidebar-user-test
  (testing "renders user block"
    (let [result (sidebar/sidebar-user {:user-name "Alice" :email "alice@example.com"})]
      (is (= :div (first result)))
      (is (= "sidebar-user" (get-in result [1 :class])))
      ;; avatar defaults to first 2 chars
      (let [avatar (nth result 2)]
        (is (= "Al" (nth avatar 2))))
      ;; name
      (let [info (nth result 3)
            name-span (nth info 2)]
        (is (= "Alice" (nth name-span 2)))))))
