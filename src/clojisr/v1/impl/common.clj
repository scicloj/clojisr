(ns clojisr.v1.impl.common)

(defn strange-name?
  "Is a given name invalid for a Clojure symbol or keyword?"
  [aname]
  (or (re-matches #"[\Q[](){}#@;:,\/`^'~\"\E].*" aname)
      (-> aname symbol name (not= aname))))

(comment
  (-> "%/%" symbol name)
;; => "%"
  (strange-name? "%/%")
  ;; => true
)

(defn strange-symbol-name?
  [aname]
  (strange-name? aname))


(defn strange-keyword-name? [aname]
  (strange-name? aname))

(defn usually-keyword
  "Given a name in an R named list, turn it into a keyword unless it contains strange characters, but turn it into a string if it does."
  [aname]
  (let [aname-string (name aname)]
    (if (strange-keyword-name? aname-string)
      aname-string
      (keyword aname))))
