(ns ui.comment-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.comment :as comment]))

(deftest helpers-test
  (testing "the replies line's default text"
    (is (= "1 reply" (comment/replies-label 1)))
    (is (= "3 replies" (comment/replies-label 3)))
    (is (= "0 replies" (comment/replies-label 0))))
  (testing "a person always gets the same tone, from 1 to 5"
    (is (= (comment/person-tone "marrow") (comment/person-tone "marrow")))
    (is (every? #(<= 1 (comment/person-tone %) 5) ["a" "marrow" "sorenb" "" nil "ÿ"]))
    (is (< 1 (count (distinct (map comment/person-tone ["a" "b" "c" "d" "e"])))))))

(deftest flat-replies-test
  (let [tree {:author "marrow"
              :replies [{:author "dunlin" :text "1"
                         :replies [{:author "marrow" :text "2"
                                    :replies [{:author "ostrakon" :text "3" :replies []}]}]}
                        {:author "sorenb" :text "4"}]}]
    (testing "reading order, nesting gone, answers to replies name whom they answer"
      (is (= [{:author "dunlin" :text "1"}
              {:author "marrow" :text "2" :to "dunlin"}
              {:author "ostrakon" :text "3" :to "marrow"}
              {:author "sorenb" :text "4"}]
             (comment/flat-replies tree))))
    (testing "a comment without replies"
      (is (= [] (comment/flat-replies {:author "x"})))
      (is (= [] (comment/flat-replies {:author "x" :replies []}))))
    (testing "other keys"
      (is (= [{:by "b"} {:by "c" :to "b"}]
             (comment/flat-replies {:by "a" :kids [{:by "b" :kids [{:by "c"}]}]}
                                   {:replies-key :kids :author-key :by}))))))

(deftest class-list-test
  (is (= ["comment"] (comment/comment-class-list {})))
  (is (= ["comment-reply"] (comment/comment-class-list {:reply true})))
  (is (= "comment comment-target comment-deleted"
         (comment/comment-classes {:target true :deleted true}))))

(deftest comment-thread-test
  (let [result (comment/comment-thread {:class "x" :attrs {:id "t"}} [:article "a"] nil [:article "b"])]
    (is (= :div (first result)))
    (is (= "comment-thread x" (get-in result [1 :class])))
    (is (= "t" (get-in result [1 :id])))
    (is (= [[:article "a"] [:article "b"]] (subvec result 2)) "nil children are dropped")))

(deftest comment-item-test
  (testing "head, body, no replies"
    (let [result (comment/comment-item {:author "marrow" :badge "author" :time "5h" :meta "42 points"}
                                       "Hello " [:a {:href "#"} "there"])]
      (is (= :article (first result)))
      (is (= "comment" (get-in result [1 :class])))
      (is (= [:div {:class "comment-head"}
              [:span {:class "comment-author"} "marrow"]
              [:span {:class "comment-badge"} "author"]
              [:span {:class "comment-time"} "5h"]
              [:span {:class "comment-meta"} "42 points"]]
             (nth result 2)))
      (is (= [:div {:class "comment-body"} "Hello " [:a {:href "#"} "there"]] (nth result 3)))
      (is (= 4 (count result)))))
  (testing "only what is given shows in the head"
    (is (= [:div {:class "comment-head"} [:span {:class "comment-author"} "x"]]
           (nth (comment/comment-item {:author "x"} "b") 2))))
  (testing "target and deleted; a deleted comment without an author reads deleted"
    (let [result (comment/comment-item {:target true :deleted true :class "mine"} "gone")]
      (is (= "comment comment-target comment-deleted mine" (get-in result [1 :class])))
      (is (= [:span {:class "comment-author"} "deleted"] (get-in result [2 2])))))
  (testing "replies come after the body"
    (let [replies (comment/comment-replies {} (comment/comment-reply {:author "b"} "r"))
          result  (comment/comment-item {:author "a" :replies replies} "body")]
      (is (= replies (nth result 4))))))

(deftest comment-reply-test
  (let [result (comment/comment-reply {:author "marrow" :time "4h" :to "dunlin" :target true} "text")]
    (is (= :div (first result)))
    (is (= "comment-reply comment-target" (get-in result [1 :class])))
    (is (= [:div {:class "comment-head"}
            [:span {:class "comment-author"} "marrow"]
            [:span {:class "comment-time"} "4h"]
            [:span {:class "comment-to"} "to " "dunlin"]]
           (nth result 2)))
    (is (= [:div {:class "comment-body"} "text"] (nth result 3)))))

(deftest comment-replies-test
  (testing "folded by default, labelled by its children"
    (let [result (comment/comment-replies {} [:div "a"] nil [:div "b"])]
      (is (= :details (first result)))
      (is (= {:class "comment-replies"} (nth result 1)))
      (let [summary (nth result 2)]
        (is (= :summary (first summary)))
        (is (= [:span {:class "comment-fold-label"} "2 replies"] (nth summary 2)))
        (is (= :svg (first (nth summary 3))) "the chevron"))
      (is (= [:div {:class "comment-branch"} [:div "a"] [:div "b"]] (nth result 3)))))
  (testing "open, a label of its own, the people as dots"
    (let [result  (comment/comment-replies {:open true :label "12 replies"
                                            :people ["dunlin" "marrow" "dunlin" "a" "b" "c"]}
                                           [:div "a"])
          summary (nth result 2)
          faces   (nth summary 2)]
      (is (true? (get-in result [1 :open])))
      (is (= "true" (get-in faces [1 :aria-hidden])))
      (is (= 4 (count (subvec faces 2))) "four distinct people at most")
      (is (= (str "comment-face comment-face-" (comment/person-tone "dunlin"))
             (get-in faces [2 1 :class])))
      (is (= [:span {:class "comment-fold-label"} "12 replies"] (nth summary 3))))))
