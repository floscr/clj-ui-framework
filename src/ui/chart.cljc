(ns ui.chart
  "Charts for a dashboard: a time series as a smooth line over a fading
   area with the period before as a dashed line and a crosshair that reads
   both, ranked rows with a thin rail under each label, a donut for the
   shares of a whole and a sparkline for the trend beside a number.

   Everything is markup and CSS: the crosshair is a column per point that
   shows its dot and value card on hover and on focus (a tap), so the charts
   work server-rendered, without the JS runtime and without app state. The
   geometry (scales, paths, arcs) is pure and runs on every target."
  (:require [clojure.string :as str]
            [ui.empty-state :as empty-state]
            [ui.util :as util]))

;; ── Numbers ──────────────────────────────────────────────────────────

(defn round1
  "`x` rounded to one decimal, as a double."
  [x]
  (/ (Math/round (* 10.0 x)) 10.0))

(defn pct
  "`n` as a share of `total` in percent with one decimal: \"12.5%\"."
  [n total]
  (str (round1 (if (pos? total) (* 100.0 (/ n total)) 0)) "%"))

(def ^:private nice-steps [1 1.2 1.6 2 2.4 3 4 5 6 8 10])

(defn nice-max
  "The top of the value axis for a largest value `m`: `m` rounded up to a
   round number whose half is round too (it labels the middle gridline),
   at least 4."
  [m]
  (let [m   (max 4 (or m 0))
        mag (loop [p 1] (if (> (* p 10) m) p (recur (* p 10))))]
    (loop [steps nice-steps]
      (let [top (* mag (first steps))]
        (if (or (>= top m) (empty? (rest steps)))
          (Math/round (* 1.0 top))
          (recur (rest steps)))))))

(defn format-number
  "A number for an axis or a value card: whole numbers without a decimal,
   others with one."
  [n]
  (let [r (round1 n)]
    (if (zero? (- r (Math/round (* 1.0 r))))
      (str (Math/round (* 1.0 r)))
      (str r))))

;; ── Geometry ─────────────────────────────────────────────────────────

(defn plot-points
  "`values` as [x y] pairs in a `w` by `h` box, the largest value `top` at
   y 0. Each value sits in the middle of its column (as the crosshair's
   columns do); with :edges the first and last sit on the box's edges (a
   sparkline). `pad` keeps the line off the top and bottom; `low` is the
   value at the bottom (default 0)."
  [values {:keys [w h top low pad edges]}]
  (let [n    (count values)
        pad  (or pad 0)
        low  (or low 0)
        span (max 1 (- (or top 1) low))
        ih   (- h (* 2 pad))]
    (vec (map-indexed
          (fn [i v]
            [(if edges
               (if (> n 1) (* w (/ (* 1.0 i) (dec n))) (/ w 2.0))
               (* w (/ (+ i 0.5) n)))
             (- h pad (* ih (/ (* 1.0 (- (or v 0) low)) span)))])
          values))))

(defn- xy [[x y]] (str (round1 x) "," (round1 y)))

(defn line-path
  "The SVG path through `pts` ([x y] pairs). :curve :smooth (default) bends
   it through the points without leaving the box of height `h`; :linear
   draws straight segments."
  [pts {:keys [curve h]}]
  (let [p (vec pts)
        n (count p)]
    (cond
      (zero? n) ""
      (= "linear" (some-> curve util/kw-name))
      (str "M" (str/join " L" (map xy p)))
      :else
      (let [clamp (fn [y] (if h (max 0 (min h y)) y))
            seg   (fn [i]
                    (let [[x0 y0] (nth p (max (dec i) 0))
                          [x1 y1] (nth p i)
                          [x2 y2] (nth p (inc i))
                          [x3 y3] (nth p (min (+ i 2) (dec n)))]
                      (str " C" (xy [(+ x1 (/ (- x2 x0) 6.0)) (clamp (+ y1 (/ (- y2 y0) 6.0)))])
                           " "  (xy [(- x2 (/ (- x3 x1) 6.0)) (clamp (- y2 (/ (- y3 y1) 6.0)))])
                           " "  (xy [x2 y2]))))]
        (str "M" (xy (first p)) (str/join "" (map seg (range (dec n)))))))))

(defn area-path
  "`line` (a path from line-path through `pts`) closed down to the bottom
   of a box of height `h`."
  [line pts h]
  (if (empty? pts)
    ""
    (str line " L" (xy [(first (last pts)) h]) " L" (xy [(first (first pts)) h]) " Z")))

