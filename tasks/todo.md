# Good-by-default apps (no extra instructions)

Diagnosis from the five screenshots + their source (streaks, service-theme,
music-server, share, willhaben-seller), 2026-10-29:

1. Framework defaults were off: `--font-sm` 12.8px / `--font-xs` 10px
   (1.25 geometric scale) on every control; no shared control height;
   solid primary/danger/success fills; inputs on `bg-0` read as holes in
   cards; no heading ramp in the base CSS.
2. No page-level primitives, so every app hand-rolled its shell: own
   max-width, Google-Fonts face, h1 at 600 to 800, uppercase eyebrows, hex
   palettes, `.card` overrides. The lint only ran on the framework.
3. Consumer AGENTS.md pointed agents at the contributor guide, not at
   composition rules.

## Done (commits 2711345, ce137a8, + follow-up)

### Phase A: defaults
- [x] Font scale: explicit ramp 12/14/16/18/22/28/36/48px (`[label mult]`
      steps; geometric form still accepted). Theme model reproduces it at
      the default ratio, sliders still work.
- [x] Base type ramp: body 16px, h1 xl … h6 sm with margin 0, tabular
      numbers in tables.
- [x] One control height: `--control-sm/md/lg` 32/40/48 (36/44 under
      `html.clj-ui-touch`) pinned on .btn, inputs, select, number field,
      tag input. Card/dialog footer button override removed.
- [x] Quiet buttons: neutral primary, outlined danger/success, `:loud true`
      for the filled version; outlined disabled keeps its shape. Badge and
      chip-active use the accent tint.
- [x] Inputs on `bg-1`, focus = accent border, no glow.
- [ ] Dark palette lift (bg-0 ~L .17): skipped for now, revisit if dark
      cards still look too black on real apps.
- [ ] Ship one body face (self-hosted Inter): not decided, system stack
      stays; the agent rules forbid font imports instead.

### Phase B: composition primitives
- [x] `ui.page`: page (:narrow/:reading/:wide/:full, :tag), page-header,
      page-section, action-row.
- [x] `ui.settings`: settings-group, settings-row.
- [x] `ui.metric`: metric (:flush), metric-row.
- [x] `ui.list-row`: list-rows, list-row (icon/thumb, meta, href/on-click,
      active).
- [x] empty-state `:fill`, `.empty-state-title`.
- [x] `util/class-attrs` shared helper; sidecars, tests, examples in all
      three targets, docs regenerated, AGENTS.md "Composing a Screen".
- [ ] Segmented control: covered by boxed button-group (keywords + note
      added); no separate component.
- [ ] server-lib `render/page` uses `ui.page` (consumer repo, rollout).

## Next: Phase C (enforcement and consumer docs)
- [ ] `lint-css` on consumer CSS in server-lib's frontend pipeline and
      `bb check`: fail on font imports, font-family on html/body, weights
      > 500, uppercase, raw hex, raw px font-size, overrides of
      `.card`/`.btn`/`.form-*`, a second page container.
- [ ] `docs/skill/`: INSTRUCTIONS.md (short always-on app rules),
      composition.md, worked `.cljc` examples (settings page, list page,
      dashboard, form page) built on `ui.page`.
- [ ] `sync-consumer-agents` section: app rules + llms.txt + skill link
      instead of the contributor AGENTS.md.
- [ ] Release (`bb release`), bump server-lib + xi, rebuild the five apps
      on `ui.page` and compare screenshots before/after.

## Review
- Verified in the browser on all three targets (hiccup site pages for
  button, form, page, settings, metric, list-row; squint and replicant
  long pages). `bb test` 273 tests green, `bb lint-css` ok, `bb check-dev`
  green.
- The hiccup hot-reloader can wedge on a batch of new interdependent
  files (SciUnbound); touching the files again in dependency order
  recovers it without a restart.
