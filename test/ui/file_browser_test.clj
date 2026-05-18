(ns ui.file-browser-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.file-browser :as fb]))

(deftest file-type->icon-test
  (testing "known file types"
    (is (= :folder (fb/file-type->icon :folder)))
    (is (= :image (fb/file-type->icon :image)))
    (is (= :film (fb/file-type->icon :video)))
    (is (= :music (fb/file-type->icon :audio)))
    (is (= :file-text (fb/file-type->icon :document)))
    (is (= :file-spreadsheet (fb/file-type->icon :spreadsheet)))
    (is (= :code (fb/file-type->icon :code)))
    (is (= :package (fb/file-type->icon :archive))))

  (testing "unknown type falls back to :file"
    (is (= :file (fb/file-type->icon :unknown)))
    (is (= :file (fb/file-type->icon nil)))))

(deftest file-type-icon-test
  (testing "renders icon with correct class"
    (let [result (fb/file-type-icon {:file-type :folder})]
      (is (= :div (first result)))
      (is (= "fb-item-icon" (get-in result [1 :class]))))))

(deftest file-item-grid-test
  (testing "basic grid item"
    (let [item {:name "photo.jpg" :file-type :image :size "2.4 MB"}
          result (fb/file-item-grid {:item item})]
      (is (= :div (first result)))
      (is (= "fb-item fb-item-grid" (get-in result [1 :class])))))

  (testing "selected grid item"
    (let [item {:name "photo.jpg" :file-type :image}
          result (fb/file-item-grid {:item item :selected true})]
      (is (= "fb-item fb-item-grid fb-item-selected" (get-in result [1 :class])))))

  (testing "grid item with context menu wraps in trigger"
    (let [item {:name "photo.jpg" :file-type :image}
          result (fb/file-item-grid {:item item
                                     :context-menu-items [{:label "Open" :icon :folder}]})]
      ;; context-menu-trigger wraps in a div
      (is (= :div (first result)))
      (is (re-find #"context-menu-trigger" (get-in result [1 :class])))))

  (testing "grid item without context menu returns bare div"
    (let [item {:name "photo.jpg" :file-type :image}
          result (fb/file-item-grid {:item item})]
      (is (= :div (first result)))
      (is (not (re-find #"context-menu" (get-in result [1 :class])))))))

(deftest file-item-list-test
  (testing "basic list item"
    (let [item {:name "doc.pdf" :file-type :document :size "1.2 MB" :modified "May 12"}
          result (fb/file-item-list {:item item})]
      (is (= :div (first result)))
      (is (= "fb-item fb-item-list" (get-in result [1 :class])))))

  (testing "selected list item"
    (let [item {:name "doc.pdf" :file-type :document}
          result (fb/file-item-list {:item item :selected true})]
      (is (= "fb-item fb-item-list fb-item-selected" (get-in result [1 :class]))))))

(deftest file-list-header-test
  (testing "renders header with columns"
    (let [result (fb/file-list-header {})]
      (is (= :div (first result)))
      (is (= "fb-list-header" (get-in result [1 :class])))))

  (testing "sortable header cells get sortable class"
    (let [result (fb/file-list-header {:on-sort identity})
          ;; first cell is Name (index 2, after tag and attrs)
          name-cell (nth result 2)]
      (is (re-find #"fb-list-header-cell-sortable" (get-in name-cell [1 :class])))))

  (testing "active sort column gets active class and indicator"
    (let [result (fb/file-list-header {:sort-key :name :sort-dir :asc :on-sort identity})
          name-cell (nth result 2)]
      (is (re-find #"fb-list-header-cell-active" (get-in name-cell [1 :class]))))))

(deftest file-view-toggle-test
  (testing "grid view active"
    (let [result (fb/file-view-toggle {:view :grid})]
      (is (= :div (first result)))))

  (testing "list view active"
    (let [result (fb/file-view-toggle {:view :list})]
      (is (= :div (first result))))))
