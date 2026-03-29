(ns ui.macros)

(defmacro inline-file
  "Read a file at compile time and return its contents as a string.
   For use in ClojureScript (shadow-cljs) to embed file content."
  [path]
  (slurp path))
