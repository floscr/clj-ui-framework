# Icon

> Inline Lucide-style SVG icons in four sizes, outlined by default with filled variants for media controls.

- Type: Component (data, Primitives)
- Namespace: `ui.icon`
- Source: `src/ui/icon.cljc`
- Public fns: `icon-class-list`, `icon-classes`, `icon`
- Keywords: icon, svg, lucide, filled icon, icon size

## When to use

- A plain icon beside its label in buttons, menus, nav items and empty states.
- Icon-only controls with an aria-label.

## When not to use

- Do not put icons in rounded tiles or circles unless the shape is the control itself.
- Use a text label when it is clearer than an icon, especially for destructive or unfamiliar actions.

## Usage

```clojure
(require '[ui.icon :as icon])

(icon/icon {:icon-name :search :size :sm})
(icon/icon {:icon-name :play :filled true})
```

## API reference

### `icon`

Returns an inline <svg> with currentColor strokes, or nil for an unknown name. See docs/components.md for the full icon list.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:icon-name` | `keyword` | – | One of the registered names, e.g. :home, :search, :settings. |
| `:size` | `:sm \| :md \| :lg \| :xl` | `:md` | 16, 20, 24 or 32px. |
| `:filled` | `boolean` | – | Solid variant; falls back to the outline paths with fill when no dedicated filled shape exists. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – | Extra SVG attributes. |

### `icon-classes`

Class string for {:size}.

### `icon-class-list`

Vector form.

## Accessibility

- Icons are aria-hidden by default; the control they sit in carries the name.

## Motion

- None.

## Responsive behavior

- Sizes are fixed; use :sm inside small buttons and :md in nav.

## Targets

- **Hiccup (`:clj`)**: Static markup.
- **Replicant (`:cljs`)**: Static markup.
- **Squint (`:squint`)**: Icon names are strings at runtime.

## Notes for AI

- Add new icons to icon-paths in src/ui/icon.cljc (Lucide 24x24 paths), then run bb gen-docs.
- Pick :sm for 16px toolbars, :md for menus, :xl only in empty states.

## Related

- [Button](button.md): A single action with quiet secondary states, press feedback and a loading spinner.
- [Empty state](empty-state.md): A centered placeholder with an optional icon for lists and views that have nothing to show yet.
- [Badge](badge.md): A small pill for status, category or a count, with an optional leading icon.

## Guidance for AI tools

Icon: Inline Lucide-style SVG icons in four sizes, outlined by default with filled variants for media controls. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
