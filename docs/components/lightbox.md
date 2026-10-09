# Lightbox

> A fullscreen image overlay with pinch, wheel and double-tap zoom and drag to pan, plus a clickable thumbnail.

- Type: Component (media, Media)
- Namespace: `ui.lightbox`
- Source: `src/ui/lightbox.cljc`
- Public fns: `lightbox`, `image-thumbnail`
- JS runtime: `ui.js.lightbox` (bundled in ui-runtime.js)
- Keywords: lightbox, image viewer, zoom, fullscreen image, gallery

## When to use

- Viewing one image large from a grid or a message.

## When not to use

- Use dialog for content that is not a single image.
- Use grid plus thumbnails for the collection itself.

## Usage

```clojure
(require '[ui.lightbox :as lightbox])

(lightbox/image-thumbnail {:src thumb :on-click #(open! full})
(lightbox/lightbox {:src (:open-image state) :on-close close!})
```

## API reference

### `lightbox`

Renders the overlay only while :src is set. Clicking the backdrop or the close button calls :on-close.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:src` | `string \| nil` | – |  |
| `:alt` | `string` | `"image"` |  |
| `:on-close` | `fn` | – |  |

### `image-thumbnail`

A clickable image.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:src` | `string` | – |  |
| `:alt` | `string` | – |  |
| `:on-click` | `fn` | – |  |
| `:class` | `string \| vector` | – |  |

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Escape | Closes (wire in the host for the overlay variant). |

## Accessibility

- The image keeps its alt text; the close button is labeled.

## Motion

- The overlay fades in over --duration-fast; zoom follows the gesture directly and snaps back into bounds on release.

## Responsive behavior

- Two-finger pinch and one-finger pan on touch; the page-level pinch block stays in place.

## Targets

- **Hiccup (`:clj`)**: Render the overlay from a query parameter or open it with inline JS.
- **Replicant (`:cljs`)**: Controlled by state via :src and :on-close.
- **Squint (`:squint`)**: Same as Replicant; require ui.js.lightbox in the SPA.

## Notes for AI

- Do not hand-roll zoom; the runtime owns it and resets on image load.

## Related

- [Tile grid](grid.md): A responsive auto-fill grid of square or masonry tiles with size presets, plus a layout toggle and size stepper for toolbars.
- [Dialog](dialog.md): A focused modal surface for decisions that need attention, as a native <dialog> or an overlay for reactive apps.
- [Camera](camera.md): A viewfinder with capture and camera switch, falling back to the native file input capture on plain HTTP, plus stream helpers.

## Guidance for AI tools

Lightbox: A fullscreen image overlay with pinch, wheel and double-tap zoom and drag to pan, plus a clickable thumbnail. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
