# Progress

> Shows how much of a known task is complete as a filled track.

- Type: Component (feedback, Status)
- Namespace: `ui.progress`
- Source: `src/ui/progress.cljc`
- Public fns: `progress-bar-class-list`, `progress-bar-classes`, `progress`
- Keywords: progress bar, percentage, upload progress, determinate

## When to use

- Known progress: an upload, an import, steps completed out of a total.
- Usage against a limit when paired with a label.

## When not to use

- Use spinner for unknown duration.
- Use skeleton while content is loading in place.
- Use file-progress for a list of files each with their own status.

## Usage

```clojure
(require '[ui.progress :as progress])

(progress/progress {:value 62 :attrs {:aria-label "Upload progress"}})
```

## Examples

### Tones

```clojure
(progress/progress {:value 90 :variant :warning})
(progress/progress {:value 100 :variant :success})
```

## API reference

### `progress`

A track with a fill at :value percent; role=progressbar with aria-valuenow.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:value` | `number 0 to 100` | `0` |  |
| `:variant` | `nil \| :success \| :warning \| :danger` | – | Fill tone; default is the accent. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – | Add aria-label or aria-labelledby. |

### `progress-bar-classes`

Class string for the fill given {:variant}.

### `progress-bar-class-list`

Vector form.

## Accessibility

- Exposes value, min and max; give it a name with aria-label.
- Show the percentage or count as text next to it when the number matters.

## Motion

- The fill width animates over --duration-standard.

## Responsive behavior

- Fills its column.

## Targets

- **Hiccup (`:clj`)**: Static; re-render to update.
- **Replicant (`:cljs`)**: Pass the live :value.
- **Squint (`:squint`)**: Pass the live :value; style uses string keys internally.

## Notes for AI

- Status tones only for real status: warning near a limit, danger when failed.

## Related

- [Spinner](spinner.md): An indeterminate loading indicator in four sizes that inherits the current text color.
- [Skeleton](skeleton.md): Reserves space with a pulsing placeholder while content loads.
- [File progress](file-progress.md): A bordered list of files with size, status and an optional progress bar and remove button.
- [Processing bar](processing-bar.md): A compact accent-bordered panel listing background jobs in progress with thumbnails, labels and spinners.

## Guidance for AI tools

Progress: Shows how much of a known task is complete as a filled track. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
