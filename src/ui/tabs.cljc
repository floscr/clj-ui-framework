(ns ui.tabs
  "Tabs — switch between panels of content.

   Pure CSS/HTML, no JS and no state: hidden radio inputs drive panel
   visibility via `:checked` + `:has()` selectors, so it works identically
   across hiccup (:clj), replicant (:cljs), and squint. Like the accordion's
   native <details>, the active tab is uncontrolled — the browser tracks it.
   Radios also give keyboard arrow-key navigation between tabs for free.

   Two visual variants:
     :boxed (default) — segmented control; the active tab is a raised pill.
     :line            — underlined tabs; the active tab has an accent underline.

   Usage:
     (tabs {:variant :line
            :default \"account\"
            :tabs [{:id \"account\"  :label \"Account\"  :content [:div \"...\"]}
                   {:id \"password\" :label \"Password\" :content [:div \"...\"]}]})

   Props:
     :variant - :boxed (default) or :line
     :default - id of the initially-active tab (defaults to the first tab)
     :tabs    - vector of {:id :label :content} maps
     :name    - radio group name (auto-generated when omitted; pass to make
                the markup deterministic, e.g. in tests)
     :class   - additional CSS classes
     :attrs   - additional HTML attributes on the root element"
  (:require [clojure.string :as str]
            [ui.util :as util]))

(defn tabs-class-list
  "Vector of CSS classes for the tabs root given :variant.
   Returns e.g. [\"tabs\" \"tabs-boxed\"]."
  [{:keys [variant]}]
  (let [v (or (some-> variant util/kw-name) "boxed")]
    ["tabs" (str "tabs-" v)]))

(defn tabs-classes
  "Space-joined class string for the tabs root."
  [opts]
  (str/join " " (tabs-class-list opts)))

(defn- rand-suffix
  "Short random string, unique per tabs instance, so multiple tabs on one
   page don't share a radio group / input ids."
  []
  #?(:squint (.slice (.toString (js/Math.random) 36) 2 9)
     :cljs   (str (rand-int 1000000000))
     :clj    (str (rand-int 1000000000))))

(defn- klass
  "Target-appropriate class value: a vector for replicant, a space-joined
   string for clj/squint."
  [& names]
  #?(:squint (str/join " " names)
     :cljs   (vec names)
     :clj    (str/join " " names)))

(defn tabs
  "Render a tabs widget. See namespace docstring for props."
  [{:keys [variant default tabs name class attrs] :as _props}]
  (let [group     (or (some-> name util/kw-name) (str "tabs-" (rand-suffix)))
        items     (mapv (fn [t] (assoc t :sid (util/kw-name (:id t)))) tabs)
        ids       (map :sid items)
        def-id    (some-> default util/kw-name)
        active-id (if (and def-id (some #(= def-id %) ids)) def-id (first ids))
        list-kids (into []
                        (mapcat (fn [t]
                                  (let [sid (:sid t)
                                        iid (str group "-" sid)]
                                    [(let [a {:class (klass "tabs-input")
                                              :type "radio"
                                              :name group
                                              :id iid}]
                                       [:input (cond-> a
                                                 (= sid active-id) (assoc :checked true))])
                                     [:label {:class (klass "tabs-trigger") :for iid}
                                      (:label t)]]))
                                items))
        panel-kids (into []
                         (map (fn [t]
                                [:div {:class (klass "tabs-content") :role "tabpanel"}
                                 (:content t)])
                              items))
        tabs-list  (into [:div {:class (klass "tabs-list") :role "tablist"}] list-kids)
        panels     (into [:div {:class (klass "tabs-panels")}] panel-kids)
        root-class #?(:squint (cond-> (tabs-classes {:variant variant}) class (str " " class))
                      :cljs   (cond-> (tabs-class-list {:variant variant}) class (conj class))
                      :clj    (cond-> (tabs-classes {:variant variant}) class (str " " class)))]
    [:div (merge {:class root-class} attrs)
     tabs-list
     panels]))
