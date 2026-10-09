# Number field

> A bounded numeric input with increment and decrement steppers and scroll-wheel support.

- Type: Component (inputs, Text fields)
- Namespace: `ui.number-field`
- Source: `src/ui/number_field.cljc`
- Public fns: `normalize-variant`, `clamp-value`, `step-value`, `number-field-class-list`, `number-field-classes`, `number-field`
- JS runtime: `ui.js.number-field` (bundled in ui-runtime.js)
- Keywords: number input, stepper, numeric field, spinner, quantity

## When to use

- Bounded numbers that people nudge: quantity, hours, font size, zoom.
- Values with a natural step where keyboard entry should still work.

## When not to use

- Use form-input :type :text with a pattern for ids, codes and phone numbers.
- Use form-range when the exact number matters less than the position.

## Usage

```clojure
(require '[ui.number-field :as number-field])

(number-field/number-field {:value 2 :min 1 :max 10 :step 1
                            :on-change #(set-qty! (.. % -target -value))})
```

## Examples

### Variants

```clojure
(number-field/number-field {:value 16 :variant :right :size :sm})
(number-field/number-field {:value 1.5 :step 0.5 :variant :spinner})
```

## API reference

### `number-field`

Wrapper with an <input type=number> and stepper buttons. The number-field runtime clamps steppers and wheel changes and fires a native input event.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:value` | `number` | – | Current value. |
| `:min` | `number` | – |  |
| `:max` | `number` | – |  |
| `:step` | `number` | `1` |  |
| `:size` | `:sm \| :md \| :lg` | `:md` |  |
| `:variant` | `:default \| :right \| :spinner` | `:default` | Steppers flanking the input, both on the right, or a detached up and down box. |
| `:disabled` | `boolean` | – |  |
| `:placeholder` | `string` | – |  |
| `:name` | `string` | – | Forwarded to the input. |
| `:id` | `string` | – | Forwarded to the input. |
| `:on-change` | `fn [event]` | – | :cljs and :squint; fires for typing, steppers and wheel. |
| `:class` | `string` | – | On the wrapper. |
| `:attrs` | `map` | – | On the wrapper. |
| `:input-attrs` | `map` | – | On the <input>. |

### `clamp-value`

Clamps a number into [min max].

### `step-value`

Steps a value by {:step :min :max} in a direction.

### `number-field-classes`

Class string for {:size :variant :disabled}.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| ArrowUp / ArrowDown | Step by :step (native). |
| Scroll wheel over the field | Steps the value (runtime). |

## Accessibility

- Native number input; steppers have aria-labels. Pair with form-field for a visible label.

## Motion

- Stepper hover tint over --duration-fast.

## Responsive behavior

- Height follows :size; steppers are at least 36px targets.

## Targets

- **Hiccup (`:clj`)**: Uncontrolled: submit the input with a form. Needs ui-runtime.js for the steppers.
- **Replicant (`:cljs`)**: Pass :value and :on-change; read the value from the event target.
- **Squint (`:squint`)**: Same as Replicant; require ui.js.number-field in the SPA.

## Notes for AI

- Wrap in form-field for the label; do not add a second stepper UI.

## Related

- [Form](form.md): Labeled fields, inputs, textareas, checkboxes, radios, file and range inputs, joined groups and a search bar.
- [Select](select.md): A compact choice field with a framework-drawn listbox, keyboard navigation and form support.

## Guidance for AI tools

Number field: A bounded numeric input with increment and decrement steppers and scroll-wheel support. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
