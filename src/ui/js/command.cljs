(ns command
  "Runtime for the command palette. Compiled from squint, loaded as a
   <script> tag in the hiccup target and reused by squint/replicant via
   window.__uiCommand.

   Operates on the static <dialog class=\"command-dialog\"> markup via
   document-level event delegation: live-filters items on input, moves the
   active highlight with the arrow keys, selects on Enter, closes on backdrop
   click, and opens on a global hotkey (data-command-hotkey).")

;; ── Helpers ─────────────────────────────────────────────────────────

(defn- items [dialog]
  (js/Array.from (.querySelectorAll dialog ".command-item")))

(defn- item-order [el]
  (js/Number (or (.. el -style -order) 0)))

(defn- visible-items
  "Visible, enabled items in *visual* order. filter! ranks matches within
   each flex-column group via `style.order` AND, while searching, orders the
   groups themselves via `style.order` on the .command-group, so DOM order
   alone is wrong while a query is active — walk groups in their flex order,
   then items within each group in theirs (stable sorts, so equal ranks keep
   DOM order) to keep keyboard nav aligned with what the user sees."
  [dialog]
  (let [vis (.filter (items dialog)
                     (fn [el]
                       (and (not (.-hidden el))
                            (not (.-disabled el)))))
        container (fn [el] (or (.closest el ".command-group") dialog))
        containers #js []]
    (.forEach vis (fn [el]
                    (let [c (container el)]
                      (when-not (.includes containers c)
                        (.push containers c)))))
    (.sort containers (fn [a b] (- (item-order a) (item-order b))))
    (.flatMap containers
              (fn [c]
                (.sort (.filter vis (fn [el] (identical? c (container el))))
                       (fn [a b] (- (item-order a) (item-order b))))))))

(defn- item-text [el]
  (let [v (.. el -dataset -commandValue)]
    (.toLowerCase (or v (.-textContent el) ""))))

(defn- item-label [el]
  (let [lbl (.querySelector el ".command-item-label")]
    (.toLowerCase (or (and lbl (.-textContent lbl)) (.-textContent el) ""))))

(defn- match-score
  "Rank a matching item for query q — lower is better. Matches on the visible
   label beat matches that only hit the hidden search value (:value keywords,
   project paths, …): 0 exact label · 1 label prefix · 2 label word-start ·
   3 label substring · 4 value-only."
  [el q]
  (let [label (item-label el)]
    (cond
      (= label q)           0
      (.startsWith label q) 1
      (.some (.split label #"[^a-z0-9]+")
             (fn [w] (.startsWith w q))) 2
      (.includes label q)   3
      :else                 4)))

;; ── Active highlight ────────────────────────────────────────────────

(defn- active-item [dialog]
  (.querySelector dialog ".command-item--active"))

(defn- set-active!
  ([dialog el] (set-active! dialog el true))
  ([dialog el scroll?]
   (let [prev (active-item dialog)]
     (when prev (.remove (.-classList prev) "command-item--active")))
   (when el
     (.add (.-classList el) "command-item--active")
     (when (and scroll? (.-scrollIntoView el))
       (.scrollIntoView el #js {:block "nearest"})))))

(defn- move-active! [dialog dir]
  (let [vis (visible-items dialog)
        len (.-length vis)]
    (when (> len 0)
      (let [cur    (active-item dialog)
            idx    (.indexOf vis cur)
            next-i (cond
                     (= dir "down") (if (< idx (- len 1)) (+ idx 1) 0)
                     (= dir "up")   (if (> idx 0) (- idx 1) (- len 1))
                     (= dir "home") 0
                     (= dir "end")  (- len 1)
                     :else idx)]
        (set-active! dialog (aget vis next-i))))))

(defn- move-group!
  "Jump the active highlight to the first visible item of the next/previous
   .command-group (ungrouped items count as one dialog-level group)."
  [dialog dir]
  (let [vis (visible-items dialog)
        len (.-length vis)]
    (when (> len 0)
      (let [group-of (fn [el] (or (and el (.closest el ".command-group")) dialog))
            groups   #js []]
        (.forEach vis (fn [el]
                        (let [g (group-of el)]
                          (when-not (.includes groups g)
                            (.push groups g)))))
        (let [glen   (.-length groups)
              cur    (active-item dialog)
              gidx   (.indexOf groups (group-of cur))
              next-g (if (= dir "down")
                       (if (< gidx (- glen 1)) (+ gidx 1) 0)
                       (if (> gidx 0) (- gidx 1) (- glen 1)))
              target (aget groups next-g)]
          (set-active! dialog
                       (.find vis (fn [el] (identical? (group-of el) target)))))))))

;; ── Filtering ───────────────────────────────────────────────────────

(defn- filter! [dialog query]
  (let [q     (.toLowerCase (.trim (or query "")))
        list  (.querySelector dialog ".command-list")
        searching? (not= q "")]
    ;; Show / hide individual items.
    (.forEach (items dialog)
              (fn [el]
                (let [match (or (not searching?) (.includes (item-text el) q))]
                  (set! (.-hidden el) (not match))
                  ;; Rank matches by quality via flex `order` (the group item
                  ;; container is a flex column) so better matches float to
                  ;; the top of their group without touching DOM order.
                  (if (or (not searching?) (not match))
                    (.removeProperty (.-style el) "order")
                    (set! (.. el -style -order) (match-score el q))))))
    ;; While searching, collapse the group structure into one flat ranked list
    ;; (see command.css): headings hide and each group is ordered by its best
    ;; child score, so the strongest match floats to the top regardless of
    ;; which group emitted it (a value-only session match no longer buries a
    ;; label-prefix project/command match in a later group).
    (if searching?
      (.add (.-classList list) "command-list--searching")
      (.remove (.-classList list) "command-list--searching"))
    ;; Hide groups whose items are all filtered out; rank the survivors.
    (.forEach (js/Array.from (.querySelectorAll dialog ".command-group"))
              (fn [grp]
                (let [visible (.filter (js/Array.from (.querySelectorAll grp ".command-item"))
                                       (fn [el] (not (.-hidden el))))]
                  (set! (.-hidden grp) (= (.-length visible) 0))
                  (if (and searching? (> (.-length visible) 0))
                    (set! (.. grp -style -order)
                          (.reduce visible
                                   (fn [best el] (js/Math.min best (item-order el)))
                                   js/Infinity))
                    (.removeProperty (.-style grp) "order")))))
    ;; Empty state + reset active to the first visible item.
    (let [vis (visible-items dialog)]
      (if (= (.-length vis) 0)
        (.add (.-classList list) "command-list--empty")
        (.remove (.-classList list) "command-list--empty"))
      (set-active! dialog (when (> (.-length vis) 0) (aget vis 0))))))

;; ── Open / close ────────────────────────────────────────────────────

(defn- find-dialog [id]
  (when id (.getElementById js/document id)))

;; Items may be populated asynchronously (e.g. a list fetched after the dialog
;; opens, or a framework re-render that swaps in a sub-page). filter! only runs
;; on open and on input, so those late items would keep a stale
;; "command-list--empty" state — showing "No results found." over real rows.
;; Watch the open dialog's list and re-run filter! with the current query when
;; its contents change. class/attribute writes from filter! itself don't
;; retrigger it (we observe childList only). The observer is stored on the
;; dialog element and replaced on the next open, so at most one runs per dialog.
(defn- observe-list! [dialog]
  (when-let [prev (aget dialog "__cmdListObs")]
    (.disconnect prev))
  (let [list (.querySelector dialog ".command-list")]
    (when list
      (let [obs (js/MutationObserver.
                 (fn [_ _]
                   (let [input (.querySelector dialog ".command-input")]
                     (filter! dialog (if input (.-value input) "")))))]
        (.observe obs list #js {:childList true :subtree true})
        (aset dialog "__cmdListObs" obs)))))

;; ── Keyboard-aware sizing ───────────────────────────────────────────
;; iOS Safari does not shrink the layout viewport when the on-screen keyboard
;; opens, so a vh-sized centered <dialog> keeps its full height and its lower
;; half — including the scrollable list — hides behind the keyboard (and the
;; off-screen scroll area can't be reached). While the keyboard is up we pin the
;; dialog into the visible band above it via --command-top / --command-max-h,
;; read from visualViewport. When it's down we clear them so the CSS defaults
;; (centered 12vh / 60vh) apply.

(defn- viewport [] (.-visualViewport js/window))

(defn- clear-viewport! [dialog]
  (let [s (.-style dialog)]
    (.removeProperty s "--command-top")
    (.removeProperty s "--command-max-h")))

(defn- sync-viewport! [dialog]
  (let [vv (viewport)]
    (when (and dialog vv)
      (let [h   (.-height vv)
            off (.-offsetTop vv)
            kb  (- (.-innerHeight js/window) h)]
        ;; Fire when the keyboard eats space (kb) OR when the visual viewport is
        ;; displaced downward (off). The latter matters on iOS when the page
        ;; behind the dialog is scroll-locked via `body{position:fixed}`, which
        ;; collapses `innerHeight` to `visualViewport.height` (kb reads 0) even
        ;; though the keyboard is up and the viewport is offset.
        (if (or (> kb 120) (> off 1))
          (let [top-gap (js/Math.max 12 (js/Math.round (* h 0.08)))
                bot-gap (js/Math.max 12 (js/Math.round (* h 0.06)))
                max-h   (js/Math.max 160 (- h top-gap bot-gap))
                s       (.-style dialog)]
            (.setProperty s "--command-top" (str (+ off top-gap) "px"))
            (.setProperty s "--command-max-h" (str max-h "px")))
          (clear-viewport! dialog))))))

(defn open [id]
  (let [dialog (find-dialog id)]
    (when (and dialog (not (.-open dialog)))
      (.showModal dialog)
      (let [input (.querySelector dialog ".command-input")]
        (when input
          (set! (.-value input) "")
          (.focus input)))
      (filter! dialog "")
      (observe-list! dialog)
      (sync-viewport! dialog))))

(defn close [id]
  (let [dialog (find-dialog id)]
    (when (and dialog (.-open dialog)) (.close dialog))))

(defn toggle [id]
  (let [dialog (find-dialog id)]
    (when dialog
      (if (.-open dialog) (.close dialog) (open id)))))

;; ── Selection ───────────────────────────────────────────────────────

(defn- select! [dialog el]
  (when (and el (not (.-disabled el)))
    (.close dialog)
    ;; Let hrefs / framework click handlers fire.
    (when (not (.-href el)) (.click el))
    (when (.-href el) (set! (.-location js/window) (.-href el)))))

;; ── Global hotkey ───────────────────────────────────────────────────

(defn- hotkey-match? [spec e]
  (let [ekey (.-key e)]
    (when ekey
      (let [parts  (.split (.toLowerCase spec) "+")
            key    (aget parts (- (.-length parts) 1))
            mod?   (.includes spec "mod")
            shift? (.includes spec "shift")
            alt?   (.includes spec "alt")]
        (and (= (.toLowerCase ekey) key)
             (= mod? (or (.-metaKey e) (.-ctrlKey e)))
             (= shift? (.-shiftKey e))
             (= alt? (.-altKey e)))))))

(defn- on-global-key [e]
  (let [dialogs (js/Array.from (.querySelectorAll js/document ".command-dialog[data-command-hotkey]"))]
    (.forEach dialogs
              (fn [dialog]
                (let [spec (.. dialog -dataset -commandHotkey)]
                  (when (and spec (hotkey-match? spec e))
                    (.preventDefault e)
                    (toggle (.-id dialog))))))))

;; ── Delegated dialog behaviour ──────────────────────────────────────

(defn- open-dialog []
  (.querySelector js/document ".command-dialog[open]"))

(defn- on-input [e]
  (let [t (.-target e)]
    (when (and (.-classList t) (.contains (.-classList t) "command-input"))
      (let [dialog (.closest t ".command-dialog")]
        (when dialog (filter! dialog (.-value t)))))))

(defn- on-keydown [e]
  (let [dialog (open-dialog)]
    (when dialog
      (let [key (.-key e)]
        (cond
          (= key "ArrowDown") (do (.preventDefault e) (move-active! dialog "down"))
          (= key "ArrowUp")   (do (.preventDefault e) (move-active! dialog "up"))
          ;; Emacs-style Ctrl+n / Ctrl+p to move next / previous. Ctrl+N is a
          ;; browser-reserved shortcut (new window) that page JS can't block in a
          ;; normal tab — it only works in a standalone PWA. Ctrl+j / Ctrl+k are
          ;; not reserved, so they work everywhere as the reliable next/prev pair.
          (and (= key "n") (.-ctrlKey e)) (do (.preventDefault e) (move-active! dialog "down"))
          (and (= key "p") (.-ctrlKey e)) (do (.preventDefault e) (move-active! dialog "up"))
          (and (= key "j") (.-ctrlKey e)) (do (.preventDefault e) (move-active! dialog "down"))
          (and (= key "k") (.-ctrlKey e)) (do (.preventDefault e) (move-active! dialog "up"))
          ;; Alt-modified pairs as well — Alt+letter is never browser-reserved,
          ;; so Alt+j/k and Alt+n/p work in any normal tab. Alt+Shift+j/k
          ;; jumps by section (first item of the next/previous group); with
          ;; Shift held the key reports uppercase, so match both cases.
          (and (or (= key "J") (= key "j")) (.-altKey e) (.-shiftKey e))
          (do (.preventDefault e) (move-group! dialog "down"))
          (and (or (= key "K") (= key "k")) (.-altKey e) (.-shiftKey e))
          (do (.preventDefault e) (move-group! dialog "up"))
          (and (= key "j") (.-altKey e)) (do (.preventDefault e) (move-active! dialog "down"))
          (and (= key "k") (.-altKey e)) (do (.preventDefault e) (move-active! dialog "up"))
          (and (= key "n") (.-altKey e)) (do (.preventDefault e) (move-active! dialog "down"))
          (and (= key "p") (.-altKey e)) (do (.preventDefault e) (move-active! dialog "up"))
          (and (= key "Home") (.-metaKey e)) (do (.preventDefault e) (move-active! dialog "home"))
          (and (= key "End")  (.-metaKey e)) (do (.preventDefault e) (move-active! dialog "end"))
          (= key "Enter")     (do (.preventDefault e) (select! dialog (active-item dialog))))))))

(defn- on-click [e]
  (let [t      (.-target e)
        dialog (when (.-closest t) (.closest t ".command-dialog"))]
    (when dialog
      (let [item (.closest t ".command-item")]
        (cond
          ;; Backdrop click (target is the dialog element itself) → close.
          (identical? t dialog) (.close dialog)
          ;; Item click → close after the native/framework handler.
          item (when (not (.-disabled item)) (.close dialog)))))))

(defn- on-pointermove [e]
  ;; Touch scrolling on iOS emits pointermove; activating the item under the
  ;; finger (and scrolling it into view) fights the scroll and glues the
  ;; highlight to the finger. Hover-activation is a mouse affordance only, so
  ;; skip touch pointers and never scroll on pointer-driven activation.
  (when (not= (.-pointerType e) "touch")
    (let [t (.-target e)]
      (when (.-closest t)
        (let [item (.closest t ".command-item")]
          (when (and item (not (.-hidden item)) (not (.-disabled item)))
            (let [dialog (.closest item ".command-dialog")]
              (when dialog (set-active! dialog item false)))))))))

;; ── Init ────────────────────────────────────────────────────────────

(defn- on-viewport-change []
  (when-let [dialog (open-dialog)]
    (sync-viewport! dialog)))

(defn- on-dialog-close [e]
  (let [t (.-target e)]
    (when (and t (.-classList t) (.contains (.-classList t) "command-dialog"))
      (clear-viewport! t))))

(defn init! []
  (.addEventListener js/document "input" on-input true)
  (.addEventListener js/document "keydown" on-keydown true)
  (.addEventListener js/document "keydown" on-global-key)
  (.addEventListener js/document "click" on-click)
  (.addEventListener js/document "pointermove" on-pointermove true)
  ;; Native <dialog> "close" doesn't bubble — capture it to reset keyboard sizing.
  (.addEventListener js/document "close" on-dialog-close true)
  (when-let [vv (viewport)]
    (.addEventListener vv "resize" on-viewport-change)
    (.addEventListener vv "scroll" on-viewport-change)))

(init!)

(aset js/window "__uiCommand" #js {:open open :close close :toggle toggle})
