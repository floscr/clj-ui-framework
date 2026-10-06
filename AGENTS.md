# UI Framework — Agent Guide

A cross-target component library for Clojure, ClojureScript (Replicant), and Squint (Eucalypt). Components are `.cljc` files using reader conditionals. CSS is generated from EDN tokens via Babashka.

## Installation (Git Dependency)

**NEVER use git submodules or worktrees** — they cause stale checkouts, missing theme CSS, and broken builds across repos.

Add as a **git dependency** in `bb.edn` or `deps.edn`:

```edn
{:deps {clj-ui-framework/clj-ui-framework
        {:git/url "https://github.com/floscr/clj-ui-framework"
         :sha "<latest-sha>"}}
 :paths ["src"]}
```

Babashka/Clojure resolves the dependency automatically — no manual path or submodule needed.

### CSS — CRITICAL

All component CSS and design tokens are bundled into **one CSS string** by `ui.css.gen/build-css`. This function finds default tokens and component CSS files on the classpath automatically — no paths to configure.

**Add a bb task** in your app's `bb.edn`:

```edn
ui:css
{:doc "Generate UI framework CSS"
 :requires ([ui.css.gen :as css])
 :task (css/build-css {:output "resources/public/ui.css"})}
```

Then run `bb ui:css` and include it in your HTML:

```html
<link rel="stylesheet" href="/ui.css">
```

**Custom theme** — pass token overrides (deep-merged with defaults):

```edn
;; Blue accent instead of purple
ui:css
{:requires ([ui.css.gen :as css])
 :task (css/build-css {:output "resources/public/ui.css"
                       :scales {:color {:accent {:hue 220}}}})}
```

**Generate at server boot** — `build-css` returns a CSS string, no file needed:

```clojure
(require '[ui.css.gen :as css])
(def theme-css (css/build-css))           ; default theme
(def theme-css (css/build-css {:tokens {:accent "var(--accent-600)"}})) ; override
```

**Do not duplicate theme tokens** in your app's CSS. If colors/spacing look wrong, you're missing the generated CSS.

**Watch mode** — auto-rebuild CSS when component styles or tokens change:

```edn
watch:css
{:doc "Watch and rebuild UI CSS on changes"
 :requires ([ui.css.gen :as css])
 :task (css/watch-css {:output "resources/public/ui.css"})}
```

`watch-css` automatically watches all `src/ui/*.css` component styles and `theme/tokens.edn` from the classpath. Pass `:watch` to add extra files or directories:

```edn
watch:css
{:requires ([ui.css.gen :as css])
 :task (css/watch-css {:output "resources/public/ui.css"
                       :watch ["src/my-app.css" "src/styles/"]
                       :scales {:color {:accent {:hue 220}}}})}
```

Options (in addition to all `build-css` options):
- `:watch` — vector of extra file paths or directories to watch
- `:interval` — poll interval in ms (default 500)
- `:on-rebuild` — callback `(fn [{:keys [output css]}])` called after each successful rebuild

The function blocks the calling thread. Run it as a standalone bb task (`bb watch:css`).

### JS Runtime — Components with Client-Side Interactivity

Some components (context menus, tooltips, etc.) need browser-side JS for positioning and event handling. The JS runtime is pre-built and bundled in the repo — no build step required.

**Get the JS string** (e.g. to inline in `<script>`):

```clojure
(require '[ui.css.gen :as css])
(def runtime-js (css/build-js))           ; returns JS string
```

**Write to a file:**

```clojure
(css/build-js {:output "resources/public/ui-runtime.js"})
```

**Add to your HTML** — load before app code:

```html
<script>{runtime-js}</script>
<!-- or as external file -->
<script src="/ui-runtime.js"></script>
```

Without this script, components that need client-side interactivity (context menus, tooltips) won't work in hiccup/server-rendered targets.

**Squint apps** — squint can't resolve git deps. Use `ui.setup/setup!` which symlinks sources AND generates CSS. Add `lib/ui` and `lib/theme.css` to `.gitignore`.

**Updating** — bump the `:sha` in `bb.edn`/`deps.edn`, then re-run `bb ui:css` (or `bb frontend:setup` for squint apps).

### Example: Babashka Server

A full working example lives in `examples/babashka-server/`. It demonstrates:
- Using the framework as a git dep
- Generating CSS at server boot (no build step, no static CSS files)
- Rendering components via hiccup on the server

**`bb.edn`:**

```edn
{:deps {clj-ui-framework/clj-ui-framework
        {:git/url "https://github.com/floscr/clj-ui-framework"
         :git/sha "<sha>"}}
 :paths ["src"]

 :tasks
 {serve
  {:doc "Start the example server"
   :requires ([example.server :as server])
   :task (do (server/start! {:port 8090})
             (deref (promise)))}}}
```

**Server pattern** — generate CSS once, inline it in `<style>`:

```clojure
(ns example.server
  (:require [org.httpkit.server :as http]
            [hiccup2.core :as h]
            [ui.css.gen :as css]
            [ui.button :as button]))

(def theme-css (css/build-css))  ; generated once at boot
(def runtime-js (css/build-js))  ; pre-built JS for interactive components

(defn page []
  (str (h/html
    [:html
     [:head [:style (h/raw theme-css)]]
     [:body
      (button/button {:variant :primary} "Click me")
      [:script (h/raw runtime-js)]]])))
```

The JS runtime is only needed for components with client-side interactivity (context menus, tooltips, etc.). If you only use static components like buttons and cards, you can skip it.

```clojure
(defn handler [_]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (page)})

(defn start! [{:keys [port]}]
  (http/run-server #'handler {:port port}))
```

Run with `bb serve`. No build step required — CSS is generated from tokens at startup.

## Project Structure

