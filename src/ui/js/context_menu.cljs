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
                   :armed nil :back nil :swap nil
                   :anchor nil :placedX nil :placedY nil}]
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
    (set! (.-swap state) nil)
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

;; ── Building blocks ─────────────────────────────────────────────────

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
(def ^:private chevron-right #js [#js ["path" #js [#js ["d" "m9 18 6-6-6-6"]]]])

(defn- make-head
  "Header of a drilled page (sub-page, confirm panel): back button + `label`."
  [label on-back]
  (let [head (make-el "div" "context-menu-panel-head")
        back (make-button "context-menu-panel-back context-menu-action" nil on-back)
        span (make-el "span" "context-menu-panel-label")]
    (.setAttribute back "aria-label" "Back")
    (.appendChild back (create-icon chevron-left))
    (.appendChild head back)
    (set! (.-textContent span) label)
    (.appendChild head span)
    head))

;; ── Positioning ─────────────────────────────────────────────────────
;; The menu opens at its anchor (the pointer) and is pulled back on-screen.
;; A page change places it from the anchor again: a taller page moves it up,
;; coming back to a shorter one moves it down where it opened.

(defn- clamp-to-viewport!
  "Keep `menu` fully on-screen: if it overflows the right or bottom viewport
   edge, pull it back (never past the 8px top/left margin). Measures via
   style left/top + offsetWidth/Height — NOT getBoundingClientRect — so the
   open animation's transform scale can't skew the numbers."
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

(defn- place!
  "Put `menu` at its anchor, pulled back on-screen. A position set from
   outside since the last placing (the app moved the menu, or a re-clamp
   after it grew) becomes the new anchor."
  [menu]
  (let [state (get-state)
        style (.-style menu)]
    (when (or (not= (.-left style) (.-placedX state))
              (not= (.-top style) (.-placedY state)))
      (set! (.-anchor state) #js {:x (.-left style) :y (.-top style)}))
    (set! (.-left style) (.. state -anchor -x))
    (set! (.-top style) (.. state -anchor -y))
    (clamp-to-viewport! menu)
    (set! (.-placedX state) (.-left style))
    (set! (.-placedY state) (.-top style))))

(defn- position-menu! [menu x y]
  (let [state (get-state)
        style (.-style menu)]
    (set! (.-left style) (str x "px"))
    (set! (.-top style) (str y "px"))
    (set! (.-anchor state) #js {:x (.-left style) :y (.-top style)})
    (set! (.-placedX state) (.-left style))
    (set! (.-placedY state) (.-top style))
    ;; Append first so we can measure
    (.appendChild js/document.body menu)
    (place! menu)
    ;; Re-clamp when the menu's contents change after open — apps may inject
    ;; extra DOM (action bars, search inputs) post-positioning, which would
    ;; otherwise push the menu past the bottom edge. MutationObserver fires as
    ;; a microtask (childList only, so our own style writes don't retrigger).
    ;; A page change places the menu itself; mid-slide the measured box is
    ;; the animated one, so the observer keeps out of it.
    (let [mo (js/MutationObserver.
              (fn [_] (when (not (.-swap (get-state))) (clamp-to-viewport! menu))))]
      (.observe mo menu #js {:childList true :subtree true})
      (set! (.-growthObserver state) mo))))

;; ── Pages ───────────────────────────────────────────────────────────
;; The menu shows one .context-menu-page at a time: the item list, the
;; sub-page of an item with :items, or a confirm panel. Drilling in slides
;; the new page in from the right over the old one, which drifts left and
;; fades; Back is the mirror image. Meanwhile the menu's box animates to
;; the new page's size and position. Reduced motion swaps instantly.

;; matches --duration-standard / --ease-standard (WAAPI can't read tokens)
(def ^:private swap-ms 240)
(def ^:private swap-easing "cubic-bezier(0.2, 0, 0, 1)")

(defn- reduced-motion? []
  (.-matches (js/window.matchMedia "(prefers-reduced-motion: reduce)")))

(defn- finish-swap!
  "Cut a running page change short: drop the old page, release the box."
  []
  (let [state (get-state)
        swap  (.-swap state)]
    (when swap
      (set! (.-swap state) nil)
      (.forEach (.-anims swap) (fn [a] (.cancel a)))
      (.remove (.-ghost swap))
      (set! (.. swap -menu -style -overflow) ""))))

(defn- px [n] (str n "px"))

(defn- slide!
  "Animate from the box (`h0` `w0` `x0` `y0`) and the outgoing page `old`
   to the menu's current box and `page`. `old` stays in the menu, inert and
   pinned where it was, until the slide ends."
  [menu page old forward? h0 w0 x0 y0]
  (let [state (get-state)
        style (.-style menu)
        opts  #js {:duration swap-ms :easing swap-easing}
        box   (.animate menu #js {:height #js [(px h0) (px (.-offsetHeight menu))]
                                  :width  #js [(px w0) (px (.-offsetWidth menu))]
                                  :left   #js [x0 (.-left style)]
                                  :top    #js [y0 (.-top style)]}
                        opts)
        in    (.animate page (if forward?
                               #js {:transform #js ["translateX(100%)" "translateX(0)"]}
                               #js {:transform #js ["translateX(-30%)" "translateX(0)"]
                                    :opacity   #js [0 1]})
                        opts)
        out   (.animate old (if forward?
                              #js {:transform #js ["translateX(0)" "translateX(-30%)"]
                                   :opacity   #js [1 0]}
                              #js {:transform #js ["translateX(0)" "translateX(100%)"]})
                        opts)
        swap  #js {:menu menu :ghost old :anims #js [box in out]}]
    ;; the pinned old page must not make the shrinking box scroll
    (set! (.-overflow style) "hidden")
    (set! (.-swap state) swap)
    (set! (.-onfinish box) (fn [] (when (= (.-swap state) swap) (finish-swap!))))))

(defn- show-page!
  "Make `page` the menu's page. `direction` \"forward\" or \"back\" slides
   from the current one, nil swaps in place. The page may carry .focusEl
   (focused after the swap), .onBack (Back / Escape) and .scrollTop0 (the
   scroll to restore, coming back to a long list)."
  [menu page direction]
  (finish-swap!)
  (disarm!)
  (let [state  (get-state)
        style  (.-style menu)
        old    (.querySelector menu ":scope > .context-menu-page")
        h0     (.-offsetHeight menu)
        w0     (.-offsetWidth menu)
        x0     (.-left style)
        y0     (.-top style)
        slide? (and direction old (.-isConnected menu) (.-animate menu)
                    (not (reduced-motion?)))
        root?  (.contains (.-classList page) "context-menu-page--root")]
    (if slide?
      ;; the old page stays for the slide, pinned over the scrolled spot;
      ;; going back it slides off above the incoming one
      (let [s (.-style old)]
        (.setAttribute old "inert" "")
        (set! (.-position s) "absolute")
        (set! (.-left s) "0")
        (set! (.-top s) (px (- (.-scrollTop menu))))
        (set! (.-width s) (px (.-clientWidth menu)))
        (set! (.-zIndex s) (if (= direction "back") "1" "0")))
      (when old (.remove old)))
    (.toggle (.-classList menu) "context-menu--panel"
             (.contains (.-classList page) "context-menu-panel"))
    ;; a drilled page never narrows the menu under the finger
    (set! (.-minWidth style) (if root? "" (px w0)))
    (.appendChild menu page)
    (set! (.-scrollTop menu) (or (.-scrollTop0 page) 0))
    (set! (.-back state) (.-onBack page))
    (when (.-isConnected menu) (place! menu))
    (when slide? (slide! menu page old (= direction "forward") h0 w0 x0 y0))
    (let [f (.-focusEl page)]
      (when f (.focus f #js {:preventScroll true})))))

;; ── Confirm: panel (:confirm-style :panel) ──────────────────────────
;; The menu drills into a confirm page: back header, what is affected, a
;; full-width action button and Cancel. Back, Cancel and Escape return to
;; the list instead of closing the menu.

(defn- confirm-page
  "The confirm page of `item`; `on-back` returns to the list."
  [item on-back]
  (let [label   (aget item "label")
        confirm (aget item "confirm")
        title   (aget item "confirm-title")
        message (if (string? confirm) confirm "This can't be undone.")
        danger  (= (aget item "variant") "danger")
        page    (make-el "div" "context-menu-page context-menu-panel")
        body    (make-el "div" "context-menu-panel-body")
        actions (make-el "div" "context-menu-panel-actions")
        run     (make-button (str "btn btn-sm context-menu-action "
                                  (if danger "btn-danger" "btn-primary"))
                             nil
                             (fn [] (dismiss!) (execute-item! item)))
        cancel  (make-button "btn btn-sm btn-ghost context-menu-action" "Cancel" on-back)]
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
    (.appendChild page (make-head label on-back))
    (.appendChild page body)
    (.appendChild page actions)
    (set! (.-onBack page) on-back)
    ;; Cancel first: a stray Enter never runs the action
    (set! (.-focusEl page) cancel)
    page))

;; ── Item pages ──────────────────────────────────────────────────────

(defn- first-item-idx [items]
  (.findIndex items (fn [item] (not= (aget item "type") "separator"))))

(defn- items-page
  "A page of `items` for `menu`. `parent` is nil for the top list, or
   #js {:label :back} for the sub-page of an item with :items: a back header
   over the rows, and Back / Escape run its :back. Focuses the row at
   `focus-idx` when shown."
  [menu items focus-idx parent]
  (let [page (make-el "div" (if parent
                               "context-menu-page context-menu-page--sub"
                               "context-menu-page context-menu-page--root"))]
    (when parent
      (.appendChild page (make-head (aget parent "label") (aget parent "back")))
      (set! (.-onBack page) (aget parent "back")))
    (.forEach items
      (fn [item idx]
        (if (= (aget item "type") "separator")
          (let [sep (make-el "div" "context-menu-separator")]
            (.setAttribute sep "role" "separator")
            (.appendChild page sep))
          (let [url  (aget item "url")
                sub  (aget item "items")
                el   (make-el (if url "a" "button")
                              (if (= (aget item "variant") "danger")
                                "context-menu-item context-menu-item--danger"
                                "context-menu-item"))
                span (make-el "span" "context-menu-item-label")
                ;; this page again, as it was: the row focused, the scroll kept
                back (fn [scroll]
                       (fn []
                         (let [p (items-page menu items idx parent)]
                           (set! (.-scrollTop0 p) scroll)
                           (show-page! menu p "back"))))]
            (.setAttribute el "role" "menuitem")
            (.setAttribute el "tabindex" "-1")
            (when url (.setAttribute el "href" url))
            (let [paths (aget item "icon-paths")]
              (when (and paths (> (.-length paths) 0))
                (.appendChild el (create-icon paths))))
            (set! (.-textContent span) (aget item "label"))
            (.appendChild el span)
            (when sub
              (.setAttribute el "aria-haspopup" "menu")
              (let [chev (create-icon chevron-right)]
                (.setAttribute chev "class" "context-menu-item-chevron")
                (.appendChild el chev)))
            (.addEventListener el "click"
              (fn [e]
                (.preventDefault e)
                (.stopPropagation e)
                (cond
                  sub
                  (show-page! menu
                              (items-page menu sub (first-item-idx sub)
                                          #js {:label (aget item "label")
                                               :back  (back (.-scrollTop menu))})
                              "forward")

                  (not (aget item "confirm"))
                  (do (dismiss!) (execute-item! item))

                  (= (aget item "confirm-style") "panel")
                  (show-page! menu (confirm-page item (back (.-scrollTop menu))) "forward")

                  (.contains (.-classList el) "context-menu-item--armed")
                  (do (dismiss!) (execute-item! item))

                  :else
                  (arm! menu el span item))))
            (.appendChild page el)
            (when (= idx focus-idx) (set! (.-focusEl page) el))))))
    page))

(defn- create-menu [items]
  (let [menu (make-el "div" "context-menu")]
    (.setAttribute menu "role" "menu")
    (show-page! menu (items-page menu items nil nil) nil)
    menu))

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

;; menu items, or the buttons of a drilled page's header and confirm panel;
;; never those of a page sliding out (inert)
(def ^:private focusable
  ".context-menu-page:not([inert]) :is(.context-menu-item, .context-menu-action)")

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
                          ;; panel or sub-page → its list, otherwise close
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
