# Changelog

Consumers pin a release by tag and sha:

```edn
clj-ui-framework/clj-ui-framework
{:git/url "https://gitea.florianschroedl.com/floscr/clj-ui-framework"
 :git/tag "v2.0.0"
 :git/sha "<sha the tag points at>"}
```

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
