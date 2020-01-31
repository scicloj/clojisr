(ns clojuress.v1.impl.common)

(defn strange-name?
  "Is a given name invalid for a Clojure symbol or keyword?"
  [aname]
  (re-matches #"[\Q[](){}#@;:,\/`^'~\"\E].*" aname))

(defn usually-keyword
  "Given a name in an R named list, turn it into a keyword unless it contains strange characters, but turn it into a string if it does."
  [aname]
  (if (strange-name? (name aname))
    (name aname)
    (keyword aname)))

