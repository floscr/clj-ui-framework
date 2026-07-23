(ns ui.toast
  "Toast — floating, auto-dismissing notification.

   Display is handled by the shared JS runtime (src/ui/js/toast.cljs,
   bundled in ui-runtime.js), so all targets share one implementation:

   - Replicant/Squint: call (show-toast! {:message \"Saved\"
                                          :variant :success})
   - Hiccup/HTMX: include (toast-flash {:message \"✓ Saved\"
                                        :variant :success}) in any
     response fragment (works with hx-swap-oob) — the runtime consumes
     the marker element and shows the toast.

   Requires ui-runtime.js to be loaded on the page.

   Variants: :info (default), :success, :warning, :danger.
   :duration is ms before auto-dismiss (default 5000, 0 = sticky)."
  (:require [clojure.string :as str]
            [ui.util :as util]))

(defn toast-class-list
  "Returns a vector of CSS class strings for a toast element."
  [{:keys [variant]}]
  (let [v (or (some-> variant util/kw-name) "info")]
    ["toast" (str "toast-" v)]))

(defn toast-classes
  "Returns a space-joined class string for a toast element."
  [opts]
  (str/join " " (toast-class-list opts)))

(defn toast
  "Static toast element. Rendered inside a .toast-container by the JS
   runtime; useful directly only for previews/demos.

   Props: :variant, :class, :attrs. Children: toast content."
  [{:keys [variant class attrs]} & children]
  #?(:squint
     (into [:div (merge {:class (cond-> (toast-classes {:variant variant})
                                  class (str " " class))}
                        attrs)]
           children)

     :cljs
     (into [:div (merge {:class (cond-> (toast-class-list {:variant variant})
                                  class (conj class))}
                        attrs)]
           children)

     :clj
     (into [:div (merge {:class (cond-> (toast-classes {:variant variant})
                                  class (str " " class))}
                        attrs)]
           children)))

(defn toast-flash
  "Invisible marker element consumed by the JS runtime, which shows the
   message as a floating toast. Include it in a server-rendered page or
   HTMX response fragment.

   Props:
     :message  - toast text (required)
     :variant  - :info (default), :success, :warning, :danger
     :duration - ms before auto-dismiss (default 5000, 0 = sticky)"
  [{:keys [message variant duration]}]
  [:div (cond-> {:data-ui-toast message
                 :style #?(:squint {"display" "none"}
                           :cljs {:display "none"}
                           :clj "display: none;")}
          variant  (assoc :data-variant (util/kw-name variant))
          duration (assoc :data-duration (str duration)))])

(do
  #?@(:squint
      [(defn show-toast!
         "Show a floating toast via the JS runtime.
          Props: :message, :variant, :duration."
         [{:keys [message variant duration]}]
         (when-let [f (aget js/window "__uiToast")]
           (f message {:variant (or variant "info")
                       :duration duration})))]

      :cljs
      [(defn show-toast!
         "Show a floating toast via the JS runtime.
          Props: :message, :variant, :duration."
         [{:keys [message variant duration]}]
         (when-let [f (aget js/window "__uiToast")]
           (f message #js {:variant (util/kw-name (or variant :info))
                           :duration duration})))]))
