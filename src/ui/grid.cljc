(ns ui.grid
  "Responsive tile grid with square and masonry layouts, plus toolbar
   controls for switching layout and stepping tile size.

   The masonry layout needs the JS runtime (dist/ui-runtime.js) in
   browsers without native `grid-template-rows: masonry` support — the
   runtime self-initializes on any .tile-grid-masonry element."
  (:require [clojure.string :as str]
            [ui.util :as util]
            [ui.button :as button]))

(def default-size :m)

(def sizes
  "Tile size presets from smallest to largest."
  [:s :m :l :xl :xxl])

(defn grid-class-list
  "Returns a vector of CSS class strings for the grid container.
   Base is a uniform square grid; :layout :masonry adds the masonry
   class; non-default sizes add tile-grid-<size>."
  [{:keys [layout size]}]
  (let [l (or (some-> layout util/kw-name) "grid")
        s (or (some-> size util/kw-name) (util/kw-name default-size))]
    (cond-> ["tile-grid"]
      (= l "masonry") (conj "tile-grid-masonry")
      (not= s "m")    (conj (str "tile-grid-" s)))))

(defn grid-classes
  "Returns a space-joined class string for the grid container."
  [opts]
  (str/join " " (grid-class-list opts)))

(defn grid
  "Responsive auto-fill tile grid container.

   Props:
     :layout - :grid (default, uniform tiles) or :masonry
     :size   - :s, :m (default), :l, :xl, :xxl — tile min-width preset
     :min    - CSS length string overriding the tile min-width
               (e.g. \"180px\"); wins over :size
     :class  - additional CSS classes
     :attrs  - additional HTML attributes map"
  [{:keys [layout size min class attrs]} & children]
  #?(:squint
     (let [classes (cond-> (grid-classes {:layout layout :size size})
                     class (str " " class))
           base-attrs (cond-> (merge {:class classes} attrs)
                        min (assoc :style {"--tile-min" min}))]
       (into [:div base-attrs] children))

     :cljs
     (let [cls (grid-class-list {:layout layout :size size})
           classes (util/conj-classes cls class)
           base-attrs (cond-> (merge {:class classes} attrs)
                        min (assoc :style {:--tile-min min}))]
       (into [:div base-attrs] children))

     :clj
     (let [classes (cond-> (grid-classes {:layout layout :size size})
                     class (str " " class))
           base-attrs (cond-> (merge {:class classes} attrs)
                        min (assoc :style (str "--tile-min: " min ";")))]
       (into [:div base-attrs] children))))

;; ── Layout toggle ───────────────────────────────────────────────────

(def default-layout-options
  [{:value :grid :icon :grid :label "Square grid"}
   {:value :masonry :icon :layout-dashboard :label "Masonry"}])

(defn layout-toggle
  "Segmented icon toggle for switching grid layouts.

   Props:
     :value     - current layout value (keyword or string)
     :options   - vector of {:value :icon :label :href} maps
                  (default: grid / masonry)
     :on-change - called with the option's :value on click
                  (browser targets)
     :class     - additional CSS classes
     :attrs     - additional HTML attributes map

   For server-rendered pages give each option an :href — the buttons
   render as links (e.g. \"/?view=grid\")."
  [{:keys [value options on-change class attrs]}]
  (let [options (or options default-layout-options)
        current (some-> value util/kw-name)
        btns (map (fn [{:keys [value icon label href]}]
                    (let [active? (= current (some-> value util/kw-name))]
                      (button/button
                       {:variant :ghost
                        :icon icon
                        :href href
                        :on-click (when on-change
                                    (fn [_] (on-change value)))
                        :class (when active? "btn-toggled")
                        :attrs {:title label
                                :aria-label label
                                :aria-pressed (if active? "true" "false")}})))
                  options)]
    #?(:squint
       (into [:div (merge {:class (cond-> "toolbar-group"
                                    class (str " " class))}
                          attrs)]
             btns)

       :cljs
       (into [:div (merge {:class (util/conj-classes ["toolbar-group"] class)}
                          attrs)]
             btns)

       :clj
       (into [:div (merge {:class (cond-> "toolbar-group"
                                    class (str " " class))}
                          attrs)]
             btns))))

;; ── Size stepper ────────────────────────────────────────────────────

(defn step-size
  "Pure: step `value` one position along `steps` in direction `dir`
   (+1 / -1), clamped to the ends. Returns the element of `steps`."
  [steps value dir]
  (let [cur (some-> value util/kw-name)
        idx (or (some (fn [[i s]] (when (= (util/kw-name s) cur) i))
                      (map-indexed vector steps))
                0)
        nxt (max 0 (min (dec (count steps)) (+ idx dir)))]
    (nth steps nxt)))

(defn size-stepper
  "− / + stepper over tile sizes, clamped and disabled at the ends.

   Props:
     :value     - current size (keyword or string)
     :sizes     - vector of sizes from smallest to largest
                  (default ui.grid/sizes: [:s :m :l :xl :xxl])
     :on-change - called with the stepped size value (browser targets)
     :class     - additional CSS classes
     :attrs     - additional HTML attributes map"
  [{:keys [value on-change class attrs] :as props}]
  (let [steps (or (:sizes props) sizes)
        cur (some-> value util/kw-name)
        smallest? (= cur (util/kw-name (first steps)))
        largest? (= cur (util/kw-name (last steps)))
        btns [(button/button
               {:variant :ghost
                :icon :minus
                :disabled smallest?
                :on-click (when on-change
                            (fn [_] (on-change (step-size steps value -1))))
                :attrs {:title "Smaller tiles" :aria-label "Smaller tiles"}})
              (button/button
               {:variant :ghost
                :icon :plus
                :disabled largest?
                :on-click (when on-change
                            (fn [_] (on-change (step-size steps value 1))))
                :attrs {:title "Larger tiles" :aria-label "Larger tiles"}})]]
    #?(:squint
       (into [:div (merge {:class (cond-> "toolbar-group"
                                    class (str " " class))}
                          attrs)]
             btns)

       :cljs
       (into [:div (merge {:class (util/conj-classes ["toolbar-group"] class)}
                          attrs)]
             btns)

       :clj
       (into [:div (merge {:class (cond-> "toolbar-group"
                                    class (str " " class))}
                          attrs)]
             btns))))
