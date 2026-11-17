(ns ui.filters
  "The parts of a filter view: what a list shows, in which order, with
   which columns and details, and the presets that remember it.

     filters            the surface, one band under the other
     filter-band        a labeled band: the key in a left gutter, the
                        controls wrapping next to it
     filter-disclosure  a line that says the filter in words and opens
                        the bands (for a filter that stays out of the way)
     preset-bar/preset  saved views in one boxed track; the changed one
                        carries its own Save and Revert
     rule-list/rule     sort, group, hide and keep as sentences: a verb,
                        then value pills, a switch to pause, a remove
     filter-toggle      a pressed toggle for a display option, a column
                        or one choice of a few
     filter-footer      the quiet status line with text actions

   Everything is stateless: the app owns the view's state and its menus."
  (:require [clojure.string :as str]
            [ui.util :as util]
            [ui.icon :as icon]
            [ui.switch :as switch]))

;; ── Cross-target shims ───────────────────────────────────────────────

(defn- handle
  "`attrs` with the handler `f` in the target's shape: flat (`:on-click`)
   on :squint, nested (`{:on {:click f}}`) on :cljs, dropped on :clj where
   handlers come in through attrs (onclick, hx-get)."
  [attrs flat nested f]
  #?(:squint (if f (assoc attrs flat f) attrs)
     :cljs   (if f (assoc-in attrs [:on nested] f) attrs)
     :clj    (do flat nested f attrs)))

(defn- button-attrs [class-list class attrs]
  (util/class-attrs class-list class (merge {:type "button"} attrs)))

;; ── Surface, bands, footer ───────────────────────────────────────────

(defn filters-class-list
  "Classes for the surface. :bare drops the border and background, for a
   filter inside a surface that already has them (a column, a popover).
   :dense packs the bands: less padding, no hairlines, the label beside
   its controls at any width (a few bands of toggles in a narrow column)."
  [{:keys [bare dense]}]
  (cond-> ["filters"]
    bare  (conj "filters-bare")
    dense (conj "filters-dense")))

(defn filters-classes
  "Space-joined form of filters-class-list."
  [opts]
  (str/join " " (filters-class-list opts)))

(defn filters
  "The surface of a filter view: bands stacked with hairlines between
   them. It is a size container, so the bands stack their label on top
   when the surface is narrow, wherever it sits.

   Props:
     :bare  - no border and background of its own
     :dense - packed bands: less padding, no hairlines, labels stay beside
              their controls on a narrow surface
     :class - additional CSS classes
     :attrs - additional HTML attributes map

   Children are filter-band and filter-footer elements."
  [{:keys [bare dense class attrs] :as _props} & bands]
  (into [:div (util/class-attrs (filters-class-list {:bare bare :dense dense}) class attrs)]
        (remove nil? bands)))

(defn filter-band
  "One band: a short label in the left gutter and the controls next to
   it, wrapping. Without :label the controls take the whole width on one
   line (the preset bar with its trailing button).

   Props:
     :label - one or two words (\"Find\", \"Rules\", \"Columns\", \"Show\")
     :class - additional CSS classes
     :attrs - additional HTML attributes map

   Children are the controls: a search field, chips, a rule-list,
   filter-toggles."
  [{:keys [label class attrs] :as _props} & controls]
  [:div (util/class-attrs (cond-> ["filter-band"] (not label) (conj "filter-band-wide")) class attrs)
   (when label [:span {:class "filter-band-label"} label])
   (into [:div {:class "filter-band-body"}] (remove nil? controls))])

(defn filter-footer
  "The last line of the surface: a status in muted text and its text
   actions (filter-action). `(filter-spacer)` pushes what follows to the
   end.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes map"
  [{:keys [class attrs] :as _props} & children]
  (into [:div (util/class-attrs ["filter-footer"] class attrs)]
        (remove nil? children)))

(defn filter-action
  "A text action of the footer (\"Update\", \"Clear rules\").

   Props:
     :on-click - click handler (:cljs and :squint)
     :disabled - boolean
     :class    - additional CSS classes
     :attrs    - additional HTML attributes map"
  [{:keys [on-click disabled class attrs] :as _props} & label]
  (into [:button (-> (button-attrs ["filter-action"] class attrs)
                     (cond-> disabled (assoc :disabled true))
                     (handle :on-click :click on-click))]
        label))

(defn filter-spacer
  "Takes the free space of a footer or a band, so what follows sits at the
   end."
  []
  [:span {:class "filter-spacer"}])

(defn filter-divider
  "A short vertical hairline between two groups of a band (the shown
   columns and the ones to add)."
  []
  [:span {:class "filter-divider" :aria-hidden "true"}])