```
src/
  theme/tokens.edn          # Design tokens (colors, borders, shadows, radii)
  ui/
    theme.cljc               # Token helpers (css-var)
    button.cljc              # Button component (reference implementation)
    button.css               # Button component styles (read by gen.clj)
    css/gen.clj              # EDN → CSS generator (babashka)
    js/                      # Shared JS runtime (squint source → compiled IIFE)
      squint.edn             # Squint compiler config for this directory
      context_menu.cljs      # Context menu JS runtime
dist/
  theme.css                  # Generated CSS (tokens + component styles)
  ui-runtime.js              # Compiled JS runtime (squint → esbuild bundle)
test/ui/
  button_test.clj            # Unit tests for button-classes, button component
  theme_test.clj             # Unit tests for CSS generation
dev/
  index.html                 # Tab shell with iframes for all 3 targets
  hiccup/src/dev/hiccup.clj  # Babashka httpkit server (port 3003)
  replicant/                 # shadow-cljs + Replicant (port 3001)
  squint/                    # Vite + Squint + Eucalypt (port 3002)
```

## Commands

```sh
bb build-theme    # Generate dist/theme.css, copy to dev targets
bb build-js-runtime # Compile squint JS runtime into dist/ui-runtime.js
bb test           # Run all unit tests
bb dev            # Start all dev servers in tmux (ui-dev session)
bb dev:stop       # Stop dev tmux session
bb dev:restart    # Restart all dev servers
bb dev:attach     # Attach to dev tmux session
bb dev:logs       # Show recent dev server logs
bb dev:status     # Show dev server status
bb dev-hiccup     # Start hiccup server only (port 3003)
bb dev-replicant  # Start replicant dev only (port 3001)
bb dev-squint     # Start squint dev only (port 3002)
bb list-components        # List all components + public API
bb list-icons             # List all icons (* = dedicated filled variant)
bb gen-docs               # Regenerate docs/components.md
bb scan-consumers         # Find repos depending on this framework
bb sync-consumer-agents   # Upsert UI-framework note into consumers' AGENTS.md
```

Replicant and squint need `npm install` in their dev directories first.

## Deploying — CRITICAL

**The deploy target is the `hetzner` remote, NOT `origin`.** Pushing to
`hetzner` triggers a post-receive hook that regenerates `dist/theme.css`,
copies assets to dev targets, and restarts the running service:

```sh
git push hetzner master   # deploys: rebuilds CSS + restarts service
```

`origin` (git.example.com) is the source-of-truth mirror consumers
pull as a git dependency — push there too so downstream repos can bump the
`:sha`. A full deploy pushes to **both**:

```sh
git push origin master
git push hetzner master
```

There is also a `github` remote (`github.com/floscr/clj-ui-framework`).
Some consumers (currently **xi**) pin the GitHub URL, not gitea, so a sha
that only exists on `origin` fails to resolve for them. When rolling out,
check each consumer's `:git/url` and push `github` too if any points there:

```sh
git push github master
```

The production server (ui.example.com) serves all three
targets: hiccup renders live, while the Replicant and Squint SPAs are
served as **committed static builds** at `/replicant/` and `/squint/`
(the server has no node/npm — same reason `src/ui/ui-runtime.js` is
committed). After changing components or the dev SPA pages, rebuild and
commit the artifacts before deploying:

```sh
bb build-demos   # → dev/replicant/prod/js/main.js + dev/squint/dist/
```

Pushing is a shared, hard-to-reverse action — only deploy when explicitly asked.

## Rolling Out a Shared Resource to All Consumers

When a shared component/feature/fix is added here and should reach the
consumer apps ("update all consumers"), follow this pipeline. Steps 2+
are shared/hard-to-reverse — only run them when explicitly asked to roll
out/deploy.

1. **Verify in the framework**: `bb build-theme`, `bb build-js-runtime`
   (if `src/ui/js/` changed), `bb test`, `bb check-dev`, and `bb gen-docs`
   (if components/icons changed). Commit.
2. **Push the remotes**: `git push origin master && git push hetzner master`,
   plus `git push github master` if any consumer pins the GitHub URL (xi
   does — verify with `grep git/url ~/Code/Projects/xi/deps.edn`; server-lib
   uses gitea). Then grab the new full sha: `git rev-parse master`.
3. **Find consumers**: `bb scan-consumers` — lists every consumer project
   and marks stale pins.
4. **Bump the active consumers** (sed-replace the old full sha with the new
   one — shas are pinned in full 40-char form):
   - `~/.config/dotfiles/modules/services/bb-services/server-lib/{bb.edn,deps.edn}`
     — covers all bb-services transitively (photos, music-server,
     explorer, image-editor, media-server, …). Commit only those two
     files — the dotfiles repo often has unrelated WIP.
   - `~/Code/Projects/xi/deps.edn` — xi ALSO carries checked-in static
     assets; refresh them from here:
     `cp dist/theme.css  ~/Code/Projects/xi/resources/public/theme.css`
     `cp dist/ui-runtime.js ~/Code/Projects/xi/resources/public/ui-runtime.js`
     (theme.css only needed for CSS changes, ui-runtime.js only for
     `src/ui/js/` changes)
   - other scanned repos (clj-ui-org, docscan, xi-* …) are bumped on
     demand, not part of the standard rollout
5. **New JS runtime module?** Squint/Replicant SPAs only bundle modules
   they require — add a side-effect require (e.g. `[ui.js.gestures]`) to
   each SPA main: `photos/app/photos/main.cljs`,
   `music-server/app/music/main.cljs`, `explorer/app/explorer/main.cljs`.
   Hiccup/server-rendered apps and xi get it free via `ui-runtime.js`.
6. **Rebuild consumer SPAs**:
   - bb-services: `bb build` in `photos` (plus `bb collector:build` — the
     collector builds from inside the photos dir), `music-server`,
     `explorer`, `image-editor` (dist dirs are untracked — nothing to commit)
   - xi: `bb web:build`
7. **Commit the consumer repos** (dotfiles + xi) with the sha bump +
   rebuilt assets.
8. **Deploy to the pi**: `hey re:deploy-pi --no-build`. Then verify over
   tailscale `http://100.64.0.3:<port>` (NOT the LAN alias): xi 7474,
   media 8096, music 8097, files 8098, photos 8100, editor 8102. Poll
   until every port returns 200 (services take ~1min to restart), then
   grep a served page / `theme.css` / bundle for a marker string from the
   change to confirm the new version is actually live.
