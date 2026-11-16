# Comment

> Comments as markers on one rail, each with its replies folded into a line that opens in place as a flat branch, so a deep discussion stays readable in a narrow pane.

- Type: Component (data, Conversation)
- Namespace: `ui.comment`
- Source: `src/ui/comment.cljc`
- Public fns: `replies-label`, `person-tone`, `flat-replies`, `comment-class-list`, `comment-classes`, `comment-thread`, `comment-item`, `comment-reply`, `comment-replies`
- Keywords: comment, comments, thread, replies, discussion, comment tree, review, notes, hacker news, feedback

## When to use

- The discussion under an article, a post or a story, in a pane or a page.
- Review notes or feedback attached to one record, where each note may have a few answers.
- A single remark with an author and a time, on its own (comment-item without a thread).

## When not to use

- Use chat for a live back-and-forth with a composer, where every message is a bubble.
- Use list-row for records that open a detail view and have no body text.
- Use accordion for a few collapsible sections of content that are not answers to one another.
- This component shows two levels. When the exact shape of a deep tree matters (who answered whom, six levels down), render the nesting yourself; flat-replies is the opposite choice.

## Usage

```clojure
(require '[ui.comment :as comment])

(comment/comment-thread {}
  (comment/comment-item {:author "marrow" :time "5h" :meta "42 points"
                         :replies (comment/comment-replies {:people ["dunlin" "sorenb"]}
                                    (comment/comment-reply {:author "dunlin" :time "4h"}
                                      "It only works because the rotation isn't random.")
                                    (comment/comment-reply {:author "sorenb" :badge "author" :time "3h" :to "dunlin"}
                                      "It never picks the variant it used last."))}
    "The contextual alternates are the whole trick.")
  (comment/comment-item {:author "tessellate" :time "5h"}
    "Printed a page next to my actual handwriting."))
```

## Examples

### From a reply tree: flat-replies turns nesting into :to

```clojure
(let [replies (comment/flat-replies story-comment)]
  (comment/comment-item {:author (:author story-comment) :time (:time story-comment)
                         :replies (when (seq replies)
                                    (apply comment/comment-replies
                                           {:people (map :author replies)}
                                           (for [{:keys [author time to text]} replies]
                                             (comment/comment-reply {:author author :time time :to to} text))))}
    (:text story-comment)))
```

### The comment a link pointed at, and a deleted one kept for its replies

```clojure
(comment/comment-thread {}
  (comment/comment-item {:author "tessellate" :time "5h" :target true} "My partner picked the font as the real one.")
  (comment/comment-item {:deleted true :time "4h"
                         :replies (comment/comment-replies {}
                                    (comment/comment-reply {:author "greyscale" :time "4h"} "The parent asked about licensing. It is OFL."))}
    "This comment was deleted."))
```

### Loading and empty

```clojure
;; while the thread loads
(skeleton/skeleton {:variant :line})

;; a thread with nothing in it
(empty-state/empty-state {:icon :inbox} "No comments yet.")
```

## API reference

### `comment-thread`

The rail: its comments are markers on one hairline, 20px apart from the next comment. Set --comment-surface on it when the thread sits on a surface other than --bg-0, so the markers and dots cut their gap out of the right color.

Children: comment-item elements.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `comment-item`

One comment, an <article>: a head line (author, badge, time, meta), the body, then its folded replies. In a comment-thread it gets a marker on the rail; outside one it is a plain block.

Children: The body: text or hiccup. Paragraphs, links and pre blocks are styled.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:author` | `string \| hiccup` | – | Who wrote it. A deleted comment without one reads "deleted". |
| `:badge` | `string` | – | A short accent word after the author: "author", "maintainer". |
| `:time` | `string` | – | When, already formatted: "5h", "12 Nov". |
| `:meta` | `string \| hiccup` | – | Trailing muted text: "42 points", "edited". |
| `:target` | `boolean` | – | The comment a link pointed at: its body is tinted and its marker takes the accent. |
| `:deleted` | `boolean` | – | A removed comment kept for its replies: muted, italic, a hollow marker. |
| `:replies` | `hiccup` | – | A comment-replies element, shown under the body. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `comment-replies`

The replies of one comment behind a line: a dot per person who answered, the count and a chevron. A native <details>: a click, Enter or Space opens the branch in place, and the replies arrive one after the other.

Children: comment-reply elements.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:label` | `string` | `"N replies" from the children` | The line's text. Pass it when the count is not the number of children (replies loaded later). |
| `:people` | `seq of strings` | – | Names of the people who answered. Up to four distinct ones become dots; a name always gets the same color. |
| `:open` | `boolean` | `false` | Opened from the start. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `comment-reply`

