# Changelog

Consumers pin a release by tag and sha:

```edn
clj-ui-framework/clj-ui-framework
{:git/url "https://gitea.florianschroedl.com/floscr/clj-ui-framework"
 :git/tag "v2.0.0"
 :git/sha "<sha the tag points at>"}
```

## v2.4.0 — 2026-11-09

### New

- **Rating** (`ui.rating` + the `ui.js.rating` runtime): a star rating
  with half stars, picked by the half of a star you point at. Choosing the
  value again or Backspace / Delete clears it, and a value that lands pops
  the stars one by one. A radio group under the stars, whose fill follows
  the checked or hovered radio in CSS, so it renders in every target and
  posts with a form (htmx `hx-trigger "change"`). `:read-only` with
  `:size :sm` shows a score on cards. Its width never changes with the
  value.
- **Runtime tooltips** in one shared layer: per-trigger delay, instant
  switching between triggers, shortcut keycaps, rich content cards, a
  danger tone and a bubble that inverts the page. `tooltip-attrs` puts a
  plain tooltip on any element (`data-tooltip`), and components use them
  instead of native titles (theme toggle, player bar, timeline, video
  player, file browser, file progress, grid, day grid, dial); icon-only
  buttons get aria-labels.
- **Context menu sub-menus**: an item with `:items` drills into a page of
  them under a back header; pages slide while the menu animates to the
  new size and position.
- **Spinner**: a stretching arc on a muted track, recolored through
  `--spinner-arc` / `--spinner-track` / `--spinner-w`.
- **Dial**: actions render as `ui.button` and share a row, sliders reset
  from a right-click menu and snap with Shift / Ctrl, `label false` hides
  a control's label.
- **Theme**: a `[data-theme=light]` block, so a subtree can be light under
  a dark root.
- Icons `palette`, `signpost` and `scissors`.

## v2.3.0 — 2026-11-02

### New

- **`file-table` multi-column sort**: `:sorts` (ordered `[{:key :dir}]`)
  marks every sorted header with its chevron and, past one key, its rank.
  `:on-sort` gets the click event as a second argument, so a host can add a
  key on Alt/Shift+click. A column's `:sortable false` opts out.
- **Resizable columns**: with `:on-resize (fn [col-key px])` each header
  gets a grip on its right edge. Dragging moves the grid's tracks directly
  (no re-render) and reports the width on release; double-clicking fits
  the column to its widest rendered content. The host keeps the widths and
  passes them back as `:width`; `:resizable false` opts a column out.
  Squint / Replicant only.
- **Header hooks**: `:header-attrs-fn` (merged onto a header cell, class
  appended, e.g. drag handlers to reorder columns, marked by
  `fb-table-th-drop-before` / `-after`) and
  `:header-context-menu-items-fn` (a right-click menu per header).
- Icons `grip-vertical` and `sliders-horizontal`.

## v2.2.1 — 2026-11-02

### Fixed

- The done tick in `task-check` is centred (a mask glyph like the states,
  so it scales with the box), and a task's check in an agenda row is
  centred on the row's first line instead of sitting above it.
- Tasks within one time slot are ordered by `:state` (active first, done
  last); a day cell moves cancelled tasks to the end along with done ones.

## v2.2.0 — 2026-11-02

### New

- **Task states in calendar checkboxes**: an event's optional `:state`
  (`:active`, `:next`, `:waiting`, `:hold`, `:cancelled`) draws its own box
  between unchecked and done, e.g. an amber box with a bolt for active.
  Events without one look as before. `task-check` takes the state as a
  third argument; `event-task-check` reads it from an event map.

## v2.1.1 — 2026-11-02

### Fixed

- **Video player autoplay** falls back to muted playback when the browser
  refuses sound (a stream that starts long after the click that opened the
  page) and shows a "Tap to unmute" pill; the next tap on the video
  unmutes instead of pausing. The controller's `play` does the same.
- An autoplaying source shows the spinner from the start instead of the
  big play button.
- An empty `:src` loads nothing (it used to make the browser load the page
  itself as media), so a host can render the player before its stream is
  ready.

## v2.1.0 — 2026-11-02

### New

