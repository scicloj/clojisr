(ns vrksasana.scratch
  (:require [vrksasana.core :as vrksa :refer [restart pick plant fruit->data data->fruit]]
            [vrksasana.impl.r.core :as r]
            [vrksasana.season :as season]
            [vrksasana.fruit :as fruit]))

(restart r/ground)

(data->fruit (range 99))

(let [x (plant (range 99))]
  (pick x))

(let [x (plant '(+ 1 ~(range 99)))]
  (pick x))

(let [x (plant '(+ 1 ~(range 99)))
      y (plant `(* ~x 10))]
  (pick y))

(let [x (plant '(+ 1 ~(range 99)))
      y (plant `(* ~x 10))]
  (->> y
       pick
       fruit->data))

(let [x (plant '(+ 1 ~(range 99)))
      y (plant `(* ~x 10))]
  (->> y
       pick
       fruit->data
       (map inc)
       data->fruit))

(let [x (plant '(+ 1 ~(range 99)))]
  (restart r/ground)
  (pick x))

(let [x (plant '(+ 1 ~(range 99)))
      x$ (pick x)
      y (plant `(* ~x$ 10))
      y$ (pick y)]
  [x$ y$])

(let [x1 (plant '(+ 1 ~(range 99)))
      x2 (plant '(+ 2 ~(range 99)))
      s1 (season/get-or-make r/ground :s1)
      s2 (season/get-or-make r/ground :s2)]
  [(pick x1 {:season s1})
   (pick x2 {:season s2})])
