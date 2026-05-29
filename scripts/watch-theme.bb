#!/usr/bin/env bb
;; Watch theme tokens + component CSS for changes.
;; On change, rebuild dist/theme.css and copy to dev targets.

(require '[ui.css.gen :as gen])

(gen/watch-css {:output     "dist/theme.css"
                :watch      ["src/theme/tokens.edn"]
                :on-rebuild (fn [{:keys [output]}]
                              (let [css (slurp output)]
                                (spit "dev/replicant/public/theme.css" css)
                                (spit "dev/squint/public/theme.css" css)
                                (println "[watch-theme] Copied to dev targets.")))})
