# Plan: Arc-level polish, blocks and agent docs for clj-ui

Reference: https://uiarc.dev (Arc UI, React + Motion, shadcn registry).
Surveyed 2026-10-09: home, component pages, block pages, docs (theming,
motion, AI), `llms.txt`, `llms-small.txt`, the agent skill files
(`SKILL.md`, `INSTRUCTIONS.md`, `design.md`, `copy.md`, `composition.md`,
`checklist.md`, `components.md`) and live CSS tokens read from the page.

Goal: reach Arc's level of finish and catalogue breadth (components first,
blocks second), rebuild the demo site to the same standard, and ship agent
facing docs of the same quality. Keep what makes clj-ui ours: one `.cljc`
rendering to Hiccup, Replicant and Squint; EDN tokens and a Babashka build;
OKLCH scales; CSS-first motion; a tinted gray and purple accent.

---

## 1. What Arc has (inventory)

### 1.1 Site structure

| Area | What it is |
| --- | --- |
| Home | Hero (display headline, two CTAs) over a soft mesh gradient card. "Every example is live" section with a real metrics dashboard, wallet block, availability picker, time dial, in-view title demos. Counts in copy (154 components, 90 blocks). |
| Top nav | Arc mark, Components, Blocks, Docs, Pricing, Try the demo. Right: Search (⌘K), Appearance (theme + accent picker), dark toggle, Sign in. |
| Left sidebar | Search field with Ctrl K. Filter pills All / Free / Pro / More. Collapsible groups with item counts (Docs, Components 113, Blocks 83). Groups within components: Buttons 9, Gestures 2, Menus 3, Text fields 7, Special inputs 6, ... Items carry "Pro" and "New" badges and a green dot for recently updated. Footer: "What's new · 8 new". |
| Component page | Breadcrumb. H1 + one-line summary. Install command with copy. "Copy page" (markdown) split button. Preview / Code tabs with "Live · keyboard ready", width presets (desktop / phone), fullscreen. Then sections: When to use, When not to use, Installation, Usage, Examples, API reference (props table), Keyboard interactions, Accessibility, Motion, Responsive behavior, Performance, Notes for AI, Related, Also in \<group\>. |
| Block page | Same shell. Preview says "Live block · sample data". Extra "Built from" chips linking to the components used. API reference is usually "No props" plus where the sample data lives. |
| Docs | Introduction, Installation, Theming (tokens, dark mode, 8 accents via `data-accent`), Motion (spring presets, easing and duration tokens, reduced motion), AI and MCP, Changelog, Roadmap, Guides (6 step-by-step builds). |
| Agent surface | `/llms.txt` (index + rules for agents), `/llms-small.txt`, `/llms-full.txt`, `/components/<id>/markdown` per item, `/r/catalog.json`, `/r/registry.json`, skill files under `/r/skills/arc/`, an MCP server (`search_components`, `list_components`, `get_component`, `get_install_command`, `get_skill`). |

### 1.2 Design language (measured from the live site)

- Color: pure neutral gray (chroma 0 in OKLCH), dark theme default on the
  docs. Semantic roles: `--background`, `--surface`, `--surface-raised`,
  `--surface-muted`, `--foreground`, `--text-secondary`, `--text-muted`,
  `--border`, `--border-subtle`, `--border-strong`, `--accent`,
  `--accent-strong`, `--accent-subtle`, `--accent-foreground`, status
  `--success/--warning/--danger`, control tokens `--control-on/-glyph/-track/-thumb/-thumb-on`,
  chart `--series-1..4`. Accent is a `data-accent` attribute with 8 hues.
- Radii are large and concentric: `--radius-control` 18px (inputs,
  buttons), `--radius-panel` 26px (menus, nested panels), `--radius-surface`
  34px (cards), pill. Inner radius = outer radius minus padding.
- Type: Geist for headings 30px+, Inter for everything else, weights 400 and
  500 only, `tabular-nums` for changing numbers. Scale 12/14/16/18/22/28/36/52/72.
