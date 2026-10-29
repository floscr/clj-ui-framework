(ns tools.registry
  "Component & icon registry — powers `bb list-components`,
   `bb list-icons` and `bb gen-docs`.

   Every component may carry a metadata sidecar `src/ui/<name>.edn` (see
   `meta-keys` and AGENTS.md § Component metadata). The sidecar drives the
   generated agent docs: docs/components/<id>.md (one page per component in
   the when-to-use / API / keyboard / a11y / notes-for-AI shape),
   docs/components.md (index), docs/llms.txt (index for assistants) and
   docs/llms-full.txt (every page inlined)."
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [ui.icon :as icon]))

;; ── Components ──────────────────────────────────────────────────────

(defn- ns-doc
  "First line of the ns docstring, if any."
  [src]
  (when-let [[_ doc] (re-find #"(?s)\(ns\s+[^\s)]+\s+\"((?:[^\"\\]|\\.)*)\"" src)]
    (-> doc
        (str/replace "\\\"" "\"")
        str/split-lines
        first
        str/trim)))

(defn- public-fns
  "Names of public defns (excludes defn-), including those nested in
   reader-conditional splices."
  [src]
  (->> (re-seq #"\(defn\s+([^\s\[]+)" src)
       (map second)
       distinct
       vec))

(defn- fn-keys
  "Props accepted by `fn-name`: the keys destructured by its first arg
   ({:keys [...]}) plus every :keyword mentioned in its docstring (for
   props passed through an `:as props` map). Nil when the fn neither
   destructures a map nor documents props."
  [src fn-name]
  (when-let [[_ body] (re-find (re-pattern (str "(?s)\\(defn " (java.util.regex.Pattern/quote fn-name)
                                                "\\s(.*?)(?=\\n\\(def|\\z)"))
                               src)]
    (let [destructured (when-let [[_ ks] (re-find #"\{:keys\s+\[([^\]]*)\]" body)]
                         (map keyword (str/split (str/trim ks) #"\s+")))
          docstring    (second (re-find #"(?s)^\s*\"((?:[^\"\\]|\\.)*)\"" body))
          documented   (when docstring (map (comp keyword second) (re-seq #":([a-z][a-z0-9-]*)" docstring)))]
      (when (or destructured documented)
        (set (concat destructured documented))))))

(defn- load-meta [name]
  (let [f (fs/file "src/ui" (str name ".edn"))]
    (when (fs/exists? f)
      (edn/read-string (slurp (str f))))))

(def helper-namespaces
  "ui/*.cljc files that are shared helpers, not components."
  #{"theme" "theme_model" "themes" "util"})

(defn components
  "All ui/*.cljc components: {:name :id :ns :doc :fns :src :meta}."
  []
  (->> (fs/glob "src/ui" "*.cljc")
       (map str)
       sort
       (remove #(helper-namespaces (str/replace (fs/file-name %) #"\.cljc$" "")))
       (map (fn [f]
              (let [src  (slurp f)
                    name (str/replace (fs/file-name f) #"\.cljc$" "")]
                {:name name
                 :id   (keyword (str/replace name "_" "-"))
                 :ns   (second (re-find #"\(ns\s+([^\s)]+)" src))
                 :doc  (ns-doc src)
                 :fns  (public-fns src)
                 :src  src
                 :meta (load-meta name)})))))

;; ── Metadata schema ─────────────────────────────────────────────────

(def categories
  "Category id → title, in sidebar order."
  (array-map :actions    "Actions"
             :inputs     "Inputs"
             :disclosure "Disclosure"
             :feedback   "Feedback"
             :data       "Data"
             :layout     "Layout"
             :media      "Media"
             :text       "Text"))

(def meta-keys
  "Allowed sidecar keys. Required ones are in `required-keys`."
  #{:id :title :summary :category :group :keywords
    :when-to-use :when-not-to-use :usage :examples
    :api :keyboard :accessibility :motion :responsive
    :targets :runtime :notes-for-ai :related})

(def required-keys #{:id :title :summary :category :when-to-use :api})

(defn validate-meta
  "Vector of error strings for one component's sidecar (empty = valid)."
  [{:keys [id fns src meta]} all-ids]
  (when meta
    (let [errs (atom [])
          err! (fn [& xs] (swap! errs conj (apply str xs)))]
      (doseq [k (remove meta-keys (keys meta))]
        (err! "unknown key " k))
      (doseq [k (remove (set (keys meta)) required-keys)]
        (err! "missing required key " k))
      (when (and (:id meta) (not= (:id meta) id))
        (err! ":id " (:id meta) " does not match file id " id))
      (when-not (contains? categories (:category meta))
        (err! "unknown :category " (:category meta)))
      (doseq [{f :fn props :props} (:api meta)]
        (if-not (some #{f} fns)
          (err! ":api fn " f " is not a public fn")
          (when-let [ks (fn-keys src f)]
            (doseq [p (map :name props)
                    :when (not (contains? ks p))]
              (err! "prop " p " is not destructured by " f)))))
      (doseq [r (:related meta) :when (not (contains? all-ids r))]
        (err! ":related " r " is not a component"))
      @errs)))

(defn validate-all
  "{:invalid {id [errors]} :missing [ids without a sidecar]}"
  []
  (let [comps   (components)
        all-ids (set (map :id comps))]
    {:invalid (into {} (keep (fn [c] (let [e (validate-meta c all-ids)] (when (seq e) [(:id c) e]))) comps))
     :missing (vec (keep (fn [c] (when-not (:meta c) (:id c))) comps))}))

;; ── Icons ───────────────────────────────────────────────────────────

(defn icons
  "All icon names (sorted strings) and the set with dedicated filled paths."
  []
  {:names  (sort (map name (keys icon/icon-paths)))
   :filled (set (map name (keys icon/filled-icon-paths)))})

;; ── Printing ────────────────────────────────────────────────────────

(defn print-components! []
  (let [comps (components)]
    (println (count comps) "components in src/ui/*.cljc\n")
    (doseq [{:keys [name ns doc fns meta]} comps]
      (println (format "%-18s %-22s %s%s" name ns (or (:summary meta) doc "") (if meta "" "  [no .edn sidecar]")))
      (when (seq fns)
        (println (str "                   " (str/join ", " fns)))))))

(defn print-icons! []
  (let [{:keys [names filled]} (icons)]
    (println (count names) "icons —" (count filled) "with dedicated filled variants (*)\n")
    (doseq [row (partition-all 4 names)]
      (println (str/join "  " (map #(format "%-22s" (str % (when (filled %) " *"))) row))))))

;; ── Markdown ────────────────────────────────────────────────────────

(defn- md-code-list [xs]
  (str/join ", " (map #(str "`" % "`") xs)))

(defn- bullets [xs] (str/join "\n" (map #(str "- " %) xs)))

(defn- section [title body]
  (when (and body (if (coll? body) (seq body) (seq (str body))))
    (str "## " title "\n\n" (if (coll? body) (bullets body) body) "\n\n")))

(defn- clj-block [code] (str "```clojure\n" code "\n```"))

(defn- md-cell [s] (str/replace (str s) "|" "\\|"))

(defn- props-table [props]
  (str "| Prop | Type | Default | Description |\n| --- | --- | --- | --- |\n"
       (str/join "\n"
                 (for [{:keys [name type default doc]} props]
                   (str "| `" name "` | `" (md-cell type) "` | "
                        (if default (str "`" (md-cell default) "`") "–")
                        " | " (md-cell doc) " |")))))

(defn- api-section [api]
  (str "## API reference\n\n"
       (str/join "\n\n"
                 (for [{f :fn :keys [doc props children]} api]
                   (str "### `" f "`\n\n" doc
                        (when children (str "\n\nChildren: " children))
                        (when (seq props) (str "\n\n" (props-table props))))))
       "\n\n"))

(defn- keyboard-section [kb]
  (when (seq kb)
    (str "## Keyboard interactions\n\n| Keys | Action |\n| --- | --- |\n"
         (str/join "\n" (map (fn [[k a]] (str "| " k " | " a " |")) kb))
         "\n\n")))

(defn- targets-section [targets]
  (when (seq targets)
    (str "## Targets\n\n"
         (bullets (for [[t note] [[:clj "Hiccup (`:clj`)"] [:cljs "Replicant (`:cljs`)"] [:squint "Squint (`:squint`)"]]
                        :when (get targets t)]
                    (str "**" note "**: " (get targets t))))
         "\n\n")))

(defn- related-section [related by-id]
  (when (seq related)
    (str "## Related\n\n"
         (bullets (for [r related :let [c (by-id r)]]
                    (if (:meta c)
                      (str "[" (get-in c [:meta :title]) "](" (name r) ".md): " (get-in c [:meta :summary]))
                      (str "`" (name r) "`" (when-let [d (:doc c)] (str ": " d))))))
         "\n\n")))

(defn component-md
  "Markdown page for one component with a sidecar."
  [{:keys [id ns fns meta] :as _c} by-id]
  (let [{:keys [title summary category group keywords when-to-use when-not-to-use usage examples api
                keyboard accessibility motion responsive targets runtime notes-for-ai related]} meta]
    (str "# " title "\n\n"
         "> " summary "\n\n"
         "- Type: Component (" (name category) (when group (str ", " group)) ")\n"
         "- Namespace: `" ns "`\n"
         "- Source: `src/ui/" (str/replace (name id) "-" "_") ".cljc`\n"
         "- Public fns: " (md-code-list fns) "\n"
         (when (seq runtime) (str "- JS runtime: " (md-code-list runtime) " (bundled in ui-runtime.js)\n"))
         (when (seq keywords) (str "- Keywords: " (str/join ", " keywords) "\n"))
         "\n"
         (section "When to use" when-to-use)
         (section "When not to use" when-not-to-use)
         (section "Usage" (when usage (clj-block usage)))
         (when (seq examples)
           (str "## Examples\n\n"
                (str/join "\n\n" (for [{:keys [title code]} examples] (str "### " title "\n\n" (clj-block code))))
                "\n\n"))
         (api-section api)
         (keyboard-section keyboard)
         (section "Accessibility" accessibility)
         (section "Motion" motion)
         (section "Responsive behavior" responsive)
         (targets-section targets)
         (section "Notes for AI" notes-for-ai)
         (related-section related by-id)
         "## Guidance for AI tools\n\n"
         title ": " summary " Use only the documented props; `:class` and `:attrs` are for layout and "
         "extra attributes, never for restyling internals. The same call renders on all three targets; "
         "see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.\n\n"
         "Full library index: docs/llms.txt\n")))

;; ── Index pages ─────────────────────────────────────────────────────

(defn- by-category [comps]
  (group-by #(or (get-in % [:meta :category]) :uncategorized) comps))

(def ^:private agent-rules
  "- Prefer an existing clj-ui component over new markup; compose components, never restyle their internals.
- Read the component's markdown before using it and use only documented props. Follow its \"When not to use\".
- Components are `.cljc`: one call renders to Hiccup (`:clj`), Replicant (`:cljs`) and Squint. Mind the target table in each page (events, `:class` vector vs string, `:style` shape).
- Use the semantic tokens (`--bg-0/1/2`, `--fg-0/1/2`, `--accent`, `--accent-subtle`, `--danger`, `--success`, `--warning`, `--border-0`, `--border-subtle`, `--radius-control/panel/surface`, `--size-N`, `--font-*`) instead of raw colors or rem values, and check light and dark themes.
- Motion: `--duration-fast/standard/considered` with `--ease-standard`; animate transform, opacity and color only. Reduced motion is handled globally.
- Buttons: the one main action is `:variant :primary` (neutral ink, not the accent); the rest are `:secondary` or `:ghost`; destructive is `:danger` (outlined). `:loud true` is for the single action on a screen that must dominate.
- Type: body 16px, controls 14px, headings from the base styles (h1 28px). Never set a font-family, a font-weight above 500, or a raw px font-size in app CSS; never import a web font.
- Copy: sentence case, verbs with objects on buttons, no eyebrow labels, no uppercase transforms, weights 400 and 500 only.
- Interactive behavior that hiccup cannot express lives in the JS runtime (ui-runtime.js); load it once per page.")

(defn- install-md []
  (str "## Install\n\n"
       "Add the framework as a git dependency in `bb.edn` or `deps.edn`:\n\n"
       "```edn\n{:deps {clj-ui-framework/clj-ui-framework\n        {:git/url \"https://github.com/floscr/clj-ui-framework\" :git/sha \"<sha>\"}}}\n```\n\n"
       "Generate the CSS once (tokens + every component style) and load the JS runtime for interactive components:\n\n"
       "```clojure\n(require '[ui.css.gen :as css])\n(def theme-css  (css/build-css))   ; CSS string, or (css/build-css {:output \"public/ui.css\"})\n(def runtime-js (css/build-js))    ; ui-runtime.js string\n```\n\n"
       "Dark mode: `data-theme=\"dark\"` on `<html>` (auto via prefers-color-scheme otherwise). Accent: `data-accent` = neutral, blue, teal, green, amber, orange, red, pink.\n\n"))

(defn- index-entry [{:keys [id meta doc]}]
  (str "- [" (or (:title meta) (name id)) "](components/" (name id) ".md): "
       (or (:summary meta) doc "")
       (when-not meta " (no metadata yet)")))

(defn llms-txt
  "docs/llms.txt — index for assistants."
  [comps]
  (let [cats (by-category comps)
        with-meta (count (filter :meta comps))]
    (str "# clj-ui\n\n"
         "> clj-ui is a cross-target Clojure UI framework: one `.cljc` component renders to Hiccup "
         "(server-side, no JS), Replicant (ClojureScript) and Squint (Eucalypt). Design tokens are EDN "
         "and the CSS is generated by Babashka; interactive behavior ships as a small JS runtime. "
         (count comps) " components, " with-meta " with full agent docs.\n\n"
         "Component links point to per-component markdown with when to use, when not to, usage, props, "
         "keyboard, accessibility, motion, responsive and per-target notes.\n\n"
         (install-md)
         "## Rules for agents\n\n" agent-rules "\n\n"
         "## Docs\n\n"
         "- [AGENTS.md](../AGENTS.md): contributor guide, target pitfalls, theme system, how to add a component\n"
         "- [components.md](components.md): index with public API per component\n"
         "- [llms-full.txt](llms-full.txt): this file with every component page inlined\n\n"
         "## Components\n\n"
         (str/join "\n\n"
                   (for [[cat title] categories :when (seq (cats cat))]
                     (str "### " title "\n\n" (str/join "\n" (map index-entry (cats cat))))))
         (when (seq (cats :uncategorized))
           (str "\n\n### Not yet categorized\n\n" (str/join "\n" (map index-entry (cats :uncategorized)))))
         "\n")))

(defn components-index-md
  "docs/components.md — index of components and icons."
  [comps]
  (let [{:keys [names filled]} (icons)
        cats (by-category comps)
        row (fn [{:keys [id ns meta doc fns]}]
              (str "| " (if meta (str "[" (:title meta) "](components/" (name id) ".md)") (name id))
                   " | `" ns "` | " (or (:summary meta) doc "") " | " (md-code-list fns) " |\n"))]
    (str "# Components & Icons\n\n"
         "> Generated by `bb gen-docs` — do not edit by hand.\n"
         "> See `AGENTS.md` for conventions, per-target pitfalls, and how to\n"
         "> add new components and icons. Agents: start at `docs/llms.txt`.\n\n"
         "## Components (" (count comps) ")\n\n"
         (str/join
          (for [[cat title] (concat categories [[:uncategorized "Not yet categorized"]])
                :when (seq (cats cat))]
            (str "### " title "\n\n"
                 "| Component | Namespace | Description | Public API |\n"
                 "|-----------|-----------|-------------|------------|\n"
                 (str/join (map row (cats cat)))
                 "\n")))
         "## Icons (" (count names) ")\n\n"
         "Lucide-compatible inline SVG icons — see `AGENTS.md` § Icons for\n"
         "usage and how to add new ones.\n\n"
         (md-code-list names) "\n\n"
         "Icons with dedicated filled path data (`:filled true`):\n\n"
         (md-code-list (sort filled)) "\n")))

(defn gen-docs!
  "Write docs/components.md, docs/components/<id>.md, docs/llms.txt and
   docs/llms-full.txt. Fails on invalid sidecars."
  []
  (let [comps (components)
        by-id (into {} (map (juxt :id identity) comps))
        {:keys [invalid missing]} (validate-all)]
    (when (seq invalid)
      (doseq [[id errs] invalid] (println "invalid sidecar" id ":" (str/join "; " errs)))
      (throw (ex-info "invalid component metadata" {:invalid invalid})))
    (fs/create-dirs "docs/components")
    (let [pages (for [c comps :when (:meta c)] [(:id c) (component-md c by-id)])]
      (doseq [[id md] pages]
        (spit (str "docs/components/" (name id) ".md") md))
      (spit "docs/components.md" (components-index-md comps))
      (spit "docs/llms.txt" (llms-txt comps))
      (spit "docs/llms-full.txt"
            (str (llms-txt comps) "\n\n---\n\n"
                 (str/join "\n\n---\n\n" (map second pages))))
      (println "Generated docs/components.md, docs/llms.txt, docs/llms-full.txt and"
               (count pages) "component pages")
      (when (seq missing)
        (println "  no sidecar yet:" (str/join ", " (map name missing)))))))
