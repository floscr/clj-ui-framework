# Theme toggle

> A three-way segmented control for light, system and dark appearance, hydrated by the JS runtime.

- Type: Component (actions, Buttons)
- Namespace: `ui.theme-toggle`
- Source: `src/ui/theme_toggle.cljc`
- Public fns: `theme-toggle-class-list`, `theme-toggle-classes`, `theme-toggle`
- JS runtime: `ui.js.theme` (bundled in ui-runtime.js)
- Keywords: theme switcher, dark mode, light mode, appearance, color scheme

## When to use

- An appearance setting in a sidebar footer, header or settings page.

## When not to use

- Use switch for a plain two-state setting that is not the color scheme.
- Do not place more than one theme toggle on a page.

## Usage

```clojure
(require '[ui.theme-toggle :as theme-toggle])

(theme-toggle/theme-toggle {:mode "auto"})
```

## Examples

### Controlled in Replicant or Squint

```clojure
(theme-toggle/theme-toggle {:mode theme :on-change set-theme! :size :sm})
```

## API reference

### `theme-toggle`

A radiogroup of three icon buttons (sun, monitor, moon). The runtime module applies data-theme on <html> and persists the choice.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:mode` | `"light" \| "auto" \| "dark"` | `"auto"` |  |
| `:on-change` | `fn [mode]` | – | :cljs and :squint; receives the new mode string. |
| `:size` | `:sm \| :md` | `:md` |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `theme-toggle-classes`

Class string for {:size}.

### `theme-toggle-class-list`

Vector form.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab / Arrow keys | Move between the three options. |
| Enter / Space | Selects. |

## Accessibility

- role=radiogroup labeled Theme; each option has role=radio with aria-checked and a label.

## Motion

- The active pill changes over --duration-fast.

## Responsive behavior

- Fixed size; :sm fits a sidebar footer.

## Targets

- **Hiccup (`:clj`)**: Markup only; ui-runtime.js wires the clicks and persistence.
- **Replicant (`:cljs`)**: Pass :mode and :on-change, or let the runtime handle it.
- **Squint (`:squint`)**: Same as Replicant.

## Notes for AI

- Themes are data-theme on <html>; accents are data-accent. The toggle only changes the theme.

## Related

- [Switch](switch.md): A toggle for a setting that takes effect immediately.
- [Button group](button-group.md): Related actions as one segmented control: ghost or boxed, with icon items, counts and separators.
- [Sidebar](sidebar.md): An app shell: a sticky sidebar with brand, search, grouped menus, collapsible sections, user footer and a mobile drawer, next to the main area.

## Guidance for AI tools

Theme toggle: A three-way segmented control for light, system and dark appearance, hydrated by the JS runtime. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