- Surfaces: cards rest on a 1px border, no shadow. Shadows only on floating
  layers (`--shadow-floating`: `0 22px 55px oklch(0 0 0 / .34)`).
- Spacing on a 4px grid. Control heights 36 / 44 / 50.
- Motion: springs `snappy` (presses), `smooth` (panels, no overshoot),
  `morph` (shared highlights), `responsive`, `gentle`. CSS tokens
  `--ease-standard` `cubic-bezier(.22,1,.36,1)`, `--ease-enter`,
  `--ease-spring`, `--duration-fast` 160ms, `--duration-standard` 240ms,
  `--duration-considered` 480ms. Every animation has a reduced motion branch.
- Copy: sentence case, no eyebrows, no em dashes, verbs with objects on
  buttons, honest sample data.
- A11y: no pointer focus rings, one shared `:focus-visible` ring, overlays
  trap and return focus, `role=status` announcements.

### 1.3 Components (113 public + 38 special)

Legend for the clj-ui column: ✓ have, ~ partial / close relative, ✗ missing.

**Actions**

| Arc | clj-ui | Note |
| --- | --- | --- |
| Button | ✓ `ui.button` | We lack press feedback on click and label width morph |
| Action button | ~ `btn-icon` + toolbar | No pending / success states |
| Split button | ✗ | |
| Button group | ✓ `ui.button-group` | No gliding hover highlight |
| Floating button group | ~ `ui.toolbar` | |
| Expanding button group | ✗ | |
| Copy button | ✗ | |
| Confirm morph | ✗ | |
| Hold to confirm | ✗ | |
| Swipe actions | ✗ | |
| Dropdown menu | ✗ | Context menu runtime can anchor to a button: cheap |
| Context menu | ✓ `ui.context-menu` | Long press on touch already solved |
| User menu | ~ `sidebar-user` | |
| Theme switcher (+ eclipse / split / rise) | ✓ `ui.theme-toggle` | No transition variants |

**Inputs**

| Arc | clj-ui | Note |
| --- | --- | --- |
| Input, Textarea | ✓ `ui.form` | |
| Password field, Password strength | ✗ | |
| Search field | ✓ `form/search-bar` | |
| Expanding search | ✗ | |
| Inline edit | ✗ | |
| Number field | ✓ `ui.number-field` | |
| Money input, Phone input | ✗ | |
| Tag input | ✓ `ui.tag-input` | |
| Mention input | ✗ | |
| Shortcut recorder (+ Kbd) | ✗ | Command already renders key badges |
| Select | ✓ `ui.select` | |
| Combobox | ~ `ui.command` | Needs a field-shaped variant |
| Multi-select | ~ `ui.tag-input` dropdown | |
| Morph select | ✗ | |
| Chip group | ~ `ui.chip` | No group / selection semantics |
| Radio cards | ✗ | |
| Billing toggle | ✗ | |
| Checkbox, Radio group | ✓ `ui.form` | Native, lightly styled |
| Switch | ✓ `ui.switch` | |
| Segmented control | ~ tabs / button-group | Should be its own component |
| Slider, Elastic slider | ~ `form-range` native | |
| Calendar | ✓ `ui.calendar` + `ui.calendar-events` | Stronger than Arc's |
| Date picker, Date range picker | ✗ | Calendar inside popover: cheap |
| Time picker | ✗ | |
| Color picker | ✓ `ui.color-picker` | |
| Rich text editor, Signature pad | ✗ | Out of scope for now |
| File dropzone | ✓ `ui.drop-zone` | |

**Disclosure**

| Arc | clj-ui | Note |
| --- | --- | --- |
| Tabs, Breadcrumb, Accordion | ✓ | |
| Scroll area | ✗ | |
| Expandable card | ✗ | |
| Resizable panels | ✓ `ui.panels` | |
| Dialog | ✓ `ui.dialog` | |
| Drawer, Bottom sheet | ✗ | Dialog variants |
| Popover | ✓ `ui.popover` | |
| Hover card | ✗ | Popover on hover / focus |
| Tooltip | ✓ `ui.tooltip` | |

