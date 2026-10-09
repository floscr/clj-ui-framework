# Color picker

> A saturation and brightness plane with hue and alpha tracks, a format switch (oklch, hex, rgb, hsl) and a CSS value field, driven by the runtime.

- Type: Component (inputs, Special inputs)
- Namespace: `ui.color-picker`
- Source: `src/ui/color_picker.cljc`
- Public fns: `normalize-formats`, `color-picker-class-list`, `color-picker-classes`, `color-picker`
- JS runtime: `ui.js.color-picker` (bundled in ui-runtime.js)
- Keywords: color picker, oklch, hex, hue, alpha, swatch

## When to use

- Choosing a color in a theme or settings UI where the exact value matters.
- Any place a CSS color string must be produced in a specific format.

## When not to use

- Use chip with :dot-color for a small set of preset colors.
- Use form-input with a pattern when only a hex code is typed.

## Usage

```clojure
(require '[ui.color-picker :as color-picker])

(color-picker/color-picker {:value "oklch(0.6 0.23 286)"
                            :formats [:oklch :hex]
                            :alpha true
                            :on-change #(set-color! (.. % -target -value))})
```

## API reference

### `color-picker`

Data-attributed markup; the color-picker runtime paints the plane and tracks and keeps a hidden input in sync.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:value` | `CSS color string` | – | Any CSS color; converted to the active format. |
| `:formats` | `vector of :oklch :hex :rgb :hsl` | `all` | Offered output formats; one format hides the switch. |
| `:alpha` | `boolean` | `false` | Shows the opacity track. |
| `:disabled` | `boolean` | – |  |
| `:name` | `string` | – | On the hidden value input (form submission). |
| `:id` | `string` | – |  |
| `:on-change` | `fn [event]` | – | :cljs and :squint; the color is event.target.value. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `normalize-formats`

Known formats from a list, deduplicated and ordered.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Arrow keys | Nudge the plane marker and the tracks. |
| Tab | Moves between plane, tracks, format switch and the value field. |

## Accessibility

- Plane and tracks are focusable with labels; the text field accepts any CSS color and shows the exact value.

## Motion

- Marker and track thumbs move instantly while dragging; no easing on input.

## Responsive behavior

- Fills its column with a minimum width around 240px; thumbs are 44px targets.

## Targets

- **Hiccup (`:clj`)**: Submit the hidden input with a form; needs ui-runtime.js.
- **Replicant (`:cljs`)**: Controlled through :value; re-rendering never resets the hue of a gray pick.
- **Squint (`:squint`)**: Same as Replicant; require ui.js.color-picker in the SPA.

## Notes for AI

- Prefer :oklch output for tokens; hex for user-facing fields.

## Related

- [Form](form.md): Labeled fields, inputs, textareas, checkboxes, radios, file and range inputs, joined groups and a search bar.
- [Chip](chip.md): A small selectable pill for filters, tags and presets, with an optional color dot.
- [Number field](number-field.md): A bounded numeric input with increment and decrement steppers and scroll-wheel support.

## Guidance for AI tools

Color picker: A saturation and brightness plane with hue and alpha tracks, a format switch (oklch, hex, rgb, hsl) and a CSS value field, driven by the runtime. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
