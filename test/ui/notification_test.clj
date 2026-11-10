(ns ui.notification-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.notification :as notification]))

(deftest notification-class-list-test
  (is (= ["notification"] (notification/notification-class-list {})))
  (is (= ["notification" "notification-loading"] (notification/notification-class-list {:status :loading})))
  (is (= "notification notification-danger notification-multiline"
         (notification/notification-classes {:status :danger :multiline true}))))

(deftest notification-stack-class-list-test
  (is (= ["notification-stack"] (notification/notification-stack-class-list {})))
  (is (= "notification-stack notification-stack-expanded notification-stack-bottom-right"
         (notification/notification-stack-classes {:expanded true :position :bottom-right}))))

(defn- icon-name-of
  "The icon tile's first child: :svg for an icon, :span for the spinner."
  [tile]
  (first (nth tile 2)))

(deftest notification-test
  (testing "a neutral one-liner has no tile, no actions, no dismiss"
    (let [result (notification/notification {:title "Saved"})]
      (is (= :div (first result)))
      (is (= {:class "notification" :role "status"} (second result)))
      (is (= [:div {:class "notification-text"} [:p {:class "notification-title"} "Saved"]]
             (nth result 2)))
      (is (= 3 (count result)))))
  (testing "status picks the tile: a spinner while loading, an icon after"
    (let [loading (nth (notification/notification {:status :loading :title "Running"}) 2)
          success (nth (notification/notification {:status :success :title "Done"}) 2)]
      (is (= "notification-icon" (get-in loading [1 :class])))
      (is (= :span (icon-name-of loading)))
      (is (= "spinner spinner-xs" (get-in loading [2 1 :class])))
      (is (= "true" (get-in loading [2 1 :aria-hidden])) "the title already says it is running")
      (is (= :svg (icon-name-of success)))))
  (testing "an explicit icon overrides the status's; false removes the tile"
    (is (= :svg (icon-name-of (nth (notification/notification {:icon :terminal :title "T"}) 2))))
    (is (= "notification-text"
           (get-in (notification/notification {:status :success :icon false :title "T"}) [2 1 :class]))))
  (testing "description and meta make it multiline"
    (let [result (notification/notification {:status :success :title "T" :description "D" :meta "M"})]
      (is (= "notification notification-success notification-multiline" (get-in result [1 :class])))
      (is (= [:div {:class "notification-text"}
              [:p {:class "notification-title"} "T"]
              [:div {:class "notification-description"} "D"]
              [:div {:class "notification-meta"} "M"]]
             (nth result 3)))))
  (testing "danger is an alert"
    (is (= "alert" (get-in (notification/notification {:status :danger :title "T"}) [1 :role]))))
  (testing "actions follow the text, dismiss comes last"
    (let [result (notification/notification {:title "T" :dismiss-attrs {:hx-delete "/n/1"}}
                                            [:button "Open"] nil)]
      (is (= [:div {:class "notification-actions"} [:button "Open"]] (nth result 3)))
      (let [dismiss (nth result 4)]
        (is (= :button (first dismiss)))
        (is (= {:class "notification-dismiss" :type "button" :aria-label "Dismiss" :hx-delete "/n/1"}
               (second dismiss))))))
  (testing "on-dismiss alone shows the button; the handler is dropped on clj"
    (let [dismiss (last (notification/notification {:title "T" :on-dismiss (fn [_] nil)}))]
      (is (= "notification-dismiss" (get-in dismiss [1 :class])))
      (is (nil? (get-in dismiss [1 :on-click])))))
  (testing "class and attrs merge onto the card"
    (let [result (notification/notification {:title "T" :class "x" :attrs {:id "n1"}})]
      (is (= "notification x" (get-in result [1 :class])))
      (is (= "n1" (get-in result [1 :id]))))))

(deftest notification-stack-test
  (let [result (notification/notification-stack {:expanded true :class "x"} [:div "a"] nil [:div "b"])]
    (is (= :section (first result)))
    (is (= {:class "notification-stack notification-stack-expanded x"
            :aria-label "Notifications"
            :aria-live "polite"}
           (second result)))
    (is (= [[:div "a"] [:div "b"]] (drop 2 result)))))

