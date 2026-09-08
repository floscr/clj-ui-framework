(ns ui.js.context-menu
  "Tiny JS runtime for context menus. Compiled from squint, loaded as
   a <script> tag in the hiccup target. Also used by squint/replicant
   targets via window.__uiContextMenu.

   Creates floating menu DOM at cursor position, handles dismiss on
   click-outside / Escape / scroll, navigates on item click.")

;; ── State (stored on window to avoid module scope issues) ───────────

(defn- get-state []
  (or (aget js/window "__uiCtxState")
      (let [s #js {:menu nil :cleanup nil}]
        (aset js/window "__uiCtxState" s)
        s)))

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

;; ── Dismiss ─────────────────────────────────────────────────────────

(defn- dismiss! []
  (let [state (get-state)]
    (when (.-menu state)
      (.remove (.-menu state))
      (set! (.-menu state) nil)
      ;; let ui/js/gestures release the held press visual in sync
      (.dispatchEvent js/document (js/CustomEvent. "clj-ui-menu-dismiss")))
    (when (.-cleanup state)
      ((.-cleanup state))
      (set! (.-cleanup state) nil))))

;; ── Confirm View ────────────────────────────────────────────────────

(defn- execute-item! [item]
  (let [on-click (aget item "on-click")
        url      (aget item "url")]
    (if on-click
      (on-click)
      (when url
        (set! (.-location js/window) url)))))

(defn- show-confirm! [menu item]
  (let [confirm-val (aget item "confirm")
        message     (if (= confirm-val true) "Are you sure?" confirm-val)
        ;; Clear existing content
        _           (set! (.-innerHTML menu) "")
        ;; Message
        msg-el      (.createElement js/document "div")]
    (set! (.-className msg-el) "context-menu-confirm-message")
    (set! (.-textContent msg-el) message)
    (.appendChild menu msg-el)
    ;; Buttons
    (let [actions (.createElement js/document "div")]
      (set! (.-className actions) "context-menu-confirm-actions")
      ;; Cancel
      (let [cancel-btn (.createElement js/document "button")]
        (set! (.-className cancel-btn) "context-menu-item")
        (set! (.-textContent cancel-btn) "Cancel")
        (.setAttribute cancel-btn "tabindex" "-1")
        (.addEventListener cancel-btn "click"
          (fn [e]
            (.preventDefault e)
            (.stopPropagation e)
            (dismiss!)))
        (.appendChild actions cancel-btn))
      ;; Confirm
      (let [danger      (= (aget item "variant") "danger")
            confirm-btn (.createElement js/document "button")]
        (set! (.-className confirm-btn)
              (if danger
                "context-menu-item context-menu-item--danger"
                "context-menu-item"))
        (set! (.-textContent confirm-btn) "Confirm")
        (.setAttribute confirm-btn "tabindex" "-1")
        (.addEventListener confirm-btn "click"
          (fn [e]
            (.preventDefault e)
            (.stopPropagation e)
            (dismiss!)
            (execute-item! item)))
        (.appendChild actions confirm-btn))
      (.appendChild menu actions))
    ;; Focus the cancel button
    (let [cancel (.querySelector menu ".context-menu-item")]
      (when cancel (.focus cancel)))))

;; ── Menu DOM Creation ───────────────────────────────────────────────

(defn- create-menu [items]
  (let [menu (.createElement js/document "div")]
    (set! (.-className menu) "context-menu")
    (.setAttribute menu "role" "menu")
    (.forEach items
      (fn [item]
        (if (= (aget item "type") "separator")
          ;; Separator
          (let [sep (.createElement js/document "div")]
            (set! (.-className sep) "context-menu-separator")
            (.setAttribute sep "role" "separator")
            (.appendChild menu sep))
          ;; Item
          (let [url    (aget item "url")
                tag    (if url "a" "button")
                el     (.createElement js/document tag)
                danger (= (aget item "variant") "danger")]
            (set! (.-className el)
                  (if danger "context-menu-item context-menu-item--danger" "context-menu-item"))
            (.setAttribute el "role" "menuitem")
            (.setAttribute el "tabindex" "-1")
            (when url (.setAttribute el "href" url))
            ;; Icon
            (let [paths (aget item "icon-paths")]
              (when (and paths (> (.-length paths) 0))
                (.appendChild el (create-icon paths))))
            ;; Label
            (let [span (.createElement js/document "span")]
              (set! (.-textContent span) (aget item "label"))
              (.appendChild el span))
            ;; Click handler
            (.addEventListener el "click"
              (fn [e]
                (.preventDefault e)
                (.stopPropagation e)
                (if (aget item "confirm")
                  (show-confirm! (.closest el ".context-menu") item)
                  (do (dismiss!)
                      (execute-item! item)))))
            (.appendChild menu el)))))
    menu))

;; ── Positioning ─────────────────────────────────────────────────────

(defn- position-menu! [menu x y]
  (set! (.. menu -style -left) (str x "px"))
  (set! (.. menu -style -top)  (str y "px"))
  ;; Append first so we can measure
  (.appendChild js/document.body menu)
  ;; Clamp to viewport
  (let [rect  (.getBoundingClientRect menu)
        vw    (.-innerWidth js/window)
        vh    (.-innerHeight js/window)
        new-x (if (> (+ x (.-width rect)) vw)
                (- vw (.-width rect) 8)
                x)
        new-y (if (> (+ y (.-height rect)) vh)
                (- vh (.-height rect) 8)
                y)]
    (set! (.. menu -style -left) (str new-x "px"))
    (set! (.. menu -style -top)  (str new-y "px"))))

;; ── Keyboard Navigation ─────────────────────────────────────────────

(defn- focus-first-item! [menu]
  (let [first-item (.querySelector menu ".context-menu-item")]
    (when first-item (.focus first-item))))

(defn- focus-next! [menu direction]
  (let [items  (.querySelectorAll menu ".context-menu-item")
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
  (let [items (let [passed (first args)]
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
    ;; Dismiss listeners
    (let [on-click  (fn [e]
                      (when (not (.contains menu (.-target e)))
                        ;; consume the tap: only close the menu, don't
                        ;; activate whatever is underneath
                        (.preventDefault e)
                        (.stopPropagation e)
                        (dismiss!)))
          on-key    (fn [e]
                      (let [key (.-key e)]
                        (cond
                          (= key "Escape")
                          (do (.preventDefault e) (dismiss!))
                          (= key "ArrowDown")
                          (do (.preventDefault e) (focus-next! menu "down"))
                          (= key "ArrowUp")
                          (do (.preventDefault e) (focus-next! menu "up")))))
          on-scroll (fn [_] (dismiss!))
          on-resize (fn [_] (dismiss!))
          cleanup   (fn []
                      (.removeEventListener js/document "click" on-click true)
                      (.removeEventListener js/document "keydown" on-key true)
                      (.removeEventListener js/window "scroll" on-scroll true)
                      (.removeEventListener js/window "resize" on-resize))]
      (.addEventListener js/document "click" on-click true)
      (.addEventListener js/document "keydown" on-key true)
      (.addEventListener js/window "scroll" on-scroll true)
      (.addEventListener js/window "resize" on-resize)
      (set! (.-cleanup state) cleanup))))

;; ── Attach to window ────────────────────────────────────────────────

(aset js/window "__uiContextMenu" open-context-menu)
