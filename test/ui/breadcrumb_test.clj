(ns ui.breadcrumb-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.breadcrumb :as breadcrumb]))

(deftest breadcrumb-component-test
  (testing "renders a nav with breadcrumb list"
    (let [result (breadcrumb/breadcrumb
                   {:items [{:label "Home" :href "/"}
                            {:label "Projects" :href "/projects"}
                            {:label "Current"}]})]
      (is (= :nav (first result)))
      (is (= "Breadcrumb" (get-in result [1 :aria-label])))
      ;; ol is inside
      (let [ol (nth result 2)]
        (is (= :ol (first ol)))
        (is (= "breadcrumb" (get-in ol [1 :class])))
        ;; 3 items
        (is (= 3 (count (drop 2 ol)))))))

  (testing "last item is active"
    (let [result (breadcrumb/breadcrumb
                   {:items [{:label "Home" :href "/"}
                            {:label "Active"}]})
          ol (nth result 2)
          items (drop 2 ol)
          last-item (last items)]
      (is (clojure.string/includes? (get-in last-item [1 :class]) "breadcrumb-item--active")))))

(deftest breadcrumb-links-test
  (testing "non-active items have links"
    (let [result (breadcrumb/breadcrumb
                   {:items [{:label "Home" :href "/"}
                            {:label "End"}]})
          ol (nth result 2)
          first-item (nth ol 2)]
      ;; first item should have an anchor child
      (is (= :a (first (nth first-item 2)))))))
