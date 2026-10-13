(ns dev.examples.markdown
  (:require [dev.example :refer [example]]
            [ui.markdown :as markdown]))

(def ^:private sample
  "## Release notes

Tabs now work **without JavaScript**: hidden radios drive the panels.

- Boxed and line variants
- Arrow keys move between tabs
- Same markup on every target

```clojure
(tabs/tabs {:tabs [{:id \"a\" :label \"Account\" :content [:p \"...\"]}]})
```

| Target | Interactive |
| --- | --- |
| Hiccup | yes |
| Squint | yes |")

(def examples
  [(example :render "Rendered markdown"
     (fn []
       (into [:div {:class "md-docs"}] (markdown/markdown->hiccup sample))))])
