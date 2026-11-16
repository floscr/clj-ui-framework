(ns dev.examples.comment
  (:require [dev.example :refer [example]]
            [ui.comment :as comment]
            [ui.skeleton :as skeleton]
            [ui.empty-state :as empty-state]))

(def ^:private discussion
  "A thread as an app has it: a tree. flat-replies turns each comment's
   subtree into the one branch the component shows."
  [{:author "marrow" :time "5h" :points 42
    :text "The contextual alternates are the whole trick. A single glyph per letter always reads as a font; three that rotate by neighbour start to read as a hand."
    :replies [{:author "dunlin" :time "4h"
               :text "Right, and it only works because the rotation isn't random. Random alternates give you the same letter twice in one word, which is exactly the tell."
               :replies [{:author "marrow" :time "4h"
                          :text "Which is what the write-up's state machine avoids: it never picks the variant it used last for that letter."
                          :replies [{:author "ostrakon" :time "3h"
                                     :text "Three variants and a rule that small. I expected a neural net."}]}]}
              {:author "sorenb" :badge "author" :time "3h"
               :text "Author here. The ligature table is 2,300 lines and I wrote about 40 of them by hand, the rest is generated from the kerning pairs."
               :replies [{:author "quoin" :time "2h" :text "Is the generator in the repo?"}]}]}
   {:author "tessellate" :time "5h" :points 23
    :text "Printed a page at 11pt next to my actual handwriting. My partner picked the font as the real one."}
   {:author "halfpx" :time "2h" :points 11
    :text "Does it survive variable font axes? Weight would be the obvious one."
    :replies [{:author "sorenb" :badge "author" :time "1h" :text "Not yet. Each master would need its own 780 glyphs."}
              {:author "quoin" :time "1h" :text "Interpolating handwriting sounds like a cursed research topic."}]}])

(defn- item
  "A top-level comment from the tree, its replies flattened onto a branch."
  [{:keys [author badge time points text] :as c} open]
  (let [replies (comment/flat-replies c)]
    (comment/comment-item
     {:author author :badge badge :time time
      :meta (when points (str points " points"))
      :replies (when (seq replies)
                 (apply comment/comment-replies
                        {:open open :people (map :author replies)}
                        (map (fn [r]
                               (comment/comment-reply {:author (:author r) :badge (:badge r)
                                                       :time (:time r) :to (:to r)}
                                                      (:text r)))
                             replies)))}
     text)))

(def examples
  [(example :thread "A thread: replies folded into a line, the first branch open"
     (fn []
       (apply comment/comment-thread {}
              (map-indexed (fn [i c] (item c (zero? i))) discussion))))

   (example :states "A linked comment, a deleted one kept for its reply, a linked reply"
     (fn []
       (comment/comment-thread {}
         (comment/comment-item {:author "tessellate" :time "5h" :meta "23 points" :target true}
           "Printed a page at 11pt next to my actual handwriting. My partner picked the font as the real one.")
         (comment/comment-item {:deleted true :time "4h"
                                :replies (comment/comment-replies {:open true :people ["greyscale" "quoin"]}
                                           (comment/comment-reply {:author "greyscale" :time "4h"}
                                             "For anyone reading later: the parent asked about licensing. It is OFL.")
                                           (comment/comment-reply {:author "quoin" :time "3h" :to "greyscale" :target true}
                                             "Thanks, that is the answer I came for."))}
           "This comment was deleted.")
         (comment/comment-item {:author "halfpx" :time "2h" :meta "edited"}
           [:p "Two paragraphs, a " [:a {:href "#"} "link"] " and some code:"]
           [:pre "calt: sub a' by a.alt2;"]))))

   (example :single "One comment on its own, outside a thread"
     (fn []
       (comment/comment-item {:author "Mara Okafor" :badge "reviewer" :time "12 Nov"}
         "Looks right to me. The empty state could say what to do next.")))

   (example :loading-empty "Loading, and a thread with nothing in it"
     (fn []
       [:div {:style #?(:squint {"display" "grid" "gap" "24px"}
                        :cljs   {:display "grid" :gap "24px"}
                        :clj    "display: grid; gap: 24px;")}
        [:div {:style #?(:squint {"display" "grid" "gap" "8px"}
                         :cljs   {:display "grid" :gap "8px"}
                         :clj    "display: grid; gap: 8px;")}
         (skeleton/skeleton {:variant :line :attrs {:style #?(:squint {"width" "30%"}
                                                              :cljs   {:width "30%"}
                                                              :clj    "width: 30%;")}})
         (skeleton/skeleton {:variant :line})
         (skeleton/skeleton {:variant :line :attrs {:style #?(:squint {"width" "65%"}
                                                              :cljs   {:width "65%"}
                                                              :clj    "width: 65%;")}})]
        (empty-state/empty-state {:icon :inbox} "No comments yet.")]))])
