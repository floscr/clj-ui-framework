(ns spike.render
  "Interprets Replicant-style hiccup (what the :cljs branch of the ui.*
   components returns) into NSH native elements, through the runtime module
   NSH's own JSX compiler emits calls to (nsh:rt)."
  (:require ["nsh:rt" :refer [element prop insert append]]
            [clojure.string :as str]))

;; Framework class tokens -> Tailwind classes NSH resolves natively.
;; Unknown tokens pass through, so plain Tailwind can be mixed in.
(def class->tw
  {"btn"            "flex-row items-center justify-center gap-2 rounded-lg px-4 h-11 self-start active:opacity-80 disabled:opacity-40"
   "btn-primary"    "bg-violet-600 text-white active:bg-violet-700"
   "btn-secondary"  "bg-zinc-100 text-zinc-900 border border-zinc-300 active:bg-zinc-200"
   "btn-ghost"      "bg-transparent text-zinc-900 active:bg-zinc-100"
   "btn-danger"     "bg-red-600 text-white active:bg-red-700"
   "btn-success"    "bg-green-600 text-white active:bg-green-700"
   "btn-link"       "bg-transparent text-violet-600"
   "btn-sm"         "h-9 px-3 text-sm"
   "btn-lg"         "h-12 px-5 text-lg"
   "btn-icon"       "w-11 px-0"
   "btn-icon-round" "rounded-full"
   "btn-loading"    "opacity-70"
   "spinner"        "w-4 h-4 rounded-full border-2 border-white/40 border-t-white animate-spin"
   "spinner-xs"     "w-3 h-3"
   "spinner-sm"     "w-4 h-4"
   "spinner-md"     "w-5 h-5"})

(defn- class-str [c]
  (cond (nil? c) nil
        (string? c) c
        (sequential? c) (str/join " " (remove nil? c))
        :else (str c)))

(defn- tailwind [classes]
  (->> (str/split (or classes "") #"\s+")
       (remove str/blank?)
       (map #(get class->tw % %))
       (str/join " ")))

(defn- text-node [s]
  (let [t (element "text")]
    (insert t (fn [] s))
    t))

(defn- flatten-children [children]
  (mapcat (fn [c] (cond (nil? c) []
                        (seq? c) (flatten-children c)
                        :else [c]))
          children))

(defn- parse-tag [tag]
  (let [[t & cls] (str/split (name tag) #"\.")]
    [(first (str/split t #"#")) (vec cls)]))

(def pressable? #{"button" "a"})
(def skipped #{"svg" "path" "circle" "rect" "line" "polyline" "polygon" "g" "use"})

(declare render)

(defn- render-children! [parent children]
  (doseq [c (flatten-children children)]
    (when-let [n (render c)]
      (append parent n))))

(defn render
  "Hiccup -> NSH node (or nil)."
  [h]
  (cond
    (nil? h) nil
    (string? h) (text-node h)
    (number? h) (text-node (str h))
    (seq? h) (let [v (element "view")] (render-children! v h) v)
    (vector? h)
    (let [[tag & more] h]
      (if (fn? tag)
        (render (apply tag more))
        (let [attrs (when (map? (first more)) (first more))
              children (if attrs (rest more) more)
              [t short-cls] (parse-tag tag)]
          (when-not (skipped t)
            (let [v (element (if (= t "input") "input" "view"))
                  cls (tailwind (str/join " " (remove nil? [(str/join " " short-cls)
                                                               (class-str (:class attrs))])))]
              (when-not (str/blank? cls) (prop v "class" cls))
              (when-let [st (:style attrs)] (prop v "style" (clj->js st)))
              (when (:disabled attrs) (prop v "disabled" true))
              (when (pressable? t) (prop v "role" "button"))
              (when-let [f (get-in attrs [:on :click])] (prop v "onPress" (fn [] (f nil))))
              (when-let [id (:id attrs)] (prop v "testID" (str id)))
              (render-children! v children)
              v)))))
    :else (text-node (str h))))