9. **AGENTS sync**: if the consumer set changed, run
   `bb sync-consumer-agents` (idempotent) so new consumers get the
   UI-framework section.

## Reader Conditional Order — CRITICAL

Squint reads `.cljc` files and matches `:cljs` if it appears before `:squint`. **Always put `:squint` first:**

```clojure
;; CORRECT — squint picks :squint
#?(:squint (do-squint-thing)
   :cljs   (do-cljs-thing)
   :clj    (do-clj-thing))

;; WRONG — squint picks :cljs, never reaches :squint
#?(:clj    (do-clj-thing)
   :cljs   (do-cljs-thing)
   :squint (do-squint-thing))
```

For conditional defs, use the splicing form inside `do`:

```clojure
(do
  #?@(:squint []
      :cljs [(defn some-cljs-only-fn [x] ...)]))
```

## Target Differences at a Glance

| Concern | `:clj` (Hiccup) | `:cljs` (Replicant) | `:squint` (Eucalypt) |
|---------|------------------|---------------------|----------------------|
| Keywords | Clojure keywords | CLJS keywords | Strings |
| `name` | `clojure.core/name` | `cljs.core/name` | Not available — stub it |
| `:class` | String `"btn btn--primary"` | Vector `["btn" "btn--primary"]` | String `"btn btn--primary"` |
| `:style` | String `"color: red;"` | Map `{:color "red"}` | Map `{"color" "red"}` (string keys) |
| Events | None (use onclick string or HTMX) | `:on {:click handler}` | `:on-click handler` |
| Children | Lazy seqs OK (hiccup2 flattens) | Lazy seqs OK (replicant flattens) | Must use `into` to flatten |

## Shared Helpers (`ui.util`)

`src/ui/util.cljc` holds the tiny cross-target shims every component needs, so
they aren't re-defined in each file. Require it as `[ui.util :as util]`.

| Helper | Targets | Purpose |
|--------|---------|---------|
| `util/kw-name` | all | Coerce a keyword (or string) prop to its plain string name. Squint: identity (keywords are already strings). `:clj`/`:cljs`: `(name kw)`, with a defensive `str` fallback for non-keywords. Use instead of raw `name`. |
| `util/conj-classes` | `:cljs` only | Append a class onto a Replicant class **vector**. Splits a space-joined string (e.g. from another component's `*-classes` fn) into individual DOMTokenList tokens, splices in a collection, and no-ops on nil. Prevents Replicant's `InvalidCharacterError`. |

**When to use `conj-classes`:** only in the `:cljs` branch, and only when the
incoming `:class` prop may be a space-joined string. If `:class` is always a
single token, a plain `(conj cls class)` is fine. It's `:cljs`-only because
`:clj`/`:squint` build class **strings** directly and never touch class vectors.

**Adding a new shared helper:** put it in `ui.util` only if it's genuinely
cross-target boilerplate (papering over `:clj`/`:cljs`/`:squint` differences).
Keep target-specific helpers local to their component.

## How to Add a New Component

### 1. Create `src/ui/COMPONENT.cljc`

Follow the button pattern:

```clojure
(ns ui.card
  (:require [clojure.string :as str]
            [ui.util :as util]))

;; Pure function for class generation — shared across all targets
;; util/kw-name is the shared squint-safe keyword→string helper (see ui.util)
(defn card-class-list
  "Returns a vector of CSS class strings."
  [{:keys [variant]}]
  (let [v (or (some-> variant util/kw-name) "default")]
    ["card" (str "card--" v)]))

(defn card-classes
  "Returns a space-joined class string."
  [opts]
  (str/join " " (card-class-list opts)))

;; Component with per-target rendering
(defn card
  [{:keys [variant class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> (card-classes {:variant variant})
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       (into [:div base-attrs] children))

     :cljs
     ;; Replicant needs a vector of individual class tokens. If `class` may be
     ;; a space-joined string (e.g. from another component's *-classes fn), use
     ;; (util/conj-classes cls class) instead of (conj cls class) to split it.
     (let [cls (card-class-list {:variant variant})
           classes (cond-> cls
                     class (conj class))
           base-attrs (merge {:class classes} attrs)]
       (into [:div base-attrs] children))

     :clj
     (let [classes (cond-> (card-classes {:variant variant})
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       (into [:div base-attrs] children))))
```

Key rules:
- **`:cljs` branch**: `:class` must be a vector, `:style` must be a keyword-keyed map
- **`:squint` branch**: `:class` is a string, `:style` is a string-keyed map, events are flat (`:on-click`)
- **`:clj` branch**: `:class` and `:style` are both strings, no event handlers

### 2. Add CSS file `src/ui/COMPONENT.css`

Create a plain CSS file next to the `.cljc` file. The generator automatically reads all `.css` files from `src/ui/`:

```css
/* src/ui/card.css */
.card {
  padding: 1rem;
  border-radius: var(--radius-md);
  background: var(--bg-1);
}

.card-elevated {
  box-shadow: var(--shadow-1);
}
```

No changes needed in `gen.clj` — it collects all `src/ui/*.css` files automatically.

CSS conventions:
- Utility-style flat classes: `.component`, `.component-variant`, `.component-size`
- Use `var(--token-name)` for all colors, borders, shadows, radii
- Use `var(--size-N)` for spacing/padding/gap/line-height — no raw `rem` values
- Use `var(--font-*)` for font-size — no raw `rem` values
- Include hover/focus/disabled states
- Keep specificity flat — no nesting beyond `:hover:not(:disabled)`

### 3. Add unit tests in `test/ui/COMPONENT_test.clj`

Test the pure class-generation functions (they run in `:clj` via Babashka):

```clojure
(ns ui.card-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.card :as card]))

(deftest card-class-list-test
  (testing "default variant"
    (is (= ["card" "card-default"] (card/card-class-list {}))))
  (testing "explicit variant"
    (is (= ["card" "card-elevated"] (card/card-class-list {:variant :elevated})))))
```

Register new test namespaces in `bb.edn`:

```clojure
test
{:requires ([clojure.test :as t]
            [ui.button-test]
            [ui.card-test]      ;; <-- add
            [ui.theme-test])
 :task (let [{:keys [fail error]} (t/run-tests 'ui.button-test 'ui.card-test 'ui.theme-test)] ...)}
```

### 4. Add to dev test pages

Add the component to all three dev targets so it renders in the visual test page. Each target has its own rendering style — see existing button examples in `dev/*/src/dev/*.cljs`.

### 5. Run verification

```sh
bb build-theme   # Regenerate CSS with new component styles
bb test          # All tests pass
bb gen-docs      # Refresh docs/components.md (component/icon listing)
```

### 6. Never start dev servers from the agent — CRITICAL

**Do not run `bb dev`, `bb dev-hiccup`, `bb dev-replicant`, `bb dev-squint`, or any long-running server process from the agent.** The user manages dev servers via `bb dev` (tmux-dev session `ui-dev`). Starting servers from the agent blocks the session, spawns orphan processes, and can break existing tmux panes.

The agent may only:
- Run short commands: `bb test`, `bb build-theme`, `curl`, `wc -l`, `grep`
- Inspect tmux panes via `tmux capture-pane`
- Touch files to trigger recompilation: `touch src/ui/<module>.cljc`

If a dev server needs restarting, **tell the user** — don't do it yourself.

### 7. Check running dev servers before committing — CRITICAL

A tmux session `ui-dev` runs all three dev servers (`bb dev`). **Always run the check script before committing:**

```sh
bb check-dev
```

This checks all tmux panes for compile errors (shadow-cljs failures, Vite/Squint errors, Babashka exceptions), verifies the hiccup server responds with content, ensures all squint `.mjs` files have content (catches silent empty-file bugs), and confirms replicant JS is compiled.

Do **not** commit if `bb check-dev` exits non-zero. Fix errors first.

If a compiled squint file is empty (1 line = just the import), touch the source to trigger rewatch:
```sh
touch src/ui/<module>.cljc
```

## Consumer Tooling & Docs

`docs/components.md` is the generated listing of every component (with
public API) and every icon. Regenerate with `bb gen-docs` whenever a
component or icon is added/removed — consumers' AGENTS.md files point to it.

- `bb scan-consumers [roots...]` — walks `~/Code` and `~/.config/dotfiles`
  (or the given roots) for `bb.edn`/`deps.edn` that depend on this
  framework: git deps under any coordinate name, vendored
  `deps/clj-ui-framework` checkouts, and transitive consumers via
  `:local/root` libs (e.g. bb-services through server-lib). Reports each
  project's pinned sha vs the current HEAD (`STALE` markers show who needs
  a bump after a release).
