# Alert

> A persistent inline message with a tone, icon and optional title that helps people recover or continue.

- Type: Component (feedback, Messages)
- Namespace: `ui.alert`
- Source: `src/ui/alert.cljc`
- Public fns: `alert-class-list`, `alert-classes`, `alert`
- Keywords: alert, banner, error message, warning, info, inline feedback

## When to use

- An error next to its cause, with how to fix it (We could not save. Check your connection and try again).
- A warning or notice that stays until the situation changes.
- A success state that needs more than a label change, for example after a multi-step import.

## When not to use

- Use toast for brief confirmation of background work.
- Use badge for a one-word status.
- Use form-field :error for a single field's validation message.

## Usage

```clojure
(require '[ui.alert :as alert])

(alert/alert {:variant :danger :title "Could not save"}
  "Check your connection and try again.")
```

## Examples

### Neutral notice without an icon

```clojure
(alert/alert {:icon-name false} "Changes apply after the next deploy.")
```

### Custom icon

```clojure
(alert/alert {:variant :info :icon-name :zap :title "New"} "Keyboard shortcuts are available.")
```

## API reference

### `alert`

Renders a tinted panel with icon, title and body.

Children: The message body.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:variant` | `:success \| :warning \| :danger \| :info \| nil` | `nil (neutral)` | Tone; picks the tint and the default icon. |
| `:title` | `string` | – | Optional bold first line. |
| `:icon-name` | `icon keyword \| false` | – | Override the tone's icon, or false to hide it. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `alert-classes`

Class string for {:variant}.

### `alert-class-list`

Vector form.

## Accessibility

- Tone is never the only signal: the title or body names the situation.
- For messages that appear after an action, add :attrs {:role "status"} (or "alert" for errors) so screen readers announce them.

## Motion

- None; alerts should not shift layout when they appear, reserve the space or place them at the top of the region.

## Responsive behavior

- Full width of its column; the body wraps.

## Targets

- **Hiccup (`:clj`)**: Static markup.
- **Replicant (`:cljs`)**: :class may be a vector.
- **Squint (`:squint`)**: Static markup.

## Notes for AI

- Error copy says what happened and how to fix it; no exclamation marks.
- One alert per region. Do not stack alerts for a single problem.

## Related

- [Toast](toast.md): A brief floating confirmation for completed background work, shown by the JS runtime on every target.
- [Badge](badge.md): A small pill for status, category or a count, with an optional leading icon.
- `empty-state`

## Guidance for AI tools

Alert: A persistent inline message with a tone, icon and optional title that helps people recover or continue. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
