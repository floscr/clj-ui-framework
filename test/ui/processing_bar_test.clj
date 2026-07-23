(ns ui.processing-bar-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.processing-bar :as pb]))

(defn- attrs-of [el] (second el))

(deftest processing-item-test
  (testing "minimal item — status only"
    (let [[tag attrs & children] (pb/processing-item {:status "Analyzing..."})]
      (is (= :div tag))
      (is (= "processing-bar-item" (:class attrs)))
      ;; no thumb rendered
      (is (nil? (first children)))
      (let [info (second children)]
        (is (= "processing-bar-info" (:class (attrs-of info))))
        ;; no label rendered
        (is (nil? (nth info 2)))
        (let [status (nth info 3)]
          (is (= "processing-bar-status" (:class (attrs-of status))))
          (is (= [:span "Analyzing..."] (last status)))))))

  (testing "thumb and label"
    (let [[_ _ thumb info] (pb/processing-item {:thumb "/t.jpg"
                                                :label "3 photos"
                                                :status "Working"})]
      (is (= "processing-bar-thumb" (:class (attrs-of thumb))))
      (is (= [:img {:src "/t.jpg" :alt ""}] (nth thumb 2)))
      (is (= [:div {:class "processing-bar-label"} "3 photos"] (nth info 2)))))

  (testing "custom class and attrs"
    (let [attrs (attrs-of (pb/processing-item {:status "x"
                                               :class "extra"
                                               :attrs {:id "job-1"}}))]
      (is (= "processing-bar-item extra" (:class attrs)))
      (is (= "job-1" (:id attrs))))))

(deftest processing-bar-test
  (testing "title and children"
    (let [item (pb/processing-item {:status "Working"})
          [tag attrs header list] (pb/processing-bar {:title "2 jobs"} item item)]
      (is (= :div tag))
      (is (= "processing-bar" (:class attrs)))
      (is (= [:div {:class "processing-bar-header"}
              [:span {:class "processing-bar-title"} "2 jobs"]]
             header))
      (is (= "processing-bar-list" (:class (attrs-of list))))
      (is (= 2 (count (drop 2 list))))))

  (testing "no title omits header"
    (let [[_ _ header list] (pb/processing-bar {} (pb/processing-item {:status "x"}))]
      (is (nil? header))
      (is (= "processing-bar-list" (:class (attrs-of list))))))

  (testing "custom class"
    (is (= "processing-bar mb-4"
           (:class (attrs-of (pb/processing-bar {:class "mb-4"})))))))
