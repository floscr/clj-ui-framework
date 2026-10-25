(ns spike.hello)

(defn greet [n] (str "hi " n))

(js/console.log (greet "nsh") (pr-str {:a [1 2 3]}))
