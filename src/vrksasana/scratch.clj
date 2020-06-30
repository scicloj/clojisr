(ns vrksasana.scratch
  (:require [vrksasana.core :as vrksa]
            [vrksasana.impl.rserve.core :as rserve]
            [clojure.tools.namespace.repl :as ctnr]))

(comment
  (ctnr/refresh-all)
  )

(vrksa/init)

(rserve/init :make-default true)

(let [x (vrksa/plant '(+ 1 ~(range 9)))]
  (vrksa/pick x))

(let [x (vrksa/plant '(+ 1 ~(range 9)))
      y (vrksa/plant `(* ~x 10))]
  (vrksa/pick y))

(let [x (vrksa/plant '(+ 1 ~(range 9)))
      y (vrksa/plant `(* ~x 10))]
  (->> y
      vrksa/pick
      vrksa/fruit->data
      (map inc)
      vrksa/data->fruit))