**Feedback**

| Arc | clj-ui | Note |
| --- | --- | --- |
| Alert, Toast, Progress, Skeleton | ✓ | |
| Toast stack | ~ `toast-flash` | |
| Announcement bar | ✗ | |
| Stepper | ✗ | |
| Countdown | ✗ | |
| Usage meter | ✗ | |

**Data**

| Arc | clj-ui | Note |
| --- | --- | --- |
| Pagination, Badge, Card, Empty state | ✓ | |
| Avatar, Avatar group | ✗ | sidebar-user draws its own |
| Metric card | ✗ | |
| Line / Bar / Donut chart, Sparkline, Gauge, Activity heatmap | ✗ | Pure SVG from data: ideal `.cljc` |
| Animated counter | ✗ | |
| Streamgraph, Brush, Waffle, Slope, Ridgeline, Treemap (+ Pro charts) | ✗ | Later |
| Sortable data table | ~ `ui.table` | No sort |
| Data grid | ✗ | Later |
| Tree view | ~ `ui.file-browser` | |
| Filter toolbar | ✗ | |
| Code block | ~ `ui.markdown` | No standalone component, no copy |
| JSON viewer | ✗ | |
| Timeline (activity feed) | ✗ | Our `ui.timeline` is an editing timeline; name clash |
| Comment thread | ✗ | |
| Chat thread | ✓ `ui.chat` | |
| Image compare, Carousel, Card stack | ✗ | |

**Text**: Text reveal, In-view title, Text morph, Text shimmer, Slot text: all ✗.

**Special (mostly Pro)**: dock, liquid tab bar, glass menu/card/tab bar,
sheet stack, wallet stack, control center, product gallery, lightbox gallery
(~ our `ui.lightbox` with pinch zoom), photo grid (~ our `ui.grid`), cover
flow, voice orb, now playing (~ our `ui.player-bar`), voice recorder,
booking pill, activity rings, time dial, date reel, typewriter terminal,
morph loader, skeleton morph, page curl, orbit logos, dot grid, gravity well,
data flow, payment flow, multi-region failover, vector search.

**clj-ui components Arc does not have**: camera, grid (tile / masonry with
size stepper), file-browser, file-progress, processing-bar, player-bar,
calendar-events (agenda, ticker strip, day and week timelines), editing
timeline, markdown renderer, toolbar, lightbox with zoom, touch and
long-press runtime.

### 1.4 Blocks (83, grouped as on the site)

- Analytics (Pro): Metrics dashboard, KPI drilldown, Metric explorer,
  Revenue explorer, MRR waterfall, Cohort retention, Usage forecast,
  Journey flow, Choropleth explorer, Connected scatter
- Finance (Pro): Market terminal, Budget variance
- Line charts (Pro): Small multiples, Line replay
- AI (Pro): AI chat, AI composer, Agent run, AI side panel
- App shell: Sidebar (workspace, rail, inbox, docs), Page header,
  Command palette, Notification center, Empty states, Search results
- Auth: Login and sign up (sign-in, centered, split, immersive, signup
  form), OTP input, Multi-step form
- Settings (Pro): Settings page, Settings command, Security settings,
  Billing overview, Usage billing, Cancel flow
- People (Pro): Team members, Invite people, Team directory, Roles and
  permissions
- Developer (Pro): API keys, Webhooks, Integrations, Environment variables
- Pricing: Plan comparison, Pricing calculator, Usage pricing, Cart drawer,
  Checkout summary
- Commerce (Pro): Invoice studio, Wallet card, Product listing, Product detail
- Support (Pro): Inbox triage, Support conversation, Support widget, Help center
- Media: File upload, Media player
- Projects (Pro): Week calendar, Availability picker, Project board,
  Release readiness
