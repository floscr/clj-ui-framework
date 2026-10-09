# Drop zone

> A generous target for dropping or picking files, with a full-page overlay variant and a click-to-add label.

- Type: Component (inputs, Special inputs)
- Namespace: `ui.drop-zone`
- Source: `src/ui/drop_zone.cljc`
- Public fns: `drop-zone-class-list`, `drop-zone-classes`, `file-label`, `drop-zone`, `init-body-drop-zone!`, `drop-zone-overlay`
- JS runtime: `ui.js.drop-zone` (bundled in ui-runtime.js)
- Keywords: drop zone, file upload, drag and drop, file input, dropzone

## When to use

- Uploading one or more files where dragging onto the page is natural (photos, documents).
- A whole-page drop target with an overlay while dragging (init-body-drop-zone! plus drop-zone-overlay).

## When not to use

- Use form-file for a single small file field inside a form.
- Use camera to take a new photo.

## Usage

```clojure
(require '[ui.drop-zone :as drop-zone])

(drop-zone/drop-zone {:title "Drop photos here" :hint "or click to choose"
                      :accept "image/*" :multiple true
                      :on-files upload!})
```

## Examples

### Custom content with a click-to-add label

```clojure
(drop-zone/drop-zone {:on-files upload!}
  (file-progress/file-progress-list {} items)
  (drop-zone/file-label {:on-files upload!} "Add more"))
```

### Page-level drop with overlay (Replicant, Squint)

```clojure
(drop-zone/init-body-drop-zone! {:on-files upload! :on-active-change set-dragging!})
(when dragging? (drop-zone/drop-zone-overlay {}))
```

## API reference

### `drop-zone`

Without children: a <label> around a hidden file input with icon, title and hint. With children: a container for your own content.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:accept` | `string` | `"image/*"` |  |
| `:multiple` | `boolean` | `true` |  |
| `:icon` | `icon keyword` | `:upload` |  |
| `:title` | `string` | – |  |
| `:hint` | `string` | – |  |
| `:size` | `:md \| :lg` | `:md` |  |
| `:drag-over?` | `boolean` | – | Externally controlled active state. |
| `:disabled` | `boolean` | – |  |
| `:on-files` | `fn [files]` | – | :cljs and :squint. |
| `:on-drag-change` | `fn [boolean]` | – | Omit to let the zone toggle its own active class. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `file-label`

Styled label wrapping a hidden file input, for custom zone content.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:accept` | `string` | – |  |
| `:multiple` | `boolean` | – |  |
| `:on-files` | `fn [files]` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `drop-zone-overlay`

Full-screen overlay shown while files are dragged over the page.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:title` | `string` | `"Drop files to upload"` |  |
| `:hint` | `string` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `init-body-drop-zone!`

Document-level drag listeners for page drops (browser only). Returns a cleanup fn. Opts :on-files :on-active-change.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Enter / Space | On the label, opens the file picker. |

## Accessibility

- The zone is a labeled file input, so keyboard users can pick files; the drag state is visual only.

## Motion

- Border and background change over --duration-standard when dragging over.

## Responsive behavior

- Full width; :lg adds padding for empty pages.

## Targets

- **Hiccup (`:clj`)**: The runtime toggles the active class and dispatches a ui:drop-zone-files CustomEvent on the zone; listen with inline JS or HTMX.
- **Replicant (`:cljs`)**: :on-files receives a vector of File objects.
- **Squint (`:squint`)**: Same as Replicant.

## Notes for AI

- Pair with file-progress-list to show each file's upload state inside the zone.

## Related

- [File progress](file-progress.md): A bordered list of files with size, status and an optional progress bar and remove button.
- [Form](form.md): Labeled fields, inputs, textareas, checkboxes, radios, file and range inputs, joined groups and a search bar.
- [Camera](camera.md): A viewfinder with capture and camera switch, falling back to the native file input capture on plain HTTP, plus stream helpers.
- [Processing bar](processing-bar.md): A compact accent-bordered panel listing background jobs in progress with thumbnails, labels and spinners.

## Guidance for AI tools

Drop zone: A generous target for dropping or picking files, with a full-page overlay variant and a click-to-add label. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
