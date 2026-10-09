# Form

> Labeled fields, inputs, textareas, checkboxes, radios, file and range inputs, joined groups and a search bar.

- Type: Component (inputs, Text fields)
- Namespace: `ui.form`
- Source: `src/ui/form.cljc`
- Public fns: `form-field-class-list`, `form-field-classes`, `form-field`, `form-input-class-list`, `form-input-classes`, `form-input`, `form-textarea-class-list`, `form-textarea-classes`, `form-textarea`, `form-textarea-auto`, `form-select`, `form-checkbox`, `form-radio-group`, `form-file`, `form-range`, `form-group`, `form-group-addon`, `search-bar`
- Keywords: input, textarea, form field, label, checkbox, radio, file input, range, search bar, validation

## When to use

- Any text, number, date or file entry; wrap each control in form-field for its label, hint and error.
- Inline validation: pass :error on both form-field and the input.
- A search entry with a joined button (search-bar) at the top of a list.

## When not to use

- Use switch for an on or off setting that applies immediately.
- Use select (or form-select) for one of many; use form-radio-group for two to five visible options.
- Use number-field for bounded numbers with steppers, tag-input for several values, color-picker for colors.

## Usage

```clojure
(require '[ui.form :as form])

(form/form-field {:label "Email" :hint "Used for sign-in and notifications." :error email-error}
  (form/form-input {:type :email :value email :error (some? email-error) :on-change set-email!}))
```

## Examples

### Search bar that submits as GET

```clojure
(form/search-bar {:action "/search" :placeholder "Search movies…" :button-label "Search"})
```

### Radio group

```clojure
(form/form-radio-group {:label "Preference" :radio-name "pref" :radio-value "a"
                        :options [{:value "a" :label "Option A"} {:value "b" :label "Option B"}]})
```

### Joined input and addon

```clojure
(form/form-group {}
  (form/form-group-addon {} "https://")
  (form/form-input {:placeholder "example.com"}))
```

## API reference

### `form-field`

Wrapper with label above, hint or error below. The error text also sets the error state.

Children: The control.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:label` | `string` | – |  |
| `:hint` | `string` | – |  |
| `:error` | `string` | – | Error message; shown in place of the hint. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `form-input`

Single-line input. With :icon-left or :icon-right it is wrapped in .form-input-wrap.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:type` | `keyword` | `:text` | :text, :email, :password, :date, :datetime-local, ... |
| `:placeholder` | `string` | – |  |
| `:value` | `string` | – |  |
| `:disabled` | `boolean` | – |  |
| `:error` | `boolean` | – | Error styling. |
| `:icon-left` | `icon keyword` | – |  |
| `:icon-right` | `icon keyword` | – |  |
| `:on-change` | `fn` | – | :cljs and :squint only. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – | name, required, autocomplete, pattern and so on. |

### `form-textarea`

Multi-line input.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:placeholder` | `string` | – |  |
| `:value` | `string` | – |  |
| `:disabled` | `boolean` | – |  |
| `:error` | `boolean` | – |  |
| `:on-change` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `form-textarea-auto`

Textarea that grows with its content up to :max-rows.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:placeholder` | `string` | – |  |
| `:value` | `string` | – |  |
| `:disabled` | `boolean` | – |  |
| `:error` | `boolean` | – |  |
| `:max-rows` | `int` | – | Growth cap in rows. |
| `:on-change` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `form-select`

Delegates to ui.select with form styling.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:options` | `vector` | – |  |
| `:placeholder` | `string` | – |  |
| `:value` | `string` | – |  |
| `:disabled` | `boolean` | – |  |
| `:on-change` | `fn [value]` | – |  |
| `:name` | `string` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `form-checkbox`

Checkbox with its label.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:label` | `string` | – |  |
| `:checked` | `boolean` | – |  |
| `:disabled` | `boolean` | – |  |
| `:on-change` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `form-radio-group`

Fieldset of radios with a legend.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:label` | `string` | – | Legend. |
| `:radio-name` | `string` | – | Shared name of the radios. |
| `:options` | `vector of {:value :label} or strings` | – |  |
| `:radio-value` | `string` | – | Selected value. |
| `:disabled` | `boolean` | – |  |
| `:on-change` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `form-file`

File input with a styled chooser button.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:accept` | `string` | – |  |
| `:multiple` | `boolean` | – |  |
| `:disabled` | `boolean` | – |  |
| `:on-change` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `form-range`

Native range slider with an accent thumb.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:min` | `number` | – |  |
| `:max` | `number` | – |  |
| `:step` | `number` | – |  |
| `:value` | `number` | – |  |
| `:disabled` | `boolean` | – |  |
| `:on-change` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `form-group`

Joins inputs, addons and buttons into one bordered row with shared corners.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `form-group-addon`

Static text cell inside a form-group.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `search-bar`

Preset: icon input joined to a primary button; wrapped in a GET <form> when :action or :on-submit is set.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:name` | `string` | `"q"` |  |
| `:value` | `string` | – |  |
| `:placeholder` | `string` | `"Search…"` |  |
| `:icon` | `icon keyword` | `:search` |  |
| `:button-label` | `string` | `"Search"` |  |
| `:disabled` | `boolean` | – |  |
| `:action` | `string` | – | Form action URL. |
| `:on-change` | `fn` | – |  |
| `:on-submit` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |
| `:form-attrs` | `map` | – | Attributes on the wrapping <form>. |

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Moves between fields. |
| Space | Toggles a checkbox or radio. |
| Arrow keys | Move within a radio group or a range. |

## Accessibility

- form-field renders a real <label>; pass an id via :attrs on the control and the label picks it up through nesting.
- Errors sit next to the field and say how to fix it; do not rely on the red border alone.
- Inputs are 44px tall by default, the phone tap minimum.

## Motion

- Border and background transitions over --duration-fast. Focus shows the shared ring.

## Responsive behavior

- Fields fill their column; forms read best at 640 to 720px wide. form-group stays on one line and scrolls its content.

## Targets

- **Hiccup (`:clj`)**: Uncontrolled inputs that submit with a <form>; use :attrs for name and required.
- **Replicant (`:cljs`)**: Controlled via :value and :on-change (receives the DOM event).
- **Squint (`:squint`)**: Controlled; never pass false to :disabled or :required (use (when x true)).

## Notes for AI

- One form-field per control; the label is the only heading a field needs.
- Use search-bar for the search pattern instead of assembling form-group by hand; the joined corners are easy to get wrong.
- Submit buttons are verb plus object: Save changes, Create project.

## Related

- [Select](select.md): A compact choice field with a framework-drawn listbox, keyboard navigation and form support.
- [Switch](switch.md): A toggle for a setting that takes effect immediately.
- `number-field`
- `tag-input`
- [Button](button.md): A single action with quiet secondary states, press feedback and a loading spinner.

## Guidance for AI tools

Form: Labeled fields, inputs, textareas, checkboxes, radios, file and range inputs, joined groups and a search bar. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