- Heroes: Hero section, Hero signup
- Features: Scroll story, Spotlight grid, Feature bento, Comparison table
- Social proof: Logo marquee, Studio perspectives, Stats band
- Content: Changelog feed, FAQ section, Contact section, Team showcase, Blog grid
- Headers and footers: Site header, Site footer, CTA section, Newsletter signup
- Showpieces (Pro): Activity terrain, Revenue globe, Customer galaxy,
  Semantic zoom, Layout morph

clj-ui has no formal blocks. The dev pages `sidebar`, `files`, `calendar`
and the `header-patterns` demo are block candidates already.

### 1.5 Per-item markdown shape (what agents get)

```
# <Title>
> one-line summary
- Type / Access / Page / Markdown / Registry / Source / Dependencies / Keywords
## When to use           (3 bullets)
## When not to use       (bullets naming the better item)
## Installation
## Usage                 (one code sample)
## Examples
## API reference         (props table: name, type, default, description)
## Keyboard interactions (table)
## Accessibility
## Motion
## Responsive behavior
## Performance
## Notes for AI
## Related / Also in <group>
## Guidance for AI tools (boilerplate rules)
```

---

## 2. Where clj-ui stands

- 48 components, 77 icons, three render targets, generated theme, JS
  runtime for touch, gestures, lightbox zoom, menus, command palette.
- Tokens: OKLCH scales for gray (hue 285) and eight hues, semantic
  `bg-0/1/2`, `fg-0/1/2`, `border-0/1/2`, `shadow-0..3`, `radius-xs..lg`
  (4/6/10/16px). Cards carry `shadow-0`. No motion tokens, no
  `accent-subtle`, no surface-muted, no control tokens, no concentric radii.
- Typography: system font stack, no documented weight rule, font scale is
  geometric 1.25.
- Demo: one long page per target behind a sidebar, anchor nav, no search,
  no per-component pages, no props tables, no code view. Replicant and
  Squint are separate SPAs reached through a target switcher.
- Agent docs: `AGENTS.md` (deep, pitfall-oriented), generated
  `docs/components.md` (names + public fns only), `calendar.md`. No "when
  to use", no props tables, no `llms.txt`, no skill files.

## 3. What we keep (the soul)

1. **One `.cljc`, three targets.** Arc is React only. Every component page
   on our demo will have a Hiccup / Replicant / Squint switcher on the
   preview, proving the same source renders everywhere. That is our
   headline feature, not an implementation detail.
2. **Server-renderable by default.** Hiccup output works with no JS; the
   runtime upgrades it. Blocks must render meaningfully as static HTML.
3. **EDN tokens, Babashka build, no node at runtime.** Theme stays
   generated from `tokens.edn`; the demo server stays a bb process.
4. **OKLCH scales and a tinted palette.** Keep the purplish gray and purple
   accent as the default identity. Add the neutral and other accents as
   `data-accent` options so Arc-style neutral is one attribute away.
5. **CSS-first motion.** No spring library. Use transitions with shared
   easing and duration tokens, `@starting-style`, `transition-behavior:
   allow-discrete`, `interpolate-size: allow-keywords`, the View Transitions
   API for shared-element morphs where available. JS only where a gesture
   or measurement is required (already the `src/ui/js` rule).
6. **Pragmatic, app-driven catalogue.** We keep the components Arc lacks
   (camera, file browser, processing bar, editing timeline, calendar
   events). Parity is a direction, not a checklist to copy blindly; Pro
   showpieces (globes, galaxies, page curls) are out of scope.

## 4. What we adopt from Arc

- Semantic token layer on top of the scales: surface levels, subtle and
  strong borders, accent subtle, control tokens, series colors.
- Concentric radius system with `--radius-control / -panel / -surface /
  -pill` and the inner = outer minus padding rule.
- Motion tokens and the "animate transform and opacity, reduced motion
  branch for everything" rule.
- Type rules: two weights, tabular numbers, sentence case, no eyebrows.
- Press feedback on every button, shared `:focus-visible` ring, no pointer
  focus rings.
