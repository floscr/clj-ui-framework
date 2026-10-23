;; cljs.core extends IEquiv/IComparable on js/Date by writing onto Date.prototype.
;; NSH's hardening freezes the shared prototypes, which turns that into
;; "object is not extensible" while the bundle evaluates. Wrap the block in an
;; isExtensible guard; the web dev host (not hardened) never needed it.
(let [path "out/cljs/core.js"
      lines (vec (clojure.string/split-lines (slurp path)))
      idxs (keep-indexed (fn [i l] (when (clojure.string/starts-with? l "(Date.prototype.") i)) lines)]
  (if (empty? idxs)
    (println "guard_date: nothing to patch (already guarded?)")
    (let [start (first idxs)
          last-stmt (last idxs)
          end (if (clojure.string/ends-with? (lines last-stmt) ";")
                last-stmt
                (first (filter #(= (lines %) "}));") (range last-stmt (count lines)))))
          patched (concat (subvec lines 0 start)
                          ["if (Object.isExtensible(Date.prototype)) { // guarded: NSH freezes shared prototypes"]
                          (subvec lines start (inc end))
                          ["}"]
                          (subvec lines (inc end)))]
      (spit path (clojure.string/join "\n" patched))
      (println "guard_date: wrapped" (count idxs) "Date.prototype statements, lines" (inc start) "-" (inc end)))))