- `bb sync-consumer-agents [roots...]` — upserts a marker-delimited
  "UI Framework" section (`<!-- clj-ui-framework:begin/end -->`) into each
  consumer project's `AGENTS.md`, pointing agents at this repo's AGENTS.md
  and `docs/components.md`. Idempotent; run it after changing the section
  template in `scripts/tools/consumers.clj` or when new consumers appear.

The implementation lives in `scripts/tools/` (`tools.registry`,
`tools.consumers`) — local bb tasks only, not shipped to consumers
(`deps.edn` `:paths` stays `["src"]`).

## Theme System

### Token naming

Semantic + scale tokens in `src/theme/tokens.edn`:

- **Backgrounds**: `bg-0` (base), `bg-1` (surface), `bg-2` (elevated)
- **Foregrounds**: `fg-0` (primary text), `fg-1` (secondary), `fg-2` (muted)
- **Semantic**: `accent`, `danger`, `success` + `fg-on-*` for contrast text
- **Borders**: `border-0/1/2` (full shorthand: `1px solid var(--gray-N)`)
- **Shadows**: `shadow-0/1/2/3` (increasing elevation)
- **Radii**: `radius-sm/md/lg`

### Algorithmic scales

Defined in `:scales` in `tokens.edn`. Generated into `:root` only (not duplicated in dark theme blocks).

**Size scale** — linear: `--size-N = base × N`

```edn
:size {:base 0.25 :unit "rem" :steps 16}
```

Produces `--size-1: 0.25rem` through `--size-16: 4rem`. Use for all spacing, padding, gap, and line-height values.

**Font scale** — geometric: `--font-{label} = base × ratio^power`

```edn
:font {:base 1 :unit "rem" :ratio 1.25
       :steps [[-2 "xs"] [-1 "sm"] [0 "base"] [1 "md"] [2 "lg"] [3 "xl"] [4 "2xl"] [5 "3xl"]]}
```

Produces `--font-xs: 0.64rem` through `--font-3xl: 3.052rem`. Use for all font-size values.

**Color scales** — OKLCH-based for perceptual uniformity, via `jon.color-tools`:

```edn
:color {:gray {:hue 285 :chroma 0.025
               :steps [[50 0.975 0.003] [100 0.955 0.005] ... [950 0.145 0.011]]}}
```

Each step is `[label lightness]` (uses default chroma) or `[label lightness chroma]` (per-step override). OKLCH ensures equal lightness steps = equal perceived brightness across hues. Produces `--gray-50: oklch(0.975 0.003 285)` through `--gray-950: oklch(...)`.

Available color scales: `gray`, `accent`, `danger`, `success`, `warning`. Each generates 11 stops (50, 100, 200–900, 950).

**To change the gray tone** (e.g. warm gray, cool blue-gray, purplish), change `hue`:
- `285` → purplish gray (current, inspired by activity-tracker)
- `255` → blue-gray
- `60` → warm/sandy gray
- any hue with chroma `0` → pure neutral gray

**To change the accent color**, change `hue` in the accent scale:
- `286` → purple (current, matches activity-tracker)
- `255` → blue
- `165` → green
- `25` → red

Semantic tokens reference scale variables: `var(--gray-50)`, `var(--accent-500)`, etc. Dark theme overrides switch which stop is used (e.g. `bg-0` goes from `gray-50` → `gray-950`).