- Cards on a border without shadow, shadows only on floating layers.
- Per-component metadata (when to use, when not, props, keyboard, a11y,
  motion, responsive, notes for AI) and the generated markdown it drives.
- The docs site information architecture: grouped sidebar with counts,
  ⌘K search, Preview / Code tabs, Copy page, llms.txt, skill files.
- Blocks as a first-class tier built only from components, with sample
  data and "Built from" links.

---

## 5. Phased plan

Each phase ends with `bb check-dev`, `bb test`, `bb gen-docs`, and a
commit. Releases and consumer rollouts only when asked (see AGENTS.md).

### Phase 0: Foundation refresh (tokens, type, motion, focus)

Deliverable: the same 48 components, visibly closer to Arc's finish, with
no API changes.

1. `tokens.edn`: add semantic tokens `surface-0/1/2/muted` (alias of
   `bg-*` for now), `border-subtle`, `border-strong`, `accent-subtle`,
   `accent-strong`, `fg-on-accent`, `control-on/-track/-thumb/-glyph`,
   `series-1..4`, `shadow-floating`. Add `radius-control`, `radius-panel`,
   `radius-surface`, `radius-pill`. Add motion tokens `duration-fast /
   -standard / -considered` and `ease-standard / -enter / -spring`. Add
   control heights `control-sm/md/lg`.
2. Accent as an attribute: generate `[data-accent="neutral|blue|green|
   amber|orange|coral|rose"]` blocks that remap `--accent-*` to another
   scale. Default stays purple.
3. Base styles (`ui/base.css`, new): font stack (decision below), weights
   400 and 500, `tabular-nums` utility, global `:focus-visible` ring using
   `--focus-outline`, `prefers-reduced-motion` kill switch for transitions
   and animations, `text-size-adjust` already in mobile.css.
4. Sweep component CSS: cards drop `shadow-0`, floating layers use
   `shadow-floating`, buttons and inputs move to `radius-control`, panels
   to `radius-panel`, cards to `radius-surface`, every transition uses the
   motion tokens, press feedback (`:active` scale 0.97) on `.btn` and
   interactive chips, hover highlights use `accent-subtle`.
5. Font decision (needs your call): (a) keep system stack, (b) self-host
   Inter for body and keep system for display, (c) self-host Inter plus a
   distinct display face so we are not a Geist clone. Recommendation: (c),
   with a display face chosen in the demo redesign step. Fonts are served
   from the demo site and optional for consumers (tokens fall back to the
   system stack).
6. Lint task `bb lint-css`: fail on raw hex, raw rem, weights above 500,
   em dashes in `.cljc` strings, `text-transform: uppercase`, hover rules
   outside the gate (already auto-wrapped).

### Phase 1: Component metadata and generated docs (the agent backbone)

Deliverable: every component has structured metadata; one generator
produces per-component markdown, `llms.txt`, and feeds the demo site.

1. Sidecar `src/ui/<component>.edn` next to each `.cljc`:
   ```edn
   {:id :button
    :title "Button"
    :summary "A clear, responsive action with quiet secondary states."
    :category :actions  :group "Buttons"
    :keywords ["action" "loading button"]
    :when-to-use ["..."]  :when-not-to-use ["Use split-button when ..."]
    :api [{:fn ui.button/button
           :props [{:name :variant :type "#{:primary :secondary :ghost :danger :success :link}" :default :primary :doc "..."}]}]
    :keyboard [["Enter / Space" "Activates the button."]]
    :accessibility ["..."] :motion ["..."] :responsive ["..."]
    :notes-for-ai ["..."]
    :related [:button-group :toolbar]
    :targets #{:clj :cljs :squint}
    :runtime [:ui.js.gestures]}
   ```
   EDN rather than docstrings so Squint bundles stay free of doc text and
   so bb tooling can read it without loading the component.
2. `tools.registry` reads the sidecars; `bb gen-docs` emits
   `docs/components/<id>.md` in the Arc markdown shape (section 1.5),
   `docs/components.md` as the index, `docs/llms.txt` (index + rules for
   agents + install), `docs/llms-full.txt` (inlined). A test fails when a
   `.cljc` has no sidecar or a sidecar names a prop the fn does not
   destructure.
