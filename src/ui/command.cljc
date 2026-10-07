(ns ui.command
  "Command palette — a searchable command menu (cmdk-style) rendered in a
   native <dialog>, with icons and keyboard-shortcut hints.

   Built on the native <dialog> element (showModal gives backdrop, focus
   trapping and Escape for free). Items render as static markup; a tiny JS
   runtime (ui-runtime.js, compiled from squint) handles live filtering,
   arrow-key navigation, Enter-to-select and the global open hotkey — so the
   component works identically across hiccup, replicant and squint.

   Usage (all targets):
     (command-trigger {:target \"cmdk\"} \"Search…\")

     (command-dialog {:id \"cmdk\" :placeholder \"Type a command or search…\"
                      :hotkey \"mod+k\"}
       (command-group {:heading \"Suggestions\"}
         (command-item {:icon :calendar :shortcut \"⌘P\"} \"Calendar\")
         (command-item {:icon :smile    :shortcut \"⌘B\"} \"Search Emoji\"))
       (command-group {:heading \"Settings\"}
         (command-item {:icon :user     :shortcut \"⌘S\"} \"Profile\")
         (command-item {:icon :settings :shortcut \"⌘,\"} \"Settings\")))

   Items may carry :url (navigate on select, rendered as <a>) or, in
   replicant/squint, an :on-click callback. Selection always closes the dialog."
  (:require [ui.icon :as icon]
            [ui.util :as util]))

;; ── Item ────────────────────────────────────────────────────────────

(defn command-item
  "Render a single command item.

   Props:
     :icon     - icon name keyword (optional, e.g. :calendar)
     :shortcut - keyboard shortcut hint string (optional, e.g. \"⌘P\")
     :url      - if set, renders an <a> that navigates on select
     :description - secondary subline text shown under the label (optional)
     :value    - explicit search text (defaults to the item's text content).
                 An empty string matches no query: the item shows only while
                 the input is empty (a \"recents\" row).
     :search-only - the inverse: hide the item while the input is empty and
                 only surface it as a search result (an archive tier, a
                 parent's sub-actions, …). Rendered as data-command-search-only.
     :on-click - selection callback (replicant/squint only)
     :disabled - boolean
     :class    - additional CSS classes
     :attrs    - additional HTML attributes
   Children form the item label."
  [{:keys [icon shortcut description url value search-only on-click disabled class attrs] :as _props} & children]
  (let [tag       (if url :a :button)
        icon-el   (when icon (icon/icon {:icon-name icon :size :sm
                                         :class "command-item-icon"}))
        shortcut-el (when shortcut [:kbd {:class "command-shortcut"} shortcut])]
    #?(:squint
       (let [classes (cond-> "command-item" class (str " " class))
             base    (merge {:class classes :role "option"}
                            (when value {:data-command-value value})
                            (when search-only {:data-command-search-only "true"})
                            (when disabled {:disabled true})
                            (when url {:href url})
                            (when (= tag :button) {:type "button"})
                            (when on-click {:on-click on-click})
                            attrs)
             label-el (into [:span {:class "command-item-label"}] children)
             text-el  (if (seq description)
                        [:span {:class "command-item-text"} label-el
                         [:span {:class "command-item-desc"} description]]
                        label-el)]
         (into [tag base] (concat (when icon-el [icon-el])
                                  [text-el]
                                  (when shortcut-el [shortcut-el]))))

       :cljs
       (let [classes (util/conj-classes ["command-item"] class)
             base    (merge {:class classes :role "option"}
                            (when value {:data-command-value value})
                            (when search-only {:data-command-search-only "true"})
                            (when disabled {:disabled true})
                            (when url {:href url})
                            (when (= tag :button) {:type "button"})
                            (when on-click {:on {:click on-click}})
                            attrs)
             label-el (into [:span {:class ["command-item-label"]}] children)
             text-el  (if (seq description)
                        [:span {:class ["command-item-text"]} label-el
                         [:span {:class ["command-item-desc"]} description]]
                        label-el)]
         (into [tag base] (concat (when icon-el [icon-el])
                                  [text-el]
                                  (when shortcut-el [shortcut-el]))))

       :clj
       (let [classes (cond-> "command-item" class (str " " class))
             base    (merge {:class classes :role "option"}
                            (when value {:data-command-value value})
                            (when search-only {:data-command-search-only "true"})
                            (when disabled {:disabled true})
                            (when url {:href url})
                            (when (= tag :button) {:type "button"})
                            attrs)
             label-el (into [:span {:class "command-item-label"}] children)
             text-el  (if (seq description)
                        [:span {:class "command-item-text"} label-el
                         [:span {:class "command-item-desc"} description]]
                        label-el)]
         (into [tag base] (concat (when icon-el [icon-el])
                                  [text-el]
                                  (when shortcut-el [shortcut-el])))))))

;; ── Group ───────────────────────────────────────────────────────────

(defn command-group
  "Group command items under an optional heading.

   Props:
     :heading - group heading text (optional)
     :class   - additional CSS classes
     :attrs   - additional HTML attributes"
  [{:keys [heading class attrs] :as _props} & children]
  (let [heading-el (when heading [:div {:class "command-group-heading"} heading])]
    #?(:squint
       (let [classes (cond-> "command-group" class (str " " class))
             base    (merge {:class classes :role "group"} attrs)]
         (into [:div base] (concat (when heading-el [heading-el])
                                   [(into [:div {:class "command-group-items"}] children)])))

       :cljs
       (let [classes (util/conj-classes ["command-group"] class)
             base    (merge {:class classes :role "group"} attrs)]
         (into [:div base] (concat (when heading-el [heading-el])
                                   [(into [:div {:class ["command-group-items"]}] children)])))

       :clj
       (let [classes (cond-> "command-group" class (str " " class))
             base    (merge {:class classes :role "group"} attrs)]
         (into [:div base] (concat (when heading-el [heading-el])
                                   [(into [:div {:class "command-group-items"}] children)]))))))

;; ── Dialog ──────────────────────────────────────────────────────────

(defn command-dialog
  "Render a command palette as a native <dialog>.

   Props:
     :id          - dialog id, matched by command-trigger :target (required)
     :placeholder - search input placeholder (default \"Type a command or search…\")
     :hotkey      - global open shortcut, e.g. \"mod+k\" (mod = ⌘ on mac, Ctrl elsewhere)
     :quick-nav   - Alt quick-select: while Alt is held the first visible rows
                    get a key badge and Alt+<key> selects that row (like avy /
                    swiper). :letters (asdfghl…, skips the j/k/n/p list-nav
                    keys), :numbers (1234567890) or a custom key string in row
                    order, e.g. \"sfgh\" when the app binds some Alt+letters
                    itself. Keys match the physical key (KeyA → a), so they work
                    where Alt+letter types a special character. Off by default.
     :empty       - empty-state text when no items match (default \"No results found.\")
     :leading     - hiccup rendered in place of the default search icon (optional);
                    e.g. a clickable back button (use class \"command-search-back\")
     :class       - additional CSS classes
     :attrs       - additional HTML attributes
   Children are command-group / command-item forms."
  [{:keys [id placeholder hotkey quick-nav empty leading class attrs] :as _props} & children]
  (let [quick-nav*   (when quick-nav (if (keyword? quick-nav) (name quick-nav) quick-nav))
        placeholder* (or placeholder "Type a command or search…")
        empty*       (or empty "No results found.")
        leading*     (or leading (icon/icon {:icon-name :search :size :sm :class "command-search-icon"}))
        search   [:div {:class "command-search"}
                  leading*
                  [:input {:class "command-input" :type "text" :role "combobox"
                           :placeholder placeholder* :autocomplete "off"
                           :spellcheck "false" :aria-label placeholder*}]]
        empty-el [:div {:class "command-empty"} empty*]]
    #?(:squint
       (let [classes (cond-> "command-dialog" class (str " " class))
             base    (merge {:class classes :role "dialog" :aria-modal "true"}
                            (when id {:id id})
                            (when hotkey {:data-command-hotkey hotkey})
                            (when quick-nav* {:data-command-quick-nav quick-nav*})
                            attrs)]
         (into [:dialog base]
               [search
                (into [:div {:class "command-list" :role "listbox"}]
                      (concat children [empty-el]))]))

       :cljs
       (let [classes (util/conj-classes ["command-dialog"] class)
             base    (merge {:class classes :role "dialog" :aria-modal "true"}
                            (when id {:id id})
                            (when hotkey {:data-command-hotkey hotkey})
                            (when quick-nav* {:data-command-quick-nav quick-nav*})
                            attrs)]
         (into [:dialog base]
               [search
                (into [:div {:class ["command-list"] :role "listbox"}]
                      (concat children [empty-el]))]))

       :clj
       (let [classes (cond-> "command-dialog" class (str " " class))
             base    (merge {:class classes :role "dialog" :aria-modal "true"}
                            (when id {:id id})
                            (when hotkey {:data-command-hotkey hotkey})
                            (when quick-nav* {:data-command-quick-nav quick-nav*})
                            attrs)]
         (into [:dialog base]
               [search
                (into [:div {:class "command-list" :role "listbox"}]
                      (concat children [empty-el]))])))))

;; ── Trigger ─────────────────────────────────────────────────────────

(defn command-trigger
  "Render a <button> that opens the matching command-dialog.

   Props:
     :target - id of the command-dialog to open (required)
     :class  - additional CSS classes (e.g. button styling)
     :attrs  - additional HTML attributes"
  [{:keys [target class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> "command-trigger" class (str " " class))
           base    (merge {:class classes :type "button" :aria-haspopup "dialog"
                           :data-command-target target
                           :on-click (fn [_]
                                       (let [f (aget js/window "__uiCommand")]
                                         (when f (.open f target))))}
                          attrs)]
       (into [:button base] children))

     :cljs
     (let [classes (util/conj-classes ["command-trigger"] class)
           base    (merge {:class classes :type "button" :aria-haspopup "dialog"
                           :data-command-target target
                           :on {:click (fn [_]
                                         (when-let [f (aget js/window "__uiCommand")]
                                           (.open f target)))}}
                          attrs)]
       (into [:button base] children))

     :clj
     (let [classes (cond-> "command-trigger" class (str " " class))
           base    (merge {:class classes :type "button" :aria-haspopup "dialog"
                           :data-command-target target
                           :onclick "window.__uiCommand&&window.__uiCommand.open(this.dataset.commandTarget)"}
                          attrs)]
       (into [:button base] children))))
