# Skeleton

> Reserves space with a pulsing placeholder while content loads.

- Type: Component (feedback, Status)
- Namespace: `ui.skeleton`
- Source: `src/ui/skeleton.cljc`
- Public fns: `skeleton-class-list`, `skeleton-classes`, `skeleton`
- Keywords: skeleton, loading placeholder, shimmer, lazy loading

## When to use

- Loading a region whose final layout is known: a card, a list, an avatar and name.
- First paint of data-driven views so nothing jumps when the data arrives.

## When not to use

- Use spinner for a short action with no layout to reserve, such as a button.
- Use progress when the completion is known.
- Use empty-state when loading finished and there is nothing to show.

## Usage

```clojure
(require '[ui.skeleton :as skeleton])

[:div.vstack
 (skeleton/skeleton {:variant :heading})
 (skeleton/skeleton {:variant :line})
 (skeleton/skeleton {:variant :line :class "w-full"})]
```

## Examples

### Avatar row

```clojure
[:div.hstack
 (skeleton/skeleton {:variant :circle})
 (skeleton/skeleton {:variant :line})]
```

## API reference

### `skeleton`

A pulsing block with role=status and an accessible Loading label.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:variant` | `:line \| :box \| :circle \| :heading` | – | Shape and default size. |
| `:class` | `string` | – | Size it with utilities (w-full) or a custom class. |
| `:attrs` | `map` | – |  |

### `skeleton-classes`

Class string for {:variant}.

### `skeleton-class-list`

Vector form.

## Accessibility

- Announces Loading once; mark the surrounding region aria-busy while skeletons show.

## Motion

- A slow pulse loops while visible; reduced motion stops it.

## Responsive behavior

- Skeletons take the dimensions of the content they stand in for; match the final layout.

## Targets

- **Hiccup (`:clj`)**: Static markup.
- **Replicant (`:cljs`)**: :class may be a vector.
- **Squint (`:squint`)**: Static markup.

## Notes for AI

- Build the skeleton in the final layout with the same dimensions, not a generic block.

## Related

- `spinner`
- [Progress](progress.md): Shows how much of a known task is complete as a filled track.
- `empty-state`

## Guidance for AI tools

Skeleton: Reserves space with a pulsing placeholder while content loads. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
