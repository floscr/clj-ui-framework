# Separator

> A hairline divider, horizontal or vertical, for grouping content.

- Type: Component (layout, Layout)
- Namespace: `ui.separator`
- Source: `src/ui/separator.cljc`
- Public fns: `separator-class-list`, `separator-classes`, `separator`
- Keywords: separator, divider, hr, rule

## When to use

- Dividing groups inside a surface where spacing alone is not enough (menus, sidebars, toolbars).

## When not to use

- Prefer spacing first; a border second; a separator only where the two cannot do the job.
- Inside toolbar or button-group use their own separator components.

## Usage

```clojure
(require '[ui.separator :as separator])

(separator/separator {})
(separator/separator {:orientation :vertical :class "mx-2"})
```

## API reference

### `separator`

A <div role=none> line.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:orientation` | `:horizontal \| :vertical` | `:horizontal` |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `separator-classes`

Class string for {:orientation}.

### `separator-class-list`

Vector form.

## Accessibility

- Decorative (role=none). Use a heading, not a separator, to announce a new section.

## Motion

- None.

## Responsive behavior

- A vertical separator needs a flex row with a defined height.

## Targets

- **Hiccup (`:clj`)**: Static markup.
- **Replicant (`:cljs`)**: :class may be a vector.
- **Squint (`:squint`)**: Static markup.

## Notes for AI

- Uses border-subtle; never draw your own 1px lines.

## Related

- [Toolbar](toolbar.md): A pill-shaped container that groups icon buttons with separators, horizontal or vertical, optionally floating in a popover.
- [Button group](button-group.md): Related actions as one segmented control: ghost or boxed, with icon items, counts and separators.
- [Sidebar](sidebar.md): An app shell: a sticky sidebar with brand, search, grouped menus, collapsible sections, user footer and a mobile drawer, next to the main area.

## Guidance for AI tools

Separator: A hairline divider, horizontal or vertical, for grouping content. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
