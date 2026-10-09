# Tooltip

> Short supporting text for an unfamiliar control, shown on hover and focus with pure CSS.

- Type: Component (disclosure, Overlays)
- Namespace: `ui.tooltip`
- Source: `src/ui/tooltip.cljc`
- Public fns: `tooltip`
- Keywords: tooltip, hint, hover text, title

## When to use

- A one-line hint for an icon-only control or an abbreviation.
- Extra context that is helpful but not required to use the control.

## When not to use

- Essential information belongs in visible text; tooltips hide it from touch users.
- Use popover for rich content or anything interactive.
- Use form-field :hint for guidance under an input.

## Usage

```clojure
(require '[ui.tooltip :as tooltip])

(tooltip/tooltip {:text "Copy link"}
  (button/button {:icon :link :variant :ghost :attrs {:aria-label "Copy link"}}))
```

## API reference

### `tooltip`

Wraps children in a span that shows :text above it on hover or focus-within.

Children: The control the hint belongs to.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:text` | `string` | – | The hint. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Focusing the wrapped control shows the tooltip. |

## Accessibility

- The tooltip is a CSS pseudo-element, so it is not read by screen readers; icon-only controls still need an aria-label.
- Hover reveal is gated to hover-capable pointers so touch users do not get a double-tap.

## Motion

- Fades and lifts in over --duration-fast.

## Responsive behavior

- Hidden on touch devices; never place required information in a tooltip.

## Targets

- **Hiccup (`:clj`)**: Static markup, works without JS.
- **Replicant (`:cljs`)**: :class may be a vector.
- **Squint (`:squint`)**: Static markup.

## Notes for AI

- Keep the text to a few words; no punctuation at the end.
- Prefer a visible label when space allows.

## Related

- [Popover](popover.md): A small anchored panel for contextual controls, built on the native Popover API so it works without app state.
- [Button](button.md): A single action with quiet secondary states, press feedback and a loading spinner.
- `icon`

## Guidance for AI tools

Tooltip: Short supporting text for an unfamiliar control, shown on hover and focus with pure CSS. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
