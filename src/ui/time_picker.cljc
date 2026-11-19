(ns ui.time-picker
  "Time picker: one field with a start time, or a start and an end with the
   length between them. Each time is a segment you type into (`1430`,
   `14:30`, `2:30pm`, `9`) or pick from a quarter-hour list under it; the
   end list counts the length from the start, and moving the start keeps the
   length.

   Renders data-attributed markup only. The `time-picker` ui-runtime.js
   module opens the lists, reads what is typed and commits it. Load
   `ui-runtime.js` (hiccup) or add a side-effect
   `(:require [ui.js.time-picker])` (squint/replicant SPAs).

   The value lands in a hidden <input data-ui-time-value> as `HH:MM`, or
   `HH:MM-HH:MM` with an end (org's time range), empty for no time. The
   runtime writes it and fires `input` and `change` on it whenever a time is
   committed; `parse-value` splits it back into minutes."
  (:require [clojure.string :as str]
            [ui.icon :as icon]
            [ui.util :as util]))

(def default-step 15)

;; ── Pure helpers (shared with the runtime, unit-tested) ─────────────

(defn- parse-int [s]
  #?(:squint (js/parseInt s 10)
     :cljs   (js/parseInt s 10)
     :clj    (Long/parseLong s)))

(defn parse-time
  "Minutes from midnight for typed text, or nil when it isn't a time.
   Reads `14:30`, `1430`, `930`, `9`, `2:30pm`, `2p`, `12am`."
  [s]
  (when (string? s)
    (when-let [[_ h m ap] (re-matches #"(\d{1,2})(?::?(\d{2}))?\s*(am|pm|a|p)?"
                                      (str/lower-case (str/trim s)))]
      (let [h (parse-int h)
            m (if m (parse-int m) 0)
            half (when ap (subs ap 0 1))]
        (when (and (< m 60)
                   (if half (<= 1 h 12) (< h 24)))
          (+ (* 60 (cond (and (= half "p") (< h 12)) (+ h 12)
                         (and (= half "a") (= h 12)) 0
                         :else h))
             m))))))

(defn- pad2 [n] (if (< n 10) (str "0" n) (str n)))

(defn format-time
  "`HH:MM` for minutes from midnight, nil for nil."
  [mins]
  (when (some? mins)
    (let [m (mod mins 1440)]
      (str (pad2 (quot m 60)) ":" (pad2 (mod m 60))))))

(defn format-length
  "A length for the field and the end list: `45m`, `1h`, `1h 15m`. nil for
   nothing or a length that isn't positive."
  [mins]
  (when (and (some? mins) (pos? mins))
    (let [h (quot mins 60) m (mod mins 60)]
      (cond (zero? h) (str m "m")
            (zero? m) (str h "h")
            :else (str h "h " m "m")))))

