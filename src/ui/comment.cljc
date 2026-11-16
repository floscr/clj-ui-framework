(ns ui.comment
  "Comments and their replies. A thread is one rail with a marker per
   comment. A comment's replies fold into one line (\"3 replies\", with a dot
   per person who answered) and open in place as a flat branch: an answer to
   a reply names who it answers instead of indenting further, so a deep
   discussion stays readable in a narrow pane.

   Folding is a native <details>, so it works on every target without state
   or runtime JS."
  (:require [clojure.string :as str]
            [ui.util :as util]
            [ui.icon :as icon]))

;; ── Pure helpers ────────────────────────────────────────────────────

(defn replies-label
  "\"1 reply\", \"3 replies\"."
  [n]
  (str n (if (= 1 n) " reply" " replies")))

(def ^:private tones 5)

(defn person-tone
  "A stable number from 1 to 5 for a name: the color of that person's dot
   on a folded replies line (comment-face-1 to comment-face-5)."
  [person]
  (let [s (str person)
        n (count s)]
    (loop [i 0, sum 0]
      (if (< i n)
        (recur (inc i)
               (+ sum #?(:squint (.charCodeAt s i)
                         :cljs   (.charCodeAt s i)
                         :clj    (int (.charAt ^String s i)))))
        (inc (mod sum tones))))))

(defn flat-replies
  "A comment's reply tree as the flat branch this component shows: every
   reply at any depth, in reading order (a reply, then the answers to it),
   without its own nested replies. An answer to a reply gets :to, the author
   it answers; a direct reply to the comment has none.

   `item` is a map with its replies under :replies and an author under
   :author; opts :replies-key and :author-key name other keys."
  ([item] (flat-replies item nil))
  ([item {:keys [replies-key author-key]}]
   (let [rk   (or replies-key :replies)
         ak   (or author-key :author)
         walk (fn walk [node to]
                (mapcat (fn [reply]
                          (cons (cond-> (dissoc reply rk) to (assoc :to to))
                                (walk reply (get reply ak))))
                        (get node rk)))]
     (vec (walk item nil)))))

(defn comment-class-list
  "Classes for a comment (or with :reply a reply): :target marks the linked
   one, :deleted a removed one that stays for its replies."
  [{:keys [reply target deleted]}]
  (cond-> [(if reply "comment-reply" "comment")]
    target  (conj "comment-target")
    deleted (conj "comment-deleted")))

(defn comment-classes
  "Space-joined form of comment-class-list."
  [opts]
  (str/join " " (comment-class-list opts)))

;; ── Elements ────────────────────────────────────────────────────────

(defn- head
  "The line above a comment: who, an optional badge, when, whom it answers,
   and trailing metadata."
  [author badge time to meta deleted]
  (cond-> [:div {:class "comment-head"}
           [:span {:class "comment-author"} (or author (when deleted "deleted"))]]
    badge (conj [:span {:class "comment-badge"} badge])
    time  (conj [:span {:class "comment-time"} time])
    to    (conj [:span {:class "comment-to"} "to " to])
    meta  (conj [:span {:class "comment-meta"} meta])))

(defn comment-thread
  "The rail that holds comments: one marker per comment on a single line.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes map

   Children are comment-item elements."
  [{:keys [class attrs] :as _props} & comments]
  (into [:div (util/class-attrs ["comment-thread"] class attrs)]
        (remove nil? comments)))

(defn comment-item
  "One comment: a head line, the body, and optionally its folded replies.
   Inside a comment-thread it is a marker on the rail; on its own it is a
   plain block.

   Props:
     :author  - who wrote it (string or hiccup); a deleted comment without
                one reads \"deleted\"
     :badge   - a short accent word after the author (\"author\", \"maintainer\")
     :time    - when, already formatted (\"5h\", \"12 Nov\")
     :meta    - trailing muted text (\"42 points\", \"edited\")
     :target  - boolean; the comment a link pointed at, tinted
     :deleted - boolean; a removed comment kept for its replies
     :replies - a comment-replies element, shown under the body
     :class   - additional CSS classes
     :attrs   - additional HTML attributes map

   Children are the body: text or hiccup (paragraphs, links, code blocks)."
  [{:keys [author badge time meta target deleted replies class attrs] :as _props} & body]
  (cond-> [:article (util/class-attrs (comment-class-list {:target target :deleted deleted})
                                      class attrs)
           (head author badge time nil meta deleted)
           (into [:div {:class "comment-body"}] body)]
    replies (conj replies)))

(defn comment-reply
  "One reply on an opened branch. Replies never nest: an answer to a reply
   sits on the same branch and names who it answers with :to.

   Props:
     :author  - who wrote it (string or hiccup)
     :badge   - a short accent word after the author
     :time    - when, already formatted
     :to      - whom it answers, when that is another reply (\"to marrow\")
     :meta    - trailing muted text
     :target  - boolean; the reply a link pointed at
     :deleted - boolean; a removed reply
     :class   - additional CSS classes
     :attrs   - additional HTML attributes map

   Children are the body."
  [{:keys [author badge time to meta target deleted class attrs] :as _props} & body]
  [:div (util/class-attrs (comment-class-list {:reply true :target target :deleted deleted})
                          class attrs)
   (head author badge time to meta deleted)
   (into [:div {:class "comment-body"}] body)])

(defn comment-replies
  "A comment's replies behind one line: the people who answered as dots,
   the count, a chevron. A click opens the branch in place (native
   <details>, no JS).

   Props:
     :label  - the line's text; defaults to the number of children
               (\"3 replies\")
     :people - names of the people who answered; up to four become dots,
               each person always the same color
     :open   - boolean; opened from the start
     :class  - additional CSS classes
     :attrs  - additional HTML attributes map

   Children are comment-reply elements."
  [{:keys [label people open class attrs] :as _props} & replies]
  (let [replies (remove nil? replies)
        faces   (take 4 (distinct people))]
    [:details (cond-> (util/class-attrs ["comment-replies"] class attrs)
                open (assoc :open true))
     (cond-> [:summary {:class "comment-fold"}]
       (seq faces)
       (conj (into [:span {:class "comment-faces" :aria-hidden "true"}]
                   (map (fn [person]
                          [:span (util/class-attrs ["comment-face" (str "comment-face-" (person-tone person))]
                                                   nil nil)])
                        faces)))
       true (conj [:span {:class "comment-fold-label"} (or label (replies-label (count replies)))])
       true (conj (icon/icon {:icon-name :chevron-right :size :sm})))
     (into [:div {:class "comment-branch"}] replies)]))
