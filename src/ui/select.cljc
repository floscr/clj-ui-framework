(ns ui.select
  "Custom select component — a framework-drawn dropdown replacing the
   native <select>. The trigger button is rendered here per-target; the
   floating listbox is created by the __uiSelect JS runtime (ui-runtime.js),
   which handles outside-tap dismiss (consuming the tap), Escape and
   keyboard navigation.

   Usage (all targets):
     (select {:options [{:value \"a\" :label \"Apple\"} ...]
              :value \"a\"
              :placeholder \"Pick a fruit\"
              :on-change (fn [value] ...)})   ; :on-change receives the value

   :clj (hiccup) is uncontrolled: pass :name to emit a hidden <input> that
   carries the value for form submission; the runtime keeps it in sync.
   :squint/:cljs are controlled: :on-change fires with the picked value and
   the framework re-renders the trigger."
  (:require [ui.context-menu :as ctx]))

(defn normalize-options
  "Coerce options into a vector of {:value :label} maps.
   Bare strings become {:value s :label s}."
  [options]
  (mapv (fn [o] (if (string? o) {:value o :label o} o)) options))

(defn selected-label
  "Return the label for the option matching value, else the placeholder,
   else an empty string."
  [options value placeholder]
  (or (some (fn [o] (when (= (:value o) value) (:label o)))
            (normalize-options options))
      placeholder
      ""))

(defn select
  "Render a custom select. See namespace docstring for props."
  [{:keys [options value placeholder disabled on-change name class attrs] :as _props}]
  (let [opts    (normalize-options options)
        label   (selected-label opts value placeholder)
        has-val (boolean (some (fn [o] (= (:value o) value)) opts))]
    #?(:squint
       (let [tclasses (cond-> "select-trigger" class (str " " class))
             vclasses (cond-> "select-value" (not has-val) (str " select-value--placeholder"))
             trigger  [:button (merge (cond-> {:type "button"
                                               :class tclasses
                                               :role "combobox"
                                               :aria-haspopup "listbox"
                                               :aria-expanded "false"
                                               :data-select-value (or value "")
                                               :on-click (fn [e]
                                                           (let [f (aget js/window "__uiSelect")]
                                                             (when f
                                                               (f (.-currentTarget e) opts
                                                                  (when on-change (fn [v] (on-change v)))))))}
                                        disabled (assoc :disabled true))
                                      attrs)
                        [:span {:class vclasses} label]]]
         [:div {:class "select"} trigger])

       :cljs
       (let [tclasses (cond-> ["select-trigger"] class (conj class))
             vclasses (cond-> ["select-value"] (not has-val) (conj "select-value--placeholder"))
             opts-js  (clj->js opts)
             trigger  [:button (merge (cond-> {:type "button"
                                               :class tclasses
                                               :role "combobox"
                                               :aria-haspopup "listbox"
                                               :aria-expanded "false"
                                               :data-select-value (or value "")
                                               :on {:click (fn [e]
                                                             (when-let [f (aget js/window "__uiSelect")]
                                                               (f (.-currentTarget e) opts-js
                                                                  (when on-change (fn [v] (on-change v))))))}}
                                        disabled (assoc :disabled true))
                                      attrs)
                        [:span {:class vclasses} label]]]
         [:div {:class ["select"]} trigger])

       :clj
       (let [json     (ctx/items->json opts)
             tclasses (cond-> "select-trigger" class (str " " class))
             vclasses (cond-> "select-value" (not has-val) (str " select-value--placeholder"))
             trigger  [:button (merge (cond-> {:type "button"
                                               :class tclasses
                                               :role "combobox"
                                               :aria-haspopup "listbox"
                                               :aria-expanded "false"
                                               :data-select-options json
                                               :data-select-value (or value "")
                                               :onclick "window.__uiSelect(this)"}
                                        disabled (assoc :disabled true))
                                      attrs)
                        [:span {:class vclasses} label]]]
         (if name
           [:div {:class "select"} trigger [:input {:type "hidden" :name name :value (or value "")}]]
           [:div {:class "select"} trigger])))))
