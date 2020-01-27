(ns clojuress.v1.impl.common)

(defn strange-name?
  "Is a given name invalid for a Clojure symbol or keyword?"
  [aname]
  (re-matches #"[\Q[](){}#@;:,\/`^'~\"\E].*" aname))
