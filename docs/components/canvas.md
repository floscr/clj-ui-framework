# Canvas

> A frame that pans and zooms a world of nodes and edges and that code can steer: fit everything, reveal a node, follow the step that runs. With a ready drawing for runs: step cards on a rail.

- Type: Component (layout, Layout)
- Namespace: `ui.canvas`
- Source: `src/ui/canvas.cljc`
- Public fns: `bounds`, `edge-path`, `canvas-attrs`, `canvas-class-list`, `canvas-classes`, `canvas`, `world`, `node`, `edge-class-list`, `edge`, `edges`, `step-class-list`, `step-classes`, `step`, `note`, `tag`, `controls`, `flow`, `fit!`, `reveal!`, `center!`, `follow!`, `zoom-by!`, `zoom-to!`, `pan-by!`, `viewport`
- JS runtime: `ui.js.canvas` (bundled in ui-runtime.js)
- Keywords: canvas, pan and zoom, flow, workflow, node graph, state machine, pipeline, viewport, fit, reveal, follow

## When to use

- A graph too big to draw at one size: a workflow, a state machine, a pipeline, a board of cards.
- A run you watch: use flow, and the camera stays on the step that runs.
- Content the app has to bring into view itself (fit!, reveal!, center!).

## When not to use

- Use timeline for clips on tracks along one time axis.
- Use chart for data series.
- Use panels when panes trade space and nothing needs to zoom.
- For five steps in a row that always fit, a plain list (list-row) reads better than a canvas.

## Usage

```clojure
(require '[ui.canvas :as canvas])

(canvas/flow {:id "run" :label "Flow pr-1" :tag "ext.penpot/pr"
              :note "check 4, next in 1:12"
              :nodes [{:id "push" :title "push" :description "Both branches, or none" :icon :terminal :status :done :x 0 :y 0}
                      {:id "ci" :title "ci" :description "Checks of both PRs" :icon :clock :status :current :x 0 :y 92}
                      {:id "fix-ci" :title "fix-ci" :description "Failed twice: fix it" :icon :zap :x 276 :y 184}]
              :edges [{:from "push" :to "ci" :taken true}
                      {:from "ci" :to "fix-ci"}
                      {:from "fix-ci" :to "push" :rail :right}]})
```

## Examples

### Any content: a frame, a world, nodes

```clojure
(canvas/canvas {:id "board" :label "Board"}
  (canvas/world {}
    (canvas/node {:id "plan" :x 24 :y 24} (card/card {} "Plan the launch"))
    (canvas/node {:id "ship" :x 520 :y 320} (card/card {} "Ship it")))
  (canvas/controls {:follow false}))
```

### Steering from code (browser targets)

```clojure
(canvas/fit! "run")
(canvas/reveal! "run" "fix-ci" {:padding 16})
(canvas/center! "run" "ci" 1)
(canvas/zoom-by! "run" 1.25)
(canvas/pan-by! "run" 80 0)
(canvas/follow! "run" "comments")
```

### Inside a scrolling page

```clojure
(canvas/flow {:id "run" :gestures :page :height 360 :nodes nodes :edges edges})
```

## API reference

### `flow`

A run drawn from data: step cards on a rail, the camera on the step that runs.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:id` | `string` | – | Element id; what the steering fns take. |
| `:label` | `string` | – | Accessible name. |
| `:nodes` | `[{:id :title :description :icon :status :x :y}]` | – | Steps with their positions in world units (pixels at zoom 1). A card is 228 by 56; leave 92 or more between rows. |
| `:edges` | `[{:from :to :taken :rail}]` | – | :rail :left or :right sends a way back up the side of the graph. |
| `:follow` | `node id` | `the :current node` | The node the camera keeps centered. |
| `:tag` | `string` | – | Label in the top left corner. |
| `:note` | `string` | – | Pill beside the current step. |
| `:controls` | `boolean \| map` | `true` | false leaves the buttons out; a map is passed to controls. |
| `:height` | `px number` | – |  |
| `:gestures` | `:canvas \| :page` | `:canvas` |  |
| `:min-zoom` | `number` | `0.25` |  |
| `:max-zoom` | `number` | `2.5` |  |
| `:padding` | `px number` | `24` |  |
| `:insets` | `{:top :right :bottom :left}` | – |  |
| `:grid` | `boolean` | `true` |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `canvas`

The frame.

Children: One world, then what floats over it (controls, tag).

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:id` | `string` | – |  |
| `:label` | `string` | – | aria-label. |
| `:follow` | `node id` | – | The node the camera keeps centered. Render another one and the camera goes there. |
| `:initial` | `:fit \| :follow \| :none` | `:follow with :follow, else :fit` | Where the viewport starts. |
| `:min-zoom` | `number` | `0.25` |  |
| `:max-zoom` | `number` | `2.5` |  |
| `:padding` | `px number` | `24` | Kept free around what fit and reveal show. |
| `:insets` | `{:top :right :bottom :left}` | – | Pixels of the frame something covers, such as a panel floating over it: the camera fits, reveals and follows in the part left free, and re-centers the followed node when they change. |
| `:gestures` | `:canvas \| :page` | `:canvas` | :page inside a scrolling page: the page keeps the wheel and vertical swipes. |
| `:height` | `px number` | – | Without it the frame is 28rem tall; or size it with CSS. |
| `:grid` | `boolean` | `true` | The dot grid. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `world`