One reply on an opened branch, tied to it by a tick. Replies never nest: an answer to a reply sits on the same branch and says whom it answers.

Children: The body: text or hiccup.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:author` | `string \| hiccup` | – |  |
| `:badge` | `string` | – |  |
| `:time` | `string` | – |  |
| `:to` | `string` | – | Whom it answers, when that is another reply; shown as "to marrow". Leave it out for a direct reply to the comment. |
| `:meta` | `string \| hiccup` | – |  |
| `:target` | `boolean` | – |  |
| `:deleted` | `boolean` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `flat-replies`

A comment's reply tree as the flat branch shown here: every reply at any depth in reading order, without its nested replies, and with :to (the author it answers) on answers to replies. Takes a map with :replies and :author; opts {:replies-key :author-key} name other keys.

### `replies-label`

"1 reply", "3 replies".

### `person-tone`

A stable number from 1 to 5 for a name, the color class of that person's dot (comment-face-N).

### `comment-classes`

Class string for {:reply :target :deleted}.

### `comment-class-list`

Vector form of comment-classes.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Moves to the next replies line, then into links inside comments. |
| Enter / Space | Opens or closes the branch of the focused replies line. |

## Accessibility

- Each comment is an <article>; the replies line is a native <summary>, so it is focusable, announces expanded or collapsed and needs no script.
- The dots on a folded line are decorative (aria-hidden); the count is text.
- State is never color alone: a linked comment is tinted and its marker ringed, a deleted one is italic and its marker hollow.

## Motion

- Opening a branch fades and drops its replies in one after the other (duration-standard, ease-enter); the chevron turns over duration-fast.
- The replies line fills on hover and scales to 0.97 on press.
- With reduced motion the replies appear at once and nothing transitions.

## Responsive behavior

- Nothing indents past one level, so a 320px pane loses 44px to the rail and the branch however deep the discussion is.
- The replies line is 28px tall, 36px on touch. Head lines wrap; long words and links in a body break.

## Targets

- **Hiccup (`:clj`)**: Static markup; the fold works without JS. Body HTML from a server must be sanitized by the caller.
- **Replicant (`:cljs`)**: The fold is uncontrolled: the browser owns <details> open. :class may be a vector.
- **Squint (`:squint`)**: Same as :cljs; no handlers are attached.

## Notes for AI

- Build a thread as comment-thread > comment-item, with the replies passed as :replies (a comment-replies element holding comment-reply children). Do not nest comment-item inside comment-item.
- For data that is a tree, call flat-replies on each top-level comment and pass :to through to comment-reply; do not indent replies yourself.
- Pass :people (the reply authors) to comment-replies so the folded line shows who answered.
- Leave long threads folded; open the first one or the one that holds the :target reply.
- Use skeleton while a thread loads and empty-state when it has no comments.

## Related

- [Chat](chat.md): Conversation building blocks: a message log, user and assistant bubbles, a thinking indicator, a toolbar and an auto-growing composer.
- [Accordion](accordion.md): Progressively reveal supporting content in place, using native details and summary with no JS.
- [List row](list-row.md): Records as rows on one surface: a leading icon or thumbnail, title and description, metadata and controls at the end, with hover for rows that lead somewhere.
- [Skeleton](skeleton.md): Reserves space with a pulsing placeholder while content loads.
- [Empty state](empty-state.md): A centered placeholder with an optional icon for lists and views that have nothing to show yet.
- [Badge](badge.md): A small pill for status, category or a count, with an optional leading icon.

## Guidance for AI tools

Comment: Comments as markers on one rail, each with its replies folded into a line that opens in place as a flat branch, so a deep discussion stays readable in a narrow pane. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
