(ns dev.site
  "The component catalogue site, rendered by the hiccup dev/production
   server. Pages are built from two sources:

     - the component sidecars (src/ui/<name>.edn) via tools.registry:
       title, summary, category, when to use, API, notes for AI, ...
     - the shared examples (dev.examples.registry): live previews, with
       their source read from the example files with rewrite-clj.

   Routes (see dev.hiccup/handler):
     /                        catalogue home, grouped by category
     /components/<id>         component page
     /components/<id>/markdown the generated agent markdown
     /llms.txt, /llms-full.txt the generated agent indexes"
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [hiccup2.core :as h]
            [rewrite-clj.zip :as z]
            [tools.registry :as registry]
            [dev.example :as ex]
            [dev.examples.registry :as examples]
            [ui.util :as util]
            [ui.badge :as badge]
            [ui.breadcrumb :as breadcrumb]
            [ui.button :as button]
            [ui.card :as card]
            [ui.command :as command]
            [ui.icon :as icon]
            [ui.tabs :as tabs]))

;; ── Data ────────────────────────────────────────────────────────────

(defn components
  "Registry components that carry metadata, in category order."
  []
  (let [order (zipmap (keys registry/categories) (range))]
    (->> (registry/components)
         (filter :meta)
         (sort-by (fn [c] [(get order (get-in c [:meta :category]) 99) (get-in c [:meta :title])])))))

