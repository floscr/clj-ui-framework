(ns ui.canvas-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ui.canvas :as canvas]))

(defn- classes [el] (set (str/split (:class (second el)) #" ")))

(defn- descendants-of
  "Every hiccup element under `el`, itself included."
  [el]
  (filter vector? (tree-seq #(or (vector? %) (seq? %)) seq el)))

(defn- find-by-class [el class]
  (filter #(and (keyword? (first %)) (map? (second %))
                (string? (:class (second %)))
                (contains? (classes %) class))
          (descendants-of el)))

(deftest canvas-attrs-test
  (testing "the minimum: what the runtime and assistive technology need"
    (is (= {:data-ui-canvas "true" :tabindex "0" :role "application"}
           (canvas/canvas-attrs {}))))
  (testing "options become data attributes"
    (is (= {:data-ui-canvas "true" :tabindex "0" :role "application"
            :id "run" :aria-label "Flow pr-1" :data-follow "ci" :data-initial "fit"
            :data-min-zoom "0.5" :data-max-zoom "2" :data-padding "16" :data-gestures "page"}
           (canvas/canvas-attrs {:id "run" :label "Flow pr-1" :follow :ci :initial :fit
                                 :min-zoom 0.5 :max-zoom 2 :padding 16 :gestures :page})))))

(deftest canvas-test
  (let [[tag attrs w] (canvas/canvas {:id "board" :label "Board" :height 320 :class "extra"}
                                     (canvas/world {} [:p "x"])
                                     nil)]
    (is (= :div tag))
    (is (= "canvas canvas-grid extra" (:class attrs)))
    (is (= "height: 320px;" (:style attrs)))
    (is (= "true" (:data-ui-canvas-world (second w))))
    (testing "nil children are dropped"
      (is (= 3 (count (canvas/canvas {} (canvas/world {}) nil))))))
  (testing "the grid can be left out"
    (is (= ["canvas"] (canvas/canvas-class-list {:grid false})))
    (is (= "canvas canvas-grid" (canvas/canvas-classes {})))))

(deftest node-test
  (let [[_ attrs child] (canvas/node {:id :plan :x 24 :y 40 :width 120} [:b "Plan"])]
    (is (= "plan" (:data-ui-canvas-node attrs)))
    (is (= "left: 24px; top: 40px; width: 120px;" (:style attrs)))
    (is (= [:b "Plan"] child))))

(deftest bounds-test
  (is (nil? (canvas/bounds [])))
  (is (= {:left -10 :top 0 :right 150 :bottom 90}
         (canvas/bounds [{:x 0 :y 0 :w 100 :h 40} {:x -10 :y 50 :w 160 :h 40}]))))

(deftest edge-path-test
  (let [a {:x 0 :y 0 :w 200 :h 50}
        below {:x 0 :y 100 :w 200 :h 50}
        aside {:x 300 :y 100 :w 200 :h 50}]
    (testing "forward: out of the bottom, into the top, the tip pointing down"
      (is (= {:d "M100,50 C100,72 100,72 100,95" :tip [100 100 90]}
             (canvas/edge-path a below {})))
      (is (= {:d "M100,50 C100,72 400,72 400,95" :tip [400 100 90]}
             (canvas/edge-path a aside {}))))
    (testing "a way back runs along a side rail and enters from that side"
      (is (= {:d "M0,125 H-20 Q-30,125 -30,115 V35 Q-30,25 -20,25 H-5" :tip [0 25 0]}
             (canvas/edge-path below a {:rail :left :rail-x -30})))
      (is (= {:d "M500,125 H520 Q530,125 530,115 V35 Q530,25 520,25 H205" :tip [200 25 180]}
             (canvas/edge-path aside a {:rail :right :rail-x 530}))))
    (testing "a rail also takes an edge down"
      (is (str/includes? (:d (canvas/edge-path a below {:rail :left :rail-x -30}))
                         "Q-30,25 -30,35 V115 Q-30,125")))))

(deftest step-test
  (testing "status and selection are classes"
    (is (= ["canvas-node" "canvas-step" "canvas-step-idle"] (canvas/step-class-list {})))
    (is (= ["canvas-node" "canvas-step" "canvas-step-current" "canvas-step-selected"]
           (canvas/step-class-list {:status :current :selected true})))
    (is (= "canvas-node canvas-step canvas-step-done" (canvas/step-classes {:status :done}))))
  (let [el (canvas/step {:id "ci" :x 276 :y 766 :title "ci" :description "Checks of both PRs"
                         :icon :clock :status :current})
        attrs (second el)]
    (is (= "ci" (:data-ui-canvas-node attrs)))
    (is (= "0" (:tabindex attrs)) "Tab reaches it")
    (is (= "left: 276px; top: 766px; width: 228px; height: 56px;" (:style attrs)))
    (is (= ["ci"] (map last (find-by-class el "canvas-step-title"))))
    (is (= ["Checks of both PRs"] (map last (find-by-class el "canvas-step-description"))))
    (is (seq (find-by-class el "spinner")) "the step that runs spins"))
  (testing "a done step has a check, an idle one a ring, and neither needs an icon"
    (let [done (canvas/step {:id "a" :x 0 :y 0 :title "a" :status :done})
          idle (canvas/step {:id "b" :x 0 :y 0 :title "b"})]
      (is (empty? (find-by-class done "canvas-step-icon")))
      (is (seq (find-by-class done "icon")))
      (is (seq (find-by-class idle "canvas-step-dot"))))))

(deftest edge-test
  (is (= ["canvas-edge"] (canvas/edge-class-list {})))
  (is (= ["canvas-edge" "canvas-edge-taken" "canvas-edge-live"]
         (canvas/edge-class-list {:taken true :live true})))
  (let [[tag attrs line tip] (canvas/edge {:from {:x 0 :y 0 :w 200 :h 50}
                                           :to {:x 0 :y 100 :w 200 :h 50} :taken true})]
    (is (= :g tag))
    (is (= "canvas-edge canvas-edge-taken" (:class attrs)))
    (is (= "M100,50 C100,72 100,72 100,95" (:d (second line))))
    (is (= "translate(100,100) rotate(90)" (:transform (second tip))))))

(deftest controls-test
  (let [acts (fn [el] (keep #(:data-ui-canvas-act (second %))
                            (filter #(map? (second %)) (descendants-of el))))]
    (testing "all of them, in reading order"
      (is (= ["follow" "zoom-out" "zoom-reset" "zoom-in" "fit"] (acts (canvas/controls {})))))
    (testing "parts can be left out"
      (is (= ["zoom-out" "zoom-reset" "zoom-in" "fit"] (acts (canvas/controls {:follow false}))))
      (is (= ["follow" "fit"] (acts (canvas/controls {:zoom false}))))
      (is (= ["follow"] (acts (canvas/controls {:zoom false :fit false})))))
    (testing "every button has a name; the readout is the reset button"
      (let [buttons (filter #(and (map? (second %)) (:data-ui-canvas-act (second %)))
                            (descendants-of (canvas/controls {})))]
        (is (every? #(seq (:aria-label (second %))) buttons))
        (is (= ["zoom-reset"] (keep #(when (:data-ui-canvas-zoom (second %)) (:data-ui-canvas-act (second %)))
                                    buttons)))))))

(def ^:private run
  {:id "run" :label "Flow pr-1" :tag "ext.penpot/pr" :note "check 4"
   :nodes [{:id "push" :title "push" :status :done :x 276 :y 0}
           {:id "ci" :title "ci" :status :current :x 276 :y 92}
           {:id "fix-ci" :title "fix-ci" :x 552 :y 184}]
   :edges [{:from "push" :to "ci" :taken true}
           {:from "ci" :to "fix-ci"}
           {:from "fix-ci" :to "push" :rail :right}
           {:from "ci" :to "nowhere"}]})

(deftest flow-test
  (let [el    (canvas/flow run)
        attrs (second el)
        edges (find-by-class el "canvas-edge")]
    (testing "the camera follows the step that runs"
      (is (= "ci" (:data-follow attrs)))
      (is (= "canvas canvas-grid canvas-flow" (:class attrs))))
    (testing "an explicit :follow wins"
      (is (= "push" (:data-follow (second (canvas/flow (assoc run :follow "push")))))))
    (testing "one card per node"
      (is (= ["push" "ci" "fix-ci"] (map #(:data-ui-canvas-node (second %)) (find-by-class el "canvas-step")))))
    (testing "edges to unknown nodes are left out; the taken one is drawn last, on top"
      (is (= 3 (count edges)))
      (is (= "canvas-edge canvas-edge-taken canvas-edge-live" (:class (second (last edges))))))
    (testing "a way back runs beside the graph, right of its right edge"
      (let [back (some #(when (str/includes? (:d (second (nth % 2))) " V") %) edges)]
        (is (str/includes? (:d (second (nth back 2))) "Q810,"))))
    (testing "the world is as large as what it holds"
      (let [w (first (filter #(:data-ui-canvas-world (second %)) (filter #(map? (second %)) (descendants-of el))))]
        (is (= "width: 840px; height: 270px;" (:style (second w))))))
    (testing "the note sits beside the running step, the tag says it is live"
      (is (= ["check 4"] (map last (find-by-class el "canvas-note"))))
      (is (= "left: 518px; top: 108px;" (:style (second (first (find-by-class el "canvas-note"))))))
      (is (seq (find-by-class el "canvas-tag-dot"))))
    (testing "controls can be left out or trimmed"
      (is (empty? (find-by-class (canvas/flow (assoc run :controls false)) "canvas-controls")))
      (is (seq (find-by-class (canvas/flow (assoc run :controls {:zoom false})) "canvas-controls"))))
    (testing "a flow without a running step follows nothing and has no note"
      (let [idle (canvas/flow (update run :nodes (fn [ns] (mapv #(dissoc % :status) ns))))]
        (is (not (contains? (second idle) :data-follow)))
        (is (empty? (find-by-class idle "canvas-note")))))))
