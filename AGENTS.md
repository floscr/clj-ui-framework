# UI Framework — Agent Guide

A cross-target component library for Clojure, ClojureScript (Replicant), and Squint (Eucalypt). Components are `.cljc` files using reader conditionals. CSS is generated from EDN tokens via Babashka.

## Project Structure

```
src/
  theme/tokens.edn          # Design tokens (colors, borders, shadows, radii)
  ui/
    theme.cljc               # Token helpers (css-var)
    button.cljc              # Button component (reference implementation)
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

### 2. Add CSS to `src/ui/css/gen.clj`

Add a `component-css-card` function and include it in `generate-css`:

```clojure
(defn component-css-card []
  (str/join "\n\n"
    [".card {
  padding: 1rem;
  border-radius: var(--radius-md);
  background: var(--bg-1);
}"
     ".card--elevated {
  box-shadow: var(--shadow-1);
}"]))

;; In generate-css, add to the components list:
(defn generate-css [{:keys [tokens themes]}]
  (let [...]
    (str/join "\n\n" [root-block dark-attr dark-media
                      (component-css-button)
                      (component-css-card)  ;; <-- add here
                      ""])))
```

CSS conventions:
- BEM-lite: `.component`, `.component--variant`, `.component--size`
- Use `var(--token-name)` for all colors, borders, shadows, radii
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
    (is (= ["card" "card--default"] (card/card-class-list {}))))
  (testing "explicit variant"
    (is (= ["card" "card--elevated"] (card/card-class-list {:variant :elevated})))))
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
bb dev-hiccup    # Visual check
```

## Theme System

### Token naming

Semantic + scale tokens in `src/theme/tokens.edn`:

- **Backgrounds**: `bg-0` (base), `bg-1` (surface), `bg-2` (elevated)
- **Foregrounds**: `fg-0` (primary text), `fg-1` (secondary), `fg-2` (muted)
- **Semantic**: `accent`, `danger`, `success` + `fg-on-*` for contrast text
- **Borders**: `border-0/1/2` (full shorthand: `1px solid #color`)
- **Shadows**: `shadow-0/1/2/3` (increasing elevation)
- **Radii**: `radius-sm/md/lg`

### Adding tokens

Add to both `:tokens` (light) and `:themes > :dark` in `tokens.edn`. They must have the same keys — the `tokens-roundtrip-test` enforces this.

### Dark mode

Three CSS layers are generated:
1. `:root { ... }` — light defaults
2. `[data-theme="dark"] { ... }` — explicit dark override
3. `@media (prefers-color-scheme: dark) { :root:not([data-theme="light"]) { ... } }` — auto dark

Toggle with: `document.documentElement.dataset.theme = "dark" | "light"`

## Squint Pitfalls

1. **`name` is not available** — define `kw-name` stubs via reader conditionals
2. **Keywords are strings** — `:primary` becomes `"primary"` at runtime
3. **Maps are JS objects** — style maps must use string keys: `{"display" "flex"}`, not `{:display "flex"}`
4. **No lazy seq flattening in Eucalypt** — use `into` with `mapcat`/`map` to build hiccup vectors eagerly
5. **Eucalypt render arg order** — `(eu/render hiccup container)`, hiccup first
6. **Eucalypt import** — `(:require ["eucalypt" :as eu])`, quoted string for npm package

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
