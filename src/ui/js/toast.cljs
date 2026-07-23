(ns toast
  "Toast runtime — floating, auto-dismissing notifications.

   Imperative API (all targets):

     window.__uiToast(message, {variant, duration})
       variant  - \"info\" (default), \"success\", \"warning\", \"danger\"
       duration - ms before auto-dismiss (default 5000, 0 = sticky)

   Declarative API (hiccup/HTMX target): any element with a
   [data-ui-toast] attribute — present at load or swapped in later
   (e.g. via hx-swap-oob) — is consumed and shown as a toast:

     <div data-ui-toast=\"✓ Saved\" data-variant=\"success\"></div>

   Optional attrs: data-variant, data-duration (ms).

   Toasts stack in a fixed bottom-center container that is created on
   demand. Clicking a toast dismisses it.")

(def ^:private container-id "ui-toast-container")

(defn- ensure-container! []
  (or (js/document.getElementById container-id)
      (let [el (js/document.createElement "div")]
        (set! (.-id el) container-id)
        (set! (.-className el) "toast-container")
        (.appendChild js/document.body el)
        el)))

(defn- dismiss! [el]
  (when-not (aget el "__uiToastDismissed")
    (aset el "__uiToastDismissed" true)
    (.add (.-classList el) "toast-leaving")
    (js/setTimeout (fn [] (.remove el)) 300)))

(defn show!
  "Show a toast. Returns the toast element."
  [message opts]
  (let [opts     (or opts {})
        variant  (or (aget opts "variant") "info")
        raw-dur  (aget opts "duration")
        duration (if (number? raw-dur) raw-dur 5000)
        container (ensure-container!)
        el (js/document.createElement "div")]
    (set! (.-className el) (str "toast toast-" variant))
    (set! (.-textContent el) (str message))
    (.setAttribute el "role" "status")
    (.addEventListener el "click" (fn [_] (dismiss! el)))
    (.appendChild container el)
    (when (pos? duration)
      (js/setTimeout (fn [] (dismiss! el)) duration))
    el))

;; ── Declarative [data-ui-toast] elements ────────────────────────────

(defn- consume! [el]
  (let [msg (.getAttribute el "data-ui-toast")
        dur (.getAttribute el "data-duration")]
    (.remove el)
    (show! msg {:variant (or (.getAttribute el "data-variant") "info")
                :duration (when dur (js/parseInt dur 10))})))

(defn- scan! []
  (doseq [el (js/Array.from (js/document.querySelectorAll "[data-ui-toast]"))]
    (consume! el)))

(defn init! []
  (scan!)
  (let [obs (js/MutationObserver. (fn [_ _] (scan!)))]
    (.observe obs js/document.body {:childList true :subtree true})))

(aset js/window "__uiToast" show!)

(if (= "loading" (.-readyState js/document))
  (.addEventListener js/document "DOMContentLoaded" init!)
  (init!))
