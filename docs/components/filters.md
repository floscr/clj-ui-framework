# Filters

> The parts of a filter view: a surface of labeled bands, a preset bar that saves where the change is, rules read as sentences with a verb in the color of their kind, rows for a table's columns, pressed toggles for display options, and a one-line summary that opens it all.

- Type: Component (data, Tables)
- Namespace: `ui.filters`
- Source: `src/ui/filters.cljc`
- Public fns: `filters-class-list`, `filters-classes`, `filters`, `filter-band`, `filter-split`, `filter-column`, `filter-footer`, `filter-action`, `filter-spacer`, `filter-disclosure`, `preset-bar`, `preset-class-list`, `preset-classes`, `preset`, `preset-add`, `preset-more`, `rule-list`, `rule-class-list`, `rule-classes`, `rule-verb-class-list`, `rule`, `rule-value`, `rule-text`, `rule-add`, `rule-empty`, `filter-toggle-class-list`, `filter-toggle-classes`, `filter-toggle`
- JS runtime: `ui.js.context-menu`, `ui.js.gestures` (bundled in ui-runtime.js)
- Keywords: filter, filter bar, view settings, preset, saved view, rules, sort, group, columns, display options, query builder, customize view

## When to use

- A list or table whose view can be changed: sorted, grouped, narrowed, with columns or details switched on and off.
- Saved views (presets) of such a list, where the changed preset offers Save and Revert in place.
- A filter that should stay out of the way: filter-disclosure shows it as one line of words and opens the choices.

## When not to use

- Use settings for app preferences that are not about one list.
- Use chip alone for a few independent facet filters with nothing to save.
- Use button-group with :variant :boxed to switch between whole views or modes.
- Use tabs when the choices lead to different content, not a different view of the same list.

## Usage

```clojure
(require '[ui.filters :as filters])

(filters/filters {}
  (filters/filter-band {}
    (filters/preset-bar {}
      (filters/preset {:on-click #(pick! :newest)} "Newest")
      (filters/preset {:active true :dirty true
                       :on-click #(pick! :jq) :on-save save! :on-revert revert!}
        "JQ")
      (filters/preset-add {:on-click save-as-new!})))
  (filters/filter-band {:label "Rules"}
    (filters/rule-list {}
      (filters/rule {:verb "Sort by" :icon :arrow-up-down :tone :warning :on-remove remove! :on-toggle toggle!}
        (filters/rule-value {:on-click pick-field!} "Played")
        (filters/rule-value {:icon :arrow-down :on-click flip!} "Newest first"))
      (filters/rule {:verb "Hide" :icon :eye-off :tone :danger :on-verb hide-or-keep! :on-remove remove! :on-toggle toggle!
                     :menu [{:label "Duplicate" :icon :copy :on-click duplicate!}
                            {:label "Delete" :icon :trash :variant :danger :on-click remove!}]}
        "where"
        (filters/rule-value {:on-click pick-field!} "Title")
        (filters/rule-value {:on-click pick-match!} "contains")
        (filters/rule-text {:value "live" :on-input set-text!}))
      (filters/rule-add {:hint "Sort, hide, keep only" :on-click add-rule!})))
  (filters/filter-band {:label "Show"}
    (filters/filter-toggle {:icon :image :on covers? :on-click toggle-covers!} "Covers")))
```

## Examples

### A table's columns: plain rows of the same list, the switch shows or hides one, the grip reorders

```clojure
(filters/filter-band {:label "Columns"}
  (filters/rule-list {}
    (filters/rule {:verb "Title" :on-toggle hide-title! :on-remove remove-title!
                   :menu [{:label "Move down" :icon :arrow-down :on-click move-title-down!}]})
    (filters/rule {:verb "Plays" :off true :on-toggle show-plays! :on-remove remove-plays!})
    (filters/rule-add {:on-click add-column-menu!} "Add column")))
```

### A server-rendered filter behind its summary, each choice an htmx request

```clojure
(filters/filter-disclosure {:summary "15+ comments · unseen only" :summary-attrs {:id "filter-sum"}}
  (filters/filters {:bare true :attrs {:id "filter-opts"}}
    (filters/filter-band {:label "Comments"}
      (filters/filter-toggle {:attrs {:hx-get "/list?c=0"}} "Any")
      (filters/filter-toggle {:on true :attrs {:hx-get "/list?c=15"}} "15+"))))
```

### An option that needs a value: the control sits inside the pressed toggle

```clojure
(filters/filter-toggle {:icon :file-text :on true
                        :value (select/select {:options lengths :value "sentence" :on-change set-length!})}
  "Body text")
```

### The footer: a status and text actions

```clojure
(filters/filter-footer {}
  "Own view for “Recently added”"
  (filters/filter-action {:disabled (not dirty?) :on-click update!} "Update")
  (filters/filter-spacer)
  (filters/filter-action {:on-click clear!} "Clear rules"))
```

## API reference

### `filters`

The surface: bands stacked with hairlines. A size container, so bands put their label on top when it is narrower than 520px.

