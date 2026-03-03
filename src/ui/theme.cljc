(ns ui.theme)

(defn css-var
  "Reference a CSS variable by token keyword.
   (css-var :accent) => \"var(--accent)\""
  [token]
  (str "var(--" (name token) ")"))
