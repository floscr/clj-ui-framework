# Arc parity: task tracker

Full inventory, gap analysis and rationale: `docs/uiarc-parity-plan.md`.
Nothing below starts until the decisions in that doc's section 6 are made.

## Decisions (taken 2026-10-09 with the recommended defaults)

- [x] Fonts: system stack behind `--font-body` / `--font-display` tokens; a self-hosted face is a one-token swap later
- [x] Default palette: tinted gray + purple; `data-accent` offers neutral, blue, teal, green, amber, orange, red, pink
- [x] Radii scale: concentric set, control 9 / panel 14 / surface 18 / pill (started at 12 / 18 / 24, scaled to 75% on review)
- [x] Metadata: EDN sidecar per component (Phase 1)
- [x] Blocks live in `ui.blocks.*` in this repo (Phase 5)

## Phase 0: Foundation refresh (done 2026-10-09)

- [x] tokens.edn: bg-1 is white in light (new gray-0 step), border-subtle, accent-subtle/strong, danger/success-strong, control-track/thumb, series-1..4, shadow-floating, focus-outline
- [x] tokens.edn: radius-control / panel / surface / pill, control heights (`:static`)
- [x] tokens.edn: duration-* and ease-* motion tokens, font + weight tokens (`:static`)
- [x] gen.clj: `:static` emitted into :root; `[data-accent]` blocks from `:accents` (scale remap or token override)
- [x] base styles: font tokens, two weights (headings/strong/th medium), mono on code/kbd, shared :focus-visible ring, prefers-reduced-motion kill switch; `.tabular-nums` utility
- [x] CSS sweep (48 files): every transition on motion tokens, no weight > 500, no uppercase labels, cards on a border without shadow, floating layers on bg-1 + shadow-floating + radius-panel/surface, controls on radius-control, concentric boxed groups, press feedback on .btn, accent-strong hovers instead of brightness filters
- [x] `bb lint-css` (errors: hex, literal durations/easings, weight > 500, uppercase, hand-made outlines; warnings: raw rem) wired into `bb release`
- [x] theme-test covers new tokens, static block, accents, base styles; `bb test` green, `bb build-theme` ok
- [ ] `bb check-dev` against running dev servers (servers were not running in this session; Replicant and Squint still need a visual pass)
- [ ] em-dash lint for UI copy in `.cljc` strings (deferred: docstrings use them legitimately)

## Phase 1: Component metadata and generated docs (done 2026-10-09, except the examples split)

- [x] Sidecar EDN schema + validation in tools.registry (required keys, public fns, destructured or documented props, related ids, categories)
- [x] `bb gen-docs` emits docs/components/<id>.md, docs/components.md, docs/llms.txt, docs/llms-full.txt
- [x] ui.registry-test: every component has a valid sidecar; markdown renders; llms.txt has the agent rules
- [x] Split dev/shared demos.cljc into dev/examples/<component>.cljc (done 2026-10-10; examples are {:id :title :render} maps, source is read from the files by the site instead of a macro)
- [x] All 46 sidecars written (ui.theme and ui.util are helpers, exempt)

## Phase 2: Demo site rebuild (decisions 2026-10-10: system font, catalogue-first home)

- [x] dev.example helpers + dev.examples.<component> files + dev.examples.registry; the three targets render from the registry
- [x] dev.site: catalogue home (`/`, grouped by category with cards), sidebar nav grouped by category from the registry, old long page moved to `/all`
- [x] Component page `/components/<id>`: breadcrumb, title, summary, meta, target switcher, every example with Preview and Code tabs (source read with rewrite-clj), then when to use, usage, API tables, keyboard, accessibility, motion, responsive, targets, notes for AI, related
- [ ] Component page extras: Copy page (markdown to clipboard), width presets, props tab per example
- [ ] Appearance popover in the shell (theme + accent); top nav
- [ ] Replicant + Squint SPAs: `#/<id>` example host routes so the switcher lands on the component; keep `#/all`
- [ ] Docs pages: introduction, installation, theming, motion, agents, changelog
- [x] Routes: /components/<id>/markdown, /llms.txt, /llms-full.txt
- [ ] /catalog.edn and /catalog.json
- [x] ⌘K search via ui.command (home page trigger + palette over every component)
- [ ] Site-driven components: segmented-control, code-block, kbd, avatar, dropdown-menu, scroll-area, announcement-bar
- [ ] check-dev: every registry id has a page and renders in all three targets

## Phase 3: Tier 1 components

- [ ] dropdown-menu
- [ ] segmented-control
- [ ] avatar, avatar-group
- [ ] kbd, code-block, copy-button
- [ ] metric-card
- [ ] date-picker, date-range-picker
- [ ] stepper
- [ ] drawer, bottom-sheet
- [ ] hover-card
- [ ] toast-stack
- [ ] table sorting, filter-toolbar
- [ ] chip-group, radio-cards
- [ ] scroll-area
- [ ] usage-meter, announcement-bar
- [ ] inline-edit, password-field + strength
- [ ] activity-feed
- [ ] tree-view
- [ ] user-menu

## Phase 4: Tier 2 components

- [ ] charts: sparkline, line, bar, donut, gauge, activity-heatmap (+ ui.js.charts)
- [ ] animated-counter, slot-text
- [ ] text-morph, text-shimmer, text-reveal, in-view-title
- [ ] slider (single + range)
- [ ] combobox, multi-select
- [ ] confirm-morph, hold-to-confirm
- [ ] swipe-actions
- [ ] carousel, image-compare
- [ ] expandable-card, countdown, json-viewer, comment-thread
- [ ] theme-switch transition variants

## Phase 5: Blocks

- [ ] From existing: app-shell sidebar, file-upload, command-palette, media-player, week-calendar, empty-states, page-header
- [ ] Product: settings-page, login + signup, otp-input, notification-center, team-members, invite-people, api-keys, metrics-dashboard, project-board, inbox-triage, agent-run, ai-chat, release-readiness
- [ ] Marketing: site-header, site-footer, hero-section, stats-band, plan-comparison, comparison-table, faq-section, cta-section, changelog-feed, logo-marquee, newsletter-signup

## Phase 6: Skill package and rollout

- [ ] docs/skill/: SKILL.md, INSTRUCTIONS.md, design.md, copy.md, motion.md, composition.md, accessibility.md, responsive.md, checklist.md, components.md, examples
- [ ] sync-consumer-agents links skill + llms.txt + site
- [ ] Optional: MCP server over the registry
- [ ] bb release, consumer bumps, deploy (only when asked)

## Review

### Phase 0 (2026-10-09)

Verified by rendering the hiccup components page to static HTML and
screenshotting it in Chrome, light and dark: buttons, cards, card lists,
accordion, table, popover (white, floating shadow, panel radius), button
groups (concentric), header patterns, forms. No console errors. Not yet
verified: Replicant and Squint targets, consumer apps (bg-1 turning white
in light mode and the larger radii are the visible changes they will pick
up on the next sha bump).
