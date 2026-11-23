(ns dev.examples.canvas
  (:require [dev.example :refer [example note sx]]
            [ui.canvas :as canvas]
            [ui.card :as card]))

;; A flow that takes a pull request from review to merge, while CI runs.
;; The main path is one column; side steps sit 276 to the left or right.
(def ^:private center 276)
(def ^:private right 552)
(defn- row [n] (+ 30 (* 92 n)))

(def ^:private pr-nodes
  [{:id "checkout" :title "checkout" :description "Worktrees for both branches" :icon :terminal :status :done :x center :y (row 0)}
   {:id "open" :title "open" :description "A chat in the worktree" :icon :inbox :status :done :x center :y (row 1)}
   {:id "review" :title "review" :description "The review-code skill" :icon :zap :status :done :x center :y (row 2)}
   {:id "brief" :title "brief" :description "The answer, shortened" :icon :zap :status :done :x center :y (row 3)}
   {:id "decide" :title "decide" :description "Waits for you" :icon :user :status :done :x center :y (row 4)}
   {:id "fix" :title "fix" :description "What you asked for" :icon :zap :x 0 :y (row 5)}
   {:id "push" :title "push" :description "Both branches, or none" :icon :terminal :status :done :x center :y (row 5)}
   {:id "pr" :title "pr" :description "A pull request for each?" :icon :terminal :status :done :x center :y (row 6)}
   {:id "create-pr" :title "create-pr" :description "Open the missing ones" :icon :zap :x right :y (row 7)}
   {:id "ci" :title "ci" :description "Checks of both PRs" :icon :clock :status :current :x center :y (row 8)}
   {:id "fix-ci" :title "fix-ci" :description "Failed twice: fix it" :icon :zap :x right :y (row 9)}
   {:id "comments" :title "comments" :description "Waits for reviewers" :icon :clock :x center :y (row 9)}
   {:id "fix-comments" :title "fix-comments" :description "Address the feedback" :icon :zap :x right :y (row 10)}
   {:id "done" :title "done" :description "Approved or merged" :icon :check :x center :y (row 10)}])

(def ^:private pr-edges
  [{:from "checkout" :to "open" :taken true}
   {:from "open" :to "review" :taken true}
   {:from "review" :to "brief" :taken true}
   {:from "brief" :to "decide" :taken true}
   {:from "decide" :to "fix"}
   {:from "fix" :to "review" :rail :left}
   {:from "decide" :to "push" :taken true}
   {:from "push" :to "pr" :taken true}
   {:from "pr" :to "ci" :taken true}
   {:from "pr" :to "create-pr"}
   {:from "create-pr" :to "ci"}
   {:from "ci" :to "comments"}
   {:from "ci" :to "fix-ci"}
   {:from "fix-ci" :to "brief" :rail :right}
   {:from "comments" :to "done"}
   {:from "comments" :to "fix-comments"}
   {:from "fix-comments" :to "brief" :rail :right}])

(defn- sticky [title text]
  (card/card {} [:div {:style (sx {:padding "0.75rem 1rem" :width "11rem"})}
                 [:strong title]
                 [:div {:style (sx {:color "var(--fg-2)" :font-size "var(--font-sm)"})} text]]))

(def examples
  [(example :flow "A run: step cards on a rail, the camera on the step that runs"
     (fn []
       [:div
        (note "Drag or scroll to pan, pinch or Ctrl+scroll to zoom, + − 0 and the arrows when focused. "
              "Moving it lets go of the step; Follow takes it back.")
        (canvas/flow {:id "canvas-run" :label "Flow pr-1" :tag "ext.penpot/pr · pr-1"
                      :note "check 4 · next in 1:12" :height 460
                      :nodes pr-nodes :edges pr-edges})]))

   (example :page "In a scrolling page: the page keeps the wheel and vertical swipes"
     (fn []
       [:div {:style (sx {:max-width "22rem"})}
        (canvas/flow {:id "canvas-page" :label "Flow pr-1" :gestures :page :height 300
                      :controls {:zoom false}
                      :nodes pr-nodes :edges pr-edges})]))

   (example :board "Any content: a frame, a world, nodes"
     (fn []
       (canvas/canvas {:id "canvas-board" :label "Board" :height 320}
         (canvas/world {}
           (canvas/node {:id "plan" :x 24 :y 24} (sticky "Plan the launch" "Dates, owners, the one metric"))
           (canvas/node {:id "copy" :x 260 :y 110} (sticky "Write the copy" "Three lines for the page"))
           (canvas/node {:id "ship" :x 70 :y 220} (sticky "Ship it" "Behind the flag first"))
           (canvas/node {:id "far" :x 620 :y 380} (sticky "Found me" "Fit shows everything")))
         (canvas/tag {} "board")
         (canvas/controls {:follow false}))))

   (example :idle "Before it starts: nothing runs, nothing is followed"
     (fn []
       (canvas/flow {:id "canvas-idle" :label "Flow, not started" :height 260
                     :nodes (mapv (fn [n] (dissoc n :status)) (take 6 pr-nodes))
                     :edges (mapv (fn [e] (dissoc e :taken)) (take 6 pr-edges))})))])
