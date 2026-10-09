# File progress

> A bordered list of files with size, status and an optional progress bar and remove button.

- Type: Component (feedback, Status)
- Namespace: `ui.file-progress`
- Source: `src/ui/file_progress.cljc`
- Public fns: `file-progress-item`, `file-progress-list`
- Keywords: upload list, file progress, upload status, attachments

## When to use

- Showing each file's upload or processing state after a drop or pick.
- Attachment lists in a composer before sending.

## When not to use

- Use progress for a single overall bar.
- Use processing-bar for background jobs that are not files.

## Usage

```clojure
(require '[ui.file-progress :as fp])

(fp/file-progress-list {}
  (fp/file-progress-item {:name "photo.jpg" :size "2.4 MB" :progress 67 :status :uploading
                          :icon (icon/icon {:icon-name :image :size :sm}) :on-remove #(cancel! 1)})
  (fp/file-progress-item {:name "doc.pdf" :size "1.1 MB" :status :complete}))
```

## API reference

### `file-progress-list`

Continuous bordered list.

Children: file-progress-item elements.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `file-progress-item`

One file row.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:name` | `string` | – |  |
| `:size` | `string` | – | Formatted, e.g. 2.4 MB. |
| `:icon` | `hiccup` | – | Icon slot, e.g. from file-type-icon. |
| `:progress` | `0 to 100 \| nil` | – | nil hides the bar. |
| `:status` | `:idle \| :uploading \| :complete \| :error` | – |  |
| `:on-remove` | `fn []` | – | Shows a remove button when given. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

## Accessibility

- Status is a word plus an icon; the progress bar carries aria values. Remove buttons are labeled with the file name.

## Motion

- Progress width animates over --duration-standard.

## Responsive behavior

- Names truncate with the full value in the title attribute.

## Targets

- **Hiccup (`:clj`)**: Static; re-render on progress updates (HTMX polling works).
- **Replicant (`:cljs`)**: Pass live :progress.
- **Squint (`:squint`)**: Same as Replicant.

## Notes for AI

- Errors say what to do: Too large, pick a file under 10 MB.

## Related

- [Drop zone](drop-zone.md): A generous target for dropping or picking files, with a full-page overlay variant and a click-to-add label.
- [Progress](progress.md): Shows how much of a known task is complete as a filled track.
- [Processing bar](processing-bar.md): A compact accent-bordered panel listing background jobs in progress with thumbnails, labels and spinners.

## Guidance for AI tools

File progress: A bordered list of files with size, status and an optional progress bar and remove button. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
