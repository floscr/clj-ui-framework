#!/usr/bin/env bb
;; Check all ui-dev tmux panes for compile errors and verify dev servers.
;; Exit 0 = all good, Exit 1 = errors found.

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[babashka.http-client :as http])

;; ── Helpers ─────────────────────────────────────────────────────────

(def red "\033[0;31m")
(def green "\033[0;32m")
(def yellow "\033[0;33m")
(def bold "\033[1m")
(def nc "\033[0m")

(defn sh [& args]
  (let [r (apply p/sh args)]
    (when (zero? (:exit r))
      (str/trim (:out r)))))

(defn println-ok [& parts] (println (str green "✓ " nc (str/join parts))))
(defn println-err [& parts] (println (str red bold "✗ " nc (str/join parts))))
(defn println-warn [& parts] (println (str yellow "⚠ " nc (str/join parts))))

(def errors (atom 0))

(defn fail! []
  (swap! errors inc))

;; ── 1. Tmux pane compile errors ────────────────────────────────────

(def error-patterns
  #"(?i)Build failure|CompilerException|WARNING:|SyntaxError|^ERROR|Exception|error:|failed")

(defn tmux-session-exists? []
  (some? (sh "tmux" "has-session" "-t" "ui-dev")))

(defn pane-indices []
  (some-> (sh "tmux" "list-panes" "-t" "ui-dev:1" "-F" "#{pane_index}")
          (str/split-lines)))

(defn capture-pane [idx]
  (some-> (sh "tmux" "capture-pane" "-t" (str "ui-dev:1." idx) "-p" "-S" "-50")
          (str/split-lines)
          (->> (remove str/blank?)
               (take-last 30)
               vec)))

(defn check-tmux-panes []
  (if-not (tmux-session-exists?)
    (println-warn "No ui-dev tmux session found — skipping pane checks")
    (doseq [idx (pane-indices)]
      (let [lines (capture-pane idx)
            matches (seq (filter #(re-find error-patterns %) lines))]
        (if matches
          (do (println-err "Pane " idx " — errors found:")
              (doseq [m matches] (println (str "  " m)))
              (fail!))
          (println-ok "Pane " idx " — " (last lines)))))))

;; ── 2. Hiccup server ───────────────────────────────────────────────

(defn check-hiccup []
  (try
    (let [body (:body (http/get "http://localhost:3003" {:throw false :timeout 3000}))]
      (if (str/includes? (or body "") "sidebar-layout")
        (println-ok "Hiccup — serving (sidebar-layout found)")
        (do (println-err "Hiccup — responds but page content missing")
            (fail!))))
    (catch Exception _
      (println-warn "Hiccup — not responding on :3003"))))

;; ── 3. Squint compiled output ───────────────────────────────────────

(defn check-squint []
  (let [dir (clojure.java.io/file "dev/squint/.compiled")]
    (if-not (.isDirectory dir)
      (println-warn "Squint — dev/squint/.compiled not found")
      (let [mjs-files (->> (file-seq dir)
                           (filter #(str/ends-with? (.getName %) ".mjs"))
                           (filter #(.isFile %)))
            broken (filter (fn [f]
                             (<= (count (str/split-lines (slurp f))) 1))
                           mjs-files)]
        (if (seq broken)
          (doseq [f broken]
            (println-err "Squint — empty/truncated: " (.getPath f))
            (fail!))
          (println-ok "Squint — all " (count mjs-files) " .mjs files have content"))))))

;; ── 4. Replicant compiled JS ────────────────────────────────────────

(defn check-replicant []
  (let [dir (clojure.java.io/file "dev/replicant/public/js/cljs-runtime")]
    (if-not (.isDirectory dir)
      (println-warn "Replicant — cljs-runtime dir not found")
      (let [ui-files (->> (.listFiles dir)
                          (filter #(str/starts-with? (.getName %) "ui."))
                          (filter #(str/ends-with? (.getName %) ".js")))]
        (if (seq ui-files)
          (println-ok "Replicant — " (count ui-files) " ui.*.js files compiled")
          (do (println-err "Replicant — no ui.*.js files compiled")
              (fail!)))))))

;; ── Run ─────────────────────────────────────────────────────────────

(check-tmux-panes)
(check-hiccup)
(check-squint)
(check-replicant)

(println)
(if (pos? @errors)
  (do (println (str red bold "✗ " @errors " error(s) found — do not commit" nc))
      (System/exit 1))
  (println (str green bold "✓ All checks passed" nc)))
