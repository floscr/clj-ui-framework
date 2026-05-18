(ns ui.file-progress-test
  (:require [clojure.test :refer [deftest testing is]]
            [ui.file-progress :as fp]
            [ui.icon :as icon]))

(deftest file-progress-item-test
  (testing "basic item with icon"
    (let [ic (icon/icon {:icon-name :file :size :sm})
          result (fp/file-progress-item {:name "report.pdf" :size "1.2 MB" :icon ic})]
      (is (= :div (first result)))
      (is (= "fp-item" (get-in result [1 :class])))
      ;; icon slot
      (let [icon-div (nth result 2)]
        (is (re-find #"fp-item-icon" (get-in icon-div [1 :class]))))
      ;; info div with name and size
      (let [info (nth result 3)
            name-div (nth info 2)
            size-div (nth info 3)]
        (is (re-find #"fp-item-name" (get-in name-div [1 :class])))
        (is (= "report.pdf" (last name-div)))
        (is (re-find #"fp-item-size" (get-in size-div [1 :class])))
        (is (= "1.2 MB" (last size-div))))))

  (testing "item without icon renders nil icon slot"
    (let [result (fp/file-progress-item {:name "file.txt"})]
      ;; icon slot is nil when no icon provided
      (is (nil? (nth result 2)))
      ;; info div is always at index 3
      (let [info (nth result 3)]
        (is (re-find #"fp-item-info" (get-in info [1 :class]))))))

  (testing "error state"
    (let [result (fp/file-progress-item {:name "bad.txt" :status :error})]
      (is (re-find #"fp-item-error" (get-in result [1 :class])))))

  (testing "complete state"
    (let [result (fp/file-progress-item {:name "done.pdf" :status :complete})]
      (is (re-find #"fp-item-complete" (get-in result [1 :class])))))

  (testing "progress bar shown when uploading"
    (let [result (fp/file-progress-item {:name "up.zip" :progress 45 :icon nil})
          info (nth result 3) ;; info always at index 3
          progress-el (last info)]
      (is (= :div (first progress-el)))
      (is (re-find #"progress" (get-in progress-el [1 :class])))))

  (testing "no progress bar when complete"
    (let [result (fp/file-progress-item {:name "done.pdf" :progress 100 :status :complete :icon nil})
          info (nth result 3)]
      (is (not (some #(and (vector? %) (re-find #"progress" (str (get-in % [1 :class])))) info)))))

  (testing "remove button present when on-remove"
    (let [result (fp/file-progress-item {:name "f.txt" :on-remove identity})]
      (is (= :button (first (last result))))
      (is (re-find #"fp-item-remove" (get-in (last result) [1 :class])))))

  (testing "no remove button without on-remove"
    (let [result (fp/file-progress-item {:name "f.txt"})]
      (is (not= :button (first (last result))))))

  (testing "custom class added"
    (let [result (fp/file-progress-item {:name "f.txt" :class "my-custom"})]
      (is (re-find #"my-custom" (get-in result [1 :class]))))))

(deftest file-progress-list-test
  (testing "wraps children in continuous list container"
    (let [child1 [:div "a"]
          child2 [:div "b"]
          result (fp/file-progress-list {} child1 child2)]
      (is (= :div (first result)))
      (is (re-find #"fp-list" (get-in result [1 :class])))
      ;; children are inside
      (is (= child1 (nth result 2)))
      (is (= child2 (nth result 3)))))

  (testing "custom class added"
    (let [result (fp/file-progress-list {:class "extra"} [:div "x"])]
      (is (re-find #"extra" (get-in result [1 :class]))))))
