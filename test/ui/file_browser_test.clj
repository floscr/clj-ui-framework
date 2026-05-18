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
  (testing "renders table with default columns"
    (let [items [{:name "test.txt" :file-type :document :size "1 KB" :modified "May 1"}]
          result (fb/file-table {:items items})]
      (is (= :div (first result)))
      (is (re-find #"fb-table-wrapper" (get-in result [1 :class])))
      ;; second element is the table
      (let [table (nth result 2)]
        (is (= :table (first table)))
        (is (= "fb-table" (get-in table [1 :class]))))))

  (testing "renders custom columns"
    (let [cols [{:key :name :label "File"}
                {:key :owner :label "Owner" :width "120px"}]
          items [{:name "a.txt" :owner "Alice"}]
          result (fb/file-table {:columns cols :items items})
          table (nth result 2)
          ;; thead is after colgroup (or directly after attrs)
          thead (first (filter #(and (vector? %) (= :thead (first %))) (rest table)))
          header-row (second thead)
          th1 (nth header-row 1)
          th2 (nth header-row 2)]
      ;; headers match custom labels — [:th attrs [:span "File"] ...]
      (is (= "File" (second (nth th1 2))))
      (is (= "Owner" (second (nth th2 2))))))

  (testing "sortable header gets active class"
    (let [result (fb/file-table {:items [] :sort-key :name :sort-dir :asc :on-sort identity})
          table (nth result 2)
          thead (first (filter #(and (vector? %) (= :thead (first %))) (rest table)))
          header-row (second thead)
          th1 (nth header-row 1)] ;; Name column
      (is (re-find #"fb-table-th-active" (get-in th1 [1 :class])))))

  (testing "selected row gets class"
    (let [items [{:name "a.txt" :file-type :file}]
          result (fb/file-table {:items items :selected-fn (constantly true)})
          table (nth result 2)
          tbody (last table)
          row (second tbody)]
      (is (re-find #"fb-table-row-selected" (get-in row [1 :class]))))))

;; ── Drop Zone Tests ─────────────────────────────────────────────────

(deftest file-dropzone-test
  (testing "renders as label for SSR"
    (let [result (fb/file-dropzone {:id "upload"})]
      (is (= :label (first result)))
      (is (= "fb-dropzone" (get-in result [1 :class])))
      (is (= "upload" (get-in result [1 :for])))))

  (testing "includes hidden file input"
    (let [result (fb/file-dropzone {:id "upload" :accept "image/*" :multiple true})
          input (nth result 2)]
      (is (= :input (first input)))
      (is (= "file" (:type (second input))))
      (is (= "upload" (:id (second input))))
      (is (= "image/*" (:accept (second input))))
      (is (true? (:multiple (second input))))))

  (testing "disabled state adds class"
    (let [result (fb/file-dropzone {:id "upload" :disabled true})]
      (is (re-find #"fb-dropzone-disabled" (get-in result [1 :class])))))

  (testing "custom title and subtitle"
    (let [result (fb/file-dropzone {:id "upload"
                                    :title "Upload images"
                                    :subtitle "PNG or JPG up to 10MB"})
          content (nth result 3)
          title-el (nth content 3)
          subtitle-el (nth content 4)]
      (is (= "Upload images" (last title-el)))
      (is (= "PNG or JPG up to 10MB" (last subtitle-el))))))

(deftest file-dropzone-item-test
  (testing "basic file item delegates to file-progress"
    (let [result (fb/file-dropzone-item {:name "photo.jpg" :size "2.4 MB" :file-type :image})]
      (is (= :div (first result)))
      (is (= "fp-item" (get-in result [1 :class])))))

  (testing "error state adds class"
    (let [result (fb/file-dropzone-item {:name "bad.txt" :status :error})]
      (is (re-find #"fp-item-error" (get-in result [1 :class])))))

  (testing "complete state adds class"
    (let [result (fb/file-dropzone-item {:name "done.pdf" :status :complete})]
      (is (re-find #"fp-item-complete" (get-in result [1 :class])))))

  (testing "includes progress bar when progress is set"
    (let [result (fb/file-dropzone-item {:name "uploading.zip" :progress 45})
          info (nth result 3)  ;; fp-item-info
          progress-el (last info)]
      (is (= :div (first progress-el)))
      (is (re-find #"progress" (get-in progress-el [1 :class])))))

  (testing "no progress bar when complete"
    (let [result (fb/file-dropzone-item {:name "done.pdf" :progress 100 :status :complete})
          info (nth result 3)]
      ;; should only have name, no progress bar
      (is (not (some #(and (vector? %) (re-find #"progress" (str (get-in % [1 :class])))) info)))))

  (testing "remove button present when on-remove provided"
    (let [result (fb/file-dropzone-item {:name "f.txt" :on-remove identity})]
      (is (= :button (first (last result))))
      (is (re-find #"fp-item-remove" (get-in (last result) [1 :class])))))

  (testing "no remove button when on-remove absent"
    (let [result (fb/file-dropzone-item {:name "f.txt"})]
      ;; last element should not be a button
      (is (not= :button (first (last result)))))))

(deftest file-dropzone-list-test
  (testing "wraps children in container div"
    (let [child1 [:div "file1"]
          child2 [:div "file2"]
          result (fb/file-dropzone-list {} child1 child2)]
      (is (= :div (first result)))
      (is (re-find #"fp-list" (get-in result [1 :class]))))))

(deftest file-dropzone-overlay-test
  (testing "renders overlay with default text"
    (let [result (fb/file-dropzone-overlay {})]
      (is (= :div (first result)))
      (is (= "fb-dropzone-overlay" (get-in result [1 :class])))
      ;; has backdrop + content
      (let [backdrop (nth result 2)
            content (nth result 3)]
        (is (re-find #"fb-dropzone-overlay-backdrop" (get-in backdrop [1 :class])))
        (is (re-find #"fb-dropzone-overlay-content" (get-in content [1 :class]))))))

  (testing "custom title and subtitle"
    (let [result (fb/file-dropzone-overlay {:title "Drop here"
                                            :subtitle "Let go now"})
          content (nth result 3)
          title-el (nth content 3)
          subtitle-el (nth content 4)]
      (is (= "Drop here" (last title-el)))
      (is (= "Let go now" (last subtitle-el))))))
