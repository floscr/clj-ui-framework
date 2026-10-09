# Camera

> A viewfinder with capture and camera switch, falling back to the native file input capture on plain HTTP, plus stream helpers.

- Type: Component (media, Media)
- Namespace: `ui.camera`
- Source: `src/ui/camera.cljc`
- Public fns: `camera-capable?`, `has-camera-api?`, `detect-camera!`, `stop-camera!`, `start-camera!`, `capture-photo!`, `camera-view`
- Keywords: camera, getUserMedia, capture photo, viewfinder, mobile camera

## When to use

- Taking a photo inside the app: scanning documents, receipts, attaching a picture.
- Mobile-first flows where the OS camera via a file input is enough on HTTP.

## When not to use

- Use drop-zone or form-file to pick existing images.
- Do not use for video recording; the helpers capture single JPEG frames.

## Usage

```clojure
(require '[ui.camera :as camera])

(camera/camera-view {:active? live?
                     :on-capture #(camera/capture-photo! {})
                     :on-switch toggle-facing!
                     :on-files handle-files!})
;; after rendering with :active? true
(camera/start-camera! {:facing "environment" :on-error show-error!})
```

## API reference

### `camera-view`

Live viewfinder (video, crosshair, round capture button, optional switch) when :active?, otherwise a tap-to-capture placeholder wrapping <input type=file capture>.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:active?` | `boolean` | `false` |  |
| `:video-id` | `string` | `"ui-camera-video"` |  |
| `:facing` | `"environment" \| "user"` | `"environment"` |  |
| `:placeholder` | `string` | `"Tap to take a photo"` |  |
| `:accept` | `string` | `"image/*"` |  |
| `:on-capture` | `fn []` | – | :cljs and :squint. |
| `:on-switch` | `fn []` | – | Shows the facing toggle when given. |
| `:on-files` | `fn [files]` | – | Native input selection. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `start-camera!`

Starts a getUserMedia stream on the <video> (browser only). Opts :video-id :facing :on-active :on-error.

### `stop-camera!`

Stops the active stream.

### `capture-photo!`

Returns {:preview data-url :data base64 :mime-type} for the current frame. Opts :video-id :quality.

### `camera-capable?`

HTTP-safe heuristic: touch plus coarse pointer.

### `has-camera-api?`

True when getUserMedia exists (secure context).

### `detect-camera!`

Refines capability via enumerateDevices in secure contexts.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Enter / Space | On the placeholder, opens the OS camera or file picker. |

## Accessibility

- The capture and switch buttons have labels; the placeholder is a labeled file input.

## Motion

- Capture button press feedback over --duration-fast.

## Responsive behavior

- The viewfinder keeps a 4 by 3 ratio and fills its column; the placeholder is a large tap target.

## Targets

- **Hiccup (`:clj`)**: Placeholder only (native capture input); the stream helpers are browser-only.
- **Replicant (`:cljs`)**: Full viewfinder with the stream helpers.
- **Squint (`:squint`)**: Same as Replicant.

## Notes for AI

- Seed with camera-capable?, then refine with detect-camera! on HTTPS.
- Call stop-camera! when leaving the view.

## Related

- [Drop zone](drop-zone.md): A generous target for dropping or picking files, with a full-page overlay variant and a click-to-add label.
- [Lightbox](lightbox.md): A fullscreen image overlay with pinch, wheel and double-tap zoom and drag to pan, plus a clickable thumbnail.
- [Form](form.md): Labeled fields, inputs, textareas, checkboxes, radios, file and range inputs, joined groups and a search bar.

## Guidance for AI tools

Camera: A viewfinder with capture and camera switch, falling back to the native file input capture on plain HTTP, plus stream helpers. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
