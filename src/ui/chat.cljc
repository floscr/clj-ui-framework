(ns ui.chat
  "Chat conversation components: message log, bubbles, thinking indicator,
   toolbar, and input bar. Cross-target (hiccup/replicant/squint).

   Bubbles take pre-rendered children (plain text or markdown hiccup) —
   markdown rendering stays with the consumer. In the hiccup target the
   input form carries no behavior of its own; pass HTMX (or other)
   attributes via :attrs / :input-attrs / :send-attrs."
  (:require [clojure.string :as str]
            [ui.util :as util]
            [ui.form :as form]
            [ui.button :as button]
            [ui.spinner :as spinner]))

;; ── Bubble classes ──────────────────────────────────────────────────

(def default-role "assistant")

(defn chat-bubble-class-list
  "Returns a vector of CSS class strings for a chat bubble.
   Roles: :user (right-aligned, accent) or :assistant (left-aligned, surface)."
  [{:keys [role pending]}]
  (let [r (or (some-> role util/kw-name) default-role)]
    (cond-> ["chat-bubble" (str "chat-bubble-" r)]
      pending (conj "chat-bubble-pending"))))

(defn chat-bubble-classes
  "Returns a space-joined class string for a chat bubble."
  [opts]
  (str/join " " (chat-bubble-class-list opts)))

;; ── Log ─────────────────────────────────────────────────────────────

(defn chat-log
  "Flex-column container for chat bubbles.

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> "chat-log" class (str " " class))]
       (into [:div (merge {:class classes} attrs)] children))

     :cljs
     (let [classes (cond-> ["chat-log"] class (util/conj-classes class))]
       (into [:div (merge {:class classes} attrs)] children))

     :clj
     (let [classes (cond-> "chat-log" class (str " " class))]
       (into [:div (merge {:class classes} attrs)] children))))

;; ── Bubble ──────────────────────────────────────────────────────────

(defn chat-bubble
  "A single chat message bubble.

   Props:
     :role    - :user or :assistant (default)
     :pending - boolean; muted 'in-progress' styling (spinner row)
     :class   - additional CSS classes
     :attrs   - additional HTML attributes

   Children are the message content — plain text or pre-rendered
   markdown hiccup."
  [{:keys [role pending class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> (chat-bubble-classes {:role role :pending pending})
                     class (str " " class))]
       (into [:div (merge {:class classes} attrs)] children))

     :cljs
     (let [classes (cond-> (chat-bubble-class-list {:role role :pending pending})
                     class (util/conj-classes class))]
       (into [:div (merge {:class classes} attrs)] children))

     :clj
     (let [classes (cond-> (chat-bubble-classes {:role role :pending pending})
                     class (str " " class))]
       (into [:div (merge {:class classes} attrs)] children))))

;; ── Thinking indicator ──────────────────────────────────────────────

(defn chat-thinking
  "A pending assistant bubble: spinner + label. Show while a reply is
   being generated.

   Props:
     :label - text next to the spinner (default \"Thinking…\")
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [label class attrs] :as _props}]
  (chat-bubble {:role :assistant :pending true :class class :attrs attrs}
    (spinner/spinner {:size :sm})
    [:span (or label "Thinking…")]))

;; ── Toolbar ─────────────────────────────────────────────────────────

(defn chat-toolbar
  "Right-aligned action row above the log (e.g. a clear-chat button).

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> "chat-toolbar" class (str " " class))]
       (into [:div (merge {:class classes} attrs)] children))

     :cljs
     (let [classes (cond-> ["chat-toolbar"] class (util/conj-classes class))]
       (into [:div (merge {:class classes} attrs)] children))

     :clj
     (let [classes (cond-> "chat-toolbar" class (str " " class))]
       (into [:div (merge {:class classes} attrs)] children))))

;; ── Input bar ───────────────────────────────────────────────────────

(defn chat-input
  "The message composer: an auto-growing textarea plus a footer row with
   an optional hint (left) and a send button (right), wrapped in a <form>.

   Props:
     :placeholder - textarea placeholder
     :hint        - optional hiccup/text shown left of the send button
     :send-label  - send button text (default \"Send\")
     :send-icon   - optional icon keyword for the send button
     :max-rows    - textarea growth cap (default 5)
     :disabled    - boolean; disables textarea + button
     :on-submit   - form submit handler (ignored in :clj target)
     :input-attrs - extra attributes for the <textarea> (e.g. {:name \"message\"})
     :send-attrs  - extra attributes for the send button
     :class       - additional CSS classes on the <form>
     :attrs       - additional attributes on the <form> (e.g. HTMX)"
  [{:keys [placeholder hint send-label send-icon max-rows disabled
           on-submit input-attrs send-attrs class attrs] :as _props}]
  (let [textarea (form/form-textarea-auto
                  {:placeholder placeholder
                   :max-rows    (or max-rows 5)
                   :disabled    disabled
                   :attrs       input-attrs})
        send-btn (button/button
                  (cond-> {:variant :primary
                           :disabled disabled
                           :attrs (merge {:type "submit"} send-attrs)}
                    send-icon (assoc :icon-left send-icon))
                  (or send-label "Send"))
        footer   [:div {:class "chat-input-footer"}
                  (when hint [:span {:class "chat-hint"} hint])
                  send-btn]]
    #?(:squint
       (let [classes (cond-> "chat-input" class (str " " class))]
         [:form (cond-> (merge {:class classes} attrs)
                  on-submit (assoc :on-submit on-submit))
          textarea
          footer])

       :cljs
       (let [classes (cond-> ["chat-input"] class (util/conj-classes class))]
         [:form (cond-> (merge {:class classes} attrs)
                  on-submit (assoc-in [:on :submit] on-submit))
          textarea
          footer])

       :clj
       (let [classes (cond-> "chat-input" class (str " " class))]
         [:form (merge {:class classes} attrs)
          textarea
          footer]))))
