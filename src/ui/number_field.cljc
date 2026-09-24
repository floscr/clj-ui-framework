(ns ui.number-field
  (:require [clojure.string :as str]
            [ui.util :as util]
            [ui.icon :as icon]))

(def default-size "md")
(def default-variant "default")

(defn normalize-variant
  "Canonical variant name. Accepts keywords or strings and aliases the legacy
   :stacked to :spinner. Variants: \"default\" (steppers flank the input),
   \"right\" (both steppers after the input), \"spinner\" (detached up/down box)."
  [variant]
  (let [v (or (some-> variant util/kw-name) default-variant)]
    (if (= v "stacked") "spinner" v)))

;; ── Pure helpers (shared, unit-tested) ───────────────────────────────

(defn- round6
  "Round off floating-point drift to 6 decimals (0.1 + 0.2 → 0.3)."
  [n]
  (/ #?(:squint (js/Math.round (* n 1e6))
        :cljs   (js/Math.round (* n 1e6))
        :clj    (Math/round (double (* n 1e6))))
     1e6))

(defn clamp-value
  "Clamp `v` into [min max]. `min`/`max` may be nil (that bound is open)."
  [v min max]
  (let [v (if (and (some? min) (< v min)) min v)
        v (if (and (some? max) (> v max)) max v)]
    v))

(defn step-value
  "Increment (`dir` 1) or decrement (`dir` -1) `v` by `step`, clamped to
   [min max]. Rounds off floating-point drift so 0.1 steps stay clean.
   `step` defaults to 1."
  [v {:keys [step min max]} dir]
  (let [step (if (and step (pos? step)) step 1)
        n    (round6 (+ (or v 0) (* dir step)))]
    (clamp-value n min max)))

;; ── Class generation ─────────────────────────────────────────────────

(defn number-field-class-list
  "Vector of CSS class strings for the number-field wrapper.
   Variants: \"default\" (steppers flank the input), \"right\" (both steppers
   after the input), \"spinner\" (detached up/down spinner box)."
  [{:keys [size variant disabled]}]
  (let [s (or (some-> size util/kw-name) default-size)
        v (normalize-variant variant)]
    (cond-> ["number-field" (str "number-field--" v)]
      (not= s "md") (conj (str "number-field--" s))
      disabled      (conj "number-field--disabled"))))

(defn number-field-classes
  "Space-joined class string for the number-field wrapper."
  [opts]
  (str/join " " (number-field-class-list opts)))

;; ── Shared, target-agnostic building blocks ──────────────────────────
;; Steppers carry no event handlers — the ui-runtime.js `number-field`
;; module wires clicks (and scroll-wheel) via [data-ui-number-step] /
;; [data-ui-number-field] and dispatches a native `input` event on the
;; <input>, so squint/replicant just re-render through their :on-change.

(defn- input-base-attrs
  [{:keys [value min max step placeholder name id disabled]}]
  (cond-> {:type "number" :inputmode "decimal" :step (or step 1)}
    (some? value)       (assoc :value value)
    (some? min)         (assoc :min min)
    (some? max)         (assoc :max max)
    (some? placeholder) (assoc :placeholder placeholder)
    name                (assoc :name name)
    id                  (assoc :id id)
    disabled            (assoc :disabled true)))

(defn- stepper
  "A stepper button. `cls` is the already target-shaped class value."
  [{:keys [dir icon-name cls label disabled]}]
  [:button (cond-> {:type "button" :class cls :aria-label label
                    :tabindex "-1" :data-ui-number-step (str dir)}
             disabled (assoc :disabled true))
   (icon/icon {:icon-name icon-name :size :sm})])

(defn- children-for
  "Ordered children of the wrapper for a given variant."
  [variant {:keys [dec inc spin input]}]
  (case variant
    "right"   [input dec inc]
    "spinner" [input spin]
    [dec input inc]))

