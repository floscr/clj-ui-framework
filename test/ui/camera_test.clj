(ns ui.camera-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.camera :as camera]))

(deftest camera-view-placeholder-test
  (testing "inactive renders placeholder label with native capture input"
    (let [[tag attrs label] (camera/camera-view {})]
      (is (= :div tag))
      (is (= "camera-section" (:class attrs)))
      (is (= :label (first label)))
      (let [[_ label-attrs input _icon text] label]
        (is (= "camera-placeholder" (:class label-attrs)))
        (is (= :input (first input)))
        (is (= "file" (:type (second input))))
        (is (= "image/*" (:accept (second input))))
        (is (= "environment" (:capture (second input))))
        (is (= "sr-only" (:class (second input))))
        (is (= [:p "Tap to take a photo"] text)))))

  (testing "facing and accept props flow into the native input"
    (let [[_ _ label] (camera/camera-view {:facing "user" :accept "image/png"})
          [_ _ input] label]
      (is (= "user" (:capture (second input))))
      (is (= "image/png" (:accept (second input))))))

  (testing "custom placeholder text"
    (let [[_ _ label] (camera/camera-view {:placeholder "Foto aufnehmen"})]
      (is (= [:p "Foto aufnehmen"] (last label))))))

(deftest camera-view-active-test
  (testing "active renders video + overlay with capture button"
    (let [[_ _ body] (camera/camera-view {:active? true :video-id "vid"})
          [_ video overlay] body]
      (is (= :video (first video)))
      (is (= "vid" (:id (second video))))
      (is (= "camera-video" (:class (second video))))
      (is (= "camera-overlay" (:class (second overlay))))
      (let [[_ _ crosshair capture-btn switch-btn] overlay]
        (is (= "camera-crosshair" (:class (second crosshair))))
        (is (= "camera-capture-btn" (:class (second capture-btn))))
        (is (nil? switch-btn)))))

  (testing "default video id"
    (let [[_ _ body] (camera/camera-view {:active? true})
          [_ video] body]
      (is (= "ui-camera-video" (:id (second video))))))

  (testing "switch button renders only when :on-switch given"
    (let [[_ _ body] (camera/camera-view {:active? true :on-switch (fn [])})
          [_ _ overlay] body
          [_ _ _ _ switch-btn] overlay]
      (is (= "camera-switch-btn" (:class (second switch-btn)))))))

(deftest camera-view-class-attrs-test
  (testing "extra class is appended"
    (let [[_ attrs] (camera/camera-view {:class "mb-4"})]
      (is (= "camera-section mb-4" (:class attrs)))))
  (testing "attrs are merged"
    (let [[_ attrs] (camera/camera-view {:attrs {:id "cam"}})]
      (is (= "cam" (:id attrs))))))
