# Chip

> A small selectable pill for filters, tags and presets, with an optional color dot.

- Type: Component (inputs, Selection)
- Namespace: `ui.chip`
- Source: `src/ui/chip.cljc`
- Public fns: `chip-class-list`, `chip-classes`, `chip`
- Keywords: chip, filter chip, tag, preset, facet

## When to use

- Filtering by a few facets where several can be on at once.
- Presets (theme, size) where the chosen one is highlighted.

## When not to use

- Use badge for a non-interactive status label.
- Use button-group for an exclusive choice between views.
- Use tag-input for free-text tags that are added and removed.

## Usage

```clojure
(require '[ui.chip :as chip])

[:div.hstack.gap-1
 (chip/chip {:active true :on-click #(toggle! :photos)} "Photos")
 (chip/chip {:on-click #(toggle! :videos)} "Videos")]
```

## Examples

### Color presets

```clojure
(chip/chip {:dot-color "var(--accent-500)" :active true} "Purple")
(chip/chip {:dot-color "var(--blue-500)"} "Blue")
```

## API reference

### `chip`

A <button> pill.

Children: The label.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:active` | `boolean` | – | Selected state (accent tint). |
| `:dot-color` | `CSS color` | – | Leading color dot. |
| `:on-click` | `fn` | – | :cljs and :squint only. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – | Add aria-pressed for toggles. |

### `chip-classes`

Class string for {:active}.

### `chip-class-list`

Vector form.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Enter / Space | Toggles the chip. |

## Accessibility

- Add aria-pressed through :attrs so the selected state is announced.

## Motion

- Background, border and color change over --duration-fast.

## Responsive behavior

- Chips wrap in an hstack; each is at least 32px tall, add hit-area for 44px targets.

## Targets

- **Hiccup (`:clj`)**: Use :attrs {:onclick ...} or a form.
- **Replicant (`:cljs`)**: :on-click becomes {:on {:click f}}.
- **Squint (`:squint`)**: Flat :on-click.

## Notes for AI

- Keep the active set in app state; chips are stateless.

## Related

- [Badge](badge.md): A small pill for status, category or a count, with an optional leading icon.
- [Button group](button-group.md): Related actions as one segmented control: ghost or boxed, with icon items, counts and separators.
- [Tag input](tag-input.md): Turns short text into removable tags with a filtered suggestion dropdown; fully controlled by the host.

## Guidance for AI tools

Chip: A small selectable pill for filters, tags and presets, with an optional color dot. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