(defn filter-disclosure
  "A filter that stays out of the way: one line that says the filter in
   words and opens what is behind it. Native <details>, no JS.

   Props:
     :summary       - the filter in words (\"15+ comments · unseen only\")
     :icon          - leading icon keyword, default :filter
     :open          - boolean, start opened
     :summary-attrs - attributes for the element holding the words (an id
                      to swap it after a server round trip)
     :class         - additional CSS classes
     :attrs         - additional HTML attributes map

   Children are what opens: usually `(filters {:bare true} ...)`."
  [{:keys [summary icon open summary-attrs class attrs] :as _props} & children]
  [:details (util/class-attrs ["filter-disclosure"] class (cond-> (or attrs {}) open (assoc :open true)))
   [:summary {:class "filter-disclosure-summary"}
    (icon/icon {:icon-name (or icon :filter) :size :sm})
    [:span (merge {:class "filter-disclosure-text"} summary-attrs) summary]
    (icon/icon {:icon-name :chevron-down :size :sm :class "filter-disclosure-chevron"})]
   (into [:div {:class "filter-disclosure-body"}] (remove nil? children))])

;; ── Presets ──────────────────────────────────────────────────────────

(defn preset-bar
  "Saved views in one boxed track that scrolls sideways when they don't
   fit.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes map

   Children are preset elements and an optional preset-add."
  [{:keys [class attrs] :as _props} & presets]
  (into [:div (util/class-attrs ["preset-bar"] class (merge {:role "group"} attrs))]
        (remove nil? presets)))

(defn preset-class-list
  "Classes for a preset. :dirty only shows on the active one."
  [{:keys [active dirty]}]
  (cond-> ["preset"]
    active               (conj "preset-active")
    (and active dirty)   (conj "preset-dirty")))

(defn preset-classes
  "Space-joined form of preset-class-list."
  [opts]
  (str/join " " (preset-class-list opts)))

(defn preset
  "One saved view. The active one is raised; when it is :dirty (the view
   on screen no longer matches it) it shows a dot and, inside its own
   pill, Save and Revert, so saving sits where the change is.

   Props:
     :active       - boolean, the selected preset
     :dirty        - boolean, the active preset has unsaved changes
     :on-click     - picks the preset (:cljs and :squint)
     :href         - renders the preset as a link instead of a button
     :on-save      - saves the changes into the preset
     :on-revert    - drops the changes
     :save-label   - text of the save action, default \"Save\"
     :revert-label - accessible name of the revert action
     :button-attrs - attributes for the preset's button or link (onclick,
                     hx-get)
     :save-attrs   - attributes for the save button
     :revert-attrs - attributes for the revert button
     :class        - additional CSS classes
     :attrs        - additional HTML attributes map (the wrapper: a key,
                     a context menu)

   Children are the label."
  [{:keys [active dirty on-click href on-save on-revert save-label revert-label
           button-attrs save-attrs revert-attrs class attrs] :as _props} & label]
  (let [dirty?  (boolean (and active dirty))
        revert  (or revert-label "Revert changes")
        main    (-> (merge (if href
                             {:class "preset-main" :href href}
                             {:class "preset-main" :type "button"})
                           (when active {:aria-current "true"})
                           button-attrs)
                    (handle :on-click :click on-click))
        main-el (cond-> (into [(if href :a :button) main] label)
                  dirty? (conj [:span {:class "preset-dot" :title "Unsaved changes"}]))]
    (cond-> [:span (util/class-attrs (preset-class-list {:active active :dirty dirty}) class attrs)
             main-el]
      (and dirty? (or on-save save-attrs))
      (conj [:button (-> (merge {:class "preset-save" :type "button"} save-attrs)
                         (handle :on-click :click on-save))
             (or save-label "Save")])
      (and dirty? (or on-revert revert-attrs))
      (conj [:button (-> (merge {:class "preset-revert" :type "button" :title revert :aria-label revert}
                                revert-attrs)
                         (handle :on-click :click on-revert))
             (icon/icon {:icon-name :undo :size :sm})]))))

