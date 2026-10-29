# Toolbar

> A pill-shaped container that groups icon buttons with separators, horizontal or vertical, optionally floating in a popover.

- Type: Component (layout, Layout)
- Namespace: `ui.toolbar`
- Source: `src/ui/toolbar.cljc`
- Public fns: `toolbar-class-list`, `toolbar-classes`, `toolbar`, `toolbar-separator`
- Keywords: toolbar, floating toolbar, icon buttons, editor toolbar

## When to use

- A cluster of independent icon actions that should read as one surface: formatting, zoom, map controls.
- A floating toolbar anchored to a selection or trigger (inside a popover-content with popover-content--flush).

## When not to use

- Use button-group when the buttons are a choice (one active).
- Use a header with button for a page's primary actions.

## Usage

```clojure
(require '[ui.toolbar :as toolbar])

(toolbar/toolbar {}
  (button/button {:variant :ghost :icon :edit :attrs {:aria-label "Edit"}})
  (button/button {:variant :ghost :icon :copy :attrs {:aria-label "Copy"}})
  (toolbar/toolbar-separator {})
  (button/button {:variant :ghost :icon :trash :attrs {:aria-label "Delete"}}))
```

## Examples

### Vertical zoom controls

```clojure
(toolbar/toolbar {:orientation :vertical}
  (button/button {:variant :ghost :icon :plus :attrs {:aria-label "Zoom in"}})
  (button/button {:variant :ghost :icon :minus :attrs {:aria-label "Zoom out"}})
  (toolbar/toolbar-separator {})
  (button/button {:variant :ghost :icon :search :attrs {:aria-label "Fit"}}))
```

## API reference

### `toolbar`

The container (role=toolbar) with a pill radius and a border; buttons inside are clipped to the pill.

Children: Buttons and toolbar-separator elements.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:orientation` | `:horizontal \| :vertical` | `:horizontal` |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – | Add aria-label to name the toolbar. |

### `toolbar-separator`

Divider perpendicular to the toolbar flow.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `toolbar-classes`

Class string for {:orientation}.

### `toolbar-class-list`

Vector form.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Moves through the buttons. |

## Accessibility

- role=toolbar with aria-orientation; every icon button needs an aria-label.

## Motion

- None on the container; buttons keep their press feedback.

## Responsive behavior

- Horizontal toolbars do not wrap; split long ones with separators or move overflow into a context menu.

## Targets

- **Hiccup (`:clj`)**: Static markup.
- **Replicant (`:cljs`)**: :class may be a vector.
- **Squint (`:squint`)**: Static markup.

## Notes for AI

- Use :variant :ghost buttons inside; the toolbar provides the surface.

## Related

- [Button](button.md): A single action: neutral primary, quiet outlined states, a loud filled option, press feedback and a loading spinner.
- [Button group](button-group.md): Related actions as one segmented control: ghost or boxed, with icon items, counts and separators.
- [Popover](popover.md): A small anchored panel for contextual controls, built on the native Popover API so it works without app state.
- [Separator](separator.md): A hairline divider, horizontal or vertical, for grouping content.

## Guidance for AI tools

Toolbar: A pill-shaped container that groups icon buttons with separators, horizontal or vertical, optionally floating in a popover. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
