(ns ui.notification-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.notification :as notification]))

(defn- attrs [el] (second el))

(defn- find-all
  "Every element under `el` whose attrs satisfy `pred`."
  [pred el]
  (filter (fn [x] (and (vector? x) (map? (second x)) (pred (second x))))
          (tree-seq (fn [x] (or (vector? x) (seq? x))) seq el)))

(deftest class-list-test
  (is (= ["notification" "notification-default"] (notification/notification-class-list {})))
  (is (= "notification notification-error notification-multiline"
         (notification/notification-classes {:type :error :multiline true}))))

(deftest card-test
  (testing "a plain card: no icon for :default, title only"
    (let [card (notification/notification {:title "Saved"})]
      (is (= :div (first card)))
      (is (= "notification notification-default" (:class (attrs card))))
      (is (= "default" (:data-type (attrs card))))
      (is (empty? (find-all :data-icon card)))
      (is (= ["Saved"] (map last (find-all :data-title card))))))
  (testing "a type picks its icon; loading shows Sonner's loader"
    (is (seq (find-all :data-icon (notification/notification {:type :success :title "T"}))))
    (let [loading (notification/notification {:type :loading :title "T"})]
      (is (= 12 (count (find-all #(= "notification-loader-bar" (:class %)) loading))))
      (is (= ["true"] (map (comp :data-visible second)
                           (find-all #(= "notification-loader" (:class %)) loading))))))
  (testing "a promise keeps the loader, hidden, next to its result"
    (let [card (notification/notification {:type :success :promise true :title "T"})]
      (is (= ["false"] (map (comp :data-visible second)
                            (find-all #(= "notification-loader" (:class %)) card))))
      (is (seq (find-all #(= "notification-glyph" (:class %)) card)))))
  (testing "icon false hides the tile"
    (is (empty? (find-all :data-icon (notification/notification {:type :error :icon false :title "T"})))))
  (testing "action and cancel are marked for the runtime; close button opt-in, never while loading"
    (let [card (notification/notification {:title "T" :close-button true
                                           :action {:label "Undo"} :cancel {:label "Keep"}})]
      (is (= 1 (count (find-all :data-action card))))
      (is (= 1 (count (find-all :data-cancel card))))
      (is (= 1 (count (find-all :data-close-button card)))))
    (is (empty? (find-all :data-close-button (notification/notification {:title "T"}))))
    (is (empty? (find-all :data-close-button
                          (notification/notification {:type :loading :title "T" :close-button true})))))
  (testing "rich colors, invert, unstyled as data attributes"
    (let [a (attrs (notification/notification {:type :error :title "T" :rich-colors true :invert true :unstyled true}))]
      (is (= ["true" "true" "false"] [(:data-rich-colors a) (:data-invert a) (:data-styled a)])))))

(deftest helpers-test
  (is (= ["bottom" "right"] (notification/position-parts nil)))
  (is (= ["top" "center"] (notification/position-parts :top-center)))
  (is (= ["bottom" "right"] (notification/default-swipe-directions :bottom-right)))
  (is (= ["top"] (notification/default-swipe-directions :top-center)))
  (is (= "alt+T" (notification/hotkey-label ["altKey" "KeyT"])))
  (is (= {:data-offset-top "24px" :data-offset-right "24px" :data-offset-bottom "24px" :data-offset-left "24px"}
         (notification/offset-attrs "offset" 24)))
  (is (= {:data-mobile-offset-bottom "1rem"} (notification/offset-attrs "mobile-offset" {:bottom "1rem"}))))

(deftest toaster-test
  (testing "no items: the live region alone, labelled with the hotkey"
    (let [t (notification/notification-toaster {})]
      (is (= :section (first t)))
      (is (= "Notifications alt+T" (:aria-label (attrs t))))
      (is (= "polite" (:aria-live (attrs t))))
      (is (= "altKey+KeyT" (:data-hotkey (attrs t))))
      (is (= 2 (count t)))))
  (testing "one list per position, toasts newest first with Sonner's flags"
    (let [items [{:id "a" :title "A" :type :success}
                 {:id "b" :title "B"}
                 {:id "c" :title "C" :removed-at 5}
                 {:id "d" :title "D"}
                 {:id "t" :title "Top" :position :top-center}]
          t     (notification/notification-toaster {:items items :close-button true :gap 10
                                                    :visible-toasts 3 :contained true})
          lists (find-all :data-ui-notification-list t)
          lis   (find-all :data-ui-notification t)]
      (is (= "notification-toaster notification-toaster-contained" (:class (attrs t))))
      (is (= [["bottom" "right"] ["top" "center"]]
             (map (juxt (comp :data-y-position second) (comp :data-x-position second)) lists)))
      (is (= "10" (:data-gap (attrs (first lists)))))
      (is (= "bottom right" (:data-swipe-directions (attrs (first lists)))))
      (is (= ["a" "b" "c" "d" "t"] (map (comp :data-id second) lis)))
      (is (= ["true" "false" "false" "false" "true"] (map (comp :data-front second) lis)))
      (is (= ["true" "true" "true" "false" "true"] (map (comp :data-visible second) lis)))
      (is (= ["false" "false" "true" "false" "false"] (map (comp :data-removed second) lis)))
      (is (= ["0" "1" "2" "3" "0"] (map (comp :data-index second) lis)))
      (is (= 5 (count (find-all :data-close-button t))) "the toaster's close-button reaches every toast")))
  (testing "a toast's own settings win over the toaster's"
    (let [t  (notification/notification-toaster {:items [{:id "a" :title "A" :rich-colors false :close-button false}]
                                                 :rich-colors true :close-button true})
          li (first (find-all :data-ui-notification t))]
      (is (= "false" (:data-rich-colors (attrs li))))
      (is (empty? (find-all :data-close-button li))))))

;; ── Queue model ──

(defn- ids [state] (mapv :id (notification/visible state)))

(deftest lifetime-test
  (is (= 4000 (notification/lifetime {:type :success})))
  (is (= 4000 (notification/lifetime {:type :error})) "Sonner closes errors too")
  (is (= 4000 (notification/lifetime {})))
  (is (nil? (notification/lifetime {:type :loading})))
  (is (nil? (notification/lifetime {:duration ##Inf})))
  (is (= 1500 (notification/lifetime {:type :error :duration 1500})))
  (is (= 4000 (notification/lifetime {:duration 0})) "0 falls back, as in Sonner")
  (is (= 6000 (notification/lifetime {} 6000)) "the toaster's default"))

(deftest post-test
  (testing "new items go in front, get an id and a deadline"
    (let [s (-> (notification/queue)
                (notification/post {:title "A"} 1000)
                (notification/post {:id "b" :type :loading :title "B"} 2000))]
      (is (= ["b" "notification-1"] (ids s)))
      (is (= 5000 (:expires-at (second (:items s)))))
      (is (nil? (:expires-at (first (:items s)))))))
  (testing "a known id updates in place; a loading one becomes a promise"
    (let [s   (-> (notification/queue)
                  (notification/post {:id "job" :type :loading :title "Running" :description "bb test"} 0)
                  (notification/post {:id "copy" :title "Copied"} 100)
                  (notification/post {:id "job" :type :success :title "Passed"} 3000))
          job (second (:items s))]
      (is (= ["copy" "job"] (ids s)))
      (is (= {:type :success :title "Passed" :description "bb test" :promise true :expires-at 7000}
             (select-keys job [:type :title :description :promise :expires-at])))))
  (testing "a leaving item with the same id is replaced, not merged"
    (let [s (-> (notification/queue)
                (notification/post {:id "a" :title "Old" :action {:label "Undo"}} 0)
                (notification/dismiss "a" 10)
                (notification/post {:id "a" :title "New"} 20))]
      (is (= [{:id "a" :title "New" :expires-at 4020}] (:items s)))))
  (testing "the default lifetime and limit come from opts"
    (let [s (reduce (fn [s i] (notification/post s {:id i} i {:limit 3 :duration 100}))
                    (notification/queue) (range 5))]
      (is (= [4 3 2] (ids s)))
      (is (= 104 (:expires-at (first (:items s))))))))

(deftest leave-test
  (let [s (-> (notification/queue)
              (notification/post {:id "a"} 0)
              (notification/post {:id "b"} 0))]
    (testing "dismiss keeps the item remove-delay ms for its exit"
      (let [d (notification/dismiss s "b" 100 :swipe)]
        (is (= ["b" "a"] (ids d)))
        (is (= {:removed-at 100 :closed :swipe} (select-keys (first (:items d)) [:removed-at :closed])))
        (is (= 300 (notification/next-due d)))
        (is (= ["a"] (mapv :id (notification/active d))))
        (is (= ["b" "a"] (ids (notification/expire d 299))))
        (is (= ["a"] (ids (notification/expire d 300))))))
    (testing "dismiss-all closes all; a closing item is not closed again"
      (let [d (-> s (notification/dismiss "a" 50) (notification/dismiss-all 100))]
        (is (= [100 50] (map :removed-at (:items d))))))))

(deftest clock-test
  (let [s (-> (notification/queue)
              (notification/post {:id "a" :type :success} 0)        ; due 4000
              (notification/post {:id "b" :duration 8000} 1000)     ; due 9000
              (notification/post {:id "c" :type :loading} 1000))]  ; never
    (is (= 4000 (notification/next-due s)))
    (testing "time up: auto-closing for remove-delay, then gone"
      (let [e (notification/expire s 4000)]
        (is (= {:removed-at 4000 :closed :auto} (select-keys (nth (:items e) 2) [:removed-at :closed])))
        (is (= 4200 (notification/next-due e)))
        (is (= ["c" "b"] (ids (notification/expire e 4200))))
        (is (= ["a"] (map :id (notification/newly-closed s e))))))
    (testing "paused: nothing starts closing, leaving ones still finish"
      (let [p (-> s (notification/dismiss "b" 3000) (notification/pause 3500))]
        (is (= 3200 (notification/next-due p)))
        (is (= ["c" "a"] (ids (notification/expire p 20000))))
        (is (= 3500 (:paused-at (notification/pause p 3900))) "pausing again keeps the first pause")
        (testing "resume gives each clock the time it had left"
          (let [r (notification/resume p 6500)]
            (is (nil? (:paused-at r)))
            (is (= 7000 (:expires-at (nth (:items r) 2))))))))
    (testing "posted while paused, the clock starts at the pause"
      (let [p (-> (notification/queue) (notification/pause 1000) (notification/post {:id "x"} 3000))]
        (is (= 5000 (:expires-at (first (:items p)))))
        (is (nil? (notification/next-due p)))
        (is (= 7000 (notification/next-due (notification/resume p 3000))))))))

(deftest mute-test
  (let [s (-> (notification/queue)
              (notification/post {:id "t" :source :terminal} 0)
              (notification/post {:id "c" :source :clipboard} 0)
              (notification/mute :terminal))]
    (is (notification/muted? s :terminal))
    (is (= ["c"] (ids s)))
    (is (= 2 (count (:items s))))
    (is (= ["c" "t"] (ids (notification/unmute s :terminal))))))
