(ns ui.player-bar-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.player-bar :as pb]))

(deftest player-bar-class-list-test
  (testing "default"
    (is (= ["player-bar"] (pb/player-bar-class-list {})))))

(deftest player-bar-classes-test
  (testing "default"
    (is (= "player-bar" (pb/player-bar-classes {})))))

(deftest player-bar-component-test
  (testing "renders div with player-bar class"
    (let [result (pb/player-bar {:track-name "Test Song" :playing false :progress 50})]
      (is (= :div (first result)))
      (is (= "player-bar" (get-in result [1 :class])))))

  (testing "appends custom class"
    (let [result (pb/player-bar {:track-name "Test" :class "extra"})]
      (is (= "player-bar extra" (get-in result [1 :class])))))

  (testing "merges extra attrs"
    (let [result (pb/player-bar {:track-name "Test" :attrs {:id "my-player"}})]
      (is (= "my-player" (get-in result [1 :id])))))

  (testing "renders progress bar with correct width"
    (let [result (pb/player-bar {:track-name "Test" :progress 75})
          progress-div (nth result 2)
          track-div (nth progress-div 2)
          fill-div (nth track-div 2)]
      (is (= "width: 75%" (get-in fill-div [1 :style])))))

  (testing "progress defaults to 0"
    (let [result (pb/player-bar {:track-name "Test"})
          progress-div (nth result 2)
          track-div (nth progress-div 2)
          fill-div (nth track-div 2)]
      (is (= "width: 0%" (get-in fill-div [1 :style])))))

  (testing "renders track name"
    (let [result (pb/player-bar {:track-name "My Song"})
          row (nth result 3)
          left (nth row 2)
          info (nth left 3)
          title (nth info 2)]
      (is (= "My Song" (nth title 2)))))

  (testing "renders subtitle when provided"
    (let [result (pb/player-bar {:track-name "Song" :subtitle "Artist"})
          row (nth result 3)
          left (nth row 2)
          info (nth left 3)
          sub (nth info 3)]
      (is (= "Artist" (nth sub 2)))))

  (testing "no subtitle when not provided"
    (let [result (pb/player-bar {:track-name "Song"})
          row (nth result 3)
          left (nth row 2)
          info (nth left 3)]
      ;; subtitle slot is nil when not provided
      (is (nil? (nth info 3)))))

  (testing "play button shows pause icon name when playing"
    (let [result (pb/player-bar {:track-name "Test" :playing true})
          row (nth result 3)
          center (nth row 3)
          play-btn (nth center 4)]
      (is (= "Pause" (get-in play-btn [1 :title])))))

  (testing "play button shows play icon name when paused"
    (let [result (pb/player-bar {:track-name "Test" :playing false})
          row (nth result 3)
          center (nth row 3)
          play-btn (nth center 4)]
      (is (= "Play" (get-in play-btn [1 :title])))))

  (testing "shuffle active adds class"
    (let [result (pb/player-bar {:track-name "Test" :shuffle true})
          row (nth result 3)
          center (nth row 3)
          shuffle-btn (nth center 2)]
      (is (clojure.string/includes?
           (get-in shuffle-btn [1 :class])
           "active"))))

  (testing "repeat active adds class"
    (let [result (pb/player-bar {:track-name "Test" :repeat true})
          row (nth result 3)
          center (nth row 3)
          repeat-btn (nth center 6)]
      (is (clojure.string/includes?
           (get-in repeat-btn [1 :class])
           "active"))))

  (testing "favorited adds fav-active class"
    (let [result (pb/player-bar {:track-name "Test" :favorited true})
          row (nth result 3)
          left (nth row 2)
          fav-btn (nth left 4)]
      (is (clojure.string/includes?
           (get-in fav-btn [1 :class])
           "player-bar-fav-active"))))

  (testing "time rendered when both current-time and duration provided"
    (let [result (pb/player-bar {:track-name "Test" :current-time "1:23" :duration "4:56"})
          row (nth result 3)
          right (nth row 4)
          time-span (nth right 2)]
      (is (= "1:23 / 4:56" (nth time-span 2)))))

  (testing "time not rendered when duration missing"
    (let [result (pb/player-bar {:track-name "Test" :current-time "1:23"})
          row (nth result 3)
          right (nth row 4)]
      ;; Only the volume button, no time span
      (is (nil? (nth right 2))))))