(defn preset-add
  "The button at the end of a preset bar that saves the view on screen as
   a new preset: a plus, or with children a labeled action (\"Save as
   preset\" for a view nobody saved yet).

   Props:
     :on-click - click handler (:cljs and :squint)
     :label    - accessible name and tooltip, default \"Save as new preset\"
     :class    - additional CSS classes
     :attrs    - additional HTML attributes map"
  [{:keys [on-click label class attrs] :as _props} & children]
  (let [title    (or label "Save as new preset")
        children (remove nil? children)
        labeled? (boolean (seq children))]
    (into [:button (-> (button-attrs (cond-> ["preset-add"] labeled? (conj "preset-add-labeled"))
                                     class
                                     (merge {:title title :aria-label title} attrs))
                       (handle :on-click :click on-click))
           (icon/icon {:icon-name :plus :size :sm})]
          children)))

(defn preset-more
  "The button at the end of a preset bar that opens a menu of every preset,
   for a bar that only shows the few used last. The app opens the menu.

   Props:
     :on-click - click handler (:cljs and :squint)
     :label    - accessible name and tooltip, default \"All presets\"
     :class    - additional CSS classes
     :attrs    - additional HTML attributes map"
  [{:keys [on-click label class attrs] :as _props}]
  (let [title (or label "All presets")]
    [:button (-> (button-attrs ["preset-add"] class
                               (merge {:title title :aria-label title :aria-haspopup "menu"} attrs))
                 (handle :on-click :click on-click))
     (icon/icon {:icon-name :chevron-down :size :sm})]))

;; ── Rules ────────────────────────────────────────────────────────────

(defn rule-list
  "Rules as sentences on one surface, a hairline between them. The order
   is the order they apply in.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes map

   Children are rule elements, then a rule-add; a rule-empty when there
   is no rule."
  [{:keys [class attrs] :as _props} & rules]
  (into [:div (util/class-attrs ["rule-list"] class attrs)]
        (remove nil? rules)))

(defn rule-class-list
  "Classes for a rule. :off is a paused rule: it stays in the list, dimmed."
  [{:keys [off]}]
  (cond-> ["rule"]
    off (conj "rule-off")))

(defn rule-classes
  "Space-joined form of rule-class-list."
  [opts]
  (str/join " " (rule-class-list opts)))

(defn- word [w]
  (if (string? w) [:span {:class "rule-word"} w] w))

(defn rule
  "One rule, read as a sentence: a grip, the verb (\"Sort by\", \"Hide\"),
   then its words. A string among the children is a connective (\"where\",
   \"is\"), everything else a value: a rule-value, a rule-text, a select.
   At the end a remove button and the switch that pauses the rule.

   Props:
     :verb         - what the rule does (\"Sort by\", \"Group by\", \"Hide\",
                     \"Keep only\")
     :icon         - icon keyword before the verb
     :off          - boolean, the rule is paused
     :grip         - false leaves the drag grip out
     :toggle       - false leaves the switch out
     :on-verb      - makes the verb a button (switch Hide and Keep only)
     :on-toggle    - change handler of the switch
     :on-remove    - click handler of the remove button; without it and
                     without :remove-attrs there is no remove button
     :grip-attrs   - attributes for the grip (a pointerdown that starts a
                     drag)
     :verb-attrs   - attributes for the verb
     :toggle-attrs - attributes for the switch's label
     :remove-attrs - attributes for the remove button
     :class        - additional CSS classes
     :attrs        - additional HTML attributes map (a key, drag and drop)

   Children are the words."
  [{:keys [verb icon off grip toggle on-verb on-toggle on-remove
           grip-attrs verb-attrs toggle-attrs remove-attrs class attrs] :as _props} & words]
  (let [verb-body (cond-> []
                    icon (conj (icon/icon {:icon-name icon :size :sm}))
                    true (conj verb))
        verb-el   (if (or on-verb verb-attrs)
                    (into [:button (-> (merge {:class "rule-verb" :type "button"} verb-attrs)
                                       (handle :on-click :click on-verb))]
                          verb-body)
                    (into [:span {:class "rule-verb"}] verb-body))
        end       (cond-> [:span {:class "rule-end"}]
                    (or on-remove remove-attrs)
                    (conj [:button (-> (merge {:class "rule-remove" :type "button"
                                               :title "Remove rule" :aria-label "Remove rule"}
                                              remove-attrs)
                                       (handle :on-click :click on-remove))
                           (icon/icon {:icon-name :x :size :sm})])
                    (not (false? toggle))
                    (conj (switch/switch-toggle
                           {:checked (not off) :size :sm :on-change on-toggle
                            :attrs (merge {:title (if off "Turn on" "Turn off")} toggle-attrs)})))]
    (cond-> [:div (util/class-attrs (rule-class-list {:off off}) class attrs)]
      (not (false? grip))
      (conj [:span (merge {:class "rule-grip" :aria-hidden "true"} grip-attrs)
             (icon/icon {:icon-name :grip-vertical :size :sm})])
      true (conj verb-el)
      true (conj (into [:span {:class "rule-words"}] (map word (remove nil? words))))
      true (conj end))))

(defn rule-value
  "A value of a rule as a pill you press to change it: the app opens its
   menu from :on-click.

   Props:
     :icon     - icon keyword before the label (an arrow for a direction)
     :on-click - click handler (:cljs and :squint)
     :class    - additional CSS classes
     :attrs    - additional HTML attributes map

   Children are the label."
  [{:keys [icon on-click class attrs] :as _props} & label]
  (into (cond-> [:button (-> (button-attrs ["rule-value"] class attrs)
                             (handle :on-click :click on-click))]
          icon (conj (icon/icon {:icon-name icon :size :sm})))
        label))

(defn rule-text
  "A free text value of a rule, typed in place.

   Props:
     :value       - the text
     :placeholder - shown while empty
     :on-input    - input handler (:cljs and :squint)
     :class       - additional CSS classes
     :attrs       - additional HTML attributes map"
  [{:keys [value placeholder on-input class attrs] :as _props}]
  [:input (-> (util/class-attrs ["rule-text"] class
                                (merge {:type "text" :value (or value "") :autocomplete "off"}
                                       (when placeholder {:placeholder placeholder})
                                       attrs))
              (handle :on-input :input on-input))])

(defn rule-add
  "The last row of a rule-list: adds a rule. One row for every kind; the
   app opens a menu of kinds when there is more than one.

   Props:
     :on-click - click handler (:cljs and :squint)
     :hint     - muted text at the end naming the kinds (\"Sort, hide,
                 keep only\")
     :class    - additional CSS classes
     :attrs    - additional HTML attributes map

   Children are the label, default \"Add rule\"."
  [{:keys [on-click hint class attrs] :as _props} & label]
  (let [label (remove nil? label)]
    (cond-> (into [:button (-> (button-attrs ["rule-add"] class attrs)
                               (handle :on-click :click on-click))
                   (icon/icon {:icon-name :plus :size :sm})]
                  (if (seq label) label ["Add rule"]))
      hint (conj [:span {:class "rule-add-hint"} hint]))))

(defn rule-empty
  "What a rule-list says when it has no rule.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes map

   Children are the sentence."
  [{:keys [class attrs] :as _props} & text]
  (into [:div (util/class-attrs ["rule-empty"] class attrs)] text))

;; ── Toggles ──────────────────────────────────────────────────────────

(defn filter-toggle-class-list
  "Classes for a toggle. :on is pressed; :reorder is the column form (a
   grip, a neutral fill while shown, an outline while hidden)."
  [{:keys [on reorder]}]
  (cond-> ["filter-toggle"]
    reorder (conj "filter-toggle-order")
    on      (conj "filter-toggle-on")))

(defn filter-toggle-classes
  "Space-joined form of filter-toggle-class-list."
  [opts]
  (str/join " " (filter-toggle-class-list opts)))

(defn filter-toggle
  "A pressed toggle: a display option (\"Covers\"), one choice of a few
   (\"15+\"), or with :reorder a column that is shown or hidden and sits in
   table order. A wrapping row of these replaces a switch per line.

   Props:
     :on       - boolean, pressed
     :icon     - leading icon keyword
     :reorder  - boolean, the column form: a grip while shown, a crossed
                 eye while hidden (drag handlers come in through :attrs)
     :value    - a control shown inside the pressed toggle after a
                 hairline, for an option that needs a value (a select,
                 a rule-value)
     :on-click - click handler (:cljs and :squint)
     :class    - additional CSS classes
     :attrs    - additional HTML attributes map

   Children are the label."
  [{:keys [on icon reorder value on-click class attrs] :as _props} & label]
  (let [lead   (cond reorder (if on :grip-vertical :eye-off)
                     icon    icon
                     :else   nil)
        button (into (cond-> [:button (-> (button-attrs (filter-toggle-class-list {:on on :reorder reorder})
                                                         class
                                                         (merge {:aria-pressed (if on "true" "false")} attrs))
                                          (handle :on-click :click on-click))]
                       lead (conj (icon/icon {:icon-name lead :size :sm})))
                     label)]
    (if value
      [:span {:class "filter-toggle-split"} button [:span {:class "filter-toggle-value"} value]]
      button)))

(defn filter-add
  "A quiet \"+ Album\" at the end of a band: adds what is not there yet (a
   column).

   Props:
     :on-click - click handler (:cljs and :squint)
     :class    - additional CSS classes
     :attrs    - additional HTML attributes map

   Children are the label."
  [{:keys [on-click class attrs] :as _props} & label]
  (into [:button (-> (button-attrs ["filter-add"] class attrs)
                     (handle :on-click :click on-click))
         (icon/icon {:icon-name :plus :size :sm})]
        label))