Children: filter-band, filter-split and filter-footer elements.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:bare` | `boolean` | – | No border and background, for a filter inside a column, a popover or a filter-disclosure. |
| `:dense` | `boolean` | – | Packed bands: less padding, no hairlines, labels stay beside their controls on a narrow surface. For a few bands of toggles in a narrow column. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `filter-band`

One band: a short label in the left gutter, the controls wrapping next to it.

Children: The controls: a search field, chips, a rule-list, filter-toggles.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:label` | `string` | – | One or two words. Without it the controls take the full width on one line (the preset bar). |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `filter-split`

Bands in columns: each filter-column child is a column on a surface at least 640px wide, with a hairline between them; narrower, they stack.

Children: filter-column elements.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `filter-column`

One column of a filter-split. A size container of its own, so its bands put their label on top when the column is under 520px.

Children: filter-band elements.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `filter-disclosure`

A <details>: one line that says the filter in words and opens its children. No JS.

Children: What opens, usually (filters {:bare true} ...).

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:summary` | `string or hiccup` | – | The filter in words. |
| `:icon` | `keyword` | `:filter` | Leading icon. |
| `:open` | `boolean` | – | Start opened. |
| `:summary-attrs` | `map` | – | Attributes of the element holding the words, e.g. an id to swap it out of band. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `preset-bar`

Saved views in one boxed track that scrolls sideways when they do not fit.

Children: preset elements, then an optional preset-more and preset-add.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `preset`

One saved view. Active and :dirty it shows a dot plus Save and Revert inside its own pill.

Children: The label.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:active` | `boolean` | – | The selected preset. |
| `:dirty` | `boolean` | – | The view on screen no longer matches the active preset. |
| `:on-click` | `fn` | – | Picks the preset (:cljs and :squint). |
| `:href` | `string` | – | Renders the preset as a link. |
| `:on-save` | `fn` | – | Saves the changes into the preset. |
| `:on-revert` | `fn` | – | Drops the changes. |
| `:save-label` | `string` | `"Save"` |  |
| `:revert-label` | `string` | `"Revert changes"` | Accessible name of the revert button. |
| `:button-attrs` | `map` | – | Attributes of the preset's button or link (onclick, hx-get). |
| `:save-attrs` | `map` | – | Attributes of the save button; it only renders with these or :on-save. |
| `:revert-attrs` | `map` | – | Attributes of the revert button; it only renders with these or :on-revert. |
| `:menu` | `vector of item maps` | – | Context menu of the preset (right click, long press), items as in context-menu: Delete on a saved one. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – | The wrapper: a key. |

### `preset-add`

Saves the view on screen as a new preset: a plus, or with children a labeled action.

Children: Optional label ("Save as preset").

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:on-click` | `fn` | – |  |
| `:label` | `string` | `"Save as new preset"` | Accessible name and tooltip. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `preset-more`

A chevron at the end of the bar that opens a menu of every preset, for a bar that only shows the few used last. The app opens the menu.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:on-click` | `fn` | – |  |
| `:label` | `string` | `"All presets"` | Accessible name and tooltip. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `rule-list`

Rules as sentences on one surface, in the order they apply.

Children: rule elements, then a rule-add; a rule-empty when there is none.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `rule`

One rule: grip, verb, words, then the switch that pauses it and the remove button, always visible. Without words it is a plain row: a table's column.

Children: The words. A string is a connective ("where", "is"); values are rule-value, rule-text or a select.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:verb` | `string` | – | "Sort by", "Group by", "Hide", "Keep only", or the row's name. |
| `:icon` | `keyword` | – | Icon before the verb. |
| `:tone` | `:accent \| :danger \| :success \| :warning` | – | The color of the rule's kind; the verb becomes a tinted label of one width. Convention: :warning sort, :accent group, :danger hide, :success keep. |
| `:menu` | `vector of item maps` | – | Context menu of the row (right click, long press), items as in context-menu: Duplicate, Turn off, Move up, Move down, Delete. |
| `:off` | `boolean` | – | The rule is paused: dimmed, switch off. |
| `:grip` | `boolean` | `true` | false leaves the drag grip out. |
| `:toggle` | `boolean` | `true` | false leaves the switch out. |
| `:on-verb` | `fn` | – | Makes the verb a button, e.g. to switch Hide and Keep only. |
| `:on-toggle` | `fn` | – | Change handler of the switch. |
| `:on-remove` | `fn` | – | Click handler of the remove button. |
| `:grip-attrs` | `map` | – | Attributes of the grip, e.g. a pointerdown that starts a drag. |
| `:verb-attrs` | `map` | – |  |
| `:toggle-attrs` | `map` | – |  |
| `:remove-attrs` | `map` | – | The remove button only renders with these or :on-remove. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – | A key, drag and drop attributes. |

### `rule-value`

A value pill you press to change it; the app opens its menu from the click.

Children: The label.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:icon` | `keyword` | – | Icon before the label (an arrow for a direction). |
| `:on-click` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `rule-text`

