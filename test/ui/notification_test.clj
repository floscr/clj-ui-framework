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
