(ns ui.lightbox-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ui.lightbox :as lb]))

(defn- nodes
  "Every hiccup element (vector with a tag keyword) in the tree."
  [tree]
  (filter #(and (vector? %) (keyword? (first %)))
          (tree-seq #(or (vector? %) (seq? %)) seq tree)))

(defn- attrs [node] (let [a (second node)] (when (map? a) a)))

(defn- classes [node] (set (str/split (or (:class (attrs node)) "") #"\s+")))

(defn- by-class [tree cls] (filter #(contains? (classes %) cls) (nodes tree)))

(defn- by-attr [tree k v] (filter #(= v (get (attrs %) k)) (nodes tree)))

(defn- text [node] (str/join (filter string? (tree-seq #(or (vector? %) (seq? %)) seq node))))

(def items
  [{:src "a.jpg" :thumb "a-t.jpg" :alt "A" :caption "First" :width 4000 :height 3000}
   {:src "b.mp4" :type :video :poster "b.jpg" :autoplay true :muted true :loop true}
   {:content [:p "Hello"] :key "note"}])

(deftest nothing-to-show
  (is (nil? (lb/lightbox {})))
  (is (nil? (lb/lightbox {:src nil :on-close identity})))
  (is (nil? (lb/lightbox {:items []}))))

(deftest single-image-shorthand
  (let [tree (lb/lightbox {:src "x.png" :alt "X"})
        [img] (by-class tree "ui-lightbox-media")]
    (is (= "1" (:data-count (attrs tree))))
    (is (= "x.png" (:data-src (attrs img))))
    (is (= "X" (:alt (attrs img))))
    (testing "the runtime sets src, the markup never does"
      (is (nil? (:src (attrs img)))))))

(deftest root-attributes
  (let [a (attrs (lb/lightbox {:items items :index 1 :id "g" :open false :thumbnails true
                               :history true :keyboard false :scroll-to-zoom true :max-zoom 4
                               :contained true :class "extra more"}))]
    (is (= "g" (:id a)))
    (is (true? (:hidden a)))
    (is (= "" (:data-lightbox a)))
    (is (= "1" (:data-index a)))
    (is (= "3" (:data-count a)))
    (is (= "" (:data-thumbnails a)))
    (is (= "" (:data-history a)))
    (is (= "off" (:data-keyboard a)))
    (is (= "" (:data-wheel-zoom a)))
    (is (= "4" (:data-max-zoom a)))
    (is (= "" (:data-contained a)))
    (is (= "ui-lightbox ui-lightbox--contained extra more" (:class a))))
  (testing "defaults leave the optional attributes out"
    (let [a (attrs (lb/lightbox {:items items}))]
      (is (nil? (:hidden a)))
      (is (= "0" (:data-index a)))
      (is (every? #(not (contains? a %))
                  [:data-thumbnails :data-history :data-keyboard :data-wheel-zoom :data-contained])))))

(deftest index-is-clamped
  (is (= "2" (:data-index (attrs (lb/lightbox {:items items :index 9})))))
  (is (= "0" (:data-index (attrs (lb/lightbox {:items items :index -3}))))))

(deftest slides
  (let [tree (lb/lightbox {:items items})
        slides (by-class tree "ui-lightbox-slide")]
    (is (= 3 (count slides)))
    (is (= ["image" "video" "content"] (map #(:data-type (attrs %)) slides)))
    (is (= ["a-t.jpg" "b.mp4" "note"] (map #(:data-key (attrs %)) slides)))
    (testing "image"
      (let [img (first (by-class (first slides) "ui-lightbox-media"))]
        (is (= :img (first img)))
        (is (= "a-t.jpg" (:data-thumb (attrs img))))
        (is (= 4000 (:width (attrs img))))
        (is (= "First" (text (first (by-class (first slides) "ui-lightbox-caption")))))
        (is (seq (by-class (first slides) "ui-lightbox-error")))))
    (testing "video: a ui.video-player that gets its source only when shown"
      (let [box (first (by-class (second slides) "ui-lightbox-player"))
            player (first (by-attr box :data-video-player ""))]
        (is (contains? (classes box) "ui-lightbox-media"))
        (is (= "" (:data-autoplay (attrs box))))
        (is (= "" (:data-muted (attrs box))))
        (is (= "" (:data-loop (attrs box))))
        (is (= "" (:data-src (attrs player))))
        (is (= "b.mp4" (:data-lightbox-src (attrs player))))
        (is (contains? (classes player) "video-player-fill"))
        (is (= "b.jpg" (:poster (attrs (first (filter #(= :video (first %)) (nodes box)))))))))
    (testing "video-player props pass through :player"
      (let [tree (lb/lightbox {:items [{:type :video :src "v.mp4" :player {:title "Clip" :attrs {:id "p"}}}]})
            player (first (by-attr tree :data-video-player ""))]
        (is (= "Clip" (:aria-label (attrs player))))
        (is (= "p" (:id (attrs player))))
        (is (= "v.mp4" (:data-lightbox-src (attrs player))))))
    (testing "content"
      (is (= "Hello" (text (first (by-class (nth slides 2) "ui-lightbox-content"))))))))

(deftest chrome
  (let [tree (lb/lightbox {:items items :index 1})]
    (is (= "2 / 3" (text (first (by-class tree "ui-lightbox-counter")))))
    (is (= #{"close" "prev" "next" "zoom-in" "zoom-out"}
           (set (keep #(:data-lightbox-action (attrs %)) (nodes tree)))))
    (is (every? #(:aria-label (attrs %)) (by-class tree "ui-lightbox-button"))))
  (testing "labels can be replaced"
    (let [tree (lb/lightbox {:items items :labels {:close "Schließen"}})]
      (is (= "Schließen" (:aria-label (attrs (first (by-attr tree :data-lightbox-action "close")))))))))

(deftest slots
  (let [dl (lb/action {:icon-name :download :label "Download" :href "a.jpg" :download true})
        tree (lb/lightbox {:items items
                           :actions [dl (lb/action {:icon-name :copy :label "Copy"})]
                           :footer [:span "Pick"]})
        actions (first (by-class tree "ui-lightbox-actions"))]
    (testing "actions come before zoom and close"
      (is (= ["Download" "Copy" "Zoom out" "Zoom in" "Close"]
             (map #(:aria-label (attrs %)) (filter #(contains? (classes %) "ui-lightbox-button")
                                                   (drop 2 actions))))))
    (is (= "Pick" (text (first (by-class tree "ui-lightbox-footer"))))))
  (testing "a download link has no new-tab target"
    (let [a (attrs (lb/action {:icon-name :download :label "D" :href "x" :download "x.jpg"}))]
      (is (= "x.jpg" (:download a)))
      (is (nil? (:target a)))))
  (testing "an external link opens in a new tab"
    (is (= "_blank" (:target (attrs (lb/action {:icon-name :external-link :label "O" :href "x"})))))))

(deftest thumbnail-strip
  (let [tree (lb/lightbox {:items items :thumbnails true})
        buttons (by-class tree "ui-lightbox-thumb-button")]
    (is (= ["0" "1" "2"] (map #(:data-index (attrs %)) buttons)))
    (is (every? #(= "go" (:data-lightbox-action (attrs %))) buttons))
    (is (= "a-t.jpg" (:src (attrs (first (by-attr (first buttons) :loading "lazy"))))))
    (is (= "b.jpg" (:src (attrs (first (by-attr (second buttons) :loading "lazy")))))))
  (is (empty? (by-class (lb/lightbox {:items items}) "ui-lightbox-thumbs"))))

(deftest image-thumbnail
  (testing "the key defaults to src, so it matches a slide without :thumb"
    (let [a (attrs (lb/image-thumbnail {:src "a.jpg"}))]
      (is (= "lightbox-thumb" (:class a)))
      (is (= "a.jpg" (:data-lightbox-key a)))
      (is (= "image" (:alt a)))))
  (testing "managed"
    (let [a (attrs (lb/image-thumbnail {:src "t.jpg" :lightbox "g" :index 3 :lightbox-key "k"
                                        :class ["x" "y"]}))]
      (is (= "g" (:data-lightbox-open a)))
      (is (= "3" (:data-lightbox-index a)))
      (is (= "k" (:data-lightbox-key a)))
      (is (= "lightbox-thumb x y" (:class a))))))

(deftest item-helpers
  (is (= "image" (lb/item-type {:src "a"})))
  (is (= "video" (lb/item-type {:type :video})))
  (is (= "content" (lb/item-type {:content [:p]})))
  (is (= "k" (lb/item-key {:key "k" :thumb "t" :src "s"})))
  (is (= "t" (lb/item-key {:thumb "t" :src "s"})))
  (is (= "s" (lb/item-key {:src "s"}))))
