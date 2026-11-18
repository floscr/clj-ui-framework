(ns ui.chart-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ui.chart :as chart]
            [ui.metric :as metric]))

(defn- find-all
  "Every hiccup element in `tree` whose class contains `cls`."
  [tree cls]
  (->> (tree-seq sequential? seq tree)
       (filter (fn [x] (and (vector? x) (map? (second x))
                            (some #{cls} (str/split (str (:class (second x))) #" ")))))))

(deftest numbers-test
  (is (= 3.7 (chart/round1 3.7333)))
  (is (= "45.0%" (chart/pct 9 20)))
  (is (= "0.0%" (chart/pct 3 0)) "no total, no share")
  (is (= "20" (chart/format-number 20)))
  (is (= "2.5" (chart/format-number 2.5)))
  (is (= "3" (chart/format-number 3.0))))

(deftest nice-max-test
  (testing "at least 4, so a quiet chart still has gridlines"
    (is (= 4 (chart/nice-max 0)))
    (is (= 4 (chart/nice-max nil)))
    (is (= 4 (chart/nice-max 3))))
  (testing "rounds up to a number whose half is round"
    (is (= 20 (chart/nice-max 18)))
    (is (= 12 (chart/nice-max 11)))
    (is (= 24 (chart/nice-max 21)))
    (is (= 160 (chart/nice-max 121)))
    (is (= 500 (chart/nice-max 401)))
    (is (= 1000 (chart/nice-max 1000))))
  (testing "never below the value"
    (is (every? (fn [m] (>= (chart/nice-max m) m)) (range 0 3000 7)))))

(deftest geometry-test
  (testing "values sit in the middle of their columns, the top value at y 0"
    (is (= [[50.0 100.0] [150.0 0.0]] (chart/plot-points [0 10] {:w 200 :h 100 :top 10}))))
  (testing "edges: first and last on the box's edges, padded"
    (is (= [[0.0 28.0] [100.0 4.0]] (chart/plot-points [0 5] {:w 100 :h 32 :top 5 :pad 4 :edges true}))))
  (testing ":low moves the bottom of the scale"
    (is (= [[0.0 10.0] [100.0 0.0]] (chart/plot-points [40 50] {:w 100 :h 10 :top 50 :low 40 :edges true}))))
  (testing "nil counts as 0"
    (is (= [[50.0 10.0]] (chart/plot-points [nil] {:w 100 :h 10 :top 4}))))
  (testing "linear path"
    (is (= "M0.0,10.0 L10.0,0.0" (chart/line-path [[0 10] [10 0]] {:curve :linear}))))
  (testing "smooth path goes through every point and stays in the box"
    (let [pts (chart/plot-points [0 18 0 2] {:w 400 :h 200 :top 20})
          d   (chart/line-path pts {:h 200})
          ys  (map (comp parse-double second) (re-seq #",(-?[0-9.]+)" d))]
      (is (str/starts-with? d "M50.0,200.0"))
      (is (= 3 (count (re-seq #" C" d))))
      (is (str/ends-with? d "350.0,180.0"))
      (is (every? #(<= 0 % 200) ys))))
  (testing "no points, no path"
    (is (= "" (chart/line-path [] {})))
    (is (= "" (chart/area-path "" [] 10))))
  (testing "area closes down to the baseline"
    (is (= "M0.0,5.0 L10.0,0.0 L10.0,10.0 L0.0,10.0 Z"
           (chart/area-path "M0.0,5.0 L10.0,0.0" [[0 5] [10 0]] 10)))))

(deftest donut-arcs-test
  (let [arcs (chart/donut-arcs [{:label "a" :value 3} {:label "b" :value 1} {:label "none" :value 0}])
        c    (* 2 Math/PI 48)
        on   (fn [arc] (parse-double (first (str/split (:dash arc) #" "))))]
    (is (= ["a" "b"] (map :label arcs)) "segments without a value are dropped")
    (is (= "0.0" (:offset (first arcs))))
    (is (< (Math/abs (- (+ (on (first arcs)) 2) (* 0.75 c))) 0.2) "three quarters, minus the gap")
    (is (< (Math/abs (+ (parse-double (:offset (second arcs))) (* 0.75 c))) 0.2) "the second starts where the first ends"))
  (is (= [] (chart/donut-arcs [])))
  (testing "a single segment is a closed ring"
    (let [[arc] (chart/donut-arcs [{:value 5}])]
      (is (str/ends-with? (:dash arc) " 0.0")))))

(deftest classes-test
  (is (= ["chart-area"] (chart/chart-class-list "chart-area" {})))
  (is (= ["chart-area" "chart-teal"] (chart/chart-class-list "chart-area" {:color :teal})))
  (is (= ["chart-area"] (chart/chart-class-list "chart-area" {:color :mauve})) "unknown colors are ignored")
  (is (= "chart-spark chart-blue" (chart/chart-classes "chart-spark" {:color "blue"}))))

(deftest chart-frame-test
  (let [result (chart/chart {:title "Visitors" :description "112" :actions [:button "30d"]} [:div "plot"])]
    (is (= :figure (first result)))
    (is (= "chart" (get-in result [1 :class])))
    (is (= [:h3 {:class "chart-title"} "Visitors"] (first (find-all result "chart-title"))))
    (is (= "112" (last (first (find-all result "chart-description")))))
    (is (= [:button "30d"] (last (first (find-all result "chart-actions")))))
    (is (= [:div "plot"] (last result))))
  (testing "no title, no head"
    (is (empty? (find-all (chart/chart {} [:div "plot"]) "chart-head")))))

(def points
  [{:label "Mon 16 Nov" :value 3 :compare 1}
   {:label "Tue 17 Nov" :value 18 :compare 4}
   {:label "Wed 18 Nov" :value 0 :compare 2}
   {:label "Thu 19 Nov" :value 5 :compare 2}])

(deftest area-chart-test
  (let [result (chart/area-chart {:points points :label "This period" :compare-label "Period before" :color :blue})
        hits   (find-all result "chart-hit")]
    (is (= "chart-area chart-blue" (get-in result [1 :class])))
    (testing "one column per point, positioned by custom properties"
      (is (= 4 (count hits)))
      (is (= "--chart-n: 4;" (get-in (first (find-all result "chart-hits")) [1 :style])))
      (is (= "--chart-v: 90.0%; --chart-c: 20.0%;" (get-in (second hits) [1 :style])))
      (is (= "-1" (get-in (first hits) [1 :tabindex])) "focusable by tap, not a tab stop"))
    (testing "the card opens to the right of the line, in the right half to the left"
      (is (= ["chart-hit" "chart-hit" "chart-hit chart-hit-end" "chart-hit chart-hit-end"]
             (map #(get-in % [1 :class]) hits))))
    (testing "the value card names the point and both series"
      (let [tip (first (find-all (second hits) "chart-tip"))]
        (is (= "Tue 17 Nov" (last (first (find-all tip "chart-tip-title")))))
        (is (= ["18" "4"] (map last (find-all tip "chart-tip-value"))))))
    (testing "axis: nice max, its half and zero"
      (is (= ["20" "10" "0"] (map last (find-all result "chart-tick")))))
    (testing "both lines, an area, a legend and the table for screen readers"
      (is (= 2 (count (find-all result "chart-line"))))
      (is (= 1 (count (find-all result "chart-line-compare"))))
      (is (= 1 (count (find-all result "chart-fill"))))
      (is (= 1 (count (find-all result "chart-legend"))))
      (let [table (first (find-all result "sr-only"))]
        (is (= :table (first table)))
        (is (= 4 (count (filter #(and (vector? %) (= :tr (first %))) (tree-seq sequential? seq (last table)))))))))
  (testing "without a comparison: one line, no legend, one dot"
    (let [result (chart/area-chart {:points (mapv #(dissoc % :compare) points)})]
      (is (= 1 (count (find-all result "chart-line"))))
      (is (empty? (find-all result "chart-legend")))
      (is (empty? (find-all result "chart-dot-compare")))
      (is (= "--chart-v: 15.0%;" (get-in (first (find-all result "chart-hit")) [1 :style])))))
  (testing ":legend false, :max and :format"
    (let [result (chart/area-chart {:points points :legend false :max 40 :format #(str % " ms")})]
      (is (empty? (find-all result "chart-legend")))
      (is (= ["40 ms" "20.0 ms" "0 ms"] (map last (find-all result "chart-tick"))))))
  (testing "x axis: given labels win, otherwise evenly spaced ones ending on the last point"
    (let [given (chart/area-chart {:points (assoc-in points [1 :axis-label] "17 Nov")})
          auto  (chart/area-chart {:points (vec (for [i (range 30)] {:label (str "d" i) :value i}))})]
      (is (= ["17 Nov"] (map last (find-all given "chart-x-label"))))
      (is (= "--chart-col: 2;" (get-in (first (find-all given "chart-x-label")) [1 :style])))
      (is (= ["d1" "d8" "d15" "d22" "d29"] (map last (find-all auto "chart-x-label"))))
      (is (= ["d8" "d22"] (map last (find-all auto "chart-x-label-minor")))
          "every second label from the end is minor: a narrow chart drops it")))
  (testing "loading and empty keep the plot's place"
    (is (= 1 (count (find-all (chart/area-chart {:loading true :points points}) "chart-skeleton"))))
    (let [empty (chart/area-chart {:points [] :empty-label "No visitors yet"})]
      (is (= 1 (count (find-all empty "chart-canvas-empty"))))
      (is (some #{"No visitors yet"} (flatten empty)))
      (is (empty? (find-all empty "chart-hit"))))))

(deftest bar-list-test
  (let [result (chart/bar-list {:rows [{:label "Blog" :description "/blog" :value 48 :note "12%" :href "/p"}
                                       {:label "Notes" :value 12 :display "12 visits" :active true}]})
        [a b]  (find-all result "chart-bar")]
    (is (= "chart-bars" (get-in result [1 :class])))
    (is (= "list" (get-in result [1 :role])))
    (testing "a row with :href is a link"
      (is (= :a (first a)))
      (is (= "/p" (get-in a [1 :href])))
      (is (= "chart-bar chart-bar-link" (get-in a [1 :class])))
      (is (= :div (first b))))
    (testing "rails are relative to the largest value"
      (is (= ["--chart-w: 100.0%;" "--chart-w: 25.0%;"]
             (map #(get-in % [1 :style]) (find-all result "chart-bar-fill")))))
    (testing "label, description, value and note"
      (is (= [:span {:class "chart-bar-description"} "/blog"] (last (first (find-all a "chart-bar-label")))))
      (is (= [:span {:class "chart-bar-note"} "12%"] (last (first (find-all a "chart-bar-value")))))
      (is (= "48" (nth (first (find-all a "chart-bar-value")) 2)))
      (is (= "12 visits" (last (first (find-all b "chart-bar-value"))))))
    (testing "active row"
      (is (str/includes? (get-in b [1 :class]) "chart-bar-active"))
      (is (= "true" (get-in b [1 :aria-current])))))
  (testing ":max sets the full rail"
    (is (= "--chart-w: 50.0%;"
           (get-in (first (find-all (chart/bar-list {:max 10 :rows [{:label "a" :value 5}]}) "chart-bar-fill")) [1 :style])))))

(deftest donut-test
  (let [result (chart/donut {:center-label "visitors"
                             :segments [{:label "Direct" :value 75}
                                        {:label "Search" :value 25 :active true}]})
        segs   (find-all result "chart-donut-segment")]
    (is (= "chart-donut" (get-in result [1 :class])))
    (is (= 2 (count segs)))
    (is (= ["chart-donut-segment chart-series-1" "chart-donut-segment chart-series-2 chart-donut-active"]
           (map #(get-in % [1 :class]) segs)))
    (is (= "100" (last (first (find-all result "chart-donut-value")))) "the total in the middle")
    (is (= "visitors" (last (first (find-all result "chart-donut-label")))))
    (is (= ["75%" "25%"] (map last (find-all result "chart-donut-share"))))
    (is (= "Direct 75.0%, Search 25.0%"
           (get-in (nth (first (find-all result "chart-donut-ring")) 2) [1 :aria-label]))
        "the ring names every segment with its share")
    (is (= 1 (count (find-all result "chart-donut-row-active")))))
  (testing "seventh segment keeps the last series color; no legend on request"
    (let [result (chart/donut {:legend false :center-value "7" :segments (vec (for [i (range 7)] {:label (str i) :value 1}))})]
      (is (str/includes? (get-in (last (find-all result "chart-donut-segment")) [1 :class]) "chart-series-6"))
      (is (empty? (find-all result "chart-donut-legend")))
      (is (= "7" (last (first (find-all result "chart-donut-value"))))))))

(deftest sparkline-test
  (let [result (chart/sparkline {:values [1 3 2] :label "Visitors per week" :color :teal})]
    (is (= :svg (first result)))
    (is (= "chart-spark chart-teal" (get-in result [1 :class])))
    (is (= "0 0 100 32" (get-in result [1 :viewBox])))
    (is (= "img" (get-in result [1 :role])))
    (is (= "Visitors per week" (get-in result [1 :aria-label])))
    (is (= 1 (count (find-all result "chart-spark-fill"))))
    (is (str/starts-with? (get-in (first (find-all result "chart-spark-line")) [1 :d]) "M0.0,")))
  (testing ":zero false scales from the smallest value"
    (let [d #(get-in (first (find-all (chart/sparkline (merge {:values [40 50] :curve :linear} %)) "chart-spark-line")) [1 :d])]
      (is (= "M0.0,8.2 L100.0,3.0" (d {})))
      (is (= "M0.0,29.0 L100.0,3.0" (d {:zero false})))))
  (testing "no label: decorative; :area false: the line alone"
    (let [result (chart/sparkline {:values [1 2] :area false})]
      (is (= "true" (get-in result [1 :aria-hidden])))
      (is (empty? (find-all result "chart-spark-fill"))))))

(deftest legend-test
  (let [result (chart/legend {:items [{:label "This period"} {:label "Before" :compare true} {:label "Search" :color :blue}]})]
    (is (= "chart-legend" (get-in result [1 :class])))
    (is (= ["chart-legend-dot" "chart-legend-dot chart-legend-dot-compare" "chart-legend-dot chart-blue"]
           (map #(get-in % [1 :class]) (find-all result "chart-legend-dot"))))
    (is (= ["This period" "Before" "Search"] (map last (find-all result "chart-legend-item"))))))

(deftest metric-chart-slot-test
  (let [spark  (chart/sparkline {:values [1 2 3]})
        result (metric/metric {:label "Visitors" :value "112" :chart spark})]
    (is (= "metric-main" (get-in result [3 1 :class])))
    (is (= [:div {:class "metric-chart"} spark] (get-in result [3 3])))
    (is (= "metric-value" (get-in result [3 2 1 :class])))))
