# Lightbox

> A fullscreen viewer for one image or a gallery: swipe, pinch and wheel zoom, pull to dismiss, and a morph out of and back into the thumbnail.

- Type: Component (media, Media)
- Namespace: `ui.lightbox`
- Source: `src/ui/lightbox.cljc`
- Public fns: `item-type`, `item-key`, `action`, `lightbox`, `image-thumbnail`
- JS runtime: `ui.js.lightbox` (bundled in ui-runtime.js)
- Keywords: lightbox, image viewer, gallery, carousel, zoom, pinch, fullscreen image, photo viewer, video

## When to use

- Looking at photos large from a grid, a message or a card.
- Paging through a set of images or videos, with captions or actions per item.

## When not to use

- Use dialog for forms and content that is not media.
- Use grid plus thumbnails for the collection itself.
- Use video-player for a single video that plays in place.

## Usage

```clojure
(require '[ui.lightbox :as lightbox])

;; host-controlled (replicant / squint)
(lightbox/lightbox {:items photos :index (:open @state)
                    :on-change #(swap! state assoc :open %)
                    :on-close #(swap! state dissoc :open)})

;; managed (any target, the only way on :clj)
(lightbox/image-thumbnail {:src (:thumb p) :lightbox "photos" :index i})
(lightbox/lightbox {:id "photos" :open false :items photos})
```

## Examples

### Gallery with captions, thumbnails and a download action

```clojure
(lightbox/lightbox
  {:items [{:src "/p/1.jpg" :thumb "/p/1-s.jpg" :caption "Harbour" :width 4000 :height 3000}
          {:src "/p/2.mp4" :type :video :poster "/p/2.jpg"}]
   :index 0
   :thumbnails true
   :history true
   :actions [(lightbox/action {:icon-name :download :label "Download" :href "/p/1.jpg" :download true})]
   :on-change on-change
   :on-close on-close})
```

## API reference

### `lightbox`

The viewer. Renders nothing without :src or :items. The runtime owns everything that moves and reports back through :on-change and :on-close (non-bubbling `change` and `close` events on the root).

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:items` | `vector` | – | Maps of :src :thumb :alt :caption :width :height :key :type (:image, :video, :content) :poster :content :href :class :autoplay :loop :muted. |
| `:src` | `string` | – | Shorthand for one image. |
| `:alt` | `string` | `"image"` | Alt text with :src. |
| `:index` | `number` | `0` | The item shown on open; changing it navigates. |
| `:id` | `string` | – | Element id; managed thumbnails open it with :lightbox id. |
| `:open` | `boolean` | `true` | false renders it hidden for managed mode. |
| `:on-close` | `fn` | – | Called after the close animation; unmount the lightbox here. |
| `:on-change` | `fn` | – | Called with the new index after a swipe, key or button. |
| `:thumbnails` | `boolean` | – | Shows a thumbnail strip. |
| `:actions` | `hiccup` | – | Toolbar buttons before zoom and close (see action). |
| `:footer` | `hiccup` | – | A bar at the bottom, for selection or links. |
| `:labels` | `map` | – | Overrides for default-labels (button names, error text). |
| `:history` | `boolean` | – | Opening pushes a history entry; Back closes. |
| `:keyboard` | `boolean` | `true` | false turns the runtime's keys off. |
| `:scroll-to-zoom` | `boolean` | – | A plain mouse wheel zooms instead of panning. |
| `:max-zoom` | `number` | – | Zoom cap; default is the image's own resolution, 2x to 8x. |
| `:contained` | `boolean` | – | A preview inside its parent: no page lock, no keys, no morph. |
| `:class` | `string \| vector` | – | Extra classes on the root. |

### `image-thumbnail`

A clickable image, and the place the lightbox morphs out of and back into.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:src` | `string` | – |  |
| `:alt` | `string` | `"image"` |  |
| `:on-click` | `fn` | – | Host-controlled: open the lightbox here. |
| `:lightbox` | `string` | – | Managed: the id of the lightbox to open. |
| `:index` | `number` | – | Managed: the item it opens. |
| `:lightbox-key` | `string` | `:src` | The item key it stands for (:key, else :thumb, else :src of the item). |
| `:class` | `string \| vector` | – |  |

### `action`

A round toolbar button for :actions: a link with :href (a download with :download), else a button.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:icon-name` | `keyword` | – |  |
| `:label` | `string` | – | Accessible name. |
| `:href` | `string` | – |  |
| `:download` | `boolean \| string` | – |  |
| `:on-click` | `fn` | – |  |

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Escape | Closes. |
| Left / Right | Previous / next; pans when zoomed. |
| Up / Down | Pan when zoomed. |
| Home / End | First / last item. |
| + / = | Zoom in. |
| - / _ | Zoom out. |
| 0 | Reset zoom. |

## Accessibility

- Every button carries an aria-label from :labels; the viewer is a role dialog.
- Image zoom uses unmodified keys only, so ctrl / cmd plus stays the browser's page zoom.

## Motion

- Opening morphs the thumbnail into the slide (clip-path un-crops an object-fit cover tile and keeps its radius); closing morphs back from the current zoom or pull, or fades when the thumbnail is off screen.
- Zoom rubber-bands past its bounds and coasts after a flick; everything collapses to a fade under prefers-reduced-motion.

## Responsive behavior

- Phones go edge to edge with the toolbar over the photo; prev / next and zoom buttons hide on touch screens, where swipe and pinch do the job.
- Safe-area insets pad every bar.

## Targets

- **Hiccup (`:clj`)**: Managed only: render it with :id and :open false, and image-thumbnail with :lightbox and :index opens it.
- **Replicant (`:cljs`)**: Host-controlled: render it while open, :on-close unmounts it, :on-change tracks the index. Managed mode works too.
- **Squint (`:squint`)**: Same as Replicant; SPAs without ui-runtime.js require ui.js.lightbox.

## Notes for AI

- Do not hand-roll zoom, swipe or a close animation; the runtime owns them.
- Unmount the lightbox in :on-close; it is invisible but still mounted until then.
- Give a thumbnail the same intrinsic aspect as the full image (crop with object-fit, not a CDN crop), or the morph and the thumbnail placeholder reframe when the full image lands.
- Set :width and :height on items when known: the slide is sized before the image loads.

## Related

- [Tile grid](grid.md): A responsive auto-fill grid of square or masonry tiles with size presets, plus a layout toggle and size stepper for toolbars.
- [Dialog](dialog.md): A focused modal surface for decisions that need attention, as a native <dialog> or an overlay for reactive apps.
- [Video player](video-player.md): A video player with glass controls over the picture, HLS through hls.js, built-in subtitles, quality and speed menus, autoplay next, and custom buttons with their own popovers.
- [Camera](camera.md): A viewfinder with capture and camera switch, falling back to the native file input capture on plain HTTP, plus stream helpers.

## Guidance for AI tools

Lightbox: A fullscreen viewer for one image or a gallery: swipe, pinch and wheel zoom, pull to dismiss, and a morph out of and back into the thumbnail. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
