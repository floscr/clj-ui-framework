# Toast

> A brief floating confirmation for completed background work, shown by the JS runtime on every target.

- Type: Component (feedback, Messages)
- Namespace: `ui.toast`
- Source: `src/ui/toast.cljc`
- Public fns: `toast-class-list`, `toast-classes`, `toast`, `toast-flash`
- JS runtime: `ui.js.toast` (bundled in ui-runtime.js)
- Keywords: toast, notification, snackbar, flash message, htmx

## When to use

- Result of background work: an export finished, a sync failed, a message was sent.
- Server-rendered and HTMX responses that need a confirmation after a swap (toast-flash).

## When not to use

- Confirm foreground actions in place (the button label, the row, the value); never as the only confirmation.
- Use alert for a message that must stay until resolved.

## Usage

```clojure
(require '[ui.toast :as toast])

;; Replicant / Squint
(toast/show-toast! {:message "Export finished" :variant :success})

;; Hiccup / HTMX: include the marker in the response fragment
(toast/toast-flash {:message "Saved" :variant :success})
```

## Examples

### Sticky error

```clojure
(toast/show-toast! {:message "Sync failed. Retry from Settings." :variant :danger :duration 0})
```

## API reference

### `toast-flash`

Invisible marker element the runtime consumes and shows as a floating toast. Works inside hx-swap-oob fragments.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:message` | `string` | – | Toast text (required). |
| `:variant` | `:info \| :success \| :warning \| :danger` | `:info` |  |
| `:duration` | `ms` | `5000` | 0 keeps the toast until dismissed. |

### `toast`

Static toast element for previews; the runtime renders real toasts into its own container.

Children: Toast content.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:variant` | `:info \| :success \| :warning \| :danger` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `toast-classes`

Class string for {:variant}.

### `toast-class-list`

Vector form.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Click / tap | Dismisses a toast early. |

## Accessibility

- The runtime container is a polite live region, so new toasts are announced.
- Keep messages to one sentence; include the next step for errors.

## Motion

- Toasts slide in over --duration-standard with --ease-enter and fade out; reduced motion fades only.

## Responsive behavior

- Toasts stack at the bottom edge and are full width on phones.

## Targets

- **Hiccup (`:clj`)**: Use toast-flash in the page or an HTMX fragment; needs ui-runtime.js.
- **Replicant (`:cljs`)**: Call show-toast!; it is a no-op when the runtime is missing.
- **Squint (`:squint`)**: Same as Replicant.

## Notes for AI

- A toast never carries the only copy of an error; errors that need action use alert.
- Success toasts are a label change in words: Export finished, not Success!

## Related

- [Alert](alert.md): A persistent inline message with a tone, icon and optional title that helps people recover or continue.
- `processing-bar`: Processing bar — compact panel showing background jobs in progress.
- [Dialog](dialog.md): A focused modal surface for decisions that need attention, as a native <dialog> or an overlay for reactive apps.

## Guidance for AI tools

Toast: A brief floating confirmation for completed background work, shown by the JS runtime on every target. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
