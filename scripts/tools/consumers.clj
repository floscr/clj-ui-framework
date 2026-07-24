(ns tools.consumers
  "Consumer discovery + AGENTS.md sync.

   `bb scan-consumers [roots...]` — find projects whose bb.edn/deps.edn
   depend on clj-ui-framework, show their pinned sha vs current HEAD.

   `bb sync-consumer-agents [roots...]` — upsert a marker-delimited
   'UI Framework' section into each consumer project's AGENTS.md,
   pointing at this repo's AGENTS.md and docs/components.md."
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def ^:private skip-dirs
  #{"node_modules" "target" "dist" "out" ".compiled"})

(defn- default-roots []
  (let [home (System/getProperty "user.home")]
    [(str home "/Code") (str home "/.config/dotfiles")]))

(defn- dep-files
  "All bb.edn/deps.edn under root, skipping vendored/build dirs, hidden
   dirs (except the root itself) and any clj-ui-framework checkout."
  [root]
  (let [acc (atom [])]
    (fs/walk-file-tree
     root
     {:pre-visit-dir
      (fn [d _]
        (let [n (fs/file-name d)]
          (if (and (not= (str d) (str root))
                   (or (skip-dirs n)
                       (str/starts-with? (str n) ".")
                       (= n "clj-ui-framework")))
            :skip-subtree
            :continue)))
      :visit-file
      (fn [f _]
        (when (#{"bb.edn" "deps.edn"} (fs/file-name f))
          (swap! acc conj (str f)))
        :continue)
      :visit-file-failed (fn [_ _] :continue)})
    @acc))

(defn- parse-dep-file
  "How (if at all) an edn dep file references clj-ui-framework:
   {:via :git :sha ..} — git dependency (any coordinate name / :git/url)
   {:via :vendored}    — vendored checkout on :paths
   plus :local-roots, the resolved :local/root dep dirs (for transitive
   detection through libs like server-lib)."
  [f]
  (try
    (let [m (edn/read-string {:default (fn [_ v] v)} (slurp f))
          deps (:deps m)
          git-dep (some (fn [[k v]]
                          (when (and (map? v)
                                     (or (str/includes? (str k) "clj-ui-framework")
                                         (str/includes? (str (:git/url v)) "clj-ui-framework")))
                            v))
                        deps)
          vendored? (some #(str/includes? (str %) "clj-ui-framework") (:paths m))
          local-roots (keep (fn [[_ v]]
                              (when-let [root (and (map? v) (:local/root v))]
                                (str (fs/normalize (fs/absolutize (fs/path (fs/parent f) root))))))
                            deps)]
      (cond-> {:local-roots (vec local-roots)}
        git-dep   (assoc :via :git :sha (or (:git/sha git-dep) (:sha git-dep)))
        (and (not git-dep) vendored?) (assoc :via :vendored)))
    (catch Exception _ nil)))

(defn consumers
  "Consumer projects: {:dir .. :files [{:file :via :sha?}]}, grouped by
   project dir. Includes projects that consume the framework transitively
   through a :local/root dep on a direct consumer (:via :transitive)."
  [roots]
  (let [parsed (->> roots
                    (filter fs/exists?)
                    (mapcat dep-files)
                    (keep (fn [f]
                            (when-let [info (parse-dep-file f)]
                              (assoc info :file f :dir (str (fs/parent f)))))))
        direct-dirs (set (map :dir (filter :via parsed)))
        entries (keep (fn [{:keys [via local-roots dir] :as e}]
                        (cond
                          via e
                          (some direct-dirs local-roots)
                          (assoc e :via :transitive
                                 :through (some direct-dirs local-roots))
                          :else nil))
                      parsed)]
    (->> entries
         (group-by :dir)
         (map (fn [[dir es]]
                {:dir dir
                 :files (mapv #(select-keys % [:file :via :sha :through]) es)}))
         (sort-by :dir))))

(defn- head-sha []
  (str/trim (:out (p/shell {:out :string} "git rev-parse HEAD"))))

(defn scan!
  [args]
  (let [roots (or (seq args) (default-roots))
        head (head-sha)
        found (consumers roots)]
    (println "clj-ui-framework HEAD:" (subs head 0 7) "\n")
    (if (empty? found)
      (println "No consumers found under" (str/join ", " roots))
      (doseq [{:keys [dir files]} found]
        (let [vias (set (map :via files))
              shas (distinct (keep :sha files))
              status (cond
                       (vias :git)
                       (if (every? #(= head %) shas)
                         "current"
                         (str "STALE (" (str/join ", " (map #(subs % 0 7) shas)) ")"))
                       (vias :vendored) "vendored"
                       :else (str "transitive via "
                                  (fs/file-name (some :through files))))]
          (println (format "%-72s %-28s %s"
                           dir
                           (str/join ", " (map #(fs/file-name (:file %)) files))
                           status)))))
    (println "\n" (count found) "consumer project(s)")))

;; ── AGENTS.md sync ──────────────────────────────────────────────────

(def ^:private begin-marker "<!-- clj-ui-framework:begin -->")
(def ^:private end-marker "<!-- clj-ui-framework:end -->")

(def ^:private section
  (str begin-marker "\n"
       "## UI Framework — clj-ui-framework\n"
       "\n"
       "This repo uses the shared **clj-ui-framework** component library\n"
       "(cross-target Clojure/ClojureScript/Squint UI components, theme\n"
       "tokens, icons, and browser JS runtime), pinned as a git dependency\n"
       "in this project's `bb.edn`/`deps.edn`.\n"
       "\n"
       "- Local checkout: `~/Code/Projects/clj-ui-framework`\n"
       "- Remote (git dep source): <https://github.com/floscr/clj-ui-framework>\n"
       "\n"
       "Before doing UI work here, read the framework's `AGENTS.md` — it\n"
       "documents the available components and icons (full generated list in\n"
       "`docs/components.md`), how to add new components and icons, per-target\n"
       "pitfalls (hiccup/replicant/squint), theming/tokens, and the JS runtime.\n"
       "Update the framework by bumping the pinned `:sha` in this project's\n"
       "`bb.edn`/`deps.edn`.\n"
       end-marker "\n"))

(defn- upsert-agents!
  "Insert or replace the framework section in dir/AGENTS.md.
   Returns :created, :updated, or :unchanged."
  [dir]
  (let [f (fs/file dir "AGENTS.md")]
    (if (fs/exists? f)
      (let [content (slurp f)]
        (cond
          (str/includes? content section)
          :unchanged

          (str/includes? content begin-marker)
          (let [re (re-pattern (str "(?s)" (java.util.regex.Pattern/quote begin-marker)
                                    ".*?" (java.util.regex.Pattern/quote end-marker) "\n?"))]
            (spit f (str/replace content re section))
            :updated)

          :else
          (do (spit f (str (str/trim-newline content) "\n\n" section))
              :updated)))
      (do (spit f section)
          :created))))

(defn sync-agents!
  [args]
  (let [roots (or (seq args) (default-roots))
        dirs (map :dir (consumers roots))
        ;; only top-level consumer dirs — nested ones (dev shells, sub-apps)
        ;; are covered by their parent's AGENTS.md
        top (remove (fn [d]
                      (some #(and (not= % d) (str/starts-with? d (str % "/"))) dirs))
                    dirs)]
    (doseq [dir top]
      (println (format "%-10s %s" (name (upsert-agents! dir)) (str dir "/AGENTS.md"))))
    (println "\n" (count top) "consumer project(s) synced")))