The layer the viewport moves.

Children: nodes, edges, notes.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:width` | `px number` | – |  |
| `:height` | `px number` | – | Both are needed when it holds edges. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `node`

A positioned box in the world that fit, reveal and follow can find.

Children: Any content.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:id` | `string` | – |  |
| `:x` | `number` | – |  |
| `:y` | `number` | – |  |
| `:width` | `px number` | – |  |
| `:height` | `px number` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `step`

A node drawn as a step of a run: icon, title, one line, and its state at the end of the card.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:id` | `string` | – |  |
| `:x` | `number` | – |  |
| `:y` | `number` | – |  |
| `:title` | `string` | – |  |
| `:description` | `string` | – |  |
| `:icon` | `ui.icon name` | – |  |
| `:status` | `:done \| :current \| :idle` | `:idle` |  |
| `:selected` | `boolean` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – | A click handler goes here. |

### `edges`

The SVG layer under the nodes.

Children: edge elements.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:width` | `px number` | – |  |
| `:height` | `px number` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `edge`

One edge between two rects.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:from` | `{:x :y :w :h}` | – |  |
| `:to` | `{:x :y :w :h}` | – |  |
| `:rail` | `:left \| :right` | – | A way back along the side. |
| `:rail-x` | `number` | – | Where that side line runs. |
| `:taken` | `boolean` | – | Drawn in full. |
| `:live` | `boolean` | – | Accent, and it moves: the way into the step that runs. |
| `:from-id` | `node id` | – | The node it leaves; with :to-id, hovering either node lights it up. |
| `:to-id` | `node id` | – | The node it enters. |
| `:class` | `string` | – |  |

### `controls`

The camera's buttons: follow, zoom out, percent, zoom in, fit. The runtime binds them.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:follow` | `boolean` | `true` |  |
| `:zoom` | `boolean` | `true` |  |
| `:fit` | `boolean` | `true` |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `tag`

A quiet label in the top left corner.

Children: Its text.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:live` | `boolean` | – | A dot that says it is running. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `note`

A small pill in the world, beside a node.

Children: Its text.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:x` | `number` | – |  |
| `:y` | `number` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `edge-path`

The geometry of an edge: {:d path-data :tip [x y degrees]} from two rects, for custom drawings.

### `bounds`

The rectangle around rects as {:left :top :right :bottom}.

### `canvas-attrs`

Data attributes for the frame, for custom markup.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| + and - | Zoom in and out around the center. |
| 0 | Zoom to 100%. |
| Arrow keys | Pan. |
| Tab | Walks the steps; the focused one is brought into view. |

## Accessibility

- The frame has role=application and takes :label as its name; it is focusable so the keys work.
- Steps are focusable, and focusing one by keyboard reveals it.
- Every control is a button with an aria-label; the follow button reports its state with aria-pressed.
- The picture is not the only source: say where a run stands in text beside the canvas as well.

## Motion

- A move from code (fit, reveal, follow, the buttons, the keys) eases over --duration-considered; a drag or pinch tracks the pointer.
- Hovering or focusing a node lights up its edges (out in the accent, in in the text color) and the nodes at their other ends, and dims the rest; flow does this for every step.
- The edge into the running step moves, and that step is lifted a little. Both stop under reduced motion.

## Responsive behavior

- The frame is as wide as its container; the camera centers the followed step again when it resizes.
- On a phone inside a scrolling page pass :gestures :page, or a swipe over the canvas never scrolls the page.
- In a narrow frame leave the zoom buttons out: (controls {:zoom false}).

## Targets

- **Hiccup (`:clj`)**: Markup plus ui-runtime.js. Steer it by rendering another :follow, or with the buttons; there is no steering fn on the server.
- **Replicant (`:cljs`)**: fit! reveal! center! follow! zoom-by! zoom-to! pan-by! viewport call the runtime. Listen for ui-canvas-viewport (detail {x y zoom}) and ui-canvas-follow (detail {following}) via :attrs.
- **Squint (`:squint`)**: Same as Replicant; require ui.js.canvas in the SPA.

## Notes for AI

- Positions are yours: the component does not lay a graph out. For flow, put the main path in one column (same :x), rows 92 apart, side steps 276 to the left or right, and give every way back a :rail on the side its source is on.
- Give every canvas an :id and a :label.
- A drag pans from anywhere, also from over a step; a press that does not move is a click on the step.
- Do not put a canvas with :gestures :canvas in the middle of a long mobile page.

## Related

- [Editing timeline](timeline.md): A media editing timeline: a scrubbable ruler, labeled tracks with draggable and trimmable segments, a playhead and a floating transport.
- [Toolbar](toolbar.md): A pill-shaped container that groups icon buttons with separators, horizontal or vertical, optionally floating in a popover.
- [Resizable panels](panels.md): Panel groups whose panes trade space by dragging a separator, with collapse, keyboard resizing and persisted sizes.
- [Card](card.md): A bordered surface that groups related content and actions, plus a divided list variant.

## Guidance for AI tools

Canvas: A frame that pans and zooms a world of nodes and edges and that code can steer: fit everything, reveal a node, follow the step that runs. With a ready drawing for runs: step cards on a rail. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