(def ^:private donut-r 48)

(defn donut-arcs
  "Segments ({:value} maps) as arcs of a circle of radius 48 that starts
   at twelve o'clock: each gets :dash (stroke-dasharray) and :offset
   (stroke-dashoffset), with a small gap to its neighbor. Segments without
   a value are dropped."
  [segments]
  (let [segs  (filterv (fn [s] (pos? (or (:value s) 0))) segments)
        total (reduce + 0 (map :value segs))
        c     (* 2 Math/PI donut-r)
        gap   (if (> (count segs) 1) 2 0)]
    (loop [todo segs start 0.0 out []]
      (if (empty? todo)
        out
        (let [s   (first todo)
              len (* c (/ (* 1.0 (:value s)) total))
              on  (max 0.8 (- len gap))]
          (recur (rest todo)
                 (+ start len)
                 (conj out (assoc s
                                  :dash   (str (round1 on) " " (round1 (- c on)))
                                  :offset (str (round1 (- start)))))))))))

;; ── Cross-target pieces ──────────────────────────────────────────────

(defn- vars
  "Inline style from [name value] pairs of CSS custom properties, in the
   target's :style shape. Charts position everything through custom
   properties, so this is the only inline style they write."
  [pairs]
  #?(:squint (reduce (fn [m [k v]] (assoc m k v)) {} pairs)
     :cljs   (reduce (fn [m [k v]] (assoc m (keyword k) v)) {} pairs)
     :clj    (str/join " " (map (fn [[k v]] (str k ": " v ";")) pairs))))