A free text value typed in place; it grows with its content.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:value` | `string` | – |  |
| `:placeholder` | `string` | – |  |
| `:on-input` | `fn` | – | Input handler (:cljs and :squint). |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `rule-add`

The last row of a rule-list: adds a rule.

Children: The label, default "Add rule".

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:on-click` | `fn` | – |  |
| `:hint` | `string` | – | Muted text at the end naming the kinds. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `rule-empty`

What a rule-list says when it has no rule.

Children: The sentence.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `filter-toggle`

A pressed toggle: a display option or one choice of a few.

Children: The label.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:on` | `boolean` | – | Pressed. |
| `:icon` | `keyword` | – | Leading icon. |
| `:value` | `hiccup` | – | A control shown inside the pressed toggle (a select, a rule-value). The toggle is then wrapped in a span. |
| `:on-click` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `filter-footer`

The last line of the surface: a muted status and its text actions.

Children: Strings, filter-action and filter-spacer.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `filter-action`

A text action of the footer.

Children: The label.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:on-click` | `fn` | – |  |
| `:disabled` | `boolean` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `filter-spacer`

Takes the free space of a footer or band, so what follows sits at the end.

### `filters-class-list`

Class vector for {:bare :dense}.

### `filters-classes`

String form.

### `preset-class-list`

Class vector for {:active :dirty}.

### `preset-classes`

String form.

### `rule-class-list`

Class vector for {:off}.

### `rule-classes`

String form.

### `rule-verb-class-list`

Class vector of a rule's verb for {:tone}.

### `filter-toggle-class-list`

Class vector for {:on}.

### `filter-toggle-classes`

String form.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Moves through presets, verbs, values, switches and toggles in reading order. |
| Enter / Space | Picks a preset, opens a value's menu, presses a toggle. |
| Space | On a rule's switch, pauses or resumes the rule. |

## Accessibility

- The active preset has aria-current, toggles have aria-pressed, the rule switch is a real checkbox.
- Dragging is the app's: give the grip a keyboard alternative (arrow keys on a focused grip, or move actions in a menu).
- A rule's kind has a color, but its verb and icon say the same, so the color is never the only cue.
- A row's menu (:menu) repeats what the row's own controls do and adds the rest (duplicate, move up and down), so reordering works without dragging.

## Motion

- Color and background change over --duration-fast; presets and toggles scale to 0.97 while pressed.
- The disclosure chevron turns over --duration-standard.

## Responsive behavior

- The surface is a size container: under 520px of its own width the band labels move above their controls and the rule-add hint goes.
- A filter-split shows its columns side by side from 640px of surface width and stacks them below; each column is measured on its own.
- The preset bar scrolls sideways instead of wrapping.
- On touch the value pills and the save half of a changed preset grow to tap size.

## Targets

- **Hiccup (`:clj`)**: No handlers: act through :attrs, :button-attrs, :save-attrs and the other *-attrs maps (onclick, hx-get, a form's button), or :href on a preset.
- **Replicant (`:cljs`)**: Handlers become {:on {:click f}}. State lives in the app.
- **Squint (`:squint`)**: Flat handlers. Give rules and presets a :key through :attrs when the list changes.

## Notes for AI

- One grammar for every filter view: bands in the order presets, Find, Rules, Columns, Show, then the footer. Leave out the bands a list does not need.
- Sort, group, hide and keep are all rules in one rule-list; do not build a separate sort section.
- When one band is a long list (a table's columns), put it in the second filter-column of a filter-split so the panel does not grow twice as tall on desktop; the preset band and the footer stay outside the split, full width.
- Saving belongs to the preset bar (the dirty preset's Save, preset-add). Do not add a row of save buttons under the panel.
- Use filter-toggle for display options instead of a switch per row.
- A table's columns are a rule-list of plain rules (name as :verb, no words): the switch shows or hides, the grip reorders top to bottom, a rule-add row opens the menu of columns to add. Do not lay columns out as chips that drag sideways.
- Give every kind of rule its :tone (:warning sort, :accent group, :danger hide, :success keep) and every rule a :menu with at least Duplicate (where it makes sense), Turn off / on, Move up, Move down and Delete.
- :menu on rule and preset needs the context menu runtime (ui.js.context-menu, plus ui.js.gestures for long press).
- Band labels are sentence case, one or two words. No uppercase section headings.
- The components are stateless and open no menus: open the app's menu (context menu runtime, select) from rule-value's click, or put a ui.select among the words, which is styled as a value pill there.

## Related

- [Chip](chip.md): A small selectable pill for filters, tags and presets, with an optional color dot.
- [Settings rows](settings.md): Label-and-control rows on one bordered surface with hairline dividers: the shape of every preferences screen.
- [Select](select.md): A compact choice field with a framework-drawn listbox, keyboard navigation and form support.
- [Switch](switch.md): A toggle for a setting that takes effect immediately.
- [Button group](button-group.md): Related actions as one segmented control: ghost or boxed, with icon items, counts and separators.
- [Table](table.md): Records in columns from a headers vector and row vectors, with striped and bordered variants.

## Guidance for AI tools

Filters: The parts of a filter view: a surface of labeled bands, a preset bar that saves where the change is, rules read as sentences with a verb in the color of their kind, rows for a table's columns, pressed toggles for display options, and a one-line summary that opens it all. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
