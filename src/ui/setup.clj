(ns ui.setup
  "Helper for squint-based frontends that consume clj-ui-framework as a git dep.

   Squint can't resolve git deps — it only reads :paths from squint.edn.
   This namespace finds the cached git dep on the babashka classpath,
   symlinks its src/ dir into the frontend app, and copies theme.css.

   Usage in bb.edn:

     frontend:setup
     {:doc \"Setup clj-ui-framework for squint frontend\"
      :requires ([ui.setup :as setup])
      :task (setup/setup! {:link-target \"app/lib/ui\"
                           :theme-target \"app/lib/theme.css\"})}"
  (:require [babashka.classpath :as cp]
            [babashka.fs :as fs]
            [babashka.process :as proc]
            [clojure.string :as str]))

(defn find-ui-src
  "Find the clj-ui-framework src dir on the babashka classpath.
   Returns the path string, or nil if not found."
  []
  (->> (str/split (cp/get-classpath) (re-pattern ":"))
       (filter (fn [p] (str/includes? p "clj-ui-framework")))
       first))

(defn setup!
  "Symlink clj-ui-framework src and copy theme.css for a squint frontend.

   Options:
     :link-target   — path where the src symlink is created (e.g. \"app/lib/ui\")
     :theme-target  — path where theme.css is copied (e.g. \"app/lib/theme.css\")"
  [{:keys [link-target theme-target]}]
  (let [ui-src (find-ui-src)]
    (when-not ui-src
      (println "ERROR: clj-ui-framework not found on classpath.")
      (println "       Add it as a git dep in bb.edn:")
      (println "       {:deps {clj-ui-framework/clj-ui-framework {:git/url \"...\" :sha \"...\"}}}")
      (System/exit 1))

    ;; Symlink src dir
    (let [parent (str (fs/parent link-target))]
      (fs/create-dirs parent)
      (fs/delete-if-exists link-target)
      (fs/create-sym-link link-target ui-src)
      (println "Linked" link-target "→" ui-src))

    ;; Build theme if not present, then copy
    (let [theme-src (str (fs/parent ui-src) "/dist/theme.css")]
      (when-not (fs/exists? theme-src)
        (println "Building theme.css...")
        (proc/shell {:dir (str (fs/parent ui-src))} "bb" "build-theme"))
      (fs/create-dirs (str (fs/parent theme-target)))
      (fs/copy theme-src theme-target {:replace-existing true})
      (println "Copied theme.css →" theme-target))))
