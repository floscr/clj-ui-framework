# Button group

> Related actions as one segmented control: ghost or boxed, with icon items, counts and separators.

- Type: Component (actions, Buttons)
- Namespace: `ui.button-group`
- Source: `src/ui/button_group.cljc`
- Public fns: `button-group-class-list`, `button-group-classes`, `button-group-item-class-list`, `button-group-item-classes`, `button-group`, `button-group-item`, `button-group-separator`
- Keywords: button group, segmented control, view switcher, filter, toggle group

## When to use

- Two to five views of the same data: a range (7d, 30d, 90d), a layout (grid, list), a filter with counts.
- Header toolbars with icon toggles and steppers (:ghost).

## When not to use

- Use tabs for peer panels of one object with their own content.
- Use toolbar for icon buttons that are independent actions, not a choice.
- Use select when there are more than five options.

## Usage

```clojure
(require '[ui.button-group :as button-group])

(button-group/button-group {:variant :boxed}
  (button-group/button-group-item {:active true :count 353} "All")
  (button-group/button-group-item {:count 28} "Downloaded")
  (button-group/button-group-item {:count 325} "Missing"))
```

## Examples

### Icon toggles with a separator

```clojure
(button-group/button-group {}
  (button-group/button-group-item {:icon :grid :active true :attrs {:aria-label "Grid"}})
  (button-group/button-group-item {:icon :list :attrs {:aria-label "List"}})
  (button-group/button-group-separator {})
  (button-group/button-group-item {:icon :settings :attrs {:aria-label "Settings"}}))
```

### Navigation segments as links

```clojure
(button-group/button-group {:variant :boxed}
  (button-group/button-group-item {:href "/photos" :active true} "Photos")
  (button-group/button-group-item {:href "/albums"} "Albums"))
```

## API reference

### `button-group`

The container (role=group).

Children: button-group-item and button-group-separator elements.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:variant` | `:ghost \| :boxed` | `:ghost` | :ghost is a tight cluster with a filled active item; :boxed is a track with a raised active segment (concentric radii). |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `button-group-item`

One segment: a <button>, or an <a> when :href is set.

Children: The label; omit for an icon-only item.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:active` | `boolean` | – | Selected state. |
| `:icon` | `icon keyword` | – | Leading icon; icon-only when there are no children. |
| `:count` | `number \| string` | – | Muted trailing count. |
| `:href` | `string` | – | Render as a link. |
| `:on-click` | `fn` | – | :cljs and :squint only. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – | aria-label for icon-only items. |

### `button-group-separator`

Thin divider between sub-clusters.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `button-group-classes`

Class string for {:variant}.

### `button-group-item-classes`

Class string for {:active :icon-only}.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Moves through the items (each is a button or link). |
| Enter / Space | Activates the focused item. |

## Accessibility

- The container has role=group; add aria-label via :attrs to name it.
- Mark the active item with aria-pressed or aria-current via :attrs when it is a toggle or a nav.

## Motion

- Background and color change over --duration-fast.

## Responsive behavior

- Items do not wrap; keep labels short. Counts use tabular numerals.

## Targets

- **Hiccup (`:clj`)**: Use :href for navigation; no handlers.
- **Replicant (`:cljs`)**: :on-click per item; manage :active in state.
- **Squint (`:squint`)**: Same as Replicant.

## Notes for AI

- Exactly one item is :active at a time.
- For a settings-style on or off, use switch instead.

## Related

- [Tabs](tabs.md): Switch between peer panels of one object, with no JS: hidden radios drive the panels.
- [Toolbar](toolbar.md): A pill-shaped container that groups icon buttons with separators, horizontal or vertical, optionally floating in a popover.
- [Button](button.md): A single action with quiet secondary states, press feedback and a loading spinner.
- [Chip](chip.md): A small selectable pill for filters, tags and presets, with an optional color dot.

## Guidance for AI tools

Button group: Related actions as one segmented control: ghost or boxed, with icon items, counts and separators. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