To adjust the entire scale, change `hue`, `saturation`, or `steps` — all values recompute on `bb build-theme`.

**Usage in component CSS:**

```css
.btn {
  padding: var(--size-2) var(--size-4);
  font-size: var(--font-sm);
  line-height: var(--size-5);
}
```

**Rule: never use raw `rem` values in component CSS** — always reference a scale variable.
**Rule: never use raw hex colors in component CSS** — always reference a token or scale variable.

### Adding tokens

Add to both `:tokens` (light) and `:themes > :dark` in `tokens.edn`. They must have the same keys — the `tokens-roundtrip-test` enforces this. Scale config (`:scales`) is theme-independent and lives at the top level.

### Dark mode

Three CSS layers are generated:
1. `:root { ... }` — light defaults + all scales (size, font, color)
2. `[data-theme="dark"] { ... }` — explicit dark override (semantic tokens only)
3. `@media (prefers-color-scheme: dark) { :root:not([data-theme="light"]) { ... } }` — auto dark

Color scales are generated once in `:root` and never duplicated. Dark theme just reassigns which scale stop each semantic token points to.

Toggle with: `document.documentElement.dataset.theme = "dark" | "light"`

## Utility Classes

`src/ui/utilities.css` provides lightweight utility classes. Use these instead of inline styles or one-off CSS. All spacing/sizing values reference scale tokens.

| Category | Classes |
|----------|---------|
| **Layout** | `.flex`, `.flex-col`, `.flex-row`, `.flex-wrap`, `.items-center`, `.justify-center`, `.justify-between`, `.justify-end` |
| **Stacks** | `.hstack` (horizontal, centered, gap-4, wrapping), `.vstack` (vertical, gap-3) |
| **Spacing** | `.gap-{1,2,3,4}`, `.mt-{2,4,6}`, `.mb-{2,4,6}`, `.p-4` |
| **Flex** | `.flex-1` (flex: 1 + min-width: 0), `.shrink-0` |
| **Typography** | `.text-xs`, `.text-sm`, `.font-semibold`, `.font-mono`, `.uppercase`, `.tracking-wide` |
| **Alignment** | `.align-left`, `.align-center`, `.align-right`, `.text-right` |
| **Color** | `.text-muted` (fg-1), `.text-faint` (fg-2) |
| **Sizing** | `.w-full` |
| **A11y** | `.sr-only` (visually hidden, screen-reader accessible) |
| **Hover reveal** | `.hover-reveal` (container) + `.hover-reveal-item` (controls) — hidden until hover, but only on hover-capable devices; always visible on touch |
| **Hit area** | `.hit-area` + `.hit-area-{2,3,4,6}` (expand clickable area via `::before` pseudo-element) |
| **Full bleed** | `.full-bleed` (escape body padding), `.full-bleed-padded` (escape + re-apply padding inside), `.full-bleed-flush` (escape + strip border/radius) — requires `--body-padding-inline` on ancestor |

### Hit area expand

`.hit-area` uses a `::before` pseudo-element to expand the clickable/tappable area beyond visual bounds. Combine with a size class or set custom properties:

```html
<!-- Uniform expansion -->
<button class="hit-area hit-area-4">×</button>

<!-- Per-side via custom properties -->
<button class="hit-area" style="--hit-area-t: 8px; --hit-area-l: 12px">×</button>

<!-- Custom uniform via --hit-area -->
<button class="hit-area" style="--hit-area: 20px">×</button>
```

Custom properties: `--hit-area` (all sides), `--hit-area-t`, `--hit-area-r`, `--hit-area-b`, `--hit-area-l` (per-side overrides).

## Mobile & Touch Friendliness — CRITICAL

### The iOS first-tap-as-hover bug

Any bare `:hover` rule that **reveals or hides content** (opacity, visibility,
display, generated content, size changes) makes iOS Safari treat the first tap
as a hover: the reveal happens, but that tap's click is suppressed — the user
must tap **twice** to activate the element. This is the single most common
mobile bug in consumer apps (e.g. a photo tile whose checkbox appears on
`.tile:hover` needed a double-tap to open the lightbox).

Style-only hovers (background, color, filter, underline) do **not** trigger
this and need no guard.

### The build gates `:hover` for you — automatically

You do **not** need to hand-wrap `:hover` in `@media (hover: hover)` anymore.
`ui.css.gen/wrap-hover-media` runs at build time and rewrites **every** rule
whose selector contains `:hover` into a `@media (hover: hover)` block — for the
generated framework theme (`generate-css`) *and* for every consumer's
`style.css` (server-lib's `frontend.clj` routes inlined/emitted CSS through it
before esbuild minify). It's idempotent (skips rules already inside a
`@media (hover: …)`) and splits grouped selectors so non-hover parts
(`:focus-visible`, state classes) stay ungated. **Author plain `:hover`;** the
build makes it touch-safe.

What the build still can't do for you:

- **Design the touch/default state.** The build moves your `:hover` rule into
  the media query verbatim — it does not invent an always-visible fallback.
  For a *reveal*, the ungated default must already be the visible state (hide
  only inside `:hover`, which the build then gates away on touch).
- **Keyboard access.** Pair `:hover` with `:focus-within` / `:focus-visible`
  yourself — the build gates `:hover` behind `hover: hover`, and keyboard
  users on touch devices would otherwise lose the reveal.

The manual pattern below still documents the *end result* the build produces,
and is what you'd write if authoring CSS outside the build pipeline.

### Rule: gate every hover-reveal behind `@media (hover: hover)`

```css
/* WRONG — iOS needs a double-tap to click .card */
.card .card-actions { opacity: 0; }
.card:hover .card-actions { opacity: 1; }

/* CORRECT — touch devices never get the hover trap; controls stay visible */
@media (hover: hover) {
  .card .card-actions { opacity: 0; }
  .card:hover .card-actions,
  .card:focus-within .card-actions { opacity: 1; }
}
```

On touch devices the ungated default applies — design it so that's the
**always-visible** state (hide only inside the media query, never outside it).
Add `:focus-within` so keyboard users can reach the controls too.

### Prefer the `.hover-reveal` utility

