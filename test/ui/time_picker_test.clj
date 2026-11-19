(ns ui.time-picker-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.time-picker :as tp]))

(defn- nodes
  "Every hiccup element vector under `form`, depth first."
  [form]
  (->> (tree-seq vector? #(filter vector? (drop 1 %)) form)
       (filter #(keyword? (first %)))))

(defn- with-attr [form k]
  (filter #(and (map? (second %)) (contains? (second %) k)) (nodes form)))

(defn- seg [form which]
  (second (first (filter #(= which (:data-ui-time-seg (second %)))
                         (with-attr form :data-ui-time-seg)))))

(deftest parse-time-test
  (testing "24-hour forms"
    (is (= 870 (tp/parse-time "14:30")))
    (is (= 870 (tp/parse-time "1430")))
    (is (= 570 (tp/parse-time "930")))
    (is (= 540 (tp/parse-time "9")))
    (is (= 0 (tp/parse-time "0")))
    (is (= 1439 (tp/parse-time " 23:59 "))))
  (testing "am and pm"
    (is (= 870 (tp/parse-time "2:30pm")))
    (is (= 840 (tp/parse-time "2p")))
    (is (= 0 (tp/parse-time "12am")))
    (is (= 720 (tp/parse-time "12 PM")))
    (is (= 570 (tp/parse-time "9:30 a"))))
  (testing "not a time"
    (is (nil? (tp/parse-time "")))
    (is (nil? (tp/parse-time "24:00")))
    (is (nil? (tp/parse-time "12:60")))
    (is (nil? (tp/parse-time "13pm")))
    (is (nil? (tp/parse-time "0am")))
    (is (nil? (tp/parse-time "noon")))
    (is (nil? (tp/parse-time nil)))))

(deftest format-test
  (is (= "09:05" (tp/format-time 545)))
  (is (= "00:00" (tp/format-time 1440)))
  (is (nil? (tp/format-time nil)))
  (is (= "45m" (tp/format-length 45)))
  (is (= "1h" (tp/format-length 60)))
  (is (= "1h 15m" (tp/format-length 75)))
  (is (nil? (tp/format-length 0)))
  (is (nil? (tp/format-length nil))))

(deftest value-test
  (testing "parse-value"
    (is (= {:start 870 :end nil} (tp/parse-value "14:30")))
    (is (= {:start 870 :end 945} (tp/parse-value "14:30-15:45")))
    (is (= {:start 870 :end 945} (tp/parse-value "14:30 - 15:45")))
    (is (= {:start nil :end nil} (tp/parse-value "")))
    (is (= {:start nil :end nil} (tp/parse-value nil))))
  (testing "value-str"
    (is (= "14:30" (tp/value-str 870 nil)))
    (is (= "14:30-15:45" (tp/value-str 870 945)))
    (is (= "14:30" (tp/value-str 870 870)) "an end not after the start is dropped")
    (is (= "" (tp/value-str nil 945))))
  (testing "round trip"
    (is (= "08:00-08:45" (let [{:keys [start end]} (tp/parse-value "8:00-8:45")]
                           (tp/value-str start end))))))

(deftest slots-test
  (is (= 96 (count (tp/start-slots 15))))
  (is (= [0 30 60] (take 3 (tp/start-slots 30))))
  (testing "end slots start one step after the start and stop before midnight"
    (is (= 885 (first (tp/end-slots 870 15))))
    (is (= 1425 (last (tp/end-slots 870 15))) "12 hours later would pass midnight")
    (is (= 1320 (last (tp/end-slots 600 15))) "12 hours after 10:00")))

(deftest move-start-test
  (is (= {:start 900 :end 975} (tp/move-start {:start 870 :end 945} 900)) "keeps the length")
  (is (= {:start 600 :end nil} (tp/move-start {:start 870 :end nil} 600)))
  (is (= {:start 540 :end nil} (tp/move-start {:start nil :end nil} 540)))
  (is (= {:start 1380 :end 1439} (tp/move-start {:start 600 :end 720} 1380)) "stops at 23:59"))

(deftest class-list-test
  (is (= ["time-picker"] (tp/time-picker-class-list {})))
  (is (= ["time-picker" "time-picker--range" "time-picker--empty" "time-picker--disabled"]
         (tp/time-picker-class-list {:range true :empty true :disabled true}))))

(deftest renders-test
  (testing "a start only"
    (let [form (tp/time-picker {:value "14:30" :name "time"})
          [tag attrs] form
          [[_ hidden]] (with-attr form :data-ui-time-value)]
      (is (= :div tag))
      (is (= "true" (:data-ui-time-picker attrs)))
      (is (= "15" (:data-step attrs)))
      (is (= "time-picker" (:class attrs)))
      (is (= "14:30" (:value (seg form "start"))))
      (is (nil? (seg form "end")) "no end segment without :range")
      (is (= "hidden" (:type hidden)))
      (is (= "14:30" (:value hidden)))
      (is (= "time" (:name hidden)))))
  (testing "a range shows the end and the length"
    (let [form (tp/time-picker {:start "14:30" :end "15:45"})
          [[_ _ len]] (with-attr form :data-ui-time-length)]
      (is (= "time-picker time-picker--range" (:class (second form))))
      (is (= "15:45" (:value (seg form "end"))))
      (is (= "1h 15m" len))
      (is (= "14:30-15:45" (:value (second (first (with-attr form :data-ui-time-value))))))))
  (testing "empty with :range waits for a start"
    (let [form (tp/time-picker {:range true :placeholder "All day"})]
      (is (= "time-picker time-picker--range time-picker--empty" (:class (second form))))
      (is (= "All day" (:placeholder (seg form "start"))))
      (is (= "" (:value (seg form "start"))))))
  (testing "an end before the start is ignored"
    (is (nil? (seg (tp/time-picker {:start "15:00" :end "14:00"}) "end"))))
  (testing "clearable and disabled"
    (is (= 1 (count (with-attr (tp/time-picker {:value "09:00"}) :data-ui-time-clear))))
    (is (empty? (with-attr (tp/time-picker {:value "09:00" :clearable false}) :data-ui-time-clear)))
    (let [form (tp/time-picker {:value "09:00" :disabled true :step 30})]
      (is (= "true" (:data-ui-time-disabled (second form))))
      (is (= "30" (:data-step (second form))))
      (is (true? (:disabled (seg form "start"))))
      (is (empty? (with-attr form :data-ui-time-clear))))))
