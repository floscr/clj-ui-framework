(ns ui.panels-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.panels :as panels]))

(deftest group-attrs-test
  (testing "default orientation"
    (is (= {:data-ui-panels-group "true" :data-orientation "horizontal"}
           (panels/group-attrs {}))))
  (testing "explicit orientation"
    (is (= "vertical"
           (:data-orientation (panels/group-attrs {:orientation :vertical}))))))

(deftest panel-attrs-test
  (testing "minimal sized panel"
    (is (= {:data-ui-panels-panel "true" :data-size "240"}
           (panels/panel-attrs {:size 240}))))
  (testing "percent size"
    (is (= "30%" (:data-size (panels/panel-attrs {:size "30%"})))))
  (testing "full options"
    (let [attrs (panels/panel-attrs {:size 240 :min-size 160 :max-size 420
                                     :default-size 240 :collapsible true
                                     :collapsed true :persist-key "sidebar"})]
      (is (= "160" (:data-min-size attrs)))
      (is (= "420" (:data-max-size attrs)))
      (is (= "240" (:data-default-size attrs)))
      (is (= "true" (:data-collapsible attrs)))
      (is (= "true" (:data-collapsed attrs)))
      (is (= "sidebar" (:data-persist-key attrs)))))
  (testing "falsy options are omitted (boolean attr pitfall)"
    (let [attrs (panels/panel-attrs {:size 240 :collapsible false :collapsed false})]
      (is (not (contains? attrs :data-collapsible)))
      (is (not (contains? attrs :data-collapsed))))))

(deftest group-test
  (let [[tag attrs] (panels/group {:orientation :vertical} [:span "x"])]
    (is (= :div tag))
    (is (= "ui-panels-group" (:class attrs)))
    (is (= "vertical" (:data-orientation attrs)))))

(deftest sized-panel-test
  (let [[tag attrs content edge] (panels/panel {:size 240 :class "extra"} [:p "body"])]
    (is (= :div tag))
    (is (= "ui-panels-panel extra" (:class attrs)))
    (is (= "240" (:data-size attrs)))
    (testing "content wrapper holds children"
      (is (= "ui-panels-content" (:class (second content))))
      (is (= [:p "body"] (nth content 2))))
    (testing "edge grip present with separator semantics"
      (let [edge-attrs (second edge)]
        (is (= "ui-panels-grip ui-panels-edge" (:class edge-attrs)))
        (is (= "separator" (:role edge-attrs)))
        (is (= "0" (:tabindex edge-attrs)))))))

(deftest fill-panel-test
  (testing "plain fill"
    (let [[_ attrs child] (panels/panel {} [:p "body"])]
      (is (= "true" (:data-ui-panels-fill attrs)))
      (is (not (contains? attrs :data-pin)))
      (is (= [:p "body"] child))))
  (testing "pinned fill wraps children in inner"
    (let [[_ attrs inner] (panels/panel {:pin true} [:p "body"])]
      (is (= "true" (:data-pin attrs)))
      (is (= "ui-panels-fill-inner" (:class (second inner))))
      (is (= [:p "body"] (nth inner 2))))))

(deftest separator-test
  (let [[tag attrs grip] (panels/separator {:label "Resize sidebar"})]
    (is (= :div tag))
    (is (= "ui-panels-separator" (:class attrs)))
    (is (= "true" (:data-ui-panels-separator attrs)))
    (let [grip-attrs (second grip)]
      (is (= "separator" (:role grip-attrs)))
      (is (= "Resize sidebar" (:aria-label grip-attrs)))
      (is (= "0" (:tabindex grip-attrs))))))