(def colors
  "Series colors a chart can take. Each has a .chart-<name> class."
  #{"accent" "blue" "teal" "orange" "pink" "success"})

(defn chart-class-list
  "Classes for a chart element: its `base` class plus the series color
   (:color, default the first series token)."
  [base {:keys [color]}]
  (let [c (some-> color util/kw-name)]
    (cond-> [base]
      (contains? colors c) (conj (str "chart-" c)))))

(defn chart-classes
  "Space-joined form of chart-class-list."
  [base opts]
  (str/join " " (chart-class-list base opts)))

(defn- series-class
  "The class of the `i`th series of a donut or a legend (1 to 6, then 6)."
  [i]
  (str "chart-series-" (min 6 (inc i))))

;; ── Frame and legend ─────────────────────────────────────────────────

(defn legend
  "A row of series names, each after a dot in its color.

   Props:
     :items - vector of {:label :color :compare}: :color one of `colors`
              (default the chart's color), :compare true for the muted dot
              of a comparison series
     :class - additional CSS classes
     :attrs - additional HTML attributes map"
  [{:keys [items class attrs] :as _props}]
  (into [:div (util/class-attrs ["chart-legend"] class attrs)]
        (map (fn [{:keys [label color compare]}]
               [:span {:class "chart-legend-item"}
                [:span (util/class-attrs
                        (cond-> (chart-class-list "chart-legend-dot" {:color color})
                          compare (conj "chart-legend-dot-compare"))
                        nil {:aria-hidden "true"})]
                label])
             items)))

(defn chart
  "The frame of a chart: a title with an optional description on the left,
   actions (a range picker) on the right, then the chart. Has no surface of
   its own; put it in a card.

   Props:
     :title       - what is charted (string or hiccup), an h3
     :description - optional line under the title: the total, the range
     :actions     - optional hiccup at the right of the title row
     :class       - additional CSS classes
     :attrs       - additional HTML attributes map

   Children are the chart and what belongs to it (a legend, a note)."
  [{:keys [title description actions class attrs] :as _props} & children]
  (let [text (cond-> [:div {:class "chart-head-text"}
                      [:h3 {:class "chart-title"} title]]
               description (conj [:p {:class "chart-description"} description]))
        head (cond-> [:div {:class "chart-head"} text]
               actions (conj [:div {:class "chart-actions"} actions]))]
    (into [:figure (util/class-attrs ["chart"] class attrs)
           (when title head)]
          children)))

;; ── Area chart ───────────────────────────────────────────────────────

(defn- axis-labels
  "[index label minor?] triples for the x axis: the points that carry
   :axis-label, or about five evenly spaced ones ending on the last point.
   Every second label, counted from the last, is minor: a narrow chart
   drops those."
  [pts]
  (let [n      (count pts)
        given  (keep-indexed (fn [i p] (when (:axis-label p) [i (:axis-label p)])) pts)
        step   (max 1 (Math/round (/ n 4.5)))
        labels (if (seq given)
                 (vec given)
                 (vec (keep-indexed (fn [i p]
                                      (when (zero? (mod (- (dec n) i) step)) [i (:label p)]))
                                    pts)))
        k      (count labels)]
    (vec (map-indexed (fn [j [i text]] [i text (odd? (- (dec k) j))]) labels))))

(defn- hit-column
  "One point's column over the plot: the crosshair line, a dot per series
   and the value card beside the line, all hidden until the column is
   hovered or focused."
  [{:keys [i n point top series-label compare-label fmt compare?]}]
  (let [{:keys [label value compare]} point
        v      (or value 0)
        c      (or compare 0)
        ;; the card sits beside the line, on the side with more room
        edge   (when (>= (* 2 i) n) "chart-hit-end")]
    [:div (util/class-attrs (cond-> ["chart-hit"] edge (conj edge)) nil
                            {:tabindex "-1"
                             :style (vars (cond-> [["--chart-v" (pct v top)]]
                                            compare? (conj ["--chart-c" (pct c top)])))})
     (when compare? [:span {:class "chart-dot chart-dot-compare"}])
     [:span {:class "chart-dot"}]
     [:div {:class "chart-tip"}
      [:span {:class "chart-tip-title"} label]
      [:span {:class "chart-tip-row"}
       [:span {:class "chart-legend-dot" :aria-hidden "true"}]
       series-label
       [:span {:class "chart-tip-value"} (fmt v)]]
      (when compare?
        [:span {:class "chart-tip-row"}
         [:span {:class "chart-legend-dot chart-legend-dot-compare" :aria-hidden "true"}]
         compare-label
         [:span {:class "chart-tip-value"} (fmt c)]])]]))

(defn- data-table
  "The points as a visually hidden table, for screen readers."
  [pts {:keys [series-label compare-label fmt compare?]}]
  [:table {:class "sr-only"}
   [:thead
    (cond-> [:tr [:th ""] [:th series-label]]
      compare? (conj [:th compare-label]))]
   (into [:tbody]
         (map (fn [{:keys [label value compare]}]
                (cond-> [:tr [:th label] [:td (fmt (or value 0))]]
                  compare? (conj [:td (fmt (or compare 0))])))
              pts))])

(defn area-chart
  "A time series: a line over a fading area, optionally with a dashed
   comparison line. Pointing at (or tapping) a point shows a crosshair with
   a dot on each line and a card with the values.

   Props:
     :points        - vector of point maps, oldest first:
                        :label      - the point's name in the value card
                                      (\"Thu 5 Nov\")
                        :value      - number
                        :compare    - optional number of the comparison
                                      series (the period before)
                        :axis-label - optional; label this point on the x
                                      axis. Without any, about five evenly
                                      spaced points are labeled with :label
     :label         - name of the series (default \"Value\")
     :compare-label - name of the comparison series (default \"Before\")
     :color         - :accent | :blue | :teal | :orange | :pink | :success
     :max           - top of the value axis (default: nice-max of the data)
     :curve         - :smooth (default) | :linear
     :format        - fn from a number to its text on the axis and in the
                      card (default format-number)
     :legend        - false hides the legend, which shows by default when
                      there is a comparison series
     :loading       - boolean; a placeholder of the same size
     :empty-label   - shown instead of the plot without points (default
                      \"No data for this range\")
     :class         - additional CSS classes
     :attrs         - additional HTML attributes map"
  [{:keys [points label compare-label color curve loading empty-label class attrs]
    top-value :max format-fn :format show-legend :legend :as _props}]
  (let [pts       (vec points)
        n         (count pts)
        fmt       (or format-fn format-number)
        s-label   (or label "Value")
        c-label   (or compare-label "Before")
        compare?  (boolean (some (fn [p] (some? (:compare p))) pts))
        biggest   (reduce (fn [m p] (max m (or (:value p) 0) (or (:compare p) 0))) 0 pts)
        top       (or top-value (nice-max biggest))
        w         600
        h         200
        geo       {:w w :h h :top top}
        line-pts  (plot-points (map :value pts) geo)
        line      (line-path line-pts {:curve curve :h h})
        root      (util/class-attrs (chart-class-list "chart-area" {:color color}) class attrs)]
    (cond
      loading
      [:div root [:div {:class "chart-canvas skeleton chart-skeleton" :role "status" :aria-label "Loading"}]]

      (zero? n)
      [:div root
       [:div {:class "chart-canvas chart-canvas-empty"}
        (empty-state/empty-state {} (or empty-label "No data for this range"))]]

      :else
      [:div root
       [:div {:class "chart-plot"}
        [:div {:class "chart-y" :aria-hidden "true"}
         [:span {:class "chart-tick" :style (vars [["--chart-at" "100%"]])} (fmt top)]
         [:span {:class "chart-tick" :style (vars [["--chart-at" "50%"]])} (fmt (/ top 2.0))]
         [:span {:class "chart-tick" :style (vars [["--chart-at" "0%"]])} (fmt 0)]]
        [:div {:class "chart-canvas"}
         [:div {:class "chart-gridline" :style (vars [["--chart-at" "100%"]])}]
         [:div {:class "chart-gridline" :style (vars [["--chart-at" "50%"]])}]
         [:div {:class "chart-gridline chart-baseline" :style (vars [["--chart-at" "0%"]])}]
         [:svg {:class "chart-svg chart-fill" :viewBox (str "0 0 " w " " h)
                :preserveAspectRatio "none" :aria-hidden "true"}
          [:path {:d (area-path line line-pts h)}]]
         [:svg {:class "chart-svg chart-lines" :viewBox (str "0 0 " w " " h)
                :preserveAspectRatio "none" :aria-hidden "true"}
          (when compare?
            [:path {:class "chart-line chart-line-compare"
                    :d (line-path (plot-points (map :compare pts) geo) {:curve curve :h h})}])
          [:path {:class "chart-line" :d line}]]
         (into [:div {:class "chart-hits" :style (vars [["--chart-n" (str n)]])}]
               (map-indexed (fn [i p]
                              (hit-column {:i i :n n :point p :top top :fmt fmt :compare? compare?
                                           :series-label s-label :compare-label c-label}))
                            pts))]
        (into [:div {:class "chart-x" :aria-hidden "true" :style (vars [["--chart-n" (str n)]])}]
              (map (fn [[i text minor?]]
                     [:span {:class (if minor? "chart-x-label chart-x-label-minor" "chart-x-label")
                             :style (vars [["--chart-col" (str (inc i))]])}
                      text])
                   (axis-labels pts)))]
       (when (and compare? (not (false? show-legend)))
         (legend {:items [{:label s-label} {:label c-label :compare true}]}))
       (data-table pts {:series-label s-label :compare-label c-label :fmt fmt :compare? compare?})])))

;; ── Bar list ─────────────────────────────────────────────────────────

(defn bar-list
  "Ranked rows: a label with its value at the end and a thin rail under
   them filled to the row's share of the largest value.

   Props:
     :rows  - vector of row maps, largest first:
                :label       - the row's name (string or hiccup)
                :description - optional muted text after the label (a path)
                :value       - number; sets the rail
                :display     - the value as shown (default: the number)
                :note        - optional muted text after the value (\"12%\")
                :href        - URL; the row is a link
                :active      - boolean; the selected row
     :max   - value of a full rail (default: the largest :value)
     :color - :accent | :blue | :teal | :orange | :pink | :success
     :class - additional CSS classes
     :attrs - additional HTML attributes map"
  [{:keys [rows color class attrs] top-value :max :as _props}]
  (let [top (or top-value (reduce (fn [m r] (max m (or (:value r) 0))) 0 rows))]
    (into [:div (util/class-attrs (chart-class-list "chart-bars" {:color color}) class
                                  (merge {:role "list"} attrs))]
          (map (fn [{:keys [label description value display note href active]}]
                 (let [v (or value 0)]
                   [(if href :a :div)
                    (util/class-attrs (cond-> ["chart-bar"]
                                        href   (conj "chart-bar-link")
                                        active (conj "chart-bar-active"))
                                      nil
                                      (cond-> {:role "listitem"}
                                        href   (assoc :href href)
                                        active (assoc :aria-current "true")))
                    [:span {:class "chart-bar-top"}
                     (cond-> [:span {:class "chart-bar-label"} label]
                       description (conj [:span {:class "chart-bar-description"} description]))
                     (cond-> [:span {:class "chart-bar-value"} (or display (format-number v))]
                       note (conj [:span {:class "chart-bar-note"} note]))]
                    [:span {:class "chart-bar-rail"}
                     [:span {:class "chart-bar-fill" :style (vars [["--chart-w" (pct v top)]])}]]]))
               rows))))

;; ── Donut ────────────────────────────────────────────────────────────

(defn donut
  "The shares of a whole as a ring with a number in its middle and a legend
   beside it: each segment's name, value and share.

   Props:
     :segments     - vector of {:label :value :display :active}, largest
                     first; colored in order (four series colors, then two
                     neutrals). :display is the value as shown, :active
                     true thickens the segment and bolds its legend row
     :center-value - the number in the middle (default: the total)
     :center-label - optional line under it (\"visitors\")
     :format       - fn from a number to its text (default format-number)
     :legend       - false leaves the legend out
     :class        - additional CSS classes
     :attrs        - additional HTML attributes map"
  [{:keys [segments center-value center-label class attrs]
    format-fn :format show-legend :legend :as _props}]
  (let [fmt   (or format-fn format-number)
        segs  (vec (map-indexed (fn [i s] (assoc s :series (series-class i))) segments))
        total (reduce + 0 (map (fn [s] (or (:value s) 0)) segs))]
    [:div (util/class-attrs ["chart-donut"] class attrs)
     [:div {:class "chart-donut-ring"}
      (into [:svg {:viewBox "0 0 120 120" :role "img"
                   :aria-label (str/join ", " (map (fn [s] (str (:label s) " " (pct (or (:value s) 0) total))) segs))}]
            (map (fn [{:keys [dash offset series active]}]
                   [:circle {:class (cond-> (str "chart-donut-segment " series)
                                      active (str " chart-donut-active"))
                             :cx "60" :cy "60" :r (str donut-r)
                             :stroke-dasharray dash :stroke-dashoffset offset}])
                 (donut-arcs segs)))
      [:div {:class "chart-donut-center"}
       [:span {:class "chart-donut-value"} (or center-value (fmt total))]
       (when center-label [:span {:class "chart-donut-label"} center-label])]]
     (when-not (false? show-legend)
       (into [:div {:class "chart-donut-legend"}]
             (map (fn [{:keys [label value display series active]}]
                    [:div {:class (if active "chart-donut-row chart-donut-row-active" "chart-donut-row")}
                     [:span {:class (str "chart-legend-dot " series) :aria-hidden "true"}]
                     [:span {:class "chart-donut-name"} label]
                     [:span {:class "chart-donut-number"} (or display (fmt (or value 0)))]
                     [:span {:class "chart-donut-share"} (str (Math/round (* 1.0 (if (pos? total) (* 100.0 (/ (or value 0) total)) 0))) "%")]])
                  segs)))]))

;; ── Sparkline ────────────────────────────────────────────────────────

(defn sparkline
  "A small trend without axes, for the space beside a number.

   Props:
     :values - numbers, oldest first
     :label  - accessible name (\"Visitors, last 13 weeks\"); without one
               the sparkline is hidden from screen readers
     :color  - :accent | :blue | :teal | :orange | :pink | :success
     :area   - false draws the line alone, without the tint under it
     :zero   - false scales from the smallest value instead of 0, to show
               the shape of values that move far from zero (a rate
               between 40 and 50); use it with :area false
     :curve  - :smooth (default) | :linear
     :class  - additional CSS classes
     :attrs  - additional HTML attributes map"
  [{:keys [values label color area zero curve class attrs] :as _props}]
  (let [w    100
        h    32
        nums (map (fn [v] (or v 0)) values)
        top  (reduce max 1 nums)
        low  (if (and (false? zero) (seq nums)) (reduce min nums) 0)
        pts  (plot-points values {:w w :h h :top top :low low :pad 3 :edges true})
        line (line-path pts {:curve curve :h h})]
    [:svg (util/class-attrs (chart-class-list "chart-spark" {:color color}) class
                            (merge {:viewBox (str "0 0 " w " " h) :preserveAspectRatio "none"}
                                   (if label {:role "img" :aria-label label} {:aria-hidden "true"})
                                   attrs))
     (when-not (false? area)
       [:path {:class "chart-spark-fill" :d (area-path line pts h)}])
     [:path {:class "chart-spark-line" :d line}]]))
