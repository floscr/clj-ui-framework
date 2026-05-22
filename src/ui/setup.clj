(ns ui.setup
  "Helpers for consuming clj-ui-framework as a git dep.

   Most apps only need build-css from ui.css.gen — see that namespace.
   This namespace adds squint-specific setup (source symlinking).

   Usage in consumer's bb.edn:

     ;; Any app — generate CSS (with optional theme overrides)
     ui:css
     {:doc \"Generate UI framework CSS\"
      :requires ([ui.css.gen :as css])
      :task (css/build-css {:output \"resources/public/ui.css\"})}

     ;; Squint app — symlink sources + generate CSS
     frontend:setup
     {:doc \"Setup clj-ui-framework for squint frontend\"
      :requires ([ui.setup :as setup])
      :task (setup/setup! {:link-target \"app/lib/ui\"
                           :theme-target \"app/lib/theme.css\"})}"
  (:require [babashka.classpath :as cp]
            [babashka.fs :as fs]
            [clojure.string :as str]
            [ui.css.gen :as css]))

(defn- find-ui-src
  "Find the clj-ui-framework src dir on the babashka classpath.
   Returns the path string, or nil if not found."
  []
  (->> (str/split (cp/get-classpath) (re-pattern ":"))
       (filter (fn [p] (str/includes? p "clj-ui-framework")))
       first))

(defn setup!
  "Symlink clj-ui-framework src and generate theme.css for a squint frontend.
   For non-squint apps, use ui.css.gen/build-css directly.

   Options:
     :link-target   — path where the src symlink is created (e.g. \"app/lib/ui\")
     :theme-target  — path where theme.css is written (e.g. \"app/lib/theme.css\")
     :theme         — optional token overrides (same as build-css opts)"
  [{:keys [link-target theme-target theme]}]
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

    ;; Generate CSS
    (css/build-css (merge theme {:output theme-target}))))