3. Examples live with the demo: `dev/shared/src/dev/demos.cljc` is split
   into `dev/shared/src/dev/examples/<component>.cljc`, each exporting a
   vector of `{:id :title :render (fn [] hiccup) :source "..."}`. The
   source string is captured by a macro at compile time so the Code tab
   shows exactly what ran.
4. Backfill metadata for all 48 components (biggest writing task; split
   across sessions, roughly 10 per sitting).

### Phase 2: Demo site rebuild (Hiccup, served by the existing bb server)

Deliverable: a docs site at the quality of uiarc.dev, built from our own
components (dogfooding), served by `dev/hiccup` locally and on the
production host.

Information architecture:

- `/` home: hero over a gradient card, three live showcases (calendar
  agenda, command palette, a metrics dashboard once charts exist), "one
  source, three targets" section with the switcher, counts pulled from the
  registry, links to components, blocks, docs, llms.txt.
- `/components` and `/components/<id>`: sidebar grouped by category with
  counts; page layout per section 1.5. Preview card with tabs Preview /
  Code / Props, a target switcher (Hiccup inline, Replicant and Squint in
  an iframe of the SPA at `/replicant/#/<id>` and `/squint/#/<id>`),
  width presets, fullscreen. "Copy page" copies the generated markdown.
- `/blocks` and `/blocks/<id>`: same shell, "Built from" chips.
- `/docs/*`: introduction, installation (git dep, bb tasks, CSS, runtime),
  theming (tokens, dark mode, accents, scales), motion, agents (llms.txt,
  skill files, how to point Claude Code at it), changelog (from git tags
  or `CHANGELOG.md`).
- `/components/<id>/markdown`, `/llms.txt`, `/llms-full.txt`,
  `/catalog.edn` and `/catalog.json` served from the registry.
- ⌘K search over components, blocks and docs using `ui.command`.
- Appearance popover: theme (light / dark / auto) and accent picker.

Work items:

1. Router and page shell in `dev/hiccup`: new `dev.site.*` namespaces
   (layout, nav, component page, block page, docs page, markdown routes).
   Keep `/dev/changes` live reload.
2. Replicant and Squint SPAs become example hosts: hash route `#/<id>`
   renders that component's examples full-bleed so the iframe switcher
   works; the existing long pages remain at `#/all` for `bb check-dev`.
3. Use the rebuild to design the missing components the site itself
   needs: segmented control (Preview / Code), code block with copy, kbd,
   avatar, dropdown menu (Copy page split), scroll area (sidebar), inline
   search field, announcement bar.
4. Visual pass against the screenshots: large concentric radii, calm
   neutral surfaces, 400/500 weights, generous section spacing (64 to
   120px on the home page), one accent use per view.
5. `bb check-dev` extended: every registry id resolves to a page, every
   example renders in all three targets without errors.

### Phase 3: Component parity, tier 1 (compose what we have)

Each item: `.cljc` + `.css` + `.edn` + examples + tests. Ordered by
leverage; roughly 2 to 4 per session.

