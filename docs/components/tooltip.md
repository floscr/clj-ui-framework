# Tooltip

> A hint or a rich read-only preview on hover and focus: the first waits for its delay, moving to the next opens it instantly.

- Type: Component (disclosure, Overlays)
- Namespace: `ui.tooltip`
- Source: `src/ui/tooltip.cljc`
- Public fns: `tooltip-data-attrs`, `tooltip-label`, `tooltip`, `tooltip-card`
- JS runtime: `ui.js.tooltip` (bundled in ui-runtime.js)
- Keywords: tooltip, hint, hover text, hover card, preview, shortcut, title

## When to use

- A one-line label for an icon-only control, with its keyboard shortcut.
- Dense toolbars where moving between icons should show labels at once after the first.
- A read-only preview behind a mention, link, status or number: a person, a repo, a job's progress, a trend.

## When not to use

- Essential information belongs in visible text; tooltips hide it from touch users.
- Use popover for anything with buttons, links or fields inside.
- Use form-field :hint for guidance under an input.

## Usage

```clojure
(require '[ui.tooltip :as tooltip])

(tooltip/tooltip {:text "Copy link" :shortcut ["⌘" "C"]}
  (button/button {:icon :link :variant :ghost :attrs {:aria-label "Copy link"}}))
```

## Examples

### Rich preview

```clojure
(tooltip/tooltip {:content (tooltip/tooltip-card {}
                             (list-row/list-row {:icon :calendar :title "Design review"
                                                 :description "Wed, 14:00 to 14:45"}))
                  :delay 300
                  :attrs {:tabindex 0}}
  "design review")
```

### One delay for a group

```clojure
[:div {:data-tooltip-delay "250"}
 (toolbar/toolbar {} ...)]
```

## API reference

### `tooltip`

Wraps the trigger in a span that carries the tooltip in a template; ui.js.tooltip shows it.

Children: The trigger. Non-interactive text needs :attrs {:tabindex 0} to be reachable by keyboard.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:text` | `string` | – | The hint, a few words. |
| `:shortcut` | `vector of strings` | – | Keycaps after the text, e.g. ["⌘" "K"]. |
| `:content` | `hiccup` | – | Rich read-only content instead of :text, usually a tooltip-card. Cards stay open while the pointer travels into them. |
| `:delay` | `ms` | `500` | Wait before the first tooltip opens. data-tooltip-delay on any ancestor sets it for a group. |
| `:side` | `:top \| :bottom \| :left \| :right` | `:top` | Preferred side; flips when there is no room. |
| `:tone` | `:danger` | – | Tints the bubble for error hints. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `tooltip-card`

An 18rem column for rich content; list rows inside sit flush.

Children: ui.* components and text: list-row, metric :flush, progress, day-grid, an image.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `tooltip-label`

The plain body tooltip renders for :text and :shortcut.

### `tooltip-data-attrs`

The data attributes the runtime reads off the trigger wrapper.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Keyboard focus opens the tooltip at once, without the delay. |
| Escape | Closes it until the pointer or focus leaves the trigger. |

## Accessibility

- The shared layer has role="tooltip" and the trigger's control gets aria-describedby while it is open.
- Icon-only controls still need an aria-label; the tooltip describes, it does not name.
- Content is cloned, so keep it read only: focus never moves into the tooltip.

## Motion

- The first tooltip rises 4px from 0.97 scale over --duration-fast, from the point nearest its trigger.
- While one is open, and for 400ms after it closes, the next opens instantly: the layer jumps and swaps content with no transition.
- Reduced motion keeps only the fade.

## Responsive behavior

- Labels wrap at 15rem, cards are 18rem, both capped at the viewport; the side flips when cramped and the layer is clamped to the viewport.
- On touch, a tap toggles the tooltip of a non-interactive trigger (text, badges); buttons and links keep their tap.

## Targets

- **Hiccup (`:clj`)**: Static markup; needs ui-runtime.js on the page.
- **Replicant (`:cljs`)**: Needs ui-runtime.js; :content may hold any hiccup.
- **Squint (`:squint`)**: Needs ui-runtime.js.

## Notes for AI

- Keep :text to a few words, no punctuation at the end.
- Rich content goes in tooltip-card, built from ui.* components; it renders on dark tokens.
- Prefer a visible label when space allows.

## Related

- [Popover](popover.md): A small anchored panel for contextual controls, built on the native Popover API so it works without app state.
- [Button](button.md): A single action: neutral primary, quiet outlined states, a loud filled option, press feedback and a loading spinner.
- [Toolbar](toolbar.md): A pill-shaped container that groups icon buttons with separators, horizontal or vertical, optionally floating in a popover.
- [List row](list-row.md): Records as rows on one surface: a leading icon or thumbnail, title and description, metadata and controls at the end, with hover for rows that lead somewhere.

## Guidance for AI tools

Tooltip: A hint or a rich read-only preview on hover and focus: the first waits for its delay, moving to the next opens it instantly. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
