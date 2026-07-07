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

(defn- visible-items [dialog]
  (.filter (items dialog)
           (fn [el]
             (and (not (.-hidden el))
                  (not (.-disabled el))))))

(defn- item-text [el]
  (let [v (.. el -dataset -commandValue)]
    (.toLowerCase (or v (.-textContent el) ""))))

;; ── Active highlight ────────────────────────────────────────────────

(defn- active-item [dialog]
  (.querySelector dialog ".command-item--active"))

(defn- set-active! [dialog el]
  (let [prev (active-item dialog)]
    (when prev (.remove (.-classList prev) "command-item--active")))
  (when el
    (.add (.-classList el) "command-item--active")
    (when (.-scrollIntoView el)
      (.scrollIntoView el #js {:block "nearest"}))))

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

;; ── Filtering ───────────────────────────────────────────────────────

(defn- filter! [dialog query]
  (let [q     (.toLowerCase (.trim (or query "")))
        list  (.querySelector dialog ".command-list")]
    ;; Show / hide individual items.
    (.forEach (items dialog)
              (fn [el]
                (let [match (or (= q "") (.includes (item-text el) q))]
                  (set! (.-hidden el) (not match)))))
    ;; Hide groups whose items are all filtered out.
    (.forEach (js/Array.from (.querySelectorAll dialog ".command-group"))
              (fn [grp]
                (let [any (.some (js/Array.from (.querySelectorAll grp ".command-item"))
                                 (fn [el] (not (.-hidden el))))]
                  (set! (.-hidden grp) (not any)))))
    ;; Empty state + reset active to the first visible item.
    (let [vis (visible-items dialog)]
      (if (= (.-length vis) 0)
        (.add (.-classList list) "command-list--empty")
        (.remove (.-classList list) "command-list--empty"))
      (set-active! dialog (when (> (.-length vis) 0) (aget vis 0))))))

;; ── Open / close ────────────────────────────────────────────────────

(defn- find-dialog [id]
  (when id (.getElementById js/document id)))

(defn open [id]
  (let [dialog (find-dialog id)]
    (when (and dialog (not (.-open dialog)))
      (.showModal dialog)
      (let [input (.querySelector dialog ".command-input")]
        (when input
          (set! (.-value input) "")
          (.focus input)))
      (filter! dialog ""))))

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
  (let [t (.-target e)]
    (when (.-closest t)
      (let [item (.closest t ".command-item")]
        (when (and item (not (.-hidden item)) (not (.-disabled item)))
          (let [dialog (.closest item ".command-dialog")]
            (when dialog (set-active! dialog item))))))))

;; ── Init ────────────────────────────────────────────────────────────

(defn init! []
  (.addEventListener js/document "input" on-input true)
  (.addEventListener js/document "keydown" on-keydown true)
  (.addEventListener js/document "keydown" on-global-key)
  (.addEventListener js/document "click" on-click)
  (.addEventListener js/document "pointermove" on-pointermove true))

(init!)

(aset js/window "__uiCommand" #js {:open open :close close :toggle toggle})
