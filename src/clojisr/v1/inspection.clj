(ns clojisr.v1.inspection
  (:require [clojisr.v1.using-sessions :as using-sessions]))

(defn r-class [r-object]
  (:class r-object))

(defn names [r-object]
  (vec
   (using-sessions/r-function-on-obj
    r-object "names" :strings)))

(defn shape [r-object]
  (vec
   (using-sessions/r-function-on-obj
    r-object "dim" :ints)))

