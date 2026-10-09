# Empty state

> A centered placeholder with an optional icon for lists and views that have nothing to show yet.

- Type: Component (feedback, Status)
- Namespace: `ui.empty-state`
- Source: `src/ui/empty_state.cljc`
- Public fns: `empty-state-class-list`, `empty-state-classes`, `empty-state`
- Keywords: empty state, no results, placeholder, zero state

## When to use

- A list, search or filter returned nothing.
- A feature not set up yet, with one next step.

## When not to use

- Use skeleton while content is still loading.
- Use alert for an error that prevented loading.

## Usage

```clojure
(require '[ui.empty-state :as empty-state])

(empty-state/empty-state {:icon :inbox}
  [:p "No projects yet"]
  [:p.text-muted "Projects you create or join appear here."]
  (button/button {:variant :primary :icon-left :plus} "Create project"))
```

## API reference

### `empty-state`

A centered column with muted text and an optional large icon above.

Children: Title, description and at most one action.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:icon` | `icon keyword` | – | Rendered at :xl above the content. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `empty-state-classes`

Class string.

### `empty-state-class-list`

Vector form.

## Accessibility

- Say why it is empty and what to do next in text; the icon is decorative.

## Motion

- None.

## Responsive behavior

- Fills its region with generous padding; keep copy to two short lines.

## Targets

- **Hiccup (`:clj`)**: Static markup.
- **Replicant (`:cljs`)**: :class may be a vector.
- **Squint (`:squint`)**: Static markup.

## Notes for AI

- Title: No projects yet. Description: why and when something appears. Action: a verb plus object.
- No results from a search gets a Clear filters action, not a Create action.

## Related

- [Skeleton](skeleton.md): Reserves space with a pulsing placeholder while content loads.
- [Alert](alert.md): A persistent inline message with a tone, icon and optional title that helps people recover or continue.
- [Button](button.md): A single action with quiet secondary states, press feedback and a loading spinner.

## Guidance for AI tools

Empty state: A centered placeholder with an optional icon for lists and views that have nothing to show yet. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
