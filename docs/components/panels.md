# Resizable panels

> Panel groups whose panes trade space by dragging a separator, with collapse, keyboard resizing and persisted sizes.

- Type: Component (layout, Layout)
- Namespace: `ui.panels`
- Source: `src/ui/panels.cljc`
- Public fns: `size-str`, `group-attrs`, `panel-attrs`, `group`, `separator`, `panel`
- JS runtime: `ui.js.panels` (bundled in ui-runtime.js)
- Keywords: resizable panels, split view, sidebar resize, drag separator, collapsible pane

## When to use

- Two or more panes that trade space: a sidebar beside content, an editor beside a preview.
- Layouts whose pane sizes should persist per user (:persist-key).

## When not to use

- Use sidebar-layout for a fixed-width app sidebar.
- Use dialog or popover for temporary surfaces.

## Usage

```clojure
(require '[ui.panels :as panels])

(panels/group {:orientation :horizontal}
  (panels/panel {:size 240 :min-size 160 :max-size 420 :collapsible true :persist-key "sidebar"}
    sidebar-content)
  (panels/separator {:label "Resize sidebar"})
  (panels/panel {:pin true} main-content))
```

## API reference

### `group`

Flex container for panels.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:orientation` | `:horizontal \| :vertical` | `:horizontal` |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `panel`

A pane. With :size it is resizable; without, it fills the rest.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:size` | `px number \| "NN%"` | – |  |
| `:min-size` | `px` | – |  |
| `:max-size` | `px` | – |  |
| `:default-size` | `px` | – |  |
| `:collapsible` | `boolean` | – |  |
| `:collapsed` | `boolean` | – |  |
| `:persist-key` | `string` | – | localStorage key for size and collapsed state. |
| `:pin` | `boolean` | – | On the fill panel: keep content anchored while a sized panel collapses. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `separator`

Draggable, keyboard-operable seam between two panels.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:label` | `string` | – | aria-label. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `panel-attrs`

Data attributes for a sized panel, for custom markup.

### `group-attrs`

Data attributes for a group.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Arrow keys on the separator | Resize in steps. |
| Enter on the separator | Toggle collapse when collapsible. |

## Accessibility

- The separator has role=separator with an aria-label and aria-valuenow.

## Motion

- Collapse and programmatic size changes animate over --duration-standard; dragging tracks the pointer.

## Responsive behavior

- Percent sizes scale with the group; set :min-size so panes stay usable on phones, or stack vertically.

## Targets

- **Hiccup (`:clj`)**: Markup plus ui-runtime.js; sizes persist in the browser.
- **Replicant (`:cljs`)**: Listen for ui-panels-resize and ui-panels-collapse CustomEvents via :attrs, or drive data-size and data-collapsed.
- **Squint (`:squint`)**: Same as Replicant; require ui.js.panels in the SPA.

## Notes for AI

- Exactly one fill panel per group; give every sized panel a :min-size.

## Related

- [Sidebar](sidebar.md): An app shell: a sticky sidebar with brand, search, grouped menus, collapsible sections, user footer and a mobile drawer, next to the main area.
- [Separator](separator.md): A hairline divider, horizontal or vertical, for grouping content.

## Guidance for AI tools

Resizable panels: Panel groups whose panes trade space by dragging a separator, with collapse, keyboard resizing and persisted sizes. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
