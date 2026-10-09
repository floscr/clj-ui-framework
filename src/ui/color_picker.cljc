(ns ui.color-picker
  "Color picker — saturation/brightness plane, hue and opacity tracks, format switch and CSS value field.

   Renders data-attributed markup only. The `color-picker` ui-runtime.js
   module owns the behaviour and everything it paints (plane hue, marker,
   track positions, active format): it paints from `data-value` on mount and
   again whenever that attribute changes to a value it did not emit itself,
   so a controlled re-render never resets the hue of a grey or black pick.
   Load `ui-runtime.js` (hiccup) or add a side-effect
   `(:require [ui.js.color-picker])` (squint/replicant SPAs).

   The picked color lands in a hidden <input data-ui-color-value>: the runtime
   fires `input` on it for every change (dragging, tracks, typing) and
   `change` when a gesture ends."
  (:require [clojure.string :as str]
            [ui.util :as util]))

(def all-formats ["oklch" "hex" "rgb" "hsl"])

(def ^:private format-labels {"oklch" "OKLCH" "hex" "Hex" "rgb" "RGB" "hsl" "HSL"})

(defn normalize-formats
  "Known output formats from `formats` (keywords or strings), deduplicated and
   in the given order. Unknown names are dropped; nil or empty → all formats."
  [formats]
  (let [known? (fn [f] (some #(= % f) all-formats))
        fs (reduce (fn [acc f]
                     (if (and (known? f) (not (some #(= % f) acc))) (conj acc f) acc))
                   []
                   (map util/kw-name formats))]
    (if (seq fs) fs all-formats)))

(defn color-picker-class-list
  "Vector of CSS class strings for the picker wrapper."
  [{:keys [disabled]}]
  (cond-> ["color-picker"]
    disabled (conj "color-picker--disabled")))

(defn color-picker-classes
  "Space-joined class string for the picker wrapper."
  [opts]
  (str/join " " (color-picker-class-list opts)))

(defn- cls
  "Target-shaped :class value from class tokens."
  [& tokens]
  #?(:squint (str/join " " tokens)
     :cljs   (vec tokens)
     :clj    (str/join " " tokens)))

(defn- wrapper-attrs
  [{:keys [value disabled alpha class attrs]} formats]
  (cond-> (merge {:class #?(:squint (cond-> (color-picker-classes {:disabled disabled})
                                      class (str " " class))
                            :cljs   (cond-> (color-picker-class-list {:disabled disabled})
                                      class (util/conj-classes class))
                            :clj    (cond-> (color-picker-classes {:disabled disabled})
                                      class (str " " class)))
                  :data-ui-color-picker "true"
                  :data-value (or value "")
                  :data-formats (str/join " " formats)}
                 attrs)
    alpha    (assoc :data-alpha "true")
    disabled (assoc :data-ui-color-disabled "true")))

(defn- format-switch [formats disabled]
  (into [:div {:class (cls "color-picker-formats") :role "group" :aria-label "Color format"}]
        (map (fn [f]
               [:button (cond-> {:type "button" :class (cls "color-picker-format")
                                 :data-ui-color-format f}
                          disabled (assoc :disabled true))
                (get format-labels f)])
             formats)))

(defn- track [label track-cls data-key {:keys [max step]} disabled]
  [:label {:class (cls "color-picker-track-row")}
   [:span label]
   [:input (cond-> {:type "range" :min 0 :max max :step step
                    :class (cls "color-picker-track" track-cls)
                    data-key "true"}
             disabled (assoc :disabled true))]])

(defn color-picker
  "Color picker. The runtime emits colors in the active format: `oklch()`
   (`oklch(L C H / a)` below full opacity), hex (`#rrggbb`, `#rrggbbaa`),
   `rgb()`/`rgba()` or `hsl()`/`hsla()`. The active format starts as the
   format of :value when that is offered, else the first of :formats. Typed
   values may be any CSS color; they are converted to the active format.

   Props:
     :value       - current color, any CSS color string
     :formats     - output formats offered, subset of [:oklch :hex :rgb :hsl]
                    (default all, in that order). With a single format the
                    format switch is hidden.
     :alpha       - show the opacity track (default false)
     :disabled    - boolean
     :name :id    - forwarded to the hidden value <input>
     :on-change   - handler for every value change (squint/replicant); wired
                    to the hidden input's `input` event, so the color is
                    `event.target.value`
     :class       - extra classes on the wrapper
     :attrs       - extra wrapper attributes"
  [{:keys [value formats alpha disabled name id on-change] :as props}]
  (let [formats (normalize-formats formats)
        value-attrs (cond-> {:type "hidden" :data-ui-color-value "true" :value (or value "")}
                      name (assoc :name name)
                      id   (assoc :id id))]
    [:div (wrapper-attrs props formats)
     (when (> (count formats) 1) (format-switch formats disabled))
     [:div {:class (cls "color-picker-plane") :tabindex (if disabled "-1" "0")
            :role "slider" :aria-label "Saturation and brightness"}
      [:span {:class (cls "color-picker-marker")}]]
     [:div {:class (cls "color-picker-tracks")}
      (track "Hue" "color-picker-hue" :data-ui-color-hue {:max 360 :step 1} disabled)
      (when alpha
        (track "Opacity" "color-picker-opacity" :data-ui-color-alpha {:max 1 :step 0.01} disabled))]
     [:input (cond-> {:type "text" :class (cls "color-picker-text") :value (or value "")
                      :spellcheck "false" :autocomplete "off" :aria-label "Color value"
                      :data-ui-color-text "true"}
               disabled (assoc :disabled true))]
     [:input #?(:squint (cond-> value-attrs on-change (assoc :on-input on-change))
                :cljs   (cond-> value-attrs on-change (assoc-in [:on :input] on-change))
                :clj    value-attrs)]]))
