(ns clojuress.v0.inspection
  (:require [clojuress.v0.codegen :as codegen]
            [clojuress.v0.using-sessions :as using-sessions]))

(defn r-class [r-object]
  (vec
   (using-sessions/r-function-on-obj
    r-object "class" :strings)))

(defn names [r-object]
  (vec
   (using-sessions/r-function-on-obj
    r-object "names" :strings)))

(defn shape [r-object]
  (vec
   (using-sessions/r-function-on-obj
    r-object "dim" :ints)))