(defn by-category
  "[[category-title [component ...]] ...] in category order."
  [comps]
  (let [groups (group-by #(get-in % [:meta :category]) comps)]
    (for [[cat title] registry/categories :when (seq (groups cat))]
      [title (groups cat)])))

(defn component
  "Registry entry for an id string, or nil."
  [id]
  (some (fn [c] (when (= (name (:id c)) id) c)) (registry/components)))

;; ── Example source ──────────────────────────────────────────────────

(defn- dedent [s]
  (let [lines (str/split-lines s)
        rest-lines (rest lines)
        indents (->> rest-lines
                     (remove str/blank?)
                     (map #(count (re-find #"^\s*" %))))
        indent (if (seq indents) (apply min indents) 0)]
    (if (pos? indent)
      (str/join "\n" (cons (first lines) (map #(if (str/blank? %) % (subs % (min indent (count %)))) rest-lines)))
      s)))

(defn example-sources
  "Map of example id (string) -> source string of its render body, read
   from dev/shared/src/dev/examples/<name>.cljc."
  [component-name]
  (let [f (io/file "dev/shared/src/dev/examples" (str component-name ".cljc"))]
    (when (.isFile f)
      (loop [loc (z/of-string (slurp f)) acc {}]
        (if (z/end? loc)
          acc
          (let [acc (if (and (z/list? loc)
                             (= 'example (some-> loc z/down z/sexpr)))
                      (let [id   (-> loc z/down z/right z/sexpr)
                            fn-loc (-> loc z/down z/right z/right z/right)
                            body (when (and fn-loc (z/list? fn-loc))
                                   (->> (iterate z/right (-> fn-loc z/down z/right z/right))
                                        (take-while some?)
                                        (map z/string)
                                        (str/join "\n")))]
                        (cond-> acc body (assoc (util/kw-name id) (dedent body))))
                      acc)]
            (recur (z/next loc) acc)))))))

;; ── Site CSS ────────────────────────────────────────────────────────

(def site-css
  ".site-hero { display: flex; flex-direction: column; gap: var(--size-3); margin-bottom: var(--size-10); max-width: 48rem; }
.site-hero h1 { margin: 0; font-size: var(--font-2xl); }
.site-hero p { margin: 0; color: var(--fg-1); font-size: var(--font-md); }
.site-category { margin-bottom: var(--size-10); }
.site-category h2 { font-size: var(--font-lg); margin: 0 0 var(--size-4); display: flex; align-items: baseline; gap: var(--size-2); }
.site-category h2 small { color: var(--fg-2); font-size: var(--font-sm); font-weight: var(--weight-regular); }
.site-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr)); gap: var(--size-4); }
.site-card { display: flex; flex-direction: column; gap: var(--size-2); padding: var(--size-5); background: var(--bg-1); border: var(--border-0); border-radius: var(--radius-surface); color: inherit; text-decoration: none; transition: border-color var(--duration-fast) var(--ease-standard), background-color var(--duration-fast) var(--ease-standard); }
.site-card:hover { border-color: var(--gray-300); }
[data-theme=dark] .site-card:hover { border-color: var(--gray-700); }
.site-card-title { display: flex; align-items: center; justify-content: space-between; gap: var(--size-2); font-weight: var(--weight-medium); }
.site-card-summary { margin: 0; color: var(--fg-1); font-size: var(--font-sm); }
.site-page-head { display: flex; flex-direction: column; gap: var(--size-2); margin-bottom: var(--size-8); }
.site-page-head h1 { margin: 0; font-size: var(--font-2xl); }
.site-page-head .site-summary { margin: 0; color: var(--fg-1); font-size: var(--font-md); max-width: 48rem; }
.site-meta { display: flex; flex-wrap: wrap; gap: var(--size-2) var(--size-4); color: var(--fg-2); font-size: var(--font-sm); }
.site-targets { display: inline-flex; gap: var(--size-1); background: var(--bg-2); border-radius: var(--radius-control); padding: var(--size-1); }
.site-targets a { padding: var(--size-1) var(--size-3); border-radius: calc(var(--radius-control) - var(--size-1)); color: var(--fg-1); text-decoration: none; font-size: var(--font-sm); }
.site-targets a.is-active { background: var(--bg-1); color: var(--fg-0); box-shadow: var(--shadow-0); }
.site-section { margin-bottom: var(--size-10); }
.site-section h2 { font-size: var(--font-lg); margin: 0 0 var(--size-4); }
.site-section h3 { font-size: var(--font-base); margin: var(--size-6) 0 var(--size-2); }
.site-section ul { margin: 0; padding-left: var(--size-5); color: var(--fg-1); display: flex; flex-direction: column; gap: var(--size-1); }
.site-section p { color: var(--fg-1); margin: 0; }
.site-example { margin-bottom: var(--size-6); }
.site-example-title { font-size: var(--font-sm); color: var(--fg-1); margin: 0 0 var(--size-2); font-weight: var(--weight-medium); }
.site-preview { padding: var(--size-6); background: var(--bg-1); border: var(--border-0); border-radius: var(--radius-surface); }
.site-code { margin: 0; padding: var(--size-4); background: var(--bg-1); border: var(--border-0); border-radius: var(--radius-surface); overflow-x: auto; font-size: var(--font-sm); line-height: 1.5; }
.site-code code { font-family: var(--font-mono); }
.site-example .tabs-list { margin-bottom: var(--size-2); }
.site-props { width: 100%; border-collapse: collapse; font-size: var(--font-sm); }
.site-props th, .site-props td { text-align: left; padding: var(--size-2) var(--size-3); border-bottom: var(--border-subtle); vertical-align: top; }
.site-props th { color: var(--fg-2); font-weight: var(--weight-medium); }
.site-props code { font-family: var(--font-mono); font-size: var(--font-xs); background: var(--bg-2); padding: 0 var(--size-1); border-radius: var(--radius-xs); }
.site-kv { display: grid; grid-template-columns: 10rem 1fr; gap: var(--size-2) var(--size-4); font-size: var(--font-sm); color: var(--fg-1); }
.site-kv dt { color: var(--fg-2); }
.site-kv dd { margin: 0; }
.site-related { display: flex; flex-wrap: wrap; gap: var(--size-2); }
.site-search-trigger { display: inline-flex; align-items: center; gap: var(--size-2); }
")

;; ── Shared bits ─────────────────────────────────────────────────────

(defn- code-block [src]
  [:pre.site-code [:code src]])

(defn- bullets [xs]
  (when (seq xs) (into [:ul] (map (fn [x] [:li x]) xs))))

(defn- section [title & body]
  (when (some some? body)
    (into [:section.site-section [:h2 title]] (remove nil? body))))

(defn search-trigger
  "⌘K trigger + palette listing every component."
  [comps]
  [:div
   (command/command-trigger {:target "site-search" :class (str (button/button-classes {:variant :secondary}) " site-search-trigger")}
     (icon/icon {:icon-name :search :size :sm})
     [:span "Search components"]
     [:kbd.command-shortcut "⌘K"])
   (command/command-dialog {:id "site-search" :hotkey "mod+k" :placeholder "Search components…"}
     (for [[title group] (by-category comps)]
       (command/command-group {:heading title}
         (for [{:keys [id meta]} group]
           (command/command-item {:url (str "/components/" (name id)) :description (:summary meta)}
             (:title meta))))))])

;; ── Home ────────────────────────────────────────────────────────────

(defn home-page []
  (let [comps (components)]
    [:div
     [:div.site-hero
      [:h1 "Components"]
      [:p (str (count comps) " components, one source: every call renders to Hiccup on the server, "
               "Replicant and Squint in the browser. Pick one for its job; the page tells you when not to.")]
      [:div {:style "display: flex; gap: var(--size-3); flex-wrap: wrap; align-items: center;"}
       (search-trigger comps)
       (button/button {:variant :ghost :href "/all"} "All examples on one page")
       (button/button {:variant :ghost :href "/llms.txt" :icon-right :external-link} "llms.txt for agents")]]
     (for [[title group] (by-category comps)]
       [:section.site-category
        [:h2 title [:small (str (count group))]]
        [:div.site-cards
         (for [{:keys [id meta]} group]
           [:a.site-card {:href (str "/components/" (name id))}
            [:div.site-card-title
             [:span (:title meta)]
             (when-let [g (:group meta)] (badge/badge {:variant :outline :size :sm} g))]
            [:p.site-card-summary (:summary meta)]])]])]))

;; ── Component page ──────────────────────────────────────────────────

(defn- props-table [props]
  [:table.site-props
   [:thead [:tr [:th "Prop"] [:th "Type"] [:th "Default"] [:th "Description"]]]
   (into [:tbody]
         (for [{:keys [name type default doc]} props]
           [:tr
            [:td [:code (str name)]]
            [:td (when type [:code type])]
            [:td (if default [:code default] "–")]
            [:td doc]]))])

(defn- api-section [api]
  (section "API"
    (for [{f :fn :keys [doc props children]} api]
      [:div
       [:h3 [:code f]]
       [:p doc]
       (when children [:p [:strong "Children: "] children])
       (when (seq props) (props-table props))])))

(defn- keyboard-section [kb]
  (when (seq kb)
    (section "Keyboard"
      [:table.site-props
       [:thead [:tr [:th "Keys"] [:th "Action"]]]
       (into [:tbody] (for [[k a] kb] [:tr [:td [:code k]] [:td a]]))])))

(defn- targets-section [targets]
  (when (seq targets)
    (section "Targets"
      [:dl.site-kv
       (for [[t label] [[:clj "Hiccup"] [:cljs "Replicant"] [:squint "Squint"]] :when (get targets t)]
         (list [:dt label] [:dd (get targets t)]))])))

(defn- examples-section [component-name entry]
  (when entry
    (let [sources (example-sources component-name)]
      (section "Examples"
        (for [{:keys [id title render]} (:examples entry)
              :let [eid (util/kw-name id)
                    src (get sources eid)]]
          [:div.site-example {:id (str "example-" eid)}
           [:p.site-example-title title]
           (tabs/tabs {:variant :boxed
                       :name (str "ex-" component-name "-" eid)
                       :tabs (cond-> [{:id "preview" :label "Preview" :content [:div.site-preview (render)]}]
                               src (conj {:id "code" :label "Code" :content (code-block src)}))})])))))

(defn component-page
  "The page for one component id (string). `targets` are the target
   switcher links ({:label :href :active})."
  [id targets]
  (if-let [{:keys [name ns meta]} (component id)]
    (let [{:keys [title summary category group when-to-use when-not-to-use usage examples api
                  keyboard accessibility motion responsive targets-notes runtime notes-for-ai related]} meta
          comps (registry/components)
          by-id (into {} (map (juxt :id identity) comps))
          entry (examples/entry id)]
      [:div
       (breadcrumb/breadcrumb {:items [{:label "Components" :href "/"} {:label title}]})
       [:div.site-page-head
        [:h1 title]
        [:p.site-summary summary]
        [:div.site-meta
         [:span [:code ns]]
         [:span (get registry/categories category) (when group (str " · " group))]
         (when (seq runtime) [:span "Runtime: " (str/join ", " runtime)])
         [:a {:href (str "/components/" id "/markdown")} "Markdown for agents"]]
        [:div.site-targets
         (for [{:keys [label href active]} targets]
           [:a {:href href :class (when active "is-active")} label])]]
       (examples-section name entry)
       (section "When to use" (bullets when-to-use))
       (section "When not to use" (bullets when-not-to-use))
       (when usage (section "Usage" (code-block usage)))
       (when (seq examples)
         (section "More examples"
           (for [{:keys [title code]} examples]
             [:div [:h3 title] (code-block code)])))
       (api-section api)
       (keyboard-section keyboard)
       (section "Accessibility" (bullets accessibility))
       (section "Motion" (bullets motion))
       (section "Responsive behavior" (bullets responsive))
       (targets-section (:targets meta))
       (section "Notes for AI" (bullets notes-for-ai))
       (when (seq related)
         (section "Related"
           [:div.site-related
            (for [r related :let [c (by-id r)] :when c]
              (button/button {:variant :secondary :size :sm :href (str "/components/" (clojure.core/name r))}
                (get-in c [:meta :title])))]))])
    [:div.site-page-head [:h1 "Not found"] [:p.site-summary (str "No component called " id ".")]]))

;; ── Nav data for the shell ──────────────────────────────────────────

(defn nav-groups
  "[{:title :items [{:label :href}]} ...] for the sidebar."
  []
  (for [[title group] (by-category (components))]
    {:title title
     :items (for [{:keys [id meta]} group]
              {:label (:title meta) :href (str "/components/" (name id))})}))

;; ── Text routes ─────────────────────────────────────────────────────

(defn component-markdown [id]
  (when-let [c (component id)]
    (let [comps (registry/components)
          by-id (into {} (map (juxt :id identity) comps))]
      (registry/component-md c by-id))))

(defn llms-txt [] (registry/llms-txt (registry/components)))

(defn llms-full-txt []
  (let [comps (registry/components)
        by-id (into {} (map (juxt :id identity) comps))]
    (str (registry/llms-txt comps) "\n\n---\n\n"
         (str/join "\n\n---\n\n" (for [c comps :when (:meta c)] (registry/component-md c by-id))))))
