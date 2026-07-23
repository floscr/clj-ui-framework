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

;; ── Column & Table Tests ───────────────────────────────────────────

(deftest col-helpers-test
  (testing "col-name returns map with :key :label :render"
    (let [col (fb/col-name)]
      (is (= :name (:key col)))
      (is (= "Name" (:label col)))
      (is (fn? (:render col)))))

  (testing "col-name with overrides"
    (let [col (fb/col-name {:label "File" :width "200px"})]
      (is (= "File" (:label col)))
      (is (= "200px" (:width col)))))

  (testing "col-size defaults"
    (let [col (fb/col-size)]
      (is (= :size (:key col)))
      (is (= "Size" (:label col)))
      (is (= "90px" (:width col)))))

  (testing "col-modified defaults"
    (let [col (fb/col-modified)]
      (is (= :modified (:key col)))
      (is (= "130px" (:width col)))))

  (testing "col-type has render fn"
    (let [col (fb/col-type)
          rendered ((:render col) {:file-type :image})]
      (is (= "Image" rendered)))))

(deftest default-columns-test
  (testing "has 4 columns"
    (is (= 4 (count fb/default-columns))))
  (testing "keys are name, size, modified, type"
    (is (= [:name :size :modified :type]
           (mapv :key fb/default-columns)))))

(deftest file-table-test
  (testing "renders grid-based table with default columns"
    (let [items [{:name "test.txt" :file-type :document :size "1 KB" :modified "May 1"}]
          result (fb/file-table {:items items})]
      (is (= :div (first result)))
      (is (re-find #"fb-table-wrapper" (get-in result [1 :class])))
      ;; second element is the CSS Grid container
      (let [grid (nth result 2)]
        (is (= :div (first grid)))
        (is (re-find #"fb-table" (get-in grid [1 :class]))))))

  (testing "renders custom columns"
    (let [cols [{:key :name :label "File"}
                {:key :owner :label "Owner" :width "120px"}]
          items [{:name "a.txt" :owner "Alice"}]
          result (fb/file-table {:columns cols :items items})
          grid (nth result 2)
          ;; first child after tag+attrs is the header group
          header (nth grid 2)
          th1 (nth header 2)
          th2 (nth header 3)]
      ;; headers match custom labels — [:div attrs [:span "File"] ...]
      (is (= "File" (second (nth th1 2))))
      (is (= "Owner" (second (nth th2 2))))))

  (testing "sortable header gets active class"
    (let [result (fb/file-table {:items [] :sort-key :name :sort-dir :asc :on-sort identity})
          grid (nth result 2)
          header (nth grid 2)
          th1 (nth header 2)] ;; Name column
      (is (re-find #"fb-table-th-active" (get-in th1 [1 :class])))))

  (testing "selected row gets class"
    (let [items [{:name "a.txt" :file-type :file}]
          result (fb/file-table {:items items :selected-fn (constantly true)})
          grid (nth result 2)
          ;; first row is after header (index 3)
          row (nth grid 3)]
      (is (re-find #"fb-table-row-selected" (get-in row [1 :class]))))))

  (testing "grid-template-columns derived from column widths"
    (let [result (fb/file-table {:items [] :columns [{:key :name :label "Name"}
                                                     {:key :size :label "Size" :width "90px"}]})
          grid (nth result 2)
          style (get-in grid [1 :style])]
      (is (= "grid-template-columns: minmax(150px,1fr) 90px" style))))