(defn number-field
  "Numeric input with increment/decrement steppers and scroll-wheel support.

   Interactivity is driven by the `number-field` ui-runtime.js module: scroll
   the field or click a stepper and the runtime updates the <input> and fires
   a native `input` event. Load `ui-runtime.js` (hiccup) or add a side-effect
   `(:require [ui.js.number-field])` (squint/replicant SPAs). In squint and
   replicant, pass :on-change to react to every value change (typing, stepper,
   wheel — it is wired to the input's `input` event); in hiccup the field is
   uncontrolled (read the <input> value or submit it in a form).

   Props:
     :value       - current numeric value
     :min :max    - bounds (native input attrs; runtime clamps stepper/wheel)
     :step        - increment (default 1)
     :size        - :sm | :md (default) | :lg
     :variant     - :default (steppers flank the input) | :right (both on the
                    right) | :spinner (detached up/down spinner box on the
                    right; :stacked is a legacy alias)
     :disabled    - boolean
     :placeholder - input placeholder
     :name :id    - forwarded to the <input>
     :on-change   - value handler (squint/replicant); receives the raw event
     :class       - extra classes on the wrapper
     :attrs       - extra wrapper attributes
     :input-attrs - extra attributes merged onto the <input>"
  [{:keys [variant size disabled on-change class attrs input-attrs] :as props}]
  (let [variant* (normalize-variant variant)
        spinner? (= variant* "spinner")
        dec-icon (if spinner? :chevron-down :minus)
        inc-icon (if spinner? :chevron-up :plus)
        base-tokens (number-field-class-list {:size size :variant variant :disabled disabled})]
    #?(:squint
       (let [wrap-cls  (cond-> (str/join " " base-tokens) class (str " " class))
             dec-el (stepper {:dir -1 :icon-name dec-icon :label "Decrease"
                              :cls "number-field-btn number-field-btn--dec" :disabled disabled})
             inc-el (stepper {:dir 1 :icon-name inc-icon :label "Increase"
                              :cls "number-field-btn number-field-btn--inc" :disabled disabled})
             spin-el [:div {:class "number-field-spin"} inc-el dec-el]
             input-el [:input (cond-> (merge (input-base-attrs props)
                                             {:class "number-field-input"} input-attrs)
                                on-change (assoc :on-input on-change))]
             wrap-attrs (cond-> (merge {:class wrap-cls :data-ui-number-field "true"} attrs)
                          disabled (assoc :data-ui-number-disabled "true"))]
         (into [:div wrap-attrs]
               (children-for variant* {:dec dec-el :inc inc-el :spin spin-el :input input-el})))

       :cljs
       (let [wrap-cls  (cond-> (vec base-tokens) class (conj class))
             dec-el (stepper {:dir -1 :icon-name dec-icon :label "Decrease"
                              :cls ["number-field-btn" "number-field-btn--dec"] :disabled disabled})
             inc-el (stepper {:dir 1 :icon-name inc-icon :label "Increase"
                              :cls ["number-field-btn" "number-field-btn--inc"] :disabled disabled})
             spin-el [:div {:class ["number-field-spin"]} inc-el dec-el]
             input-el [:input (cond-> (merge (input-base-attrs props)
                                             {:class ["number-field-input"]} input-attrs)
                                on-change (assoc-in [:on :input] on-change))]
             wrap-attrs (cond-> (merge {:class wrap-cls :data-ui-number-field "true"} attrs)
                          disabled (assoc :data-ui-number-disabled "true"))]
         (into [:div wrap-attrs]
               (children-for variant* {:dec dec-el :inc inc-el :spin spin-el :input input-el})))

       :clj
       (let [wrap-cls  (cond-> (str/join " " base-tokens) class (str " " class))
             dec-el (stepper {:dir -1 :icon-name dec-icon :label "Decrease"
                              :cls "number-field-btn number-field-btn--dec" :disabled disabled})
             inc-el (stepper {:dir 1 :icon-name inc-icon :label "Increase"
                              :cls "number-field-btn number-field-btn--inc" :disabled disabled})
             spin-el [:div {:class "number-field-spin"} inc-el dec-el]
             input-el [:input (merge (input-base-attrs props)
                                     {:class "number-field-input"} input-attrs)]
             wrap-attrs (cond-> (merge {:class wrap-cls :data-ui-number-field "true"} attrs)
                          disabled (assoc :data-ui-number-disabled "true"))]
         (into [:div wrap-attrs]
               (children-for variant* {:dec dec-el :inc inc-el :spin spin-el :input input-el}))))))
