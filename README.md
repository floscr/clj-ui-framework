# clj-ui-framework

A cross-target component library for Clojure (Hiccup), ClojureScript (Replicant), and Squint (Eucalypt). Components are `.cljc` files that compile to all three targets using reader conditionals.

## Setup

Requires [Babashka](https://github.com/babashka/babashka).

```sh
bb build-theme   # Generate dist/theme.css
bb test          # Run tests
```

For dev servers (Hiccup on :3003, Replicant on :3001, Squint on :3002):

```sh
bb dev-all       # Starts all three in a tmux session
```

Replicant and Squint need `npm install` in their dev directories first.

## Components

Accordion, Alert, Badge, Breadcrumb, Button, Card, Dialog, Form, Icon, Pagination, Progress, Sidebar, Skeleton, Spinner, Switch, Table, Tooltip.

Each component is a `.cljc` file in `src/ui/` with a matching `.css` file. The CSS is collected automatically during theme generation.

## Color System

Colors are generated from OKLCH parameters defined in `src/theme/tokens.edn`. OKLCH is a perceptually uniform color space — equal lightness steps produce equal perceived brightness across all hues, unlike HSL. You define a hue and chroma, and the generator produces an 11-stop scale (50–950) for each color, output as `oklch()` CSS values.

Five scales ship by default: `gray`, `accent`, `danger`, `success`, `warning`.

### How it works

Each scale is defined by a hue, a default chroma, and a list of lightness steps:

```edn
:gray {:hue 285 :chroma 0.025
       :steps [[50  0.975 0.003]   ;; [label lightness chroma]
               [100 0.955 0.005]
               [200 0.915 0.010]
               ...
               [950 0.145 0.011]]}
```

This generates CSS variables in `:root`:

```css
--gray-50:  oklch(0.975 0.003 285);
--gray-100: oklch(0.955 0.005 285);
--gray-200: oklch(0.915 0.010 285);
...
--gray-950: oklch(0.145 0.011 285);
```

Semantic tokens reference these scales. Light theme points at the light end, dark theme at the dark end:

```edn
;; Light
:bg-0 "var(--gray-50)"
:fg-0 "var(--gray-950)"

;; Dark
:bg-0 "var(--gray-950)"
:fg-0 "var(--gray-50)"
```

The scale variables are generated once and never change between themes. Only the semantic mapping shifts.

### Changing the palette

The gray hue controls the tint of all neutral surfaces, borders, and text. OKLCH hues differ from HSL — change it to shift the entire feel:

| Hue | Result |
|-----|--------|
| `285` | Purplish gray (default) |
| `255` | Blue-gray |
| `60` | Warm/sandy |
| any, chroma `0` | Pure neutral |

The accent hue controls buttons, focus rings, links:

| Hue | Result |
|-----|--------|
| `286` | Purple (default) |
| `255` | Blue |
| `25` | Red |
| `165` | Green |

After changing values, run `bb build-theme` to regenerate `dist/theme.css`.

### Per-step chroma

Steps can be `[label lightness]` to use the scale's default chroma, or `[label lightness chroma]` to override it. This is important for chromatic colors where high chroma looks wrong at the extremes — the accent scale peaks at chroma 0.255 in the mid-range and tapers to 0.020 for light tints and 0.130 for dark shades. The gray scale uses very low chroma (0.003–0.035) that peaks in the mid-range for a subtle tint.

### Current palette

The default ships with a purplish gray (hue 285) and a vivid purple accent (hue 286, `--accent-500: oklch(0.595 0.23 286)`), inspired by the [activity-tracker](https://github.com/user/piui) app. OKLCH ensures the lightness steps are perceptually even — a gray at L=0.5 looks equally bright regardless of hue, unlike HSL where different hues have wildly different perceived brightness.

## Theme Tokens

Beyond color scales, the theme includes:

- **Size scale** — `--size-1` (0.25rem) through `--size-16` (4rem), linear
- **Font scale** — `--font-xs` through `--font-3xl`, geometric (ratio 1.25)
- **Borders** — `--border-0/1/2`, referencing gray scale stops
- **Shadows** — `--shadow-0/1/2/3`, increasing elevation
- **Radii** — `--radius-sm` (6px), `--radius-md` (10px), `--radius-lg` (16px)

Light/dark mode switches automatically via `prefers-color-scheme`, or manually with `data-theme="dark"` on the root element.
