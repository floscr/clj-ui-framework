# Badge

> A small pill for status, category or a count, with an optional leading icon.

- Type: Component (data, Status)
- Namespace: `ui.badge`
- Source: `src/ui/badge.cljc`
- Public fns: `badge-class-list`, `badge-classes`, `badge`
- Keywords: status, tag, label, pill, count

## When to use

- Status next to a record: Active, Pending, Failed (use the matching tone).
- A category or type marker in a list or table cell.
- Emphasis beside a heading: Recommended, New, Beta (use :primary).

## When not to use

- Use chip for a selectable or removable token (filters, tag inputs).
- Use alert for a message with a sentence of text and an action.
- Do not use status tones for decoration; green means success, red means failure.

## Usage

```clojure
(require '[ui.badge :as badge])

(badge/badge {:variant :success :icon-name :check} "Verified")
```

## Examples

### Outline and compact

```clojure
(badge/badge {:variant :outline :size :sm} "Draft")
```

### Count

```clojure
(badge/badge {:variant :secondary} "12")
```

## API reference

### `badge`

Renders an inline <span> pill.

Children: The label text.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:variant` | `:primary \| :secondary \| :outline \| :success \| :warning \| :danger` | `:primary` | Tone. :primary uses the accent and is for emphasis; the status tones are for real status. |
| `:icon-name` | `icon keyword` | – | Optional leading icon at the small size. |
| `:size` | `:sm` | – | Compact variant. |
| `:class` | `string` | – | Extra classes. |
| `:attrs` | `map` | – | Extra HTML attributes. |

### `badge-classes`

Class string for {:variant :size}.

### `badge-class-list`

Vector form of badge-classes.

## Accessibility

- A badge is plain text; the tone must never be the only carrier of meaning, so the label says the status.
- Decorative icons inside a badge are aria-hidden by the icon component.

## Motion

- None.

## Responsive behavior

- Text does not wrap; keep labels to one or two words.

## Targets

- **Hiccup (`:clj`)**: Static markup.
- **Replicant (`:cljs`)**: :class may be a vector.
- **Squint (`:squint`)**: Static markup; keywords are strings.

## Notes for AI

- Pair every status badge with the word for the status; never a colored dot alone.
- Use :primary only for emphasis (Recommended, New), not for a status.

## Related

- [Chip](chip.md): A small selectable pill for filters, tags and presets, with an optional color dot.
- [Alert](alert.md): A persistent inline message with a tone, icon and optional title that helps people recover or continue.
- [Icon](icon.md): Inline Lucide-style SVG icons in four sizes, outlined by default with filled variants for media controls.

## Guidance for AI tools

Badge: A small pill for status, category or a count, with an optional leading icon. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
