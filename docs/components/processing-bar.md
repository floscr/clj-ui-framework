# Processing bar

> A compact accent-bordered panel listing background jobs in progress with thumbnails, labels and spinners.

- Type: Component (feedback, Status)
- Namespace: `ui.processing-bar`
- Source: `src/ui/processing_bar.cljc`
- Public fns: `processing-item`, `processing-bar`
- Keywords: background jobs, processing, in progress, queue status

## When to use

- Several background jobs running at once (scans, imports, conversions) that people should see without blocking.

## When not to use

- Use toast for the result of one finished job.
- Use file-progress for per-file upload progress.

## Usage

```clojure
(require '[ui.processing-bar :as processing-bar])

(when (seq jobs)
  (processing-bar/processing-bar {:title "2 scans in progress"}
    (for [j jobs]
      (processing-bar/processing-item {:thumb (:thumb j) :label (:label j) :status (:status j)}))))
```

## API reference

### `processing-bar`

The panel; renders unconditionally, the host decides visibility.

Children: processing-item elements.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:title` | `string` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `processing-item`

One job row with spinner and status.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:thumb` | `image URL` | – |  |
| `:label` | `string` | – | Secondary line above the status. |
| `:status` | `string \| hiccup` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

## Accessibility

- Mark the panel as a polite live region in :attrs when jobs change often.

## Motion

- The panel slides in over --duration-standard with --ease-enter.

## Responsive behavior

- Full width of its column; thumbnails are 32px.

## Targets

- **Hiccup (`:clj`)**: Re-render with HTMX polling.
- **Replicant (`:cljs`)**: Render from state.
- **Squint (`:squint`)**: Same as Replicant.

## Notes for AI

- Status text is a present participle: Analyzing, Resizing.

## Related

- [File progress](file-progress.md): A bordered list of files with size, status and an optional progress bar and remove button.
- [Toast](toast.md): A brief floating confirmation for completed background work, shown by the JS runtime on every target.
- [Spinner](spinner.md): An indeterminate loading indicator in four sizes that inherits the current text color.

## Guidance for AI tools

Processing bar: A compact accent-bordered panel listing background jobs in progress with thumbnails, labels and spinners. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
