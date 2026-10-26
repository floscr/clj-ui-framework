(ns ui.js.context-menu
  "Tiny JS runtime for context menus. Compiled from squint, loaded as
   a <script> tag in the hiccup target. Also used by squint/replicant
   targets via window.__uiContextMenu.

   Creates floating menu DOM at cursor position, handles dismiss on
   click-outside / Escape / scrolling the trigger, navigates on item click.")

;; ── State (stored on window to avoid module scope issues) ───────────

(defn- get-state []
  (or (aget js/window "__uiCtxState")
      (let [s #js {:menu nil :cleanup nil :growthObserver nil
                   :armed nil :back nil}]
        (aset js/window "__uiCtxState" s)
        s)))

;; on <html> while a menu is open — context_menu.css turns off text
;; selection for the whole page under it
(def ^:private open-class "clj-ui-menu-open")

;; ── SVG Icon Creation ───────────────────────────────────────────────

(defn- create-icon [elements]
  (let [ns-uri "http://www.w3.org/2000/svg"
        svg    (.createElementNS js/document ns-uri "svg")]
    (.setAttribute svg "viewBox" "0 0 24 24")
    (.setAttribute svg "width" "18")
    (.setAttribute svg "height" "18")
    (.setAttribute svg "fill" "none")
    (.setAttribute svg "stroke" "currentColor")
    (.setAttribute svg "stroke-width" "1.6")
    (.setAttribute svg "stroke-linecap" "round")
    (.setAttribute svg "stroke-linejoin" "round")
    (.setAttribute svg "style" "flex-shrink:0")
    ;; Each element is [tag-string [[attr-name attr-value] ...]].
    (.forEach elements
      (fn [el]
        (let [tag   (aget el 0)
              attrs (aget el 1)
              node  (.createElementNS js/document ns-uri tag)]
          (.forEach attrs
            (fn [pair]
              (.setAttribute node (aget pair 0) (aget pair 1))))
          (.appendChild svg node))))
    svg))

;; ── Confirm: tap again (default) ───────────────────────────────────
;; First activation arms the row in place: it fills with the danger color,
;; reads "Tap again to …" and a bar drains over `arm-ms`. A second
;; activation runs it; the timeout, Escape or closing the menu disarms.

(def ^:private arm-ms 3000)

(defn- disarm!
  "Restore the armed row, if any. Returns true when one was armed."
  []
  (let [state (get-state)
        armed (.-armed state)]
    (when armed
      (js/clearTimeout (.-timer armed))
      (.remove (.. armed -el -classList) "context-menu-item--armed")
      (.remove (.. armed -menu -classList) "context-menu--armed")
      (set! (.. armed -labelEl -textContent) (.-label armed))
      (set! (.-armed state) nil)
      true)))

(defn- again-label [label]
  (let [verb (if (.contains (.. js/document -documentElement -classList) "clj-ui-touch")
               "Tap" "Click")]
    (str verb " again to " (.toLowerCase (.charAt label 0)) (.slice label 1))))

(defn- arm! [menu el label-el item]
  (disarm!)
  (let [label (aget item "label")]
    (.setProperty (.-style el) "--context-menu-arm-duration" (str arm-ms "ms"))
    (.add (.-classList el) "context-menu-item--armed")
    (.add (.-classList menu) "context-menu--armed")
    (set! (.-textContent label-el) (again-label label))
    (set! (.-armed (get-state))
          #js {:el el :menu menu :labelEl label-el :label label
               :timer (js/setTimeout disarm! arm-ms)})))

;; ── Dismiss ─────────────────────────────────────────────────────────

(defn- dismiss! []
  (disarm!)
  (let [state (get-state)]
    (set! (.-back state) nil)
    (when (.-menu state)
      (.remove (.-menu state))
      (set! (.-menu state) nil)
      ;; let ui/js/gestures release the held press visual in sync
      (.dispatchEvent js/document (js/CustomEvent. "clj-ui-menu-dismiss")))
    (when (.-growthObserver state)
      (.disconnect (.-growthObserver state))
      (set! (.-growthObserver state) nil))
    (when (.-cleanup state)
      ((.-cleanup state))
      (set! (.-cleanup state) nil))))

