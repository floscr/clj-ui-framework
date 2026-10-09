# Select

> A compact choice field with a framework-drawn listbox, keyboard navigation and form support.

- Type: Component (inputs, Selection)
- Namespace: `ui.select`
- Source: `src/ui/select.cljc`
- Public fns: `normalize-options`, `selected-label`, `select`
- JS runtime: `ui.js.select` (bundled in ui-runtime.js)
- Keywords: select, dropdown, listbox, combobox, choice field

## When to use

- One of many options (more than about five) in a form or a toolbar.
- A choice that must look the same in every browser and both themes.

## When not to use

- Use form-radio-group for two to five visible options.
- Use command for a searchable list of actions or a long list that needs filtering.
- Use button-group to switch between views.

## Usage

```clojure
(require '[ui.select :as select])

(select/select {:options [{:value "eu" :label "Europe"}
                          {:value "us" :label "United States"}]
                :value region
                :placeholder "Pick a region"
                :on-change set-region!})
```

## Examples

### Hiccup form field (uncontrolled, submits with the form)

```clojure
(select/select {:name "region" :options ["Europe" "United States"] :value "Europe"})
```

## API reference

### `select`

Renders the trigger button; the listbox is created by the __uiSelect runtime on open.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:options` | `vector of {:value :label} or strings` | – | The choices. Bare strings use the string as value and label. |
| `:value` | `string` | – | Selected value. |
| `:placeholder` | `string` | – | Shown when nothing is selected. |
| `:disabled` | `boolean` | – |  |
| `:on-change` | `fn [value]` | – | Receives the picked value (:cljs and :squint). |
| `:name` | `string` | – | On :clj emits a hidden <input> so the value submits with a form. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `normalize-options`

Coerces strings into {:value :label} maps.

### `selected-label`

Label for the selected value, else the placeholder.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Enter / Space / ArrowDown | Opens the listbox. |
| ArrowUp / ArrowDown | Moves the highlighted option. |
| Enter | Picks the highlighted option. |
| Escape | Closes without changing. |

## Accessibility

- The trigger has role=combobox with aria-haspopup=listbox and aria-expanded.
- Options are focusable rows; the selected one is marked.

## Motion

- The listbox fades and scales in over --duration-fast.

## Responsive behavior

- The listbox is capped to the viewport and scrolls; on touch the outside tap that dismisses it is consumed.

## Targets

- **Hiccup (`:clj`)**: Uncontrolled; pass :name for form submission. Needs ui-runtime.js.
- **Replicant (`:cljs`)**: Controlled through :value and :on-change.
- **Squint (`:squint`)**: Controlled; the trigger re-renders with the new label.

## Notes for AI

- form-select in ui.form delegates here; use it inside form-field for a label and hint.
- Keep option labels short; put the long explanation in a hint.

## Related

- [Form](form.md): Labeled fields, inputs, textareas, checkboxes, radios, file and range inputs, joined groups and a search bar.
- [Command palette](command.md): A searchable command menu in a native dialog with groups, icons, shortcut hints, a global hotkey and Alt quick-select.
- [Popover](popover.md): A small anchored panel for contextual controls, built on the native Popover API so it works without app state.
- [Tag input](tag-input.md): Turns short text into removable tags with a filtered suggestion dropdown; fully controlled by the host.

## Guidance for AI tools

Select: A compact choice field with a framework-drawn listbox, keyboard navigation and form support. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
