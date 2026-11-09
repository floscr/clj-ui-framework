(ns ui.rating
  "A star rating: half stars picked by which half of a star you point at,
   committed on a click or tap, cleared by choosing the current value
   again. Built as a radio group, so it works as a form field and with the
   arrow keys; the stars' fill follows the checked or hovered radio in CSS
   (`:has()`), so it renders in every target without JS. The `rating`
   ui-runtime.js module adds what CSS can't: clearing on a second tap,
   Backspace / Delete, and a pop when a value lands.

   Read-only, it is an image of the value, for cards and lists."
  (:require [clojure.string :as str]
            [ui.util :as util]))

(def default-max 5)

(def ^:private star-path
  "The `:star` icon's outline, filled here instead of stroked."
  "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z")

;; ── Values ───────────────────────────────────────────────────────────

(defn value-str
  "The value as the radios and `data-value` carry it: \"4\", \"3.5\", \"0\"."
  [v]
  (let [n (or v 0)]
    #?(:clj (if (== n (Math/floor (double n))) (str (long n)) (str (double n)))
       :default (str n))))

(defn steps
  "The values a rating can take, lowest first: 0.5, 1, … `max` with
   `half`, else 1, 2, … `max`."
  [{:keys [max half]}]
  (let [m (or max default-max)]
    (if (false? half)
      (mapv inc (range m))
      (mapv #(/ (inc %) 2) (range (* 2 m))))))

(defn format-value
  "A value for reading: 3.5 → \"3½\", 4 → \"4\", 0.5 → \"½\"."
  [v]
  (let [s (value-str v)]
    (cond
      (= s "0.5") "½"
      (str/ends-with? s ".5") (str (subs s 0 (- (count s) 2)) "½")
      :else s)))

(defn value-label
  "What a value says to a screen reader: \"Not rated\", \"½ star\",
   \"1 star\", \"3½ stars\"."
  [v]
  (let [s (value-str v)]
    (cond
      (= s "0") "Not rated"
      (= s "1") "1 star"
      (= s "0.5") "½ star"
      :else (str (format-value v) " stars"))))

;; ── Class generation ─────────────────────────────────────────────────

(defn rating-class-list
  "Classes for the rating wrapper: its size (:sm for cards and lists,
   :md, :lg), read-only and disabled."
  [{:keys [size read-only disabled]}]
  (let [s (or (some-> size util/kw-name) "md")]
    (cond-> ["rating" (str "rating--" s)]
      read-only (conj "rating--read-only")
      disabled  (conj "rating--disabled"))))

(defn rating-classes
  "Space-joined form of rating-class-list."
  [opts]
  (str/join " " (rating-class-list opts)))

;; ── Markup ───────────────────────────────────────────────────────────

(defn- star []
  [:span {:class "rating-star"}
   [:svg {:class "rating-star-empty" :viewBox "0 0 24 24" :aria-hidden "true"} [:path {:d star-path}]]
   [:svg {:class "rating-star-fill" :viewBox "0 0 24 24" :aria-hidden "true"} [:path {:d star-path}]]])

(defn- stars [max]
  (into [:span {:class "rating-stars" :aria-hidden "true"}]
        (repeat max (star))))

(defn- radio
  "One radio of the group. `checked` must render as the boolean attribute
   in every target."
  [{:keys [group v checked disabled cls]}]
  [:input (cond-> {:type "radio" :class cls :name group :value (value-str v)
                   :aria-label (value-label v)}
            checked  (assoc :checked true)
            disabled (assoc :disabled true))])

(defonce ^:private counter (atom 0))

(defn rating
  "A star rating.

   Props:
     :value     - current value: 0 or nil for none, else a step (3.5)
     :max       - number of stars (default 5, up to 10)
     :half      - half stars (default true); false allows whole stars only
     :name      - radio group name, submitted with a form (default a
                  generated one; set it when the value is read by name)
     :label     - accessible name of the group (default \"Rating\")
     :size      - :sm (cards, lists) | :md (default) | :lg
     :read-only - boolean; an image of the value, no inputs
     :disabled  - boolean; shown, not changeable
     :on-change - :cljs and :squint; gets the change event, its target the
                  checked radio (value \"0\" when cleared)
     :class     - additional CSS classes
     :attrs     - additional HTML attributes on the wrapper"
  [{:keys [value max half name label size read-only disabled on-change class attrs] :as _props}]
  (let [m (or max default-max)
        v (or value 0)
        cls (rating-class-list {:size size :read-only read-only :disabled disabled})
        base {:data-value (value-str v)}]
    (if read-only
      [:span (util/class-attrs cls class
                               (merge base {:role "img"
                                            :aria-label (str (or label "Rating") ": "
                                                             (if (zero? v) "not rated"
                                                                 (str (format-value v) " out of " m)))}
                                      attrs))
       (stars m)]
      (let [group (or name (str "rating-" (swap! counter inc)))
            hit (fn [step]
                  [:label {:class "rating-hit" :data-value (value-str step) :title (value-label step)}
                   (radio {:group group :v step :checked (= (value-str v) (value-str step))
                           :disabled disabled :cls "rating-input"})])
            wrapper (merge base
                           {:role "radiogroup" :aria-label (or label "Rating")
                            :data-ui-rating "true"}
                           #?(:squint (when on-change {:on-change on-change})
                              :cljs   (when on-change {:on {:change on-change}})
                              :clj    nil)
                           attrs)]
        [:span (util/class-attrs cls class wrapper)
         (stars m)
         (into [:span {:class "rating-hits"}
                (radio {:group group :v 0 :checked (zero? v) :disabled disabled
                        :cls "rating-input rating-input-none"})]
               (map hit (steps {:max m :half half})))]))))