1. dropdown-menu (context menu runtime anchored to a trigger)
2. segmented-control
3. avatar, avatar-group
4. kbd, code-block (with copy), copy-button
5. metric-card, stat band primitives
6. date-picker, date-range-picker (calendar in popover)
7. stepper
8. drawer and bottom-sheet (dialog placement variants, drag to dismiss in runtime)
9. hover-card
10. toast-stack
11. sortable columns for `ui.table`, filter-toolbar
12. chip-group, radio-cards
13. scroll-area
14. usage-meter, announcement-bar
15. inline-edit, password-field with strength
16. activity-feed (Arc's "timeline"; keeps our editing `ui.timeline` name)
17. tree-view (generalise file-browser list)
18. user-menu (sidebar-user + dropdown)

### Phase 4: Component parity, tier 2 (needs runtime or SVG work)

1. Charts as pure functions over data, SVG output, `.cljc`: sparkline,
   line-chart, bar-chart, donut-chart, gauge, activity-heatmap. Hover
   crosshair and keyboard scrubbing in a `ui.js.charts` runtime module.
2. animated-counter and slot-text (CSS counters plus a small runtime)
3. text-morph, text-shimmer, text-reveal, in-view-title (CSS and
   IntersectionObserver)
4. slider (custom track and thumb, range variant)
5. combobox and multi-select (command runtime in a field)
6. confirm-morph, hold-to-confirm (gestures runtime)
7. swipe-actions (gestures runtime)
8. carousel, image-compare
9. expandable-card, countdown, json-viewer, comment-thread
10. theme-switch transition variants via View Transitions

### Phase 5: Blocks

`src/ui/blocks/<id>.cljc` plus `.css` and `.edn`; built only from
components; sample data in a `sample` var; render statically in Hiccup.
Start with the ones we are closest to, then the ones the site needs, then
the marketing set.

1. From existing work: app-shell sidebar (from the `sidebar` page),
   file-upload (drop-zone + file-progress + processing-bar), command
   palette, media-player (player-bar), week-calendar and agenda (calendar
   events), empty-states, page-header (header patterns demo).
2. Product: settings-page, login and signup, otp-input, notification-center,
   team-members, invite-people, api-keys, metrics-dashboard (after
   charts), project-board, inbox-triage, agent-run and ai-chat (natural for
   xi), release-readiness.
3. Marketing: site-header, site-footer, hero-section, stats-band,
   plan-comparison, comparison-table, faq-section, cta-section,
   changelog-feed, logo-marquee, newsletter-signup.

### Phase 6: Agent skill package and consumer rollout

1. `docs/skill/` mirroring Arc's set, written for our stack: `SKILL.md`
   (workflow, never-do list, quick decisions), `INSTRUCTIONS.md` (short
   rules for AGENTS.md), `design.md`, `copy.md`, `motion.md`,
   `composition.md`, `accessibility.md`, `responsive.md`, `checklist.md`
   with bb grep sweeps, `components.md` (choose by job, with ids and target
   notes), worked examples (settings page, dashboard, xi-style agent run).
2. `bb sync-consumer-agents` links the skill, `llms.txt` and the site in
   every consumer's AGENTS.md section. The existing deep AGENTS.md stays
   the contributor guide.
3. Optional later: an MCP server (bb http-kit) exposing
   `search_components`, `get_component`, `get_skill` from the registry,
   registered as an xi tool.
4. `bb release`, bump consumers, rebuild, deploy per AGENTS.md.

---

## 6. Decisions needed before Phase 0

1. Fonts: keep system stack, or self-host Inter (plus a display face)?
2. Default look: keep tinted gray + purple as default with neutral as an
   accent option (recommended), or flip to neutral by default?
3. Radii: go as large as Arc (18 / 26 / 34px) or a tamer set (12 / 18 / 24px)?
   Large radii are most of Arc's visual identity; tamer keeps us distinct.
4. Metadata location: EDN sidecar per component (recommended) or a
   `defmeta` inside the `.cljc`?
5. Block namespace: `ui.blocks.*` inside this repo (recommended) or a
   separate git dep?

## 7. Order of work and rough effort

| Phase | Sessions | Depends on |
| --- | --- | --- |
| 0 Foundation | 2 to 3 | decisions 1 to 3 |
| 1 Metadata + docs generator | 3 to 5 (writing heavy) | decision 4 |
| 2 Demo site | 4 to 6 | 0, 1 |
| 3 Tier 1 components | 6 to 9 | 1 (each lands with metadata) |
| 4 Tier 2 components | 8 to 12 | 3 |
| 5 Blocks | 6 to 10 | 3, partly 4 |
| 6 Skill + rollout | 2 to 3 | 1, 2 |

Phases 3 to 5 interleave with the site: every new component or block
appears on the site the moment its sidecar exists.
