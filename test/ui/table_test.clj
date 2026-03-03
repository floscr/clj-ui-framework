(ns ui.table-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.table :as table]))

(deftest table-class-list-test
  (testing "default table"
    (is (= ["table"] (table/table-class-list {}))))

  (testing "striped variant"
    (is (= ["table" "table--striped"] (table/table-class-list {:variant :striped}))))

  (testing "bordered variant"
    (is (= ["table" "table--bordered"] (table/table-class-list {:variant :bordered})))))

(deftest table-classes-test
  (testing "space-joined output"
    (is (= "table" (table/table-classes {})))
    (is (= "table table--striped" (table/table-classes {:variant :striped})))))

(deftest table-component-test
  (testing "renders a table with headers and rows"
    (let [result (table/table {:headers ["Name" "Email"]
                               :rows [["Alice" "alice@example.com"]
                                      ["Bob" "bob@example.com"]]})]
      ;; outer div.table-wrapper
      (is (= :div (first result)))
      (is (= "table-wrapper" (get-in result [1 :class])))
      ;; table element inside
      (let [tbl (nth result 2)]
        (is (= :table (first tbl)))
        (is (= "table" (get-in tbl [1 :class])))))))
