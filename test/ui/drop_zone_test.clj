(ns ui.drop-zone-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.drop-zone :as drop-zone]))

(deftest drop-zone-class-list-test
  (testing "defaults to md size"
    (is (= ["drop-zone" "drop-zone-md"] (drop-zone/drop-zone-class-list {})))
    (is (= "drop-zone drop-zone-md" (drop-zone/drop-zone-classes {}))))

  (testing "explicit size"
    (is (= ["drop-zone" "drop-zone-lg"]
           (drop-zone/drop-zone-class-list {:size :lg}))))

  (testing "drag-over adds active class"
    (is (= ["drop-zone" "drop-zone-md" "drop-zone-active"]
           (drop-zone/drop-zone-class-list {:drag-over? true}))))

  (testing "disabled adds disabled class"
    (is (= ["drop-zone" "drop-zone-md" "drop-zone-disabled"]
           (drop-zone/drop-zone-class-list {:disabled true})))))

(deftest drop-zone-empty-test
  (testing "renders a label with hidden file input and empty state"
    (let [result (drop-zone/drop-zone {:title "Upload" :hint "Drag & drop"})]
      (is (= :label (first result)))
      (is (= "drop-zone drop-zone-md" (get-in result [1 :class])))
      (is (= "" (get-in result [1 :data-ui-drop-zone])))
      (let [[tag attrs] (nth result 2)]
        (is (= :input tag))
        (is (= "file" (:type attrs)))
        (is (= "image/*" (:accept attrs)))
        (is (true? (:multiple attrs)))
        (is (= "sr-only" (:class attrs))))
      (let [[tag attrs icon-el title-el hint-el] (nth result 3)]
        (is (= :div tag))
        (is (= "drop-zone-empty" (:class attrs)))
        (is (= :svg (first icon-el)))
        (is (= [:p {:class "drop-zone-title"} "Upload"] title-el))
        (is (= [:p {:class "drop-zone-hint"} "Drag & drop"] hint-el)))))

  (testing "accept and multiple overrides"
    (let [result (drop-zone/drop-zone {:accept "application/pdf" :multiple false})
          attrs (second (nth result 2))]
      (is (= "application/pdf" (:accept attrs)))
      (is (false? (:multiple attrs)))))

  (testing "extra class gets appended"
    (let [result (drop-zone/drop-zone {:class "extra"})]
      (is (= "drop-zone drop-zone-md extra" (get-in result [1 :class]))))))

(deftest drop-zone-children-test
  (testing "children render in a content wrapper div (no input)"
    (let [result (drop-zone/drop-zone {} [:p "thumbs"])]
      (is (= :div (first result)))
      (is (= "" (get-in result [1 :data-ui-drop-zone])))
      (let [[tag attrs child] (nth result 2)]
        (is (= :div tag))
        (is (= "drop-zone-content" (:class attrs)))
        (is (= [:p "thumbs"] child))))))

(deftest file-label-test
  (testing "renders a label with hidden input and children"
    (let [result (drop-zone/file-label {} "+ Add more")]
      (is (= :label (first result)))
      (is (= "drop-zone-add" (get-in result [1 :class])))
      (let [[tag attrs] (nth result 2)]
        (is (= :input tag))
        (is (= "file" (:type attrs)))
        (is (= "sr-only" (:class attrs))))
      (is (= "+ Add more" (nth result 3)))))

  (testing "extra class gets appended"
    (let [result (drop-zone/file-label {:class "extra"} "x")]
      (is (= "drop-zone-add extra" (get-in result [1 :class]))))))

(deftest drop-zone-disabled-test
  (testing "disabled zone has no data attribute and a disabled input"
    (let [result (drop-zone/drop-zone {:disabled true})]
      (is (= "drop-zone drop-zone-md drop-zone-disabled"
             (get-in result [1 :class])))
      (is (nil? (get-in result [1 :data-ui-drop-zone])))
      (let [attrs (second (nth result 2))]
        (is (true? (:disabled attrs)))))))

(deftest drop-zone-overlay-test
  (testing "renders overlay with backdrop and content"
    (let [result (drop-zone/drop-zone-overlay {})]
      (is (= :div (first result)))
      (is (= "drop-zone-overlay" (get-in result [1 :class])))
      (let [backdrop (nth result 2)
            content (nth result 3)]
        (is (= "drop-zone-overlay-backdrop" (get-in backdrop [1 :class])))
        (is (= "drop-zone-overlay-content" (get-in content [1 :class])))
        (is (= [:div {:class "drop-zone-title"} "Drop files to upload"]
               (nth content 3)))
        (is (= [:div {:class "drop-zone-hint"} "Release to add your files"]
               (nth content 4))))))

  (testing "custom title and hint"
    (let [result (drop-zone/drop-zone-overlay {:title "Drop here" :hint "Let go"})
          content (nth result 3)]
      (is (= "Drop here" (last (nth content 3))))
      (is (= "Let go" (last (nth content 4)))))))