(defn- execute-item! [item]
  (let [on-click (aget item "on-click")
        url      (aget item "url")]
    (if on-click
      (on-click)
      (when url
        (set! (.-location js/window) url)))))

;; ── Confirm: panel (:confirm-style :panel) ──────────────────────────
;; The menu drills into a second page: back header, what is affected, a
;; full-width action button and Cancel. Back, Cancel and Escape return to
;; the menu instead of closing it.

;; matches --duration-standard / --ease-standard (WAAPI can't read tokens)
(def ^:private resize-ms 240)
(def ^:private resize-easing "cubic-bezier(0.2, 0, 0, 1)")

(defn- reduced-motion? []
  (.-matches (js/window.matchMedia "(prefers-reduced-motion: reduce)")))

(defn- animate-height!
  "Animate `menu` from height `h0` to its current height."
  [menu h0]
  (let [h1 (.-offsetHeight menu)]
    (when (and (not= h0 h1) (.-animate menu) (not (reduced-motion?)))
      (.animate menu #js {:height #js [(str h0 "px") (str h1 "px")]}
                #js {:duration resize-ms :easing resize-easing}))))

(defn- make-el [tag class-name]
  (let [el (.createElement js/document tag)]
    (set! (.-className el) class-name)
    el))

(defn- make-button [class-name text on-click]
  (let [btn (make-el "button" class-name)]
    (.setAttribute btn "type" "button")
    (.setAttribute btn "tabindex" "-1")
    (when text (.appendChild btn (.createTextNode js/document text)))
    (.addEventListener btn "click"
      (fn [e]
        (.preventDefault e)
        (.stopPropagation e)
        (on-click)))
    btn))

(def ^:private chevron-left #js [#js ["path" #js [#js ["d" "m15 18-6-6 6-6"]]]])

(defn- show-panel!
  "Swap the menu's items for the confirm page of `item`. `on-back` restores
   the item list."
  [menu item on-back]
  (let [h0      (.-offsetHeight menu)
        w0      (.-offsetWidth menu)
        label   (aget item "label")
        confirm (aget item "confirm")
        title   (aget item "confirm-title")
        message (if (string? confirm) confirm "This can't be undone.")
        danger  (= (aget item "variant") "danger")
        panel   (make-el "div" "context-menu-panel")
        head    (make-el "div" "context-menu-panel-head")
        back    (make-button "context-menu-panel-back context-menu-action" nil on-back)
        body    (make-el "div" "context-menu-panel-body")
        actions (make-el "div" "context-menu-panel-actions")
        run     (make-button (str "btn btn-sm context-menu-action "
                                  (if danger "btn-danger" "btn-primary"))
                             nil
                             (fn [] (dismiss!) (execute-item! item)))
        cancel  (make-button "btn btn-sm btn-ghost context-menu-action" "Cancel" on-back)]
    (disarm!)
    (.setAttribute back "aria-label" "Back")
    (.appendChild back (create-icon chevron-left))
    (.appendChild head back)
    (let [span (make-el "span" "")]
      (set! (.-textContent span) label)
      (.appendChild head span))
    (when title
      (let [t (make-el "p" "context-menu-panel-title")]
        (set! (.-textContent t) title)
        (.appendChild body t)))
    (let [m (make-el "p" "context-menu-panel-message")]
      (set! (.-textContent m) message)
      (.appendChild body m))
    (let [paths (aget item "icon-paths")]
      (when (and paths (> (.-length paths) 0))
        (.appendChild run (create-icon paths))))
    (.appendChild run (.createTextNode js/document label))
    (.appendChild actions run)
    (.appendChild actions cancel)
    (.appendChild panel head)
    (.appendChild panel body)
    (.appendChild panel actions)
    (set! (.-innerHTML menu) "")
    (set! (.. menu -style -minWidth) (str w0 "px"))
    (.add (.-classList menu) "context-menu--panel")
    (.appendChild menu panel)
    (set! (.-back (get-state)) on-back)
    (animate-height! menu h0)
    ;; Cancel first: a stray Enter never runs the action
    (.focus cancel)))

;; ── Menu DOM Creation ───────────────────────────────────────────────

(defn- fill-menu!
  "Render `items` into `menu`, replacing its content. Focuses the item at
   index `focus-idx` when given (returning from a confirm panel)."
  [menu items focus-idx]
  (let [h0 (.-offsetHeight menu)]
    (set! (.-innerHTML menu) "")
    (.remove (.-classList menu) "context-menu--panel")
    (set! (.. menu -style -minWidth) "")
    (set! (.-back (get-state)) nil)
    (.forEach items
      (fn [item idx]
        (if (= (aget item "type") "separator")
          ;; Separator
          (let [sep (make-el "div" "context-menu-separator")]
            (.setAttribute sep "role" "separator")
            (.appendChild menu sep))
          ;; Item
          (let [url    (aget item "url")
                el     (make-el (if url "a" "button")
                                (if (= (aget item "variant") "danger")
                                  "context-menu-item context-menu-item--danger"
                                  "context-menu-item"))
                span   (.createElement js/document "span")]
            (.setAttribute el "role" "menuitem")
            (.setAttribute el "tabindex" "-1")
            (when url (.setAttribute el "href" url))
            ;; Icon
            (let [paths (aget item "icon-paths")]
              (when (and paths (> (.-length paths) 0))
                (.appendChild el (create-icon paths))))
            ;; Label
            (set! (.-textContent span) (aget item "label"))
            (.appendChild el span)
            ;; Click handler
            (.addEventListener el "click"
              (fn [e]
                (.preventDefault e)
                (.stopPropagation e)
                (cond
                  (not (aget item "confirm"))
                  (do (dismiss!) (execute-item! item))

                  (= (aget item "confirm-style") "panel")
                  (show-panel! menu item (fn [] (fill-menu! menu items idx)))

                  (.contains (.-classList el) "context-menu-item--armed")
                  (do (dismiss!) (execute-item! item))

                  :else
                  (arm! menu el span item))))
            (.appendChild menu el)
            (when (= idx focus-idx) (.focus el))))))
    (when (some? focus-idx) (animate-height! menu h0))))

(defn- create-menu [items]
  (let [menu (make-el "div" "context-menu")]
    (.setAttribute menu "role" "menu")
    (fill-menu! menu items nil)
    menu))

;; ── Positioning ─────────────────────────────────────────────────────

(defn- clamp-to-viewport!
  "Keep `menu` fully on-screen: if it overflows the right or bottom viewport
   edge, pull it back (never past the 8px top/left margin). Measures via
   style left/top + offsetWidth/Height — NOT getBoundingClientRect — so the
   open animation's transform scale can't skew the numbers. Respects any
   repositioning done after open."
  [menu]
  (let [x  (js/parseFloat (.. menu -style -left))
        y  (js/parseFloat (.. menu -style -top))
        w  (.-offsetWidth menu)
        h  (.-offsetHeight menu)
        vw (.-innerWidth js/window)
        vh (.-innerHeight js/window)]
    (when (> (+ x w) (- vw 8))
      (set! (.. menu -style -left) (str (max 8 (- vw w 8)) "px")))
    (when (> (+ y h) (- vh 8))
      (set! (.. menu -style -top) (str (max 8 (- vh h 8)) "px")))))

(defn- position-menu! [menu x y]
  (set! (.. menu -style -left) (str x "px"))
  (set! (.. menu -style -top)  (str y "px"))
  ;; Append first so we can measure
  (.appendChild js/document.body menu)
  (clamp-to-viewport! menu)
  ;; Re-clamp when the menu's contents change after open — apps may inject
  ;; extra DOM (action bars, search inputs) post-positioning, which would
  ;; otherwise push the menu past the bottom edge. MutationObserver fires as
  ;; a microtask (childList only, so our own style writes don't retrigger).
  (let [mo (js/MutationObserver. (fn [_] (clamp-to-viewport! menu)))]
    (.observe mo menu #js {:childList true :subtree true})
    (set! (.-growthObserver (get-state)) mo)))

(defn- moves-trigger?
  "Whether a scroll event's target carries `trigger` along: the document or a
   scroll container around it. Unrelated scrollers (a chat pane following new
   output) leave the menu's anchor where it is."
  [trigger e]
  (let [t (.-target e)]
    (or (not trigger)
        (not (.-contains t))
        (.contains t trigger))))

;; ── Keyboard Navigation ─────────────────────────────────────────────

;; menu items, or the buttons of a confirm panel
(def ^:private focusable ".context-menu-item, .context-menu-action")

(defn- focus-first-item! [menu]
  (let [first-item (.querySelector menu focusable)]
    (when first-item (.focus first-item))))

(defn- focus-next! [menu direction]
  (let [items  (.querySelectorAll menu focusable)
        active (.-activeElement js/document)
        len    (.-length items)]
    (when (> len 0)
      (let [current-idx (let [result (atom -1)]
                          (.forEach items
                            (fn [item i]
                              (when (= item active)
                                (reset! result i))))
                          @result)
            next-idx    (cond
                          (= direction "down") (if (< current-idx (- len 1))
                                                 (+ current-idx 1) 0)
                          (= direction "up")   (if (> current-idx 0)
                                                 (- current-idx 1) (- len 1))
                          :else current-idx)]
        (.focus (aget items next-idx))))))

;; ── Open ────────────────────────────────────────────────────────────

(defn open-context-menu
  "Open a context menu at the cursor position.
   event  - the contextmenu MouseEvent
   items  - (optional) JS array of item objects. If nil, reads from
            the trigger element's data-context-menu attribute."
  [event & args]
  (dismiss!)
  (let [trigger (.-currentTarget event)
        items (let [passed (first args)]
                (if passed
                  passed
                  (let [json (.. event -currentTarget -dataset -contextMenu)]
                    (when json (js/JSON.parse json)))))
        _     (when (not items) (js/console.warn "Context menu: no items provided"))
        menu  (create-menu items)
        state (get-state)]
    (set! (.-menu state) menu)
    (position-menu! menu (.-clientX event) (.-clientY event))
    (focus-first-item! menu)
    ;; While open: no text selection anywhere (CSS on .clj-ui-menu-open,
    ;; see context_menu.css) and no further right-click — a contextmenu
    ;; anywhere only closes this menu, it neither opens another runtime
    ;; menu nor the browser's. Registered on document capture AFTER the
    ;; opening contextmenu event has passed the document, so it never
    ;; sees the event that opened us.
    (.add (.-classList js/document.documentElement) open-class)
    ;; Dismiss listeners
    (let [on-click  (fn [e]
                      (when (not (.contains menu (.-target e)))
                        ;; consume the tap: only close the menu, don't
                        ;; activate whatever is underneath
                        (.preventDefault e)
                        (.stopPropagation e)
                        (dismiss!)))
          on-ctx    (fn [e]
                      (.preventDefault e)
                      (.stopImmediatePropagation e)
                      (dismiss!))
          on-key    (fn [e]
                      (let [key (.-key e)]
                        (cond
                          ;; Escape steps back: armed row → disarm, confirm
                          ;; panel → item list, otherwise close
                          (= key "Escape")
                          (do (.preventDefault e)
                              (cond
                                (disarm!) nil
                                (.-back state) ((.-back state))
                                :else (dismiss!)))
                          (= key "ArrowDown")
                          (do (.preventDefault e) (focus-next! menu "down"))
                          (= key "ArrowUp")
                          (do (.preventDefault e) (focus-next! menu "up")))))
          on-scroll (fn [e] (when (moves-trigger? trigger e) (dismiss!)))
          on-resize (fn [_] (dismiss!))
          cleanup   (fn []
                      (.remove (.-classList js/document.documentElement) open-class)
                      (.removeEventListener js/document "click" on-click true)
                      (.removeEventListener js/document "contextmenu" on-ctx true)
                      (.removeEventListener js/document "keydown" on-key true)
                      (.removeEventListener js/window "scroll" on-scroll true)
                      (.removeEventListener js/window "resize" on-resize))]
      (.addEventListener js/document "click" on-click true)
      (.addEventListener js/document "contextmenu" on-ctx true)
      (.addEventListener js/document "keydown" on-key true)
      (.addEventListener js/window "scroll" on-scroll true)
      (.addEventListener js/window "resize" on-resize)
      (set! (.-cleanup state) cleanup))))

;; ── Attach to window ────────────────────────────────────────────────

(aset js/window "__uiContextMenu" open-context-menu)
