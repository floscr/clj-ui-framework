# Chat

> Conversation building blocks: a message log, user and assistant bubbles, a thinking indicator, a toolbar and an auto-growing composer.

- Type: Component (data, Conversation)
- Namespace: `ui.chat`
- Source: `src/ui/chat.cljc`
- Public fns: `chat-bubble-class-list`, `chat-bubble-classes`, `chat-log`, `chat-bubble`, `chat-thinking`, `chat-toolbar`, `chat-input`
- Keywords: chat, messages, assistant, composer, ai chat, bubbles

## When to use

- An assistant or support conversation with a composer at the bottom.
- Streaming replies where a pending bubble shows progress.

## When not to use

- Use toast or alert for one-off system messages.
- Use form-textarea-auto alone when there is no conversation to show.

## Usage

```clojure
(require '[ui.chat :as chat])

[:div
 (chat/chat-log {}
   (chat/chat-bubble {:role :user} "Summarize this page")
   (chat/chat-bubble {:role :assistant} rendered-markdown)
   (chat/chat-thinking {}))
 (chat/chat-input {:placeholder "Message…" :on-submit send! :input-attrs {:name "message"}})]
```

## Examples

### Hiccup with HTMX

```clojure
(chat/chat-input {:attrs {:hx-post "/chat" :hx-target "#log" :hx-swap "beforeend"}
                  :input-attrs {:name "message"}})
```

## API reference

### `chat-log`

Flex column for bubbles.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `chat-bubble`

One message. Children are text or pre-rendered markdown hiccup.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:role` | `:user \| :assistant` | `:assistant` |  |
| `:pending` | `boolean` | – | Muted in-progress styling. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `chat-thinking`

Pending assistant bubble with a spinner.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:label` | `string` | `"Thinking…"` |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `chat-toolbar`

Right-aligned actions above the log.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `chat-input`

A <form> with an auto-growing textarea, an optional hint and a send button.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:placeholder` | `string` | – |  |
| `:hint` | `hiccup \| string` | – |  |
| `:send-label` | `string` | `"Send"` |  |
| `:send-icon` | `icon keyword` | – |  |
| `:max-rows` | `int` | `5` |  |
| `:disabled` | `boolean` | – |  |
| `:on-submit` | `fn [event]` | – | :cljs and :squint. |
| `:input-attrs` | `map` | – | On the textarea (name, required). |
| `:send-attrs` | `map` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – | On the form (HTMX attributes). |

### `chat-bubble-classes`

Class string for {:role :pending}.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Enter | Submits the composer (Shift+Enter inserts a newline). |

## Accessibility

- Bubbles are plain blocks; mark the log as a live region in :attrs when replies stream in.

## Motion

- Thinking spinner only; new bubbles do not animate in.

## Responsive behavior

- Bubbles cap at 85% width; the composer sticks to the bottom of its container.

## Targets

- **Hiccup (`:clj`)**: Composer submits as a normal form or via HTMX attributes.
- **Replicant (`:cljs`)**: Handle :on-submit and append bubbles from state.
- **Squint (`:squint`)**: Same as Replicant.

## Notes for AI

- Render markdown yourself (ui.markdown or your own) and pass hiccup as bubble children.

## Related

- [Markdown](markdown.md): A minimal markdown to hiccup renderer for docs and chat replies: headings, code blocks, tables, lists, inline code and bold.
- [Form](form.md): Labeled fields, inputs, textareas, checkboxes, radios, file and range inputs, joined groups and a search bar.
- [Spinner](spinner.md): An indeterminate loading indicator in four sizes that inherits the current text color.

## Guidance for AI tools

Chat: Conversation building blocks: a message log, user and assistant bubbles, a thinking indicator, a toolbar and an auto-growing composer. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