For the common "controls appear when the container is hovered" pattern, use
the built-in utility instead of writing custom CSS:

```html
<div class="tile hover-reveal">
  <img src="...">
  <button class="tile-check hover-reveal-item">✓</button>
</div>
```

- `.hover-reveal` — the hoverable container
- `.hover-reveal-item` — each control to hide until hover/focus-within
- On touch devices (`hover: none`) the items are simply always visible

### Baseline mobile optimizations — automatic

The framework ships mobile hardening on two layers; apps must **not**
duplicate these per-app:

**CSS (`ui/mobile.css`, part of the generated theme):**

- `text-size-adjust: 100%` — no text inflation on orientation change
- `-webkit-tap-highlight-color: transparent` — no gray tap flash
- `touch-action: manipulation` on interactive elements (`a`, `button`,
  inputs, `label`, `summary`, `[role=button]`) — kills double-tap-to-zoom
  and the 300ms click delay while keeping scrolling intact

**JS runtime (`ui.js.touch`, touch devices only):**

- Rewrites (or creates) the viewport meta to
  `width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no`
  — disables the zoom gesture on Android and iOS zoom-on-input-focus
- Deliberately does **not** add `viewport-fit=cover`: in an iOS standalone
  PWA it draws the page under the status bar, where iOS 26+ lays a
  progressive blur/fade over the top edge (washes out tabs/headers). Keep
  it out of app HTML too, and don't set
  `apple-mobile-web-app-status-bar-style: black-translucent` (same blur).
- Blocks the iOS Safari pinch gesture (`gesturestart` preventDefault),
  which ignores `user-scalable=no`
- Desktop is untouched (trackpad pinch zoom keeps working)

Apps keep a plain `<meta name="viewport" content="width=device-width, initial-scale=1.0">`
in their HTML — the runtime upgrades it on touch devices. Don't hand-write
`user-scalable=no` metas or `gesturestart` handlers in apps.

### Checklist for new components

1. **No bare hover-reveals** — the build auto-gates `:hover` behind
   `@media (hover: hover)`, so don't hand-wrap. Do make sure the *default*
   (ungated) state is always-visible for reveals, since that's what touch gets.
2. **Decide the touch state** — always-visible (default outside the media
   query) or an explicit `.clj-ui-touch` override (see next section).
3. **Tap targets** — use `.hit-area` / `--hit-area-*` so controls hit ≥44px.
4. **Keyboard** — pair `:hover` with `:focus-within`/`:focus-visible`.

Framework components already following this: tooltip (`ui/tooltip.css`,
`ui/form.css` error tooltips) and the player-bar scrubber
(`ui/player_bar.css`).

## Touch Alternatives (`.clj-ui-touch`)

The class `.clj-ui-touch` on `<html>` activates touch-friendly alternatives throughout the UI.

### How it's applied — automatic via the JS runtime

The `ui.js.touch` runtime module detects touch devices and applies/removes the
class on `<html>` automatically. Detection uses `matchMedia("(hover: none)")`
— the primary input can't hover — mirroring the `@media (hover: hover)` gating
in component CSS, and stays in sync when the media query changes (e.g.
attaching a mouse to a tablet). `window.__uiTouch()` re-syncs on demand.

Apps get it for free depending on target:

- **Hiccup/server-rendered apps** — nothing to do; it's bundled in
  `ui-runtime.js` (`css/build-js`).
- **Squint/Replicant SPAs** — add a side-effect require, same as the other
  runtime modules:

  ```clojure
  (:require [ui.js.touch])
  ```

Do **not** write per-app detection snippets (`'ontouchstart' in window`
etc.) — use the runtime module so detection stays consistent.

### What changes

| Component | Desktop (default) | Touch (`.clj-ui-touch`) |
|-----------|-------------------|-------------------------|
| Player bar scrubber | Shown on hover | Always visible |

### Adding touch alternatives in CSS

Use `.clj-ui-touch` as an ancestor selector to override hover-dependent interactions:

```css
/* Default: hidden, shown on hover — gated so touch never gets the
   first-tap-as-hover trap (see "Mobile & Touch Friendliness") */
@media (hover: hover) {
  .my-handle { opacity: 0; }
  .my-container:hover .my-handle { opacity: 1; }
}

/* Touch: always visible */
.clj-ui-touch .my-handle { opacity: 1; }
```

**Convention:** touch rules go directly after the hover rule they override, with a `/* Touch: ... */` comment. The hover rules themselves must live inside `@media (hover: hover)`.

## Long-Press → Context Menu (`ui.js.gestures`) — automatic

iOS Safari never fires `contextmenu` from a long-press (Android does), so
right-click menus would be unreachable on iPhone. The `ui.js.gestures`
runtime module (bundled in ui-runtime.js) fixes this globally: a
document-level long-press recognizer (Pointer Events, touch pointers only)
dispatches a **synthetic `contextmenu` MouseEvent** on the pressed element
after 500ms. Every existing contextmenu wiring — `context-menu-trigger`
wrappers, app-level `:on-context-menu` handlers, hiccup inline handlers —
works on touch with zero changes.

**Opt-in surface** (which elements respond to long-press):

- `.context-menu-trigger` — everything wrapped in
  `ui.context-menu/context-menu-trigger` gets it automatically
- `[data-long-press]` — add this attribute to any element with a custom
  `contextmenu` handler (e.g. an imperative `window.__uiContextMenu` call)

```clojure
;; custom handler + touch long-press support:
[:div {:data-long-press "true"
       :on-context-menu (fn [e] (open-my-menu! e))}
 ...]
```

**Built-in behaviors** — do NOT reimplement these per-app:

- movement beyond 10px or release before 500ms cancels the press
- a native contextmenu during the press (Android long-press) cancels the
  synthetic one — no double-open
- the click following a fired long-press is suppressed, so the press
  doesn't also activate the element underneath
- the pressed element gets `.clj-ui-pressing` while the press is pending —
  CSS scales it down slightly (`scale(0.97)`, iOS-style press feedback);
  the scale is held while the context menu is open and animates back when
  the menu dismisses (the runtime menu fires a `clj-ui-menu-dismiss`
  document event; next-pointerdown is the fallback for custom handlers)
