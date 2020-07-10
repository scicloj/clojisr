(ns vrksasana.scratch
  (:require [vrksasana.core :as vrksa]
            [vrksasana.impl.r.core :as r]
            [vrksasana.catalog :as catalog]
            [vrksasana.season :as season]
            [vrksasana.fruit :as fruit]))

(vrksa/restart)

(vrksa/setup-ground r/ground)

(catalog/current-season-name r/ground)

(vrksa/season-to-use {})

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

