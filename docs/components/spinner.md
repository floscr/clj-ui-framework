# Spinner

> An indeterminate loading indicator in four sizes that inherits the current text color.

- Type: Component (feedback, Status)
- Namespace: `ui.spinner`
- Source: `src/ui/spinner.cljc`
- Public fns: `spinner-class-list`, `spinner-classes`, `spinner`
- Keywords: spinner, loading, indeterminate, busy

## When to use

- Short waits with no layout to reserve: inside a button, next to a label, in a toolbar.
- Unknown duration.

## When not to use

- Use skeleton to reserve space for content that is loading.
- Use progress when the completion is known.
- Prefer button :loading over placing a spinner next to a button.

## Usage

```clojure
(require '[ui.spinner :as spinner])

(spinner/spinner {:size :sm})
```

## API reference

### `spinner`

A <span> with role=status and a Loading label; the ring uses currentColor.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:size` | `:xs \| :sm \| :md \| :lg` | `:md` |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – | Override aria-label with something specific. |

### `spinner-classes`

Class string for {:size}.

### `spinner-class-list`

Vector form.

## Accessibility

- Announces Loading once via role=status; pass a specific aria-label when several spinners are on screen.

## Motion

- Spins continuously; reduced motion stops the rotation (the ring stays visible).

## Responsive behavior

- Sizes are fixed; :sm fits inside a button, :lg centers in an empty region.

## Targets

- **Hiccup (`:clj`)**: Static markup.
- **Replicant (`:cljs`)**: :class may be a vector.
- **Squint (`:squint`)**: Static markup.

## Notes for AI

- Inside a button use the button's :loading prop; it picks the right size and color.

## Related

- [Skeleton](skeleton.md): Reserves space with a pulsing placeholder while content loads.
- [Progress](progress.md): Shows how much of a known task is complete as a filled track.
- [Button](button.md): A single action: neutral primary, quiet outlined states, a loud filled option, press feedback and a loading spinner.

## Guidance for AI tools

Spinner: An indeterminate loading indicator in four sizes that inherits the current text color. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
