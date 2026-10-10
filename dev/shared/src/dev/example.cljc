(ns dev.example
  "Shared helpers for the component examples under dev.examples.*.

   An example is a plain map {:id :title :render} built with `example`;
   `render` is a thunk returning hiccup so examples stay cheap to define and
   the hiccup site can also show the source (it reads the example files
   with rewrite-clj, so no macros are needed and squint bundles stay free of
   source text).

   Cross-target normalization:
     - sx  : inline style map -> string for clj, map as-is for cljs/squint
     - cls : class names -> vector for replicant, string for clj/squint
     - log-click : a click handler that logs on browser targets, nil on clj"
  (:require [clojure.string :as str]
            [ui.util :as util]))

(defn sx
  "Normalize an inline style map to the target's representation.
   clj -> \"k: v; ...\" string. cljs/squint -> map unchanged (squint compiles
   keyword keys to string keys, which is what eucalypt expects)."
  [m]
  #?(:squint m
     :cljs   m
     :clj    (str/join " " (map (fn [[k v]] (str (util/kw-name k) ": " v ";")) m))))

(defn cls
  "Join CSS class names. Replicant requires a vector; clj/squint take a string."
  [& parts]
  #?(:squint (str/join " " parts)
     :cljs   (vec parts)
     :clj    (str/join " " parts)))

(defn log-click
  "Click handler that logs `label` on browser targets; nil on clj."
  [label]
  #?(:squint (fn [_] (js/console.log (str "Clicked: " label)))
     :cljs   (fn [_] (js/console.log (str "Clicked: " label)))
     :clj    nil))

(def hstack
  "Style map for a wrapping horizontal row."
  {:display "flex" :gap "0.75rem" :flex-wrap "wrap" :align-items "center"})

(def vstack
  "Style map for a vertical column."
  {:display "flex" :flex-direction "column" :gap "0.75rem"})

(defn row
  "A wrapping horizontal row of children."
  [& children]
  (into [:div {:style (sx hstack)}] children))

(defn column
  "A vertical column of children."
  [& children]
  (into [:div {:style (sx vstack)}] children))

(defn note
  "Muted helper text above an example."
  [& children]
  (into [:p {:style (sx {:margin "0" :color "var(--fg-2)" :font-size "var(--font-sm)"})}]
        children))

(defn example
  "Build an example map. `render` is a zero-arg fn returning hiccup."
  [id title render]
  {:id id :title title :render render})

;; ── Layout helpers for the long demo pages ───────────────────────────

(defn section [title & children]
  (let [id (-> title str/lower-case (str/replace #"\s+" "-"))]
    [:section {:id id :style (sx {:margin-bottom "2.5rem"})}
     [:h3 {:style (sx {:color "var(--fg-1)" :margin-bottom "1rem"
                       :border-bottom "var(--border-0)" :padding-bottom "0.5rem"})} title]
     (into [:div {:style (sx {:display "flex" :flex-direction "column" :gap "1rem"})}]
           children)]))

(defn page-header [title subtitle]
  [:div {:style (sx {:margin-bottom "2rem"})}
   [:h2 {:style (sx {:margin "0 0 0.25rem" :color "var(--fg-0)"})} title]
   (when subtitle
     [:p {:style (sx {:margin "0" :color "var(--fg-2)" :font-size "var(--font-sm)"})} subtitle])])

(defn render-examples
  "Render a vector of examples as a column. With more than one example each
   gets a small muted title."
  [examples]
  (let [labeled? (> (count examples) 1)]
    (into [:div {:style (sx {:display "flex" :flex-direction "column" :gap "1.25rem"})}]
          (map (fn [{:keys [id title render]}]
                 [:div {:id (str "example-" (util/kw-name id))
                        :style (sx {:display "flex" :flex-direction "column" :gap "0.5rem"})}
                  (when labeled?
                    [:h5 {:style (sx {:margin "0" :color "var(--fg-1)" :font-size "var(--font-sm)"
                                      :font-weight "500"})} title])
                  (render)])
               examples))))