- `.clj-ui-touch` CSS disables the iOS press callout / text selection on
  the opt-in surface (ui/context_menu.css)

**Rule: never hand-roll long-press recognizers in apps** (touchstart
timers, click-suppression atoms, etc.) — add `data-long-press` (or use the
trigger wrapper) and handle `contextmenu`.

Imperative API: `window.__uiLongPress(el, x, y)` dispatches the same
synthetic contextmenu (rarely needed).

Squint/Replicant SPAs that don't load ui-runtime.js get it with a
side-effect require, like the other runtime modules:

```clojure
(:require [ui.js.gestures])
```

## Lightbox Zoom (`ui.js.lightbox`) — automatic

Every `ui.lightbox` image is zoomable without per-app wiring: the
`ui.js.lightbox` runtime module (bundled in ui-runtime.js) installs
document-level Pointer Event handlers on `.lightbox-overlay`.

- two-finger pinch zooms around the pinch midpoint; one finger / mouse
  drag pans a zoomed image
- double-tap / double-click on the image toggles 1x ↔ 2.5x at that point
- mouse wheel / trackpad pinch (ctrl+wheel) zooms around the cursor
- release snaps into bounds (1x–5x, no panning past the image edges)
- the click ending a drag/pinch is suppressed, so it never closes the
  lightbox; a plain tap on the backdrop still does
- zoom lives in the image's inline `transform` + a `data-zoomed`
  attribute (grab cursor) — no component state; it resets when the image
  (re)loads

`ui/lightbox.css` sets `touch-action: none` on the overlay so the browser
hands the gestures to the runtime (the page-level pinch block from
`ui.js.touch` stays in place). **Don't hand-roll zoom in apps.**
Squint/Replicant SPAs without ui-runtime.js add `(:require [ui.js.lightbox])`.

## Icons (`ui.icon`)

Inline SVG icons using Lucide-compatible 24×24 paths. All icons are defined in `src/ui/icon.cljc`.

### Usage

```clojure
(icon/icon {:icon-name :play :size :sm})
(icon/icon {:icon-name :play :size :lg :filled true})
(icon/icon {:icon-name :home :class "extra" :attrs {:id "nav-icon"}})
```

### Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `:icon-name` | keyword | required | Icon name (e.g. `:home`, `:play`, `:search`) |
| `:size` | keyword | `:md` | `:sm` (16px), `:md` (20px), `:lg` (24px), `:xl` (32px) |
| `:filled` | boolean | `false` | Render filled variant (solid shapes instead of strokes) |
| `:class` | string | `nil` | Additional CSS classes |
| `:attrs` | map | `nil` | Extra HTML/SVG attributes merged onto the `<svg>` element |

### Outline vs Filled

By default icons render as **stroked outlines** (`fill: none`, `stroke: currentColor`). Pass `:filled true` to get **solid fills** (`fill: currentColor`, `stroke: none`).

Filled variants live in `filled-icon-paths`. When a filled variant isn't defined for an icon, it falls back to the outline paths with filled SVG attrs.

Icons with dedicated filled path data: `:play`, `:pause`, `:skip-back`, `:skip-forward`, `:repeat`, `:volume-2`.

**Special cases:**
- **`:music`** — filled by default (note-head circles have `fill: currentColor` baked into path attrs), no need for `:filled true`
- **`:shuffle`** — no filled variant (inherently stroke-based)
- **`:volume-2`** filled — speaker body fills, wave arcs keep their stroke via per-element attr overrides
- **`:repeat`** filled — arrow chevrons close into solid triangles, arc paths keep their stroke

### Adding a new icon

1. Add an entry to `icon-paths` with a keyword name and a vector of hiccup SVG child elements (`:path`, `:rect`, `:circle`, etc.)
2. If it needs a filled variant with different paths, add a matching entry to `filled-icon-paths`
3. For elements that must keep their stroke in filled mode, add per-element attrs: `{:fill "none" :stroke "currentColor" :stroke-width "2"}`

## JS Runtime (`src/ui/js/`) — Shared Browser Logic

The hiccup target renders static HTML on the server — there is no ClojureScript runtime in the browser. Any component that needs **client-side interactivity in hiccup** (positioning, dismiss-on-click, keyboard navigation, etc.) must use a shared JS runtime.

**Always put shared browser logic in `src/ui/js/`.** Do not write inline `onclick` strings for anything beyond trivial one-liners. Do not duplicate DOM-manipulation code across targets.

### Architecture

```
src/ui/js/
  squint.edn              # Squint compiler config
  context_menu.cljs       # Context menu runtime (squint source)
  .compiled/              # Squint output (gitignored)
dist/
  ui-runtime.js           # Bundled IIFE (~12kb), loaded via <script>
```

- **Source**: squint `.cljs` files in `src/ui/js/` — write ClojureScript, compiled to JS
- **Build**: `bb build-js-runtime` compiles via squint → esbuild → `dist/ui-runtime.js`
- **Consumed by all targets**: loaded as a `<script>` tag before app code
- **Bridge**: functions are attached to `window` (e.g. `window.__uiContextMenu`)

### How it works across targets

The `.cljc` component file uses reader conditionals for the **trigger** (how items get to the runtime), but all three targets call the **same JS runtime** for DOM creation:

| Target | Trigger mechanism | Runtime call |
|--------|-------------------|--------------|
| `:clj` (Hiccup) | JSON in `data-*` attribute + inline `oncontextmenu` | `window.__uiFn(event)` reads data attr |
| `:squint` | Event handler passes JS objects directly | `window.__uiFn(event, items)` |
| `:cljs` (Replicant) | Event handler passes `(clj->js items)` | `window.__uiFn(event, items)` |

### When to use `src/ui/js/`

**Use it when a component needs browser-side behavior that hiccup can't do with pure HTML/CSS:**
- Floating/positioned UI (menus, tooltips, popovers, dropdowns)
- Click-outside dismiss, Escape handling
- Keyboard navigation within a widget
- Drag and drop, resize handles
- Any DOM measurement (getBoundingClientRect, viewport clamping)

