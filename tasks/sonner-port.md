# Port Sonner to ui.notification

Source read: sonner `src/index.tsx`, `src/styles.css`, `src/types.ts`, `src/state.ts`
(main, 10 Nov 2026). Values below are Sonner's.

## Split

Sonner keeps everything in React state. Here it splits along what each target can do:

| Layer | Where | Owns |
|---|---|---|
| Model (pure cljc) | `ui.notification` | queue, ids, types, durations, two-step removal, pause, mute, positions |
| Markup (cljc, all targets) | `ui.notification` | toaster `section` > `ol` per position > `li` per toast, Sonner's data attributes and props |
| CSS (tokens) | `notification.css` | positions, offsets, lift, stacking, enter/exit, swipe-out keyframes, mobile, rich colors, invert |
| Runtime (squint, DOM only) | `ui.js.notification` | measuring heights, mount flag, hover expand, swipe, hotkey, focus restore, tab hidden; reports back through DOM events |

The runtime only sets attributes the markup never renders (`data-mounted`, `data-expanded`,
`data-swiping`, `--offset`, ...), so a Replicant/Squint re-render never fights it.

## 1. Model

- [x] Types `:default :success :info :warning :error :loading` (keep `:danger` as alias of `:error`)
- [x] Default lifetime 4000 for every type; `:loading` and `:duration ##Inf`/0 never expire
      (today: success 5 s, warning 8 s, danger stays; Sonner's error leaves after 4 s)
- [x] Two-step removal: `dismiss` / `expire` mark `:removed-at now`; the item stays rendered
      (`data-removed`) and is dropped 200 ms later (`expire` + `next-due` cover both steps)
- [x] `:dismissible false`: no close button, no swipe, `dismiss` refuses it
- [x] Per-item `:position`, toaster default; `visible` grouped by position
- [x] `:auto-closed` vs dismissed recorded so the host can fire `on-auto-close` / `on-dismiss`
- [x] Keep: update by id, pause/resume (expanded, interacting, tab hidden), mute, limit
- [x] Tests for each

## 2. Markup + props

Toaster (`notification-toaster`): `:position` (6), `:expand`, `:visible-toasts` 3, `:gap` 14,
`:offset` / `:mobile-offset` (number, string or per side), `:close-button`, `:rich-colors`,
`:invert`, `:dir`, `:hotkey` (label in aria: "Notifications alt+T"), `:label`,
`:swipe-directions`, `:icons`, `:contained` (ours, not Sonner: absolute in the nearest
positioned ancestor, what xi needs above the composer).

Toast (`notification`): `:id :type :title :description :icon` (nil hides it), `:action`
and `:cancel` `{:label :on-click}` or hiccup, `:close-button :dismissible :rich-colors
:invert :unstyled :position :class :class-names :style :on-dismiss`.

- [x] Data attributes as Sonner: `data-type data-front data-index data-visible data-removed
      data-rich-colors data-invert data-styled data-dismissible data-y-position data-x-position`,
      vars `--index --toasts-before --z-index`
- [x] Sonner's loader (12 bars) as the loading icon, fading into the result icon (promise look)

## 3. CSS (port `styles.css` onto tokens)

- [x] Toaster fixed at offsets (24 px, 16 px under 600 px wide; full width on mobile)
- [x] Enter: `translateY(±100%)`, opacity 0 → `data-mounted`
- [x] Collapsed: back toasts `height: var(--front-toast-height)`,
      `translateY(lift * gap * index) scale(1 - 0.05 * index)`, children hidden; beyond 3 hidden
- [x] Expanded: `translateY(lift * --offset)`, own height; `::after` bridge over the gap
- [x] Exit: front up/down 100% + fade; collapsed back ones `translateY(40%)`, 500 ms / 200 ms
- [x] Swipe: follows `--swipe-amount-x/y`; `swipe-out-{left,right,up,down}` 200 ms ease-out
- [x] Transitions 400 ms transform/opacity/height (motion tokens where they match)
- [x] Rich colors and invert on our tokens; close button top-left `translate(-35%,-35%)`, RTL mirrored
- [x] Reduced motion: no transitions/animations

## 4. Runtime `ui.js.notification` (squint, in ui-runtime.js)

- [x] Heights: ResizeObserver per toast → `--initial-height`, `--offset` (heights before + gap
      * index), `--front-toast-height` on the list; removed toasts keep their offset
- [x] Mount: `data-mounted` the frame after insertion
- [x] Expand on mouseenter/mousemove, collapse on leave unless a pointer is down; `expand` default
- [x] Swipe: pointer capture, direction lock after 1 px, damping `1/(1.5+|d|/20)` off-axis,
      dismiss at 45 px or 0.11 px/ms in an allowed direction; ignores buttons and text selection
- [x] Alt+T focuses and expands, Escape collapses, focus returns where it came from
- [x] Emits DOM events on the toast/list: `notification-dismiss {id, reason swipe|close|action}`,
      `notification-expand {expanded}`, `notification-interact {on}`, `notification-visibility {hidden}`
- [ ] Tests for the pure parts (offset math, swipe decision, damping)

## 5. HTMX / hiccup: Sonner's imperative API

- [x] `window.__uiNotify` / `toast.success|error|info|warning|loading|promise|dismiss` on the
      model in the runtime, rendering with the same markup; `toast-flash` markers feed it
- [ ] `ui.toast` becomes a thin alias, or stays as is (decide)

## 6. Docs, examples, verify

- [x] Sidecar, examples (positions, rich colors, action/cancel, promise, swipe), gen-docs
- [ ] Side-by-side screenshots against sonner.emilkowal.ski (collapsed, expanded, exit, swipe)
- [ ] `bb build-theme` `bb lint-css` `bb test` `bb gen-docs` `bb check-dev`

## 7. xi

- [x] Switch `xi.web.notifications` to the toaster (`:contained` above the composer), listen to
      the runtime's events (dismiss, expand → pause/resume, tab hidden), swipe on the phone
- [x] Push framework, bump pin, restart, verify on the demo

## Decisions (10 Nov 2026)

1. Skin: ours (tinted icon tile, description, meta line), without the colored status border.
2. Everything else like Sonner: 4 s for every type, errors included; close button opt-in.
3. Phase 5 (imperative API) is in.

API follows Sonner: `:type` with `:default :success :info :warning :error :loading`
(replaces `:status`/`:danger`); the toaster renders items (data) instead of card children,
since index, front and visibility come from the list. Close, action and cancel clicks and
swipes are handled by the runtime, which tells the host through one `notification-dismiss`
event; pausing (expanded, pointer down, tab hidden) through `notification-pause`.

## Review (10 Nov 2026)

Done in clj-ui-framework 7a80228 + ebab406, xi 7a8ad569. Verified in Chrome: static
cards, contained and page toasters, hover fan-out offsets, swipe (preview and xi),
__uiNotify (close, action, auto-close, promise), 390px layout, xi demo round trip.

Open:
- The runtime's pure parts (offsets, swipe decision, damping) have no tests: squint
  modules have no test runner here.
- `ui.toast` stays as it is; `__uiNotify` is the Sonner API.
- No formal side-by-side against Sonner's site; only the measured values were compared.
- `bb check-dev` not run (no dev servers); build-theme, lint-css, test, gen-docs pass.
- ui-runtime.js grew from 313 KB to 424 KB (unminified): the runtime now carries
  ui.notification, ui.icon and ui.button for __uiNotify.
