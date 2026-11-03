(ns ui.tooltip
  "Hover and focus hints shown by the ui.js.tooltip runtime: the trigger
   carries its content in a template, the runtime shows it in one shared
   layer, so the first tooltip waits for its delay and the next ones open
   instantly. tooltip-attrs is the wrapper-free form for plain text."
  (:require [clojure.string :as str]
            [ui.util :as util]))

(defn tooltip-data-attrs
  "The data attributes ui.js.tooltip reads off the trigger wrapper."
  [{:keys [content delay side tone theme]}]
  (cond-> {}
    content       (assoc :data-tooltip-kind "card")
    (some? delay) (assoc :data-tooltip-delay (str delay))
    side          (assoc :data-tooltip-side (util/kw-name side))
    tone          (assoc :data-tooltip-tone (util/kw-name tone))
    theme         (assoc :data-tooltip-theme (util/kw-name theme))))

(defn tooltip-attrs
  "Attributes that give any element a plain tooltip, without a wrapper: merge
   them into a button's or icon's attrs. Takes :text, :shortcut and the
   :delay :side :tone :theme of tooltip; nil :text gives no tooltip."
  [{:keys [text shortcut delay side tone theme]}]
  (when (seq text)
    (cond-> (assoc (tooltip-data-attrs {:delay delay :side side :tone tone :theme theme})
                   :data-tooltip text)
      (seq shortcut) (assoc :data-tooltip-shortcut (str/join " " shortcut)))))

(defn content-nodes
  "Content as a sequence of child nodes: a list or lazy seq is spliced in
   (squint does not flatten seqs in hiccup), anything else (a hiccup vector,
   a string, a number) is one node, nil none. On squint strings and vectors
   are seq? too, hence the extra checks."
  [content]
  (cond
    (nil? content) []
    (and (seq? content) (not (vector? content)) (not (string? content))) content
    :else [content]))

(defn tooltip-label
  "The plain body: the hint text, then its shortcut as keycaps."
  [text shortcut]
  (cond-> [:span (util/class-attrs ["tooltip-label"] nil nil) [:span text]]
    (seq shortcut)
    (conj (into [:span (util/class-attrs ["tooltip-keys"] nil nil)]
                (map (fn [k] [:kbd (util/class-attrs ["tooltip-key"] nil nil) k]) shortcut)))))

(defn tooltip
  "Wrap a trigger in a tooltip.

   Props:
     :text     - the hint, a few words
     :shortcut - keycaps after the text, e.g. [\"⌘\" \"K\"]
     :content  - rich content instead of :text: a hiccup node, a string or a
                 list/seq of nodes (see tooltip-card). On Replicant and
                 squint it may change while open; the tooltip follows
     :delay    - ms before the first tooltip opens (default 500); also
                 settable for a whole group as data-tooltip-delay on an ancestor
     :side     - :top (default), :bottom, :left or :right; another side is
                 taken when it has no room
     :tone     - :danger for error hints
     :theme    - :light or :dark bubble; default the inverse of the page.
                 Also settable for a group as data-tooltip-theme on an ancestor
     :class    - additional CSS classes
     :attrs    - additional HTML attributes"
  [{:keys [text shortcut content delay side tone theme class attrs]} & children]
  (let [body  (or content (tooltip-label text shortcut))
        attrs (merge (tooltip-data-attrs {:content content :delay delay :side side
                                          :tone tone :theme theme})
                     attrs)]
    (-> [:span (util/class-attrs ["tooltip"] class attrs)]
        (into children)
        (conj (into [:template (util/class-attrs ["tooltip-content"] nil nil)]
                    (content-nodes body))))))

(defn tooltip-card
  "A column layout for rich tooltip content, 18rem wide."
  [{:keys [class attrs]} & children]
  (into [:div (util/class-attrs ["tooltip-card"] class attrs)]
        (mapcat content-nodes children)))