**Don't use it for:**
- Pure CSS interactions (hover states, transitions, `:focus-visible`)
- Things that only squint/replicant need (they have full ClojureScript)
- Simple toggles that can use `<details>`/`<dialog>` or HTMX

### Adding a new JS runtime module

1. Create `src/ui/js/my_feature.cljs` — write squint-compatible ClojureScript
2. Attach the public API to `window`: `(aset js/window "__uiMyFeature" my-fn)`
3. Import it in `src/ui/js/context_menu.cljs` or create a new entry point
4. If adding a new entry point, update the esbuild command in `bb.edn` (`build-js-runtime` task)
5. Run `bb build-js-runtime` to rebuild `dist/ui-runtime.js`
6. Create the `.cljc` component that calls the runtime via reader conditionals
7. The `bb sync-dev-assets` task copies `dist/ui-runtime.js` to dev targets automatically

### Naming convention

Runtime functions on `window` use the `__ui` prefix: `__uiContextMenu`, `__uiTooltip`, etc.

## Squint Pitfalls

1. **`name` is not available** — use `ui.util/kw-name` (the shared squint-safe helper) instead of `name`. Don't re-define local stubs.
2. **Keywords are strings** — `:primary` becomes `"primary"` at runtime
3. **Maps are JS objects** — style maps must use string keys: `{"display" "flex"}`, not `{:display "flex"}`
4. **No lazy seq flattening in Eucalypt** — use `into` with `mapcat`/`map` to build hiccup vectors eagerly
5. **Eucalypt render arg order** — `(eu/render hiccup container)`, hiccup first
6. **Eucalypt import** — `(:require ["eucalypt" :as eu])`, quoted string for npm package
7. **Blank page from squint watcher race condition** — The squint watcher can produce truncated/empty `.mjs` files when it detects a file change mid-save. Vite picks up the broken module and the page goes blank with no terminal errors (the crash is browser-side only). This commonly happens during rapid edits or when multiple files change at once.

   **How to detect:** Page is blank, no compile errors in the tmux pane. Verify with:
   ```sh
   wc -l dev/squint/.compiled/ui/<module>.mjs  # Should be >1 line
   ```

   **How to recover:**
   ```sh
   # Option A: touch the source file to trigger recompile
   touch src/ui/<module>.cljc
   # Then hard-refresh the browser (Ctrl+Shift+R)

   # Option B: restart the squint tmux pane
   # Kill existing processes and recreate:
   tmux split-window -v -t ui-dev \
     "bash -c 'cd dev/squint && npx squint watch & cd dev/squint && npx vite --port 4002'"
   tmux select-layout -t ui-dev tiled
   # Then hard-refresh the browser
   ```

   **After recovering**, always verify the compiled output is complete before committing.
8. **Eucalypt shorthand class wiping** — When using hiccup shorthand classes (e.g., `[:button.foo.bar {:class (when active "active")}]`), Eucalypt **replaces ALL classes** when the `:class` value transitions reactively (e.g., from `"active"` to `nil`). The shorthand classes (`foo bar`) get wiped to an empty string, leaving the element unstyled.

   **Wrong — classes disappear when `active` becomes falsy:**
   ```clojure
   [:button.player-bar-icon-btn.player-bar-hide-sm {:class (when repeat "active")}]
   ```

   **Right — always build the full class string explicitly:**
   ```clojure
   [:button {:class (str "player-bar-icon-btn player-bar-hide-sm" (when repeat " active"))}]
   ```

   This applies to any element with shorthand classes and a dynamic `:class` attribute. The same issue occurs with `[:div.dl-bar {:class status}]` — use `[:div {:class (str "dl-bar" (when status (str " " status)))}]` instead.
9. **Never pass `false` to a boolean HTML attribute** (`disabled`, `readonly`, `required`, `hidden`, …) — use `(when cond true)` or omit the key. Eucalypt's attribute setter only *removes* an attribute when the value is `nil`; **any** other value, including `false`, calls `setAttributeNS(null, k, v)`, which *sets* the attribute. For boolean HTML attributes presence alone activates them, so `:disabled false` renders `disabled="false"` → still disabled.

   **Wrong — button is permanently disabled (the `or` yields `false`, not `nil`):**
   ```clojure
   [:button {:disabled (or (not ok?) submitting?)} "Save"]
   ```

   **Right — attribute is present only when truthy, absent (nil) otherwise:**
   ```clojure
   [:button {:disabled (when (or (not ok?) submitting?) true)} "Save"]
   ```

   The `ui.button` component already does this via `(cond-> attrs disabled (assoc :disabled true))` — the pitfall only bites raw `[:button]`/`[:input]` hiccup. **Exception:** `checked` and `selected` are special-cased by Eucalypt (set as a DOM *property*, `element[k] = v`), so `false` correctly unchecks/deselects them — no `when` needed there.

## Replicant Pitfalls

1. **`:class` must be a vector of strings** — not a space-joined string. Replicant asserts on this.
2. **`:style` must be a map with keyword keys** — `{:color "red"}`, not `"color: red;"`. Replicant asserts no string styles.
3. **Events use nested `:on` map** — `{:on {:click handler}}`, not `:on-click`
4. **`set-dispatch!` required** — call `(d/set-dispatch! (fn [_ _]))` before first render, even if no-op
5. **Lazy seqs are fine** — Replicant flattens `for`/`map` results as children

## Hiccup (Backend) Pitfalls

1. **`:style` is a plain string** — `"color: red; display: flex;"`
2. **`:class` is a plain string** — `"btn btn--primary"`
3. **No event handlers** — use inline JS via `:onclick` strings or HTMX attributes
4. **Uses hiccup2** — wrap in `(h/html ...)` and call `str` on the result

## Babashka (bb.edn) Notes

- Task names are **unquoted symbols**, not keywords: `build-theme` not `:build-theme`
- Use `:requires` in task config, not inline `(require ...)` in the task body
- `:depends` references other task symbols
