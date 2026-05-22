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
```

Replicant and squint need `npm install` in their dev directories first.

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

## How to Add a New Component

### 1. Create `src/ui/COMPONENT.cljc`

Follow the button pattern:

```clojure
(ns ui.card
  (:require [clojure.string :as str]))

;; Stub for squint (keywords are strings, name is identity)
#?(:squint (defn- kw-name [s] s)
   :cljs   (defn- kw-name [s] (name s))
   :clj    (defn- kw-name [s] (name s)))

;; Pure function for class generation — shared across all targets
(defn card-class-list
  "Returns a vector of CSS class strings."
  [{:keys [variant]}]
  (let [v (or (some-> variant kw-name) "default")]
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

## Touch Alternatives (`.clj-ui-touch`)

The class `.clj-ui-touch` on an ancestor element (typically `<html>` or `<body>`) activates touch-friendly alternatives throughout the UI. This is a **consumer responsibility** — the framework provides the CSS rules, the app applies the class.

### How to apply

```js
// Detect touch and apply at startup
if ('ontouchstart' in window || navigator.maxTouchPoints > 0) {
  document.documentElement.classList.add('clj-ui-touch');
}
```

### What changes

| Component | Desktop (default) | Touch (`.clj-ui-touch`) |
|-----------|-------------------|-------------------------|
| Player bar scrubber | Shown on hover | Always visible |

### Adding touch alternatives in CSS

Use `.clj-ui-touch` as an ancestor selector to override hover-dependent interactions:

```css
/* Default: hidden, shown on hover */
.my-handle { opacity: 0; }
.my-container:hover .my-handle { opacity: 1; }

/* Touch: always visible */
.clj-ui-touch .my-handle { opacity: 1; }
```

**Convention:** touch rules go directly after the hover rule they override, with a `/* Touch: ... */` comment.

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

1. **`name` is not available** — define `kw-name` stubs via reader conditionals
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
