(ns ui.grid-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.grid :as grid]))

(deftest grid-class-list-test
  (testing "defaults: square layout, size m"
    (is (= ["tile-grid"] (grid/grid-class-list {}))))
  (testing "masonry layout"
    (is (= ["tile-grid" "tile-grid-masonry"]
           (grid/grid-class-list {:layout :masonry}))))
  (testing "non-default size"
    (is (= ["tile-grid" "tile-grid-xl"]
           (grid/grid-class-list {:size :xl}))))
  (testing "default size m adds no class"
    (is (= ["tile-grid"] (grid/grid-class-list {:size :m}))))
  (testing "masonry + size"
    (is (= ["tile-grid" "tile-grid-masonry" "tile-grid-s"]
           (grid/grid-class-list {:layout :masonry :size :s}))))
  (testing "string values"
    (is (= ["tile-grid" "tile-grid-masonry" "tile-grid-l"]
           (grid/grid-class-list {:layout "masonry" :size "l"})))))

(deftest grid-classes-test
  (is (= "tile-grid tile-grid-masonry" (grid/grid-classes {:layout :masonry}))))

(deftest grid-component-test
  (testing "renders a div with grid classes and children"
    (let [[tag attrs & children] (grid/grid {} [:span "a"] [:span "b"])]
      (is (= :div tag))
      (is (= "tile-grid" (:class attrs)))
      (is (= [[:span "a"] [:span "b"]] (vec children)))))
  (testing ":min sets --tile-min inline style"
    (let [[_ attrs] (grid/grid {:min "180px"})]
      (is (= "--tile-min: 180px;" (:style attrs)))))
  (testing "no :min, no style"
    (let [[_ attrs] (grid/grid {})]
      (is (nil? (:style attrs)))))
  (testing "extra class and attrs"
    (let [[_ attrs] (grid/grid {:layout :masonry :class "my-grid"
                                :attrs {:id "g"}})]
      (is (= "tile-grid tile-grid-masonry my-grid" (:class attrs)))
      (is (= "g" (:id attrs))))))

(deftest layout-toggle-test
  (testing "renders toolbar-group with default grid/masonry buttons"
    (let [[tag attrs & btns] (grid/layout-toggle {:value :grid})]
      (is (= :div tag))
      (is (= "toolbar-group" (:class attrs)))
      (is (= 2 (count btns)))))
  (testing "active option gets btn-toggled and aria-pressed"
    (let [[_ _ grid-btn masonry-btn] (grid/layout-toggle {:value :masonry})
          grid-attrs (second grid-btn)
          masonry-attrs (second masonry-btn)]
      (is (not (clojure.string/includes? (:class grid-attrs) "btn-toggled")))
      (is (= "false" (:aria-pressed grid-attrs)))
      (is (clojure.string/includes? (:class masonry-attrs) "btn-toggled"))
      (is (= "true" (:aria-pressed masonry-attrs)))))
  (testing "labels become title and aria-label"
    (let [[_ _ grid-btn] (grid/layout-toggle {:value :grid})
          attrs (second grid-btn)]
      (is (= "Square grid" (:title attrs)))
      (is (= "Square grid" (:aria-label attrs)))))
  (testing "href mode renders anchors (server-rendered pages)"
    (let [[_ _ a b] (grid/layout-toggle
                     {:value :grid
                      :options [{:value :grid :icon :grid :label "Grid"
                                 :href "/?view=grid"}
                                {:value :list :icon :list :label "List"
                                 :href "/?view=list"}]})]
      (is (= :a (first a)))
      (is (= "/?view=grid" (:href (second a))))
      (is (= :a (first b)))
      (is (= "/?view=list" (:href (second b)))))))

(deftest step-size-test
  (testing "steps up and down"
    (is (= :l (grid/step-size grid/sizes :m 1)))
    (is (= :s (grid/step-size grid/sizes :m -1))))
  (testing "clamps at the ends"
    (is (= :s (grid/step-size grid/sizes :s -1)))
    (is (= :xxl (grid/step-size grid/sizes :xxl 1))))
  (testing "string values"
    (is (= :xl (grid/step-size grid/sizes "l" 1))))
  (testing "unknown value falls back to first"
    (is (= :m (grid/step-size grid/sizes nil 1)))
    (is (= :s (grid/step-size grid/sizes nil -1))))
  (testing "custom steps"
    (is (= "b" (grid/step-size ["a" "b"] "a" 1)))))

(deftest size-stepper-test
  (testing "renders minus/plus buttons in a toolbar-group"
    (let [[tag attrs & btns] (grid/size-stepper {:value :m})]
      (is (= :div tag))
      (is (= "toolbar-group" (:class attrs)))
      (is (= 2 (count btns)))))
  (testing "minus disabled at smallest"
    (let [[_ _ minus-btn plus-btn] (grid/size-stepper {:value :s})]
      (is (true? (:disabled (second minus-btn))))
      (is (nil? (:disabled (second plus-btn))))))
  (testing "plus disabled at largest"
    (let [[_ _ minus-btn plus-btn] (grid/size-stepper {:value :xxl})]
      (is (nil? (:disabled (second minus-btn))))
      (is (true? (:disabled (second plus-btn))))))
  (testing "neither disabled in the middle"
    (let [[_ _ minus-btn plus-btn] (grid/size-stepper {:value :l})]
      (is (nil? (:disabled (second minus-btn))))
      (is (nil? (:disabled (second plus-btn)))))))
