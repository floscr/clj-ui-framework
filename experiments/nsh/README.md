# Spike: clj-ui-framework components on Native Shell (nsh.dev) via clojurific

Question tested: can [clojurific](https://github.com/WhimsicalCode/clojurific)
(a ClojureScript compiler fork that emits ES modules) get our `.cljc`
components onto [Native Shell](https://nsh.dev) (NSH), the runtime that draws
one JS module as native UI on iOS, Android, macOS, Windows, Linux, GPUI and the
web?

Result: yes, end to end. The real `ui.button` (its `:cljs` / Replicant branch,
untouched) renders as native NSH views, its `:on-click` fires under NSH's
hardened headless engine, and the same bundle runs in NSH's web dev host.

## Pipeline

```
src/ui/*.cljc  +  src/spike/*.cljs
      |  clojurific  (clojure -M -m cljf.esm build ...)      ~2 s cold, 0.6 s warm
      v
out/**/*.js   ES modules, one per namespace (cljs.core included)
      |  scripts/guard_date.clj  (see Caveats)
      v
app/App.tsx   = `import "../out/spike/app.js"`
      |  nsh build / nsh trace / nsh run      (rolldown bundle -> QuickJS bytecode)
      v
native views
```

- `src/spike/render.cljs`: a ~70-line interpreter from Replicant-style hiccup
  to NSH nodes. It calls `element`, `prop`, `insert`, `append` from `nsh:rt`,
  the runtime module NSH's own JSX compiler emits calls to (user code may import
  it; calling `View()` / `Text()` from `nsh` directly works too, see
  `rt-probe/`). Tags map to `view` / `input` / `text`; `:on {:click f}` becomes
  `onPress`; `:disabled`, `:style`, `:id` pass through; SVG subtrees are
  skipped.
- Framework class tokens (`btn`, `btn-primary`, `btn-sm`, `spinner`, ...) map to
  Tailwind classes, which NSH resolves natively. Unknown tokens pass through.
- `src/spike/app.cljs`: `mount` + one `signal`; `insert(root, () => render(view(count())))`
  rebuilds the whole subtree on every change (no VDOM diff, see Caveats).

## Run it

Needs Java 17+, the Clojure CLI and the `nsh` binary
(`curl -fsSL https://nsh.dev/install.sh | sh`, or unpack
`nsh-linux-x86_64.tar.gz` from the GitHub releases; on NixOS the prebuilt
binary runs as is through nix-ld, `steam-run` does not find it). Point `NSH`
at the binary if it is not on `PATH`.

```sh
cd experiments/nsh
NSH=/path/to/nsh bb trace     # headless, hardened engine, taps the button
NSH=/path/to/nsh bb web       # http://127.0.0.1:8790/ in a browser
NSH=/path/to/nsh bb release   # minified bundle sizes
```

`bb trace` prints the mount batches; look for `Text { ... "Clicked 1 times" }`
after `tap #1` and `"Clicked 0 times"` after the `Reset` tap.

## Numbers (nsh 0.1.1, clojurific 0.12.7)

| Step | Result |
|---|---|
| clojurific compile (8 namespaces: cljs.core, clojure.string, ui.util, ui.icon, ui.spinner, ui.button, spike.*) | 2.0 s cold, 0.6 s incremental |
| `nsh build` (dev) | 2.0 MB js, 2.8 MB bytecode, 150 ms |
| `nsh build --release` | 869 KB js, 625 KB bytecode |
| `nsh trace` first frame | 71 mount items |
| one tap (whole subtree rebuilt) | 82 mount items |

The release size is cljs.core without clojurific's prune pass (that lives in
its Vite plugin, which this pipeline does not use). Running the clojurific
Vite build first and handing NSH the pruned bundle would shrink it.

## Caveats found

1. **Frozen prototypes.** NSH's hardening (release builds, `nsh trace`) freezes
   the shared prototypes. cljs.core extends `IEquiv` / `IComparable` on
   `js/Date` by assigning to `Date.prototype`, which throws
   `object is not extensible` while the bundle evaluates and the app renders
   nothing. `scripts/guard_date.clj` wraps those six statements in an
   `Object.isExtensible` guard after each compile. The proper fix is one guard
   in cljs.core / clojurific. The web dev host is not hardened, so it works
   without the patch.
2. **Styling is a port, not a translation.** Our CSS (tokens as CSS variables,
   `:hover`, `:focus-visible`, media queries, `::before` hit areas, the JS
   runtime for menus and tooltips) has no counterpart on NSH. What transfers
   is the component logic and the class vocabulary; each class token needs a
   Tailwind mapping (`class->tw` in `render.cljs`), and NSH's Tailwind theme
   (`nsh.json` `"tailwind"`) could be generated from `theme/tokens.edn`.
3. **Icons.** NSH has no SVG. `ui.icon` paths could be drawn with
   `nsh/canvas` (`Path2D` is supported); the spike skips them.
4. **Reactivity.** NSH is Solid-style (components run once, props are getters).
   Our components are pure hiccup functions, so the spike re-renders the whole
   subtree per signal change. Fine for small screens; a real target would keep
   the hiccup API but diff, or wrap stateful leaves (inputs, lists) in NSH
   primitives.
5. **nsh 0.1.1 vs docs.** The release has `trace` (with `--tap`) but not yet the
   documented `nsh test`.
