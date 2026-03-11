# UI Framework — Agent Guide

A cross-target component library for Clojure, ClojureScript (Replicant), and Squint (Eucalypt). Components are `.cljc` files using reader conditionals. CSS is generated from EDN tokens via Babashka.

## Project Structure

```
src/
  theme/tokens.edn          # Design tokens (colors, borders, shadows, radii)
  ui/
    theme.cljc               # Token helpers (css-var)
    button.cljc              # Button component (reference implementation)
    button.css               # Button component styles (read by gen.clj)
    css/gen.clj              # EDN → CSS generator (babashka)
dist/
  theme.css                  # Generated CSS (tokens + component styles)
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
bb test           # Run all unit tests
bb dev-hiccup     # Start hiccup server (port 3003)
bb dev-replicant  # Start replicant dev (port 3001)
bb dev-squint     # Start squint dev (port 3002)
bb dev            # Build theme + start hiccup + print instructions
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

**Do not run `bb dev`, `bb dev-hiccup`, `bb dev-replicant`, `bb dev-squint`, or any long-running server process from the agent.** The user manages dev servers in a separate tmux session (`ui-dev`). Starting servers from the agent blocks the session, spawns orphan processes, and can break existing tmux panes.

The agent may only:
- Run short commands: `bb test`, `bb build-theme`, `curl`, `wc -l`, `grep`
- Inspect tmux panes via `tmux capture-pane`
- Touch files to trigger recompilation: `touch src/ui/<module>.cljc`

If a dev server needs restarting, **tell the user** — don't do it yourself.

### 7. Check running dev servers before committing — CRITICAL

A tmux session `ui-dev` runs all three dev servers (`bb dev-all`). **Always run the check script before committing:**

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
