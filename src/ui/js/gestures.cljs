(ns gestures
  "Long-press gesture runtime — makes context menus reachable on touch.

   iOS Safari never fires `contextmenu` from a long-press (Android
   does), so right-click menus are otherwise unreachable on iPhone.
   This module installs a document-level long-press recognizer
   (Pointer Events, touch pointers only) that dispatches a synthetic
   `contextmenu` MouseEvent on the pressed element after 500ms — so
   every existing contextmenu wiring (ui.context-menu triggers,
   app-level :on-context-menu handlers) works on touch unchanged.

   Opt-in surface — elements matching:
     .context-menu-trigger   (the ui.context-menu wrapper — automatic)
     [data-long-press]       (any element that opts in)

   Behaviors:
   - pointer movement beyond 10px or release before 500ms cancels
   - a native contextmenu during the press (Android fires it from
     long-press itself) cancels the synthetic one — no double-open
   - the click that follows a fired long-press is suppressed, so the
     press doesn't also activate the element underneath
   - the pressed element gets a `.clj-ui-pressing` class while the
     press is pending (CSS scales it down slightly for feedback);
     removed on fire/cancel/release
   - pair with ui/context_menu.css: `.clj-ui-touch` disables the iOS
     press callout / text selection on the opt-in surface

   Imperative API: window.__uiLongPress(el, x, y) dispatches the same
   synthetic contextmenu on el at viewport coords x/y.")

(def ^:private press-ms 500)
(def ^:private slop-px 10)
(def ^:private selector ".context-menu-trigger, [data-long-press]")
(def ^:private press-class "clj-ui-pressing")

;; {:el .. :x .. :y .. :timer ..} while a press is pending, else nil
(def ^:private press (atom nil))
(def ^:private suppress-click? (atom false))

(defn- clear-press-visual! [el]
  (when el
    (.remove (.-classList el) press-class)))

(defn- cancel! []
  (when-let [p @press]
    (js/clearTimeout (:timer p))
    (clear-press-visual! (:el p))
    (reset! press nil)))

(defn- dispatch-contextmenu!
  "Dispatch a synthetic contextmenu MouseEvent on el at viewport x/y."
  [el x y]
  (.dispatchEvent el (js/MouseEvent. "contextmenu"
                                     {:bubbles true
                                      :cancelable true
                                      :view js/window
                                      :clientX x
                                      :clientY y})))

(defn- on-pointerdown [e]
  (reset! suppress-click? false)
  (when (= (.-pointerType e) "touch")
    (when-let [el (some-> (.-target e) (.closest selector))]
      (cancel!)
      (let [x (.-clientX e)
            y (.-clientY e)]
        (.add (.-classList el) press-class)
        (reset! press
                {:el el :x x :y y
                 :timer (js/setTimeout
                         (fn []
                           (reset! press nil)
                           (clear-press-visual! el)
                           (reset! suppress-click? true)
                           (dispatch-contextmenu! el x y))
                         press-ms)})))))

(defn- on-pointermove [e]
  (when-let [p @press]
    (when (or (> (js/Math.abs (- (.-clientX e) (:x p))) slop-px)
              (> (js/Math.abs (- (.-clientY e) (:y p))) slop-px))
      (cancel!))))

(defn- on-pointer-end [_e]
  (cancel!))

(defn- on-native-contextmenu [e]
  ;; Android long-press fires contextmenu natively — drop the pending
  ;; synthetic one so the menu doesn't open twice. Synthetic events
  ;; have isTrusted=false and must pass through untouched.
  (when (.-isTrusted e)
    (cancel!)))

(defn- on-click-capture [e]
  (when @suppress-click?
    (reset! suppress-click? false)
    (.preventDefault e)
    (.stopPropagation e)))

(.addEventListener js/document "pointerdown" on-pointerdown true)
(.addEventListener js/document "pointermove" on-pointermove true)
(.addEventListener js/document "pointerup" on-pointer-end true)
(.addEventListener js/document "pointercancel" on-pointer-end true)
(.addEventListener js/document "contextmenu" on-native-contextmenu true)
(.addEventListener js/document "click" on-click-capture true)

(aset js/window "__uiLongPress" dispatch-contextmenu!)
