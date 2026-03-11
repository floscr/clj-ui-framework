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

Colors are generated from HSL parameters defined in `src/theme/tokens.edn`, using [jon/color-tools](https://github.com/jramosg/color-tools) for conversion. Instead of picking individual hex values, you define a hue and saturation, and the generator produces an 11-stop scale (50–950) for each color.

Five scales ship by default: `gray`, `accent`, `danger`, `success`, `warning`.

### How it works

Each scale is defined by a hue, a default saturation, and a list of lightness steps:

```edn
:gray {:hue 240 :saturation 18
       :steps [[50  97 14]    ;; [label lightness saturation]
               [100 95 14]
               [200 90 12]
               ...
               [950  5 18]]}
```

This generates CSS variables in `:root`:

```css
--gray-50:  #f6f6f8;
--gray-100: #f0f0f4;
--gray-200: #e2e2e9;
--gray-300: #cdcdd6;
--gray-400: #9a9aac;
--gray-500: #6a6a81;
--gray-600: #4c4c61;
--gray-700: #38384d;
--gray-800: #1f1f2d;
--gray-900: #13131b;
--gray-950: #0a0a0f;
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

The gray hue controls the tint of all neutral surfaces, borders, and text. Change it to shift the entire feel:

| Hue | Result |
|-----|--------|
| `240` | Purplish gray (default) |
| `220` | Blue-gray |
| `30` | Warm/sandy |
| `0` sat `0` | Pure neutral |

The accent hue controls buttons, focus rings, links:

| Hue | Result |
|-----|--------|
| `252` | Purple (default) |
| `220` | Blue |
| `0` | Red |
| `142` | Green |

After changing values, run `bb build-theme` to regenerate `dist/theme.css`.

### Per-step saturation

Steps can be `[label lightness]` to use the scale's default saturation, or `[label lightness saturation]` to override it. This is useful for chromatic colors where high saturation looks wrong at the extremes — the accent scale uses ~100% saturation for light tints and drops to ~70% for dark shades.

### Current palette

The default ships with a purplish gray (hue 240) and a vivid purple accent (hue 252, `--accent-500: #7a5afc`), inspired by the [activity-tracker](https://github.com/user/piui) app. The purple tint is strongest in dark backgrounds and fades to near-neutral in light ones, achieved by tapering saturation from 18% at the dark end to 14% at the light end.

## Theme Tokens

Beyond color scales, the theme includes:

- **Size scale** — `--size-1` (0.25rem) through `--size-16` (4rem), linear
- **Font scale** — `--font-xs` through `--font-3xl`, geometric (ratio 1.25)
- **Borders** — `--border-0/1/2`, referencing gray scale stops
- **Shadows** — `--shadow-0/1/2/3`, increasing elevation
- **Radii** — `--radius-sm` (6px), `--radius-md` (10px), `--radius-lg` (16px)

Light/dark mode switches automatically via `prefers-color-scheme`, or manually with `data-theme="dark"` on the root element.
