#!/usr/bin/env bb
;; Watch src/ui/*.css and src/theme/tokens.edn for changes.
;; On change, rebuild dist/theme.css and copy to dev targets.

(require '[babashka.fs :as fs]
         '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[ui.css.gen :as gen])

(defn source-files []
  (concat (map str (fs/glob "src/ui" "*.css"))
          ["src/theme/tokens.edn"]))

(defn get-mtimes []
  (into {}
    (keep (fn [p]
            (when (fs/exists? p)
              [p (fs/last-modified-time p)])))
    (source-files)))

(defn rebuild! []
  (gen/build-theme! {:input "src/theme/tokens.edn" :output "dist/theme.css"})
  (let [css (slurp "dist/theme.css")]
    (spit "dev/replicant/public/theme.css" css)
    (io/make-parents "dev/squint/public/theme.css")
    (spit "dev/squint/public/theme.css" css)))

(defn changed-filenames [prev curr]
  (->> (keys curr)
       (filter (fn [p] (not= (get prev p) (get curr p))))
       (map (fn [p] (last (str/split p #"/"))))))

(println "[watch-theme] Watching src/ui/*.css and src/theme/tokens.edn ...")
(loop [prev (get-mtimes)]
  (Thread/sleep 500)
  (let [curr (get-mtimes)]
    (when (not= prev curr)
      (println (str "[watch-theme] Changed: " (str/join ", " (changed-filenames prev curr))))
      (try
        (rebuild!)
        (println "[watch-theme] Rebuilt and copied.")
        (catch Exception e
          (println (str "[watch-theme] ERROR: " (.getMessage e))))))
    (recur curr)))
