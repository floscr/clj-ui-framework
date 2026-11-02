(ns ui.video-player-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ui.video-player :as vp]))

(defn- nodes
  "Every hiccup element (vector with a tag keyword) in the tree."
  [tree]
  (filter #(and (vector? %) (keyword? (first %)))
          (tree-seq #(or (vector? %) (seq? %)) seq tree)))

(defn- attrs [node] (let [a (second node)] (when (map? a) a)))

(defn- find-attr
  "Elements whose attribute k equals v (or has k at all when v is nil)."
  ([tree k] (filter #(contains? (attrs %) k) (nodes tree)))
  ([tree k v] (filter #(= v (get (attrs %) k)) (nodes tree))))

(defn- text [node] (str/join (filter string? (tree-seq #(or (vector? %) (seq? %)) seq node))))

(def sleep-button
  {:id :sleep :icon :moon :label "Sleep timer"
   :options [{:value 0 :label "Off"} {:value 30 :label "30 minutes"}]
   :value 30})

(deftest class-list-test
  (is (= ["video-player"] (vp/video-player-class-list {})))
  (is (= ["video-player" "video-player-fill"] (vp/video-player-class-list {:fit :fill})))
  (is (= "video-player video-player-fill" (vp/video-player-classes {:fit :fill})))
  (testing "variants"
    (is (= ["video-player"] (vp/video-player-class-list {:variant :glass})))
    (is (= ["video-player" "video-player-plain"] (vp/video-player-class-list {:variant :plain})))
    (is (= "video-player video-player-plain" (:class (second (vp/video-player {:src "a.mp4" :variant :plain})))))))

(deftest root-test
  (let [[tag a] (vp/video-player {:src "/v/index.m3u8" :title "Ep 1" :start-at 62 :autoplay? true
                                  :class "extra" :attrs {:id "player"}})]
    (testing "root carries the runtime's data"
      (is (= :div tag))
      (is (= "" (:data-video-player a)))
      (is (= "/v/index.m3u8" (:data-src a)))
      (is (= "62" (:data-start-at a)))
      (is (= "true" (:data-autoplay a)))
      (is (= "Ep 1" (:aria-label a)))
      (is (= "player" (:id a)))
      (is (= "video-player extra" (:class a))))
    (testing "no next: autonext off and no next href"
      (is (= "false" (:data-autonext a)))
      (is (nil? (:data-next-href a))))))

(deftest default-controls-test
  (let [tree (vp/video-player {:src "a.mp4"})]
    (testing "built-in menus have empty panels the runtime fills"
      (doseq [id ["subtitles" "quality" "speed"]]
        (is (= 1 (count (find-attr tree :data-vp-open id))) id)
        (is (= 1 (count (find-attr tree :data-vp-panel id))) id)))
    (testing "no next: no next button, no autonext toggle, no up-next card"
      (is (empty? (find-attr tree :data-vp-act "next")))
      (is (empty? (find-attr tree :data-vp-toggle "autonext")))
      (is (empty? (find-attr tree :data-vp "upnext"))))
    (testing "fullscreen and pip actions"
      (is (= 1 (count (find-attr tree :data-vp-act "fullscreen"))))
      (is (= 1 (count (find-attr tree :data-vp-act "pip")))))))

(deftest next-test
  (let [tree (vp/video-player {:src "a.mp4" :next {:title "Episode 2" :href "/watch/2"}})
        [_ a] tree]
    (is (= "true" (:data-autonext a)))
    (is (= "/watch/2" (:data-next-href a)))
    (is (= 2 (count (find-attr tree :data-vp-act "next"))) "dock button + up-next Play")
    (is (= "true" (:aria-pressed (attrs (first (find-attr tree :data-vp-toggle "autonext"))))))
    (is (str/includes? (text (first (find-attr tree :data-vp "upnext"))) "Episode 2")))
  (testing ":autonext? false starts it off"
    (let [tree (vp/video-player {:src "a.mp4" :next {:title "N"} :autonext? false})]
      (is (= "false" (:data-autonext (second tree))))
      (is (= "false" (:aria-pressed (attrs (first (find-attr tree :data-vp-toggle "autonext")))))))))

(deftest custom-menu-test
  (let [tree (vp/video-player {:src "a.mp4" :buttons [sleep-button]})
        trigger (attrs (first (find-attr tree :data-vp-open "sleep")))
        panel (first (find-attr tree :data-vp-panel "sleep"))
        opts (find-attr panel :data-vp-value)]
    (testing "custom buttons go into the default controls"
      (is (some? trigger)))
    (testing "a picked non-default option lights the trigger"
      (is (= "true" (:aria-pressed trigger))))
    (testing "options carry string values and the checked state"
      (is (= ["0" "30"] (map (comp :data-vp-value attrs) opts)))
      (is (= ["false" "true"] (map (comp :aria-checked attrs) opts)))
      (is (= "true" (:data-vp-default (attrs (first opts))))))
    (testing "resting value: trigger not pressed"
      (let [t (vp/video-player {:src "a.mp4" :buttons [(assoc sleep-button :value 0)]})]
        (is (= "false" (:aria-pressed (attrs (first (find-attr t :data-vp-open "sleep"))))))))
    (testing ":default? picks the resting option"
      (let [b {:id :q :label "Q" :options [{:value "1080" :label "1080p"} {:value "720" :label "720p" :default? true}]
               :value "720" :show-value? true}
            t (vp/video-player {:src "a.mp4" :buttons [b]})
            trig (first (find-attr t :data-vp-open "q"))]
        (is (= "false" (:aria-pressed (attrs trig))))
        (is (= "720p" (text (first (find-attr trig :data-vp-summary "q"))))))))
  (testing "a custom button with a built-in id replaces the built-in"
    (let [tree (vp/video-player {:src "a.mp4"
                                 :buttons [{:id :quality :label "Quality"
                                            :options [{:value "hi" :label "High"}] :value "hi"}]})
          trigger (attrs (first (find-attr tree :data-vp-open "quality")))]
      (is (nil? (:data-vp-builtin trigger)))
      (is (= 1 (count (find-attr tree :data-vp-panel "quality")))))))

(deftest toggle-and-action-test
  (let [tree (vp/video-player {:src "a.mp4"
                               :controls [:loop :share :fullscreen]
                               :buttons [{:id :loop :type :toggle :icon :repeat :label "Loop" :pressed? true}
                                         {:id :share :type :action :icon :share :label "Share"}]})]
    (is (= "true" (:aria-pressed (attrs (first (find-attr tree :data-vp-toggle "loop"))))))
    (is (= 1 (count (find-attr tree :data-vp-action "share"))))
    (testing ":controls is exact"
      (is (empty? (find-attr tree :data-vp-open "speed"))))))

(deftest tracks-test
  (let [tree (vp/video-player {:src "a.mp4" :tracks [{:src "en.vtt" :label "English" :lang "en" :default? true}]})
        [_ a] (first (filter #(= :track (first %)) (nodes tree)))]
    (is (= {:kind "subtitles" :src "en.vtt" :label "English" :srclang "en" :default true} a))))
