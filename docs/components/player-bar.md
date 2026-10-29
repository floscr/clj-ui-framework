# Player bar

> A music player bar with artwork, track info, transport controls, a scrubbable progress track and volume.

- Type: Component (media, Media)
- Namespace: `ui.player-bar`
- Source: `src/ui/player_bar.cljc`
- Public fns: `player-bar-class-list`, `player-bar-classes`, `player-bar`
- Keywords: player, audio player, transport controls, now playing, scrubber

## When to use

- A persistent playback bar at the bottom of a music or podcast app.

## When not to use

- Use timeline for editing media with segments.
- Use progress for a plain non-interactive bar.

## Usage

```clojure
(require '[ui.player-bar :as player-bar])

(player-bar/player-bar {:track-name "Across the Universe" :subtitle "The Beatles"
                        :playing true :progress 36 :current-time "1:23" :duration "3:48"
                        :on-play-pause toggle! :on-next next! :on-previous prev!
                        :on-seek seek-from-event!})
```

## API reference

### `player-bar`

The full bar. All state comes from props; every control reports through a callback.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:track-name` | `string` | – |  |
| `:subtitle` | `string` | – |  |
| `:artwork` | `hiccup` | – |  |
| `:playing` | `boolean` | – |  |
| `:progress` | `0 to 100` | – |  |
| `:current-time` | `string` | – |  |
| `:duration` | `string` | – |  |
| `:shuffle` | `boolean` | – |  |
| `:repeat` | `boolean` | – |  |
| `:favorited` | `boolean` | – |  |
| `:on-play-pause` | `fn` | – |  |
| `:on-next` | `fn` | – |  |
| `:on-previous` | `fn` | – |  |
| `:on-shuffle` | `fn` | – |  |
| `:on-repeat` | `fn` | – |  |
| `:on-favorite` | `fn` | – |  |
| `:on-seek` | `fn [pointer-event]` | – | Fires on tap and while dragging the track; compute the position from clientX and the target rect. |
| `:on-volume` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `player-bar-classes`

Class string for the container.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Moves through the transport buttons. |
| Space on play | Toggles playback. |

## Accessibility

- Every control is a labeled icon button; times use tabular numerals.

## Motion

- The scrubber thumb appears on hover (always on touch) over --duration-fast; press feedback on buttons.

## Responsive behavior

- Secondary controls hide below the small breakpoint; the track stays scrubbable by touch.

## Targets

- **Hiccup (`:clj`)**: Static bar; wire controls with :attrs or HTMX.
- **Replicant (`:cljs`)**: All callbacks; :on-seek receives the pointer event.
- **Squint (`:squint`)**: Same as Replicant.

## Notes for AI

- Keep the bar outside scrolling containers so it stays fixed at the bottom.

## Related

- [Editing timeline](timeline.md): A media editing timeline: a scrubbable ruler, labeled tracks with draggable and trimmable segments, a playhead and a floating transport.
- [Progress](progress.md): Shows how much of a known task is complete as a filled track.
- [Button](button.md): A single action: neutral primary, quiet outlined states, a loud filled option, press feedback and a loading spinner.

## Guidance for AI tools

Player bar: A music player bar with artwork, track info, transport controls, a scrubbable progress track and volume. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
