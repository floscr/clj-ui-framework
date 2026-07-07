(ns ui.tag-input
  (:require [clojure.string :as str]
            [ui.icon :as icon]))

;; ── Class helpers ───────────────────────────────────────────────────

(defn tag-input-class-list
  "Returns a vector of CSS class strings for the tag-input container.
   Options:
     :focused - whether the input is focused
     :disabled - whether the input is disabled"
  [{:keys [focused disabled]}]
  (cond-> ["tag-input"]
    focused  (conj "tag-input-focused")
    disabled (conj "tag-input-disabled")))

(defn tag-input-classes [opts]
  (str/join " " (tag-input-class-list opts)))

(defn tag-pill-class-list
  "Returns a vector of CSS class strings for a tag pill."
  [_opts]
  ["tag-pill"])

(defn tag-pill-classes [opts]
  (str/join " " (tag-pill-class-list opts)))

;; ── Tag Pill ────────────────────────────────────────────────────────

(defn tag-pill
  "Render a single tag pill with label and optional remove button.

   Props:
     :label     - display text for the tag
     :on-remove - callback to remove this tag (omit for read-only)
     :class     - additional CSS classes
     :attrs     - additional HTML attributes"
  [{:keys [label on-remove class attrs] :as _props}]
  #?(:squint
     (let [classes (cond-> (tag-pill-classes {})
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       [:span base-attrs
        [:span {:class "tag-pill-label"} label]
        (when on-remove
          [:button {:class "tag-pill-remove"
                    :type "button"
                    :aria-label (str "Remove " label)
                    :on-click on-remove}
           (icon/icon {:icon-name "x" :size "sm"})])])

     :cljs
     (let [cls (tag-pill-class-list {})
           classes (cond-> cls class (conj class))
           base-attrs (merge {:class classes} attrs)]
       [:span base-attrs
        [:span {:class ["tag-pill-label"]} label]
        (when on-remove
          [:button {:class ["tag-pill-remove"]
                    :type "button"
                    :aria-label (str "Remove " label)
                    :on {:click on-remove}}
           (icon/icon {:icon-name :x :size :sm})])])

     :clj
     (let [classes (cond-> (tag-pill-classes {})
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       [:span base-attrs
        [:span {:class "tag-pill-label"} label]
        (when on-remove
          [:button {:class "tag-pill-remove"
                    :type "button"
                    :aria-label (str "Remove " label)}
           (icon/icon {:icon-name :x :size :sm})])])))

;; ── Dropdown Item ───────────────────────────────────────────────────

(defn tag-dropdown-item
  "Render a single item in the tag dropdown suggestion list.

   Props:
     :label     - display text
     :on-select - callback when this item is selected
     :active    - whether this item is highlighted (keyboard nav)
     :class     - additional CSS classes
     :attrs     - additional HTML attributes"
  [{:keys [label on-select active class attrs] :as _props}]
  #?(:squint
     (let [classes (cond-> "tag-dropdown-item"
                     active (str " tag-dropdown-item-active")
                     class  (str " " class))
           base-attrs (merge {:class classes :role "option" :aria-selected (str (boolean active))} attrs)
           base-attrs (cond-> base-attrs
                        on-select (assoc :on-click on-select))]
       [:div base-attrs label])

     :cljs
     (let [cls (cond-> ["tag-dropdown-item"]
                 active (conj "tag-dropdown-item-active"))
           classes (cond-> cls class (conj class))
           base-attrs (merge {:class classes :role "option" :aria-selected (str (boolean active))} attrs)
           base-attrs (cond-> base-attrs
                        on-select (assoc-in [:on :click] on-select))]
       [:div base-attrs label])

     :clj
     (let [classes (cond-> "tag-dropdown-item"
                     active (str " tag-dropdown-item-active")
                     class  (str " " class))
           base-attrs (merge {:class classes :role "option" :aria-selected (str (boolean active))} attrs)]
       [:div base-attrs label])))

;; ── Dropdown ────────────────────────────────────────────────────────

(defn tag-dropdown
  "Render the dropdown suggestion list.

   Props:
     :open      - whether the dropdown is visible
     :items     - seq of {:label, :value} maps to display
     :on-select - callback (fn [item]) when an item is selected
     :active-index - index of the currently highlighted item
     :empty-text   - text shown when no items match (default: \"No tags found.\")
     :class     - additional CSS classes
     :attrs     - additional HTML attributes"
  [{:keys [open items on-select active-index empty-text class attrs] :as _props}]
  (when open
    (let [empty-msg (or empty-text "No tags found.")]
      #?(:squint
         (let [classes (cond-> "tag-dropdown"
                         class (str " " class))
               base-attrs (merge {:class classes :role "listbox"} attrs)]
           (if (seq items)
             (into [:div base-attrs]
                   (map-indexed
                     (fn [idx item]
                       (tag-dropdown-item {:label (:label item)
                                           :active (= idx active-index)
                                           :on-select (when on-select
                                                        (fn [_] (on-select item)))}))
                     items))
             [:div base-attrs
              [:div {:class "tag-dropdown-empty"} empty-msg]]))

         :cljs
         (let [cls (cond-> ["tag-dropdown"] class (conj class))
               base-attrs (merge {:class cls :role "listbox"} attrs)]
           (if (seq items)
             (into [:div base-attrs]
                   (map-indexed
                     (fn [idx item]
                       (tag-dropdown-item {:label (:label item)
                                           :active (= idx active-index)
                                           :on-select (when on-select
                                                        (fn [_] (on-select item)))}))
                     items))
             [:div base-attrs
              [:div {:class ["tag-dropdown-empty"]} empty-msg]]))

         :clj
         (let [classes (cond-> "tag-dropdown"
                         class (str " " class))
               base-attrs (merge {:class classes :role "listbox"} attrs)]
           (if (seq items)
             (into [:div base-attrs]
                   (map-indexed
                     (fn [idx item]
                       (tag-dropdown-item {:label (:label item)
                                           :active (= idx active-index)}))
                     items))
             [:div base-attrs
              [:div {:class "tag-dropdown-empty"} empty-msg]]))))))

;; ── Main Tag Input ──────────────────────────────────────────────────

(defn tag-input
  "Render the full tag input component.

   This is a controlled component — the consumer manages state.

   Props:
     :tags         - vector of currently selected tags [{:label \"React\" :value \"react\"} ...]
     :input-value  - current text in the search input
     :open         - whether dropdown is shown
     :filtered-items - items to show in dropdown (already filtered by consumer)
     :active-index - index of highlighted dropdown item
     :placeholder  - input placeholder text (default: \"Add tag...\")
     :disabled     - boolean
     :on-input     - callback (fn [value]) when input text changes
     :on-remove    - callback (fn [tag]) when a tag is removed
     :on-select    - callback (fn [item]) when an item is selected from dropdown
     :on-backspace - callback (fn []) when backspace is pressed on empty input
     :on-clear     - callback (fn []) when clear button is clicked
     :on-focus     - callback (fn []) when input gains focus
     :on-blur      - callback (fn []) when input loses focus
     :on-key-down  - callback (fn [key-code]) for arrow/enter key handling
     :class        - additional CSS classes
     :attrs        - additional HTML attributes"
  [{:keys [tags input-value open filtered-items active-index placeholder
           disabled on-input on-remove on-select on-backspace on-clear
           on-focus on-blur on-key-down class attrs]
    :as _props}]
  (let [ph (or placeholder "Add tag...")]
    #?(:squint
       (let [classes (cond-> (tag-input-classes {:disabled disabled})
                       class (str " " class))
              wrapper-attrs (merge {:class classes} attrs)]
         [:div wrapper-attrs
          [:div {:class "tag-input-area"}
           (into [:div {:class "tag-input-pills"}]
                 (map (fn [tag]
                        (tag-pill {:label (:label tag)
                                   :on-remove (when (and on-remove (not disabled))
                                                (fn [_] (on-remove tag)))}))
                      tags))
           [:div {:class "tag-input-field-wrap"}
            [:input {:class "tag-input-field"
                     :type "text"
                     :value (or input-value "")
                     :placeholder (if (seq tags) "" ph)
                     :disabled disabled
                     :autocomplete "off"
                     :role "combobox"
                     :aria-expanded (str (boolean open))
                     :on-input (when on-input
                                 (fn [e] (on-input (.. e -target -value))))
                     :on-focus on-focus
                     :on-blur on-blur
                     :on-keydown (fn [e]
                                   (let [key (.-key e)]
                                     (cond
                                       (and (= key "Backspace") (= (.. e -target -value) ""))
                                       (when on-backspace (on-backspace))

                                       (or (= key "ArrowDown") (= key "ArrowUp") (= key "Enter"))
                                       (do (.preventDefault e)
                                           (when on-key-down (on-key-down key))))))}]]
           (when on-clear
             [:button {:class "tag-input-clear"
                       :type "button"
                       :aria-label "Clear"
                       :on-click (fn [_] (on-clear))}
              (icon/icon {:icon-name "x" :size "sm"})])]
          (tag-dropdown {:open open
                         :items filtered-items
                         :on-select on-select
                         :active-index active-index})])

       :cljs
       (let [cls (tag-input-class-list {:disabled disabled})
             classes (cond-> cls class (conj class))
             wrapper-attrs (merge {:class classes} attrs)]
         [:div wrapper-attrs
          [:div {:class ["tag-input-area"]}
           (into [:div {:class ["tag-input-pills"]}]
                 (map (fn [tag]
                        (tag-pill {:label (:label tag)
                                   :on-remove (when (and on-remove (not disabled))
                                                (fn [_] (on-remove tag)))}))
                      tags))
           [:div {:class ["tag-input-field-wrap"]}
            [:input {:class ["tag-input-field"]
                     :type "text"
                     :value (or input-value "")
                     :placeholder (if (seq tags) "" ph)
                     :disabled disabled
                     :autocomplete "off"
                     :role "combobox"
                     :aria-expanded (str (boolean open))
                     :on {:input (when on-input
                                   (fn [e] (on-input (.. e -target -value))))
                          :focus on-focus
                          :blur on-blur
                          :keydown (fn [e]
                                     (let [key (.-key e)]
                                       (cond
                                         (and (= key "Backspace") (= (.. e -target -value) ""))
                                         (when on-backspace (on-backspace))

                                         (or (= key "ArrowDown") (= key "ArrowUp") (= key "Enter"))
                                         (do (.preventDefault e)
                                             (when on-key-down (on-key-down key))))))}}]]
           (when on-clear
             [:button {:class ["tag-input-clear"]
                       :type "button"
                       :aria-label "Clear"
                       :on {:click (fn [_] (on-clear))}}
              (icon/icon {:icon-name :x :size :sm})])]
          (tag-dropdown {:open open
                         :items filtered-items
                         :on-select on-select
                         :active-index active-index})])

       :clj
       (let [classes (cond-> (tag-input-classes {:disabled disabled})
                       class (str " " class))
             wrapper-attrs (merge {:class classes} attrs)]
         [:div wrapper-attrs
          [:div {:class "tag-input-area"}
           (into [:div {:class "tag-input-pills"}]
                 (map (fn [tag]
                        (tag-pill {:label (:label tag)}))
                      tags))
           [:div {:class "tag-input-field-wrap"}
            [:input {:class "tag-input-field"
                     :type "text"
                     :value (or input-value "")
                     :placeholder (if (seq tags) "" ph)
                     :disabled disabled
                     :autocomplete "off"
                     :role "combobox"
                     :aria-expanded (str (boolean open))}]]
           (when on-clear
             [:button {:class "tag-input-clear"
                       :type "button"
                       :aria-label "Clear"}
              (icon/icon {:icon-name :x :size :sm})])]
          (tag-dropdown {:open open
                         :items filtered-items
                         :active-index active-index})]))))

;; ── State helpers (for interactive targets) ─────────────────────────

(do
  #?@(:squint []
      :cljs [(defn filter-tags
               "Filter available tags by input value, excluding already-selected tags.
                Returns a seq of matching items."
               [all-tags selected-tags input-value]
               (let [q (str/lower-case (or input-value ""))
                     selected-labels (set (map :label selected-tags))]
                 (filter (fn [tag]
                           (and (not (selected-labels (:label tag)))
                                (str/includes? (str/lower-case (:label tag)) q)))
                         all-tags)))]
      :clj  [(defn filter-tags
               "Filter available tags by input value, excluding already-selected tags.
                Returns a seq of matching items."
               [all-tags selected-tags input-value]
               (let [q (str/lower-case (or input-value ""))
                     selected-labels (set (map :label selected-tags))]
                 (filter (fn [tag]
                           (and (not (selected-labels (:label tag)))
                                (str/includes? (str/lower-case (:label tag)) q)))
                         all-tags)))]))

#?(:squint
   (defn filter-tags
     "Filter available tags by input value, excluding already-selected tags.
      Returns a seq of matching items."
     [all-tags selected-tags input-value]
     (let [q (.toLowerCase (or input-value ""))
           selected-labels (set (map :label selected-tags))]
       (filter (fn [tag]
                 (and (not (contains? selected-labels (:label tag)))
                      (.includes (.toLowerCase (:label tag)) q)))
               all-tags))))
