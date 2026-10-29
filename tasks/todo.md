# Good-by-default apps (no extra instructions)

Diagnosis from the five screenshots + their source (streaks, service-theme,
music-server, share, willhaben-seller), 2026-10-29:

1. Framework defaults are off: `--font-sm` is 12.8px and `--font-xs` 10px
   (1.25 geometric scale), yet every control (button, input, select, table)
   uses `font-sm`. Controls have no shared `min-height`, so buttons, inputs
   and selects in one row end up 36/44/56px tall. Primary/danger/success are
   solid saturated fills; inputs use `bg-0` so they read as holes inside
   cards. Headings have no sizes in base CSS.
2. The framework has no page-level primitives (page container, page header,
   section, settings row, metric, list row), so every app writes its own
   shell: own max-width, own Google-Fonts face, h1 at 600/700/800, uppercase
   eyebrow labels, hex palettes, `.card` overrides. The lint that forbids all
   of this only runs on the framework.
3. Consumer AGENTS.md points agents at the contributor guide, not at app
   composition rules or worked examples.

## Plan

### Phase A: defaults (framework CSS, no API changes)
- [ ] Font scale: explicit steps (xs 12, sm 14, base 16, md 18, lg 22,
      xl 28, 2xl 36, 3xl 48). Add `[label value]` support to
      `generate-font-scale`, keep ratio form working.
- [ ] Base type ramp in `base-css`: body 16/1.5, h1 xl 500, h2 lg, h3 md,
      h4 base, margins 0, `p` margin, `td`/`.metric` tabular-nums.
- [ ] One control height: `min-height: var(--control-md)` on `.btn`,
      `.form-input`, `.form-select`, `.select-trigger`, `.number-field`,
      `.tag-input`, `.chip`-as-control; sm/lg map to `--control-sm/lg`.
      Desktop md 40px, `.clj-ui-touch` raises md to 44px.
- [ ] Quieter buttons: primary = fg on bg (neutral, Arc style; accent via
      `data-accent` stays available), secondary = surface + border,
      danger = outlined with danger text (solid only `.btn-danger-solid`),
      drop solid success default. Ghost text `fg-1`.
- [ ] Inputs on `bg-1`, border `border-0`, focus = accent border (no inset
      glow). Select trigger same.
- [ ] Dark palette: lift `bg-0` to ~L .17 and `border-0` so cards and
      secondary buttons separate on dark surfaces.
- [ ] Ship one body face (self-hosted Inter Variable via `build-css`
      `@font-face`, served by server-lib), so apps stop importing fonts.

### Phase B: composition primitives (new components with sidecars)
- [ ] `ui.page`: `page` (:width :narrow 640 | :reading 720 | :wide 1200,
      fluid gutters, grid gap 24), `page-header` (title, description,
      actions), `section` (h2, description, children), `actions-row`.
- [ ] `ui.settings`: `settings-group` + `settings-row` (label, hint, control).
- [ ] `ui.metric`: `metric` (value, label, delta, optional ring) + `metric-row`.
- [ ] `ui.list`: `list-row` (leading icon/thumb, title, meta, trailing)
      on top of `card-list`.
- [ ] `ui.segmented-control` (mode pickers like service-theme rows).
- [ ] `empty-state` `:fill true` centers in its region.
- [ ] server-lib `render/page` uses `ui.page/page` + `page-header`.

### Phase C: enforcement and agent docs
- [ ] `lint-css` runs on consumer CSS in server-lib's frontend pipeline
      and `bb check`: fail on font imports, `font-family` on html/body,
      weights > 500, uppercase, raw hex, raw px font-size, overriding
      `.card`/`.btn`/`.form-*` internals, a second page container.
- [ ] `docs/skill/`: INSTRUCTIONS.md (short always-on app rules),
      composition.md (one container, 16/24/32 rhythm, settings rows,
      states), worked examples: settings page, list page, dashboard card,
      form page, all as `.cljc` using `ui.page`.
- [ ] `sync-consumer-agents` section → app rules + link to llms.txt and
      skill, not the contributor AGENTS.md.
- [ ] Rebuild demo site pages to use `ui.page` (dogfood), screenshot
      before/after of the five apps.

## Review
(filled in after implementation)
