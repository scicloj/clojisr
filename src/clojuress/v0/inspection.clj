(ns clojuress.v0.inspection
  (:require [clojuress.v0.codegen :as codegen]
            [clojuress.v0.using-sessions :as using-sessions]))

(defn r-class [r-object]
  (vec
   (using-sessions/r-function-on-obj
    r-object "class" :strings)))

(comment
  (let [s (clojuress.v0.using-sessions/fetch-or-make-and-init {})]
    (using-sessions/init-session-memory s)
    (-> "1+2"
        (using-sessions/eval-r s)
        (r-class))))

(defn names [r-object]
  (vec
   (using-sessions/r-function-on-obj
    r-object "names" :strings)))

(comment
  (let [s (clojuress.v0.using-sessions/fetch-or-make-and-init {})]
    (using-sessions/init-session-memory s)
    (-> "list(a=1,b=2)"
        (using-sessions/eval-r s)
        names)))

(defn shape [r-object]
  (vec
   (using-sessions/r-function-on-obj
    r-object "dim" :ints)))

(comment
  (let [s (clojuress.v0.using-sessions/fetch-or-make-and-init {})]
    (using-sessions/init-session-memory s)
    (-> "matrix(1:6,3,2)"
        (using-sessions/eval-r s)
        shape)))