- **Video player** (`ui.video-player` + the `ui.js.video-player` runtime):
  glass capsules over the picture, HLS through hls.js (`hls-script-url`),
  subtitles drawn from the video's text tracks, quality and speed menus,
  autoplay next with an up-next countdown, fullscreen, picture in picture
  and keyboard control. Custom controls are data (`:buttons`): menus with
  their own popover, toggles and actions, reported through `:on-select`,
  `:on-toggle`, `:on-action` and `:on-next` (a `change` event on the root
  for hiccup pages). `:variant :plain` drops the glass: controls on a
  gradient, solid popovers, no backdrop blur.
- **Day grid** (`ui.day-grid`): a week-by-week grid of days for habits
  and countdowns.
- Icons: `volume-1`, `volume-x`, `captions`, `gauge`, `maximize`,
  `minimize`, `picture-in-picture-2`, `rotate-ccw`, `rotate-cw`,
  `list-video`.

### Fixed

- Date and time inputs on iOS Safari shrink to their column, match the
  text fields and align left.

## v2.0.0 — 2026-10-29

Good-looking apps by default: quieter controls, a smaller type scale, one
control height, and a page layer so apps stop hand-rolling their shell.
Every component keeps its API; the changes below are visual, plus new
components and props.

### Breaking (visual)

- **Buttons are quiet.** `:variant :primary` is neutral ink on the page
  color (black on light, white on dark), not the accent. `:danger` and
  `:success` are outlined with a colored label. The old filled look is
  `:loud true`, for the one action on a screen that must dominate.
- **Type scale** is 12 / 13 / 14 / 16 / 20 / 28 / 36 / 48px (Fluid
  Functionalism) instead of the 1.25 geometric ramp. `--font-xs` grows
  from 10px to 12px, `--font-sm` is 13px, `--font-base` drops from 16px
  to 14px, `--font-md` from 20px to 16px, `--font-lg` from 25px to 20px.
  The body sits on `--font-sm`.
- **Base styles size headings**: `h1` 28px, `h2` 16px, `h3` 14px, `h4` to
  `h6` 13px, all with `margin: 0` and `text-wrap: balance`; `p` gets
  `text-wrap: pretty`.
- **One control height.** Buttons, inputs, selects, number fields and tag
  inputs pin `min-height` to `--control-sm/md/lg`: 28 / 36 / 44px, and
  36 / 44 / 48px under `html.clj-ui-touch`.
- **Fields** sit on `--bg-1` (not the page color); focus is the accent
  border, no glow.
- **Card radius** (`--radius-surface`) is 11px instead of 18px.
- Badges (`:primary`) and selected chips use the accent tint, not a solid
  fill.
- `ui.list-row` rows stay a `<div>`; with `:href` / `:on-click` the title is
  the link or button, stretched over the row.
- `ui.file-browser` tables give the first flexible column 200px and twice
  the share, so names stay readable on a phone; the table scrolls inside
  its wrapper.

### Added

- `ui.page`: `page` (`:width` narrow / reading / wide / full),
  `page-header`, `page-section`, `action-row`.
- `ui.settings`: `settings-group`, `settings-row`.
- `ui.metric`: `metric`, `metric-row`.
- `ui.list-row`: `list-rows`, `list-row`.
- `ui.empty-state` `:fill true` and the `.empty-state-title` class.
- `ui.button` `:loud`.
- `ui.util/class-attrs` for the per-target `:class` attribute map.
- Font scale steps may be `[label multiplier]` in `tokens.edn`.

### Migrating

1. Bump the pin to the tag above and regenerate CSS (`bb ui:css`, or
   restart a server that builds it at boot). Apps that check in
   `theme.css` / `ui-runtime.js` copy them again.
2. Delete vendored copies of framework namespaces (for example
   `src/ui/theme_model.cljc`, `src/ui/themes.cljc`): an older copy on the
   classpath shadows the new theme model and writes the old type sizes
   inline whenever a theme is active.
3. Where a screen needs the old accent-filled button, add `:loud true`.
4. Replace hand-rolled app shells (`.container`, `.app-shell`, own
   `max-width`, font imports, heading sizes) with `ui.page`. See
   AGENTS.md, "Composing a Screen".
5. Reading surfaces that relied on a 16px body (chat, prose) can set their
   own size to `var(--font-md)`.