(deftest notification-stack-dismiss-all-test
  (testing "Close all comes last, so the cards keep their nth-child places"
    (let [result (notification/notification-stack {:dismiss-all-attrs {:hx-delete "/n"}} [:div "a"] [:div "b"])]
      (is (= [:div "a"] (nth result 2)))
      (is (= [:div {:class "notification-stack-bar"}
              [:button {:class "notification-stack-close" :type "button" :hx-delete "/n"} "Close all"]]
             (last result)))))
  (testing "no button without a handler or without cards"
    (is (= 3 (count (notification/notification-stack {} [:div "a"]))))
    (is (= 2 (count (notification/notification-stack {:dismiss-all-attrs {}}))))))

;; ── Queue model ──

(defn- ids [state] (mapv :id (notification/visible state)))

(deftest lifetime-test
  (is (= 5000 (notification/lifetime {:status :success})))
  (is (= 5000 (notification/lifetime {})))
  (is (= 8000 (notification/lifetime {:status :warning})))
  (is (nil? (notification/lifetime {:status :loading})))
  (is (nil? (notification/lifetime {:status :danger})))
  (is (= 1500 (notification/lifetime {:status :danger :duration 1500})) ":duration wins")
  (is (nil? (notification/lifetime {:status :success :duration 0})) "0 keeps it"))

(deftest post-test
  (testing "new items go in front and get an id and a deadline"
    (let [s (-> (notification/queue)
                (notification/post {:title "A"} 1000)
                (notification/post {:id "b" :status :loading :title "B"} 2000))]
      (is (= ["b" "notification-1"] (ids s)))
      (is (= 6000 (:expires-at (second (:items s)))))
      (is (nil? (:expires-at (first (:items s)))) "loading stays")))
  (testing "a known id updates in place, keeps its other keys and restarts its clock"
    (let [s (-> (notification/queue)
                (notification/post {:id "job" :status :loading :title "Running" :description "bb test"} 0)
                (notification/post {:id "copy" :title "Copied"} 100)
                (notification/post {:id "job" :status :success :title "Tests passed"} 3000))
          job (second (:items s))]
      (is (= ["copy" "job"] (ids s)) "the job keeps its place")
      (is (= {:status :success :title "Tests passed" :description "bb test" :expires-at 8000}
             (select-keys job [:status :title :description :expires-at])))))
  (testing "past the limit the oldest drop"
    (let [s (reduce (fn [s i] (notification/post s {:id i} i 3)) (notification/queue) (range 5))]
      (is (= [4 3 2] (ids s))))))

(deftest dismiss-test
  (let [s (-> (notification/queue)
              (notification/post {:id "a"} 0)
              (notification/post {:id "b"} 0))]
    (is (= ["a"] (ids (notification/dismiss s "b"))))
    (is (= [] (ids (notification/dismiss-all s))))
    (testing "a dismissed job's result comes back as a new card"
      (is (= ["a"] (ids (-> s notification/dismiss-all (notification/post {:id "a" :status :success} 10)))))))) 

(deftest mute-test
  (let [s (-> (notification/queue)
              (notification/post {:id "t" :source :terminal} 0)
              (notification/post {:id "c" :source :clipboard} 0)
              (notification/mute :terminal))]
    (is (notification/muted? s :terminal))
    (is (= ["c"] (ids s)))
    (is (= 2 (count (:items s))) "muted items are kept")
    (is (= ["c" "t"] (ids (notification/unmute s :terminal))))))

(deftest clock-test
  (let [s (-> (notification/queue)
              (notification/post {:id "a" :status :success} 0)        ; due 5000
              (notification/post {:id "b" :status :warning} 1000)     ; due 9000
              (notification/post {:id "c" :status :loading} 1000))]  ; never
    (is (= 5000 (notification/next-due s)))
    (is (= ["c" "b" "a"] (ids (notification/expire s 4999))))
    (is (= ["c" "b"] (ids (notification/expire s 5000))))
    (testing "paused: nothing expires and nothing is due"
      (let [p (notification/pause s 4000)]
        (is (nil? (notification/next-due p)))
        (is (= ["c" "b" "a"] (ids (notification/expire p 20000))))
        (is (= 4000 (:paused-at (notification/pause p 4500))) "pausing again keeps the first pause")
        (testing "resume gives each clock the time it had left"
          (let [r (notification/resume p 7000)]
            (is (nil? (:paused-at r)))
            (is (= 8000 (notification/next-due r)))
            (is (= ["c" "b"] (ids (notification/expire r 8000))))))))
    (testing "an item posted while paused starts its clock at the pause"
      (let [p (-> (notification/queue) (notification/pause 1000) (notification/post {:id "x"} 3000))]
        (is (= 6000 (:expires-at (first (:items p)))))
        (is (= 8000 (notification/next-due (notification/resume p 3000))))))))
