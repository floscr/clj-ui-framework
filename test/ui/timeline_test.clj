(ns ui.timeline-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ui.timeline :as tl]))

(defn- pct->num [s]
  (Double/parseDouble (subs s 0 (dec (count s)))))

(deftest total-time-test
  (testing "adds ~5% end padding"
    (is (< (Math/abs (- (tl/total-time 1.52) 1.6)) 1e-9))
    (is (< (Math/abs (- (tl/total-time 9.5) 10.0)) 1e-9)))
  (testing "guards zero/nil duration"
    (is (pos? (tl/total-time 0)))
    (is (pos? (tl/total-time nil)))))

(deftest tick-step-test
  (is (= 0.2 (tl/tick-step 1.52)))
  (is (= 2 (tl/tick-step 10)))
  (is (= 10 (tl/tick-step 60)))
  (is (= 120 (tl/tick-step 900))))

(deftest ticks-test
  (testing "1.52s → nine ticks every 0.2s up to 1.6"
    (let [ts (tl/ticks 1.52)]
      (is (= 9 (count ts)))
      (is (== 0 (first ts)))
      (is (< (Math/abs (- (last ts) 1.6)) 1e-9))))
  (testing "ticks never exceed the ruler extent"
    (doseq [d [0.5 3 12.5 61 300]]
      (is (<= (last (tl/ticks d)) (+ (tl/total-time d) 1e-9))))))

(deftest time->pct-test
  (testing "duration lands at 95%"
    (is (< (Math/abs (- (pct->num (tl/time->pct 10 10)) 95.0)) 1e-6)))
  (testing "zero at 0%"
    (is (= 0.0 (pct->num (tl/time->pct 0 10)))))
  (testing "span width"
    (is (< (Math/abs (- (pct->num (tl/span->pct 0 10 10)) 95.0)) 1e-6))))

(deftest format-time-test
  (testing "short timelines: trimmed seconds"
    (is (= "0" (tl/format-time 0 1.52)))
    (is (= "0.2" (tl/format-time 0.2 1.52)))
    (is (= "0.6" (tl/format-time 0.6000000000000001 1.52)))
    (is (= "1" (tl/format-time 1.0 1.52)))
    (is (= "10" (tl/format-time 10.0 30))))
  (testing "long timelines: m:ss"
    (is (= "0:00" (tl/format-time 0 90)))
    (is (= "1:05" (tl/format-time 65 90)))
    (is (= "1:30" (tl/format-time 90 300)))))

(deftest format-readout-test
  (is (= "0.79 / 1.52s" (tl/format-readout 0.79 1.52)))
  (is (= "0.00 / 1.52s" (tl/format-readout nil 1.52)))
  (is (= "1:23 / 4:00" (tl/format-readout 83 240))))

(deftest timeline-classes-test
  (is (= ["ui-timeline"] (tl/timeline-class-list {})))
  (is (= "ui-timeline" (tl/timeline-classes {}))))

(def sample-props
  {:duration 10
   :current 5
   :tracks [{:id :clip :label "Clip"
             :segments [{:id :trim :start 1 :end 9}]}]})

(deftest timeline-component-test
  (testing "root div with class, custom class and attrs"
    (let [result (tl/timeline (assoc sample-props :class "extra" :attrs {:id "tl"}))]
      (is (= :div (first result)))
      (is (= "ui-timeline extra" (get-in result [1 :class])))
      (is (= "tl" (get-in result [1 :id])))))

  (testing "ruler renders slider role and one element per tick"
    (let [tracks-div (nth (tl/timeline sample-props) 2)
          ruler (nth tracks-div 2)]
      (is (= "ui-timeline-ruler" (get-in ruler [1 :class])))
      (is (= "slider" (get-in ruler [1 :role])))
      (is (= "10" (get-in ruler [1 :aria-valuemax])))
      ;; duration 10 → step 2 → ticks 0,2,4,6,8,10 (total ≈ 10.53)
      (is (= (count (tl/ticks 10)) (count (drop 2 ruler))))))

  (testing "track renders label and segment bar with position"
    (let [tracks-div (nth (tl/timeline sample-props) 2)
          track-list (nth tracks-div 3)
          track (nth track-list 2)
          label (nth track 2)
          lane (nth track 3)
          bar (nth lane 2)]
      (is (= "ui-timeline-track" (get-in track [1 :class])))
      (is (= "Clip" (nth label 2)))
      (is (= :button (first bar)))
      (is (= "ui-timeline-bar" (get-in bar [1 :class])))
      (is (< (Math/abs (- (pct->num (get-in bar [1 :style :left])) 9.5)) 1e-6))
      ;; two handles follow the bar
      (is (= ["ui-timeline-handle ui-timeline-handle-start"
              "ui-timeline-handle ui-timeline-handle-end"]
             [(get-in (nth lane 3) [1 :class])
              (get-in (nth lane 4) [1 :class])]))))

  (testing "bar and handles carry pointer/key handlers"
    (let [tracks-div (nth (tl/timeline sample-props) 2)
          track-list (nth tracks-div 3)
          lane (-> track-list (nth 2) (nth 3))
          bar (nth lane 2)]
      (is (fn? (get-in bar [1 :on-pointerdown])))
      (is (fn? (get-in bar [1 :on-keydown])))
      (is (fn? (get-in (nth lane 3) [1 :on-pointerdown])))
      (is (fn? (get-in (nth lane 4) [1 :on-pointerdown])))))

  (testing "playhead positioned from :current"
    (let [tracks-div (nth (tl/timeline sample-props) 2)
          overlay (nth tracks-div 4)
          playhead (nth overlay 2)]
      (is (= "ui-timeline-playhead" (get-in playhead [1 :class])))
      (is (str/starts-with? (get-in playhead [1 :style :left]) "47.5"))))

  (testing "transport shown by default with readout"
    (let [transport (nth (tl/timeline sample-props) 3)]
      (is (= "ui-timeline-transport" (get-in transport [1 :class])))
      (is (= "5.00 / 10.00s" (nth (nth transport 4) 2)))))

  (testing "transport hidden with :transport false"
    (is (nil? (nth (tl/timeline (assoc sample-props :transport false)) 3))))

  (testing "loop button only rendered with :on-loop"
    (let [transport (nth (tl/timeline sample-props) 3)]
      (is (nil? (nth transport 5))))
    (let [transport (nth (tl/timeline (assoc sample-props
                                             :loop true
                                             :on-loop (fn [_] nil)))
                         3)
          loop-btn (nth transport 5)]
      (is (str/includes? (get-in loop-btn [1 :class]) "ui-timeline-loop-active"))
      (is (= "true" (get-in loop-btn [1 :aria-pressed]))))))
