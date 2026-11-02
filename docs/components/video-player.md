# Video player

> A video player with glass controls over the picture, HLS through hls.js, built-in subtitles, quality and speed menus, autoplay next, and custom buttons with their own popovers.

- Type: Component (media, Media)
- Namespace: `ui.video-player`
- Source: `src/ui/video_player.cljc`
- Public fns: `video-player-class-list`, `video-player-classes`, `video-player`
- JS runtime: `ui.js.video-player` (bundled in ui-runtime.js)
- Keywords: video, player, hls, hls.js, m3u8, subtitles, captions, quality, playback speed, autoplay next, up next, fullscreen, picture in picture

## When to use

- Playing a film, an episode or a clip, from a file or an HLS stream.
- A player that needs app-specific controls next to the built-ins: a sleep timer, an audio switch, a share action.
- Series playback with an up-next countdown into the next episode.

## When not to use

- Use player-bar for audio: a persistent bar with artwork and transport.
- Use timeline to edit or trim media.
- Use a plain <video> for a muted, looping background clip without controls.

## Usage

```clojure
(require '[ui.video-player :as vp])

(vp/video-player
  {:src "/hls/abc/index.m3u8" :title "Episode 1" :start-at 62
   :tracks [{:src "/subs/en.vtt" :label "English" :lang "en"}]
   :next {:title "Episode 2" :thumb "/thumbs/2.jpg"}
   :buttons [{:id :sleep :icon :moon :label "Sleep timer"
              :options [{:value 0 :label "Off"} {:value 30 :label "30 minutes"}]
              :value 0}]
   :on-select (fn [id value] ...)
   :on-next next-episode!})
```

## Examples

### Hiccup page: load hls.js, then listen for the runtime's change event

```clojure
[:head [:script {:src vp/hls-script-url}]]
...
(vp/video-player {:src src :title title :next {:title "Episode 2" :href "/watch/2"}})
[:script "document.querySelector('[data-video-player]').addEventListener('change', e => console.log(e.detail))"]
```

### Replace a built-in: the host's own quality list

```clojure
(vp/video-player {:src src
                  :buttons [{:id :quality :label "Quality" :show-value? true
                             :options [{:value "1080" :label "1080p"} {:value "720" :label "720p" :default? true}]
                             :value "720"}]
                  :on-select (fn [id v] (when (= id "quality") (reload-at! v))))
```

## API reference

### `video-player`

The player. Render it once; the ui.js.video-player runtime owns playback state. Changing :src (the root's data-src) loads the new source.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:src` | `url` | – | Media URL. A .m3u8 plays through window.Hls when loaded, natively on Safari. |
| `:hls?` | `boolean` | – | Treat :src as HLS even without .m3u8 in it. |
| `:poster` | `url` | – |  |
| `:title` | `string` | – | Top chip and the label next to the transport. |
| `:back` | `{:href :label}` | – | A back link in the top chip. |
| `:start-at` | `seconds` | – | Start position, e.g. a resume point. |
| `:autoplay?` | `boolean` | – | Try to play at once; stays paused when the browser refuses. |
| `:tracks` | `[{:src :label :lang :default?}]` | – | WebVTT subtitle tracks. HLS subtitle renditions appear on their own. |
| `:next` | `{:title :thumb :href}` | – | The next item: a Next button, the up-next card, the autonext toggle. :href navigates; else only :on-next fires. |
| `:autonext?` | `boolean` | `true` | Initial state of autoplay next. |
| `:controls` | `vector of ids` | – | Right capsule in order. Built-ins: :subtitles :quality :speed :autonext :pip :fullscreen. |
| `:buttons` | `vector of maps` | – | Custom controls: menus {:id :icon :label :options :value :show-value?}, {:type :toggle :pressed?} or {:type :action}. |
| `:fit` | `:contain \| :fill` | `:contain` | :fill fills the parent (full-page players) instead of 16:9. |
| `:variant` | `:glass \| :plain` | `:glass` | :glass floats the controls in blurred capsules; :plain sets them straight on a gradient with solid popovers and no blur (cheaper on TVs and old phones). |
| `:on-select` | `fn [id value]` | – | A menu option was picked. Custom menus get the option's original value. |
| `:on-toggle` | `fn [id on?]` | – | A toggle flipped, :autonext included. |
| `:on-action` | `fn [id]` | – | An action button was pressed. |
| `:on-next` | `fn []` | – | Next pressed or the up-next countdown ran out. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `video-player-classes`

Class string for the root.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Space / k | Play or pause. |
| Left / Right | Seek 5 seconds. |
| Up / Down | Volume. |
| m | Mute. |
| c | Subtitles on or off. |
| f | Fullscreen. |
| n | Next, when there is one. |
| Escape | Closes an open popover. |

## Accessibility

- The root is a labelled region; keys work while focus is inside it, or anywhere when it is the only player on the page.
- The seek bar and volume are sliders with values; options are menuitemradio with aria-checked; triggers carry aria-expanded and aria-pressed.
- Captions are drawn from the video's text tracks into the page, so they can sit above the controls.

## Motion

- Controls fade after 2.6 s without input while playing; popovers pop in over --duration-fast; the scrub track grows on hover with --ease-spring.
- prefers-reduced-motion drops the transitions.

## Responsive behavior

- Below 600px the title, volume slider and pill labels hide and the controls shrink; below 420px the dock moves closer to the edges.
- Container queries, so the player adapts to its own width, not the viewport.

## Targets

- **Hiccup (`:clj`)**: Load hls.js (hls-script-url) and ui-runtime.js; listen for the root's change event yourself.
- **Replicant (`:cljs`)**: Callbacks via :on-select, :on-toggle, :on-action, :on-next. Require ui.js.video-player when the page doesn't load ui-runtime.js.
- **Squint (`:squint`)**: Same as Replicant.

## Notes for AI

- Don't re-render the player on every timeupdate: the runtime owns time, play state and the open popover. Re-render only when :src, :next or a custom button's :value changes.
- Custom option values travel through the DOM as strings; on-select maps them back to the original value.
- A button with a built-in id (e.g. :quality) replaces the built-in.
- window.__uiVideoPlayer(root) returns {video play pause seek toast load} for host scripts (progress saving, resume).

## Related

- [Player bar](player-bar.md): A music player bar with artwork, track info, transport controls, a scrubbable progress track and volume.
- [Editing timeline](timeline.md): A media editing timeline: a scrubbable ruler, labeled tracks with draggable and trimmable segments, a playhead and a floating transport.
- [Popover](popover.md): A small anchored panel for contextual controls, built on the native Popover API so it works without app state.

## Guidance for AI tools

Video player: A video player with glass controls over the picture, HLS through hls.js, built-in subtitles, quality and speed menus, autoplay next, and custom buttons with their own popovers. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
