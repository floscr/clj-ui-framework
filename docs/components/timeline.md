# Editing timeline

> A media editing timeline: a scrubbable ruler, labeled tracks with draggable and trimmable segments, a playhead and a floating transport.

- Type: Component (media, Media)
- Namespace: `ui.timeline`
- Source: `src/ui/timeline.cljc`
- Public fns: `total-time`, `tick-step`, `ticks`, `time->pct`, `span->pct`, `format-time`, `format-readout`, `timeline-class-list`, `timeline-classes`, `dragging?`, `timeline`
- Keywords: timeline, video editor, segments, scrub, playhead, trim

## When to use

- Editing clips, zoom ranges or keyframes against a media duration.
- Scrubbing a playhead with keyboard nudges and snapping.

## When not to use

- Use player-bar for playback without editing.
- For an activity feed (events newest first) build a list, this timeline is not that.

## Usage

```clojure
(require '[ui.timeline :as timeline])

(timeline/timeline {:duration 12.5 :current 4.2 :playing false
                    :tracks [{:id :clip :label "Clip" :segments [{:id 1 :start 0.8 :end 11.4 :label "video.mp4"}]}
                             {:id :zoom :label "Zoom" :segments [{:id 2 :start 3.5 :end 6.2} {:id 3 :start 7.6 :end 9.1}]}]
                    :on-seek seek! :on-segment-change edit! :on-play-pause toggle!})
```

## API reference

### `timeline`

Controlled: renders from props and reports edits through callbacks. Interactive on Squint only; other targets render it static.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:duration` | `seconds` | – | Required. |
| `:current` | `seconds` | – |  |
| `:tracks` | `vector of {:id :label :segments [{:id :start :end :label}]}` | – |  |
| `:playing` | `boolean` | – |  |
| `:loop` | `boolean` | – |  |
| `:transport` | `boolean` | `true` | false hides the transport. |
| `:min-gap` | `seconds` | `0` |  |
| `:step` | `seconds` | `0.1` |  |
| `:big-step` | `seconds` | `1` |  |
| `:on-seek` | `fn [t]` | – | Ruler click or drag and keyboard; shift snaps to segment edges. |
| `:on-segment-change` | `fn [{:track-id :segment-id :start :end :edge :phase}]` | – | :phase is :drag or :commit; persist on :commit. |
| `:on-play-pause` | `fn` | – |  |
| `:on-skip-start` | `fn` | – |  |
| `:on-loop` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `ticks`

Tick times for a duration.

### `format-time`

Tick label format (m:ss or seconds).

### `format-readout`

Transport readout, e.g. 1:23 / 4:56.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| ArrowLeft / ArrowRight | Nudge the playhead or focused segment by :step (Shift for :big-step). |
| Space | Play or pause from the transport. |

## Accessibility

- Segments and the ruler are focusable with keyboard editing; values are announced through the readout.

## Motion

- Segments follow the pointer directly; the playhead has no easing. Snapping highlights over --duration-fast.

## Responsive behavior

- The ruler fills its width; on narrow screens long timelines get fewer ticks.

## Targets

- **Hiccup (`:clj`)**: Static rendering of the current state.
- **Replicant (`:cljs`)**: Static rendering; interaction is Squint only for now.
- **Squint (`:squint`)**: Fully interactive.

## Notes for AI

- Persist segment edits only on :phase :commit; :drag fires continuously.

## Related

- [Player bar](player-bar.md): A music player bar with artwork, track info, transport controls, a scrubbable progress track and volume.
- [Progress](progress.md): Shows how much of a known task is complete as a filled track.

## Guidance for AI tools

Editing timeline: A media editing timeline: a scrubbable ruler, labeled tracks with draggable and trimmable segments, a playhead and a floating transport. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