(defn parse-value
  "{:start :end} in minutes from a value string: `HH:MM` or `HH:MM-HH:MM`.
   Missing or unreadable parts are nil."
  [s]
  (let [[a b] (str/split (str/trim (str (or s ""))) #"\s*-\s*")]
    {:start (when (seq a) (parse-time a))
     :end   (when (seq b) (parse-time b))}))

(defn value-str
  "The value string for `start` and `end` minutes: `HH:MM`, `HH:MM-HH:MM`,
   or empty when there is no start."
  [start end]
  (cond (nil? start) ""
        (and (some? end) (> end start)) (str (format-time start) "-" (format-time end))
        :else (format-time start)))

(defn start-slots
  "The start list: every `step` minutes of the day."
  [step]
  (vec (range 0 1440 (or step default-step))))

(defn end-slots
  "The end list: from one `step` after `start` up to 12 hours later, ending
   before midnight (an org range stays inside its day)."
  [start step]
  (let [step (or step default-step)]
    (vec (range (+ start step) (min 1440 (+ start 721)) step))))

(defn move-start
  "{:start :end} after the start moves to `new-start`: an end moves along so
   the length stays, and stops at 23:59."
  [{:keys [start end]} new-start]
  {:start new-start
   :end (when (and (some? start) (some? end))
          (min 1439 (+ new-start (- end start))))})

;; ── Class generation ─────────────────────────────────────────────────

(defn time-picker-class-list
  "Vector of CSS class strings for the picker field."
  [{:keys [range empty disabled]}]
  (cond-> ["time-picker"]
    range    (conj "time-picker--range")
    empty    (conj "time-picker--empty")
    disabled (conj "time-picker--disabled")))

(defn time-picker-classes
  "Space-joined class string for the picker field."
  [opts]
  (str/join " " (time-picker-class-list opts)))

(defn- cls
  "Target-shaped :class value from class tokens."
  [& tokens]
  #?(:squint (str/join " " tokens)
     :cljs   (vec tokens)
     :clj    (str/join " " tokens)))

(defn- segment [which value placeholder label disabled]
  [:input (cond-> {:type "text"
                   :class (cls "time-picker-seg")
                   :inputmode "numeric"
                   :autocomplete "off"
                   :spellcheck "false"
                   :role "combobox"
                   :aria-haspopup "listbox"
                   :aria-expanded "false"
                   :aria-label label
                   :data-ui-time-seg which
                   :value (or value "")
                   :placeholder placeholder}
            disabled (assoc :disabled true))])

(defn time-picker
  "Time field with typeable segments and slot lists.

   Props:
     :value       - `HH:MM`, `HH:MM-HH:MM` or empty; :start / :end override it
     :start :end  - the times as `HH:MM` strings, instead of :value
     :range       - show the end segment and the length (on whenever an
                    end is given)
     :step        - minutes between slots (default 15)
     :placeholder - text of the empty start segment (default \"Add time\")
     :clearable   - a clear button once there is a time (default true)
     :disabled    - boolean
     :name :id    - on the hidden value input (form submission)
     :on-change   - :squint/:cljs; receives the event, the value is
                    event.target.value (`parse-value` splits it)
     :class       - extra classes on the field
     :attrs       - extra field attributes"
  [{:keys [value start end range step placeholder clearable disabled name id on-change class attrs]}]
  (let [{s :start e :end} (parse-value value)
        s      (if (some? start) (parse-time start) s)
        e      (if (some? end) (parse-time end) e)
        e      (when (and (some? s) (some? e) (> e s)) e)
        range? (boolean (or range (some? e)))
        empty? (nil? s)
        ;; rendered while empty too (CSS hides it), so a field the runtime
        ;; fills without a re-render (hiccup) still gets one
        clear? (and (not (false? clearable)) (not disabled))
        step   (or step default-step)
        base   (time-picker-class-list {:range range? :empty empty? :disabled disabled})
        field-attrs (cond-> (merge (util/class-attrs base class nil)
                                   {:data-ui-time-picker "true" :data-step (str step)}
                                   attrs)
                      disabled (assoc :data-ui-time-disabled "true"))
        hidden (cond-> {:type "hidden"
                        :data-ui-time-value "true"
                        :value (value-str s e)}
                 name (assoc :name name)
                 id   (assoc :id id)
                 on-change #?(:squint (assoc :on-change on-change)
                              :cljs   (assoc-in [:on :change] on-change)
                              :clj    identity))]
    (cond-> [:div field-attrs
             (icon/icon {:icon-name :clock :size :sm :class "time-picker-icon"})
             (segment "start" (format-time s) (or placeholder "Add time") "Start time" disabled)]
      range? (conj [:span {:class (cls "time-picker-arrow") :aria-hidden "true"}
                    (icon/icon {:icon-name :arrow-right :size :sm})]
                   (segment "end" (format-time e) "end" "End time" disabled)
                   [:span {:class (cls "time-picker-length") :data-ui-time-length "true"}
                    (or (when (and s e) (format-length (- e s))) "")])
      clear? (conj [:button {:type "button" :class (cls "time-picker-clear")
                             :data-ui-time-clear "true" :aria-label "Clear time" :tabindex "-1"}
                    (icon/icon {:icon-name :x :size :sm})])
      true   (conj [:input hidden]))))
