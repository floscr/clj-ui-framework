(ns ui.util
  "Cross-target helpers shared by UI components.

   These smooth over the differences between the three render targets:

     :clj    — Hiccup (server-rendered HTML)
     :cljs   — Replicant (browser)
     :squint — Eucalypt (browser, keywords are strings)

   Every component that touches keyword props or Replicant class vectors
   needs the same one or two tiny shims. Rather than re-defining them in
   each component (they were duplicated across ~25 files), require this
   namespace:

     (:require [ui.util :as util])

   and call `util/kw-name` / `util/conj-classes`."
  (:require [clojure.string :as str]))

(defn kw-name
  "Coerce a keyword (or string) prop to its plain string name.

   Targets differ in how keywords work:
     :squint — keywords are already strings, so this is identity
     :cljs / :clj — real keywords, so use `name`

   Defensive: non-keyword, non-nil values fall back to `str`, so passing an
   already-stringified value (or a number) is safe. Callers still typically
   guard nil with `(some-> x kw-name)`."
  [s]
  #?(:squint s
     :cljs   (if (keyword? s) (name s) (str s))
     :clj    (if (keyword? s) (name s) (str s))))

;; Replicant treats each element of a :class vector as a single DOMTokenList
;; token, so a space-joined string (e.g. from button-classes) must be split
;; into individual tokens first before conj-ing onto the class vector.
;; Only meaningful under :cljs (Replicant) — :clj / :squint build class
;; strings directly and never call this.
#?(:cljs
   (defn conj-classes
     "Append `class` onto the Replicant class vector `base`.

      Accepts a space-joined string (split into individual tokens), a
      collection of tokens (spliced in), nil (no-op), or any other value
      (conj'd as-is)."
     [base class]
     (cond
       (nil? class)    base
       (string? class) (into base (remove str/blank? (str/split class #"\s+")))
       (coll? class)   (into base class)
       :else           (conj base class))))
