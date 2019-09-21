(ns basic
  (:require [clojuress :as r :refer [r]]
            [tech.ml.dataset :as dataset]))

;; Run some R code, and keep a Clojure handle to the return value.

(def x (r "1+2"))

;; Convert the R object to Java, then to Clojure:

(r/r->java->clj x)
;; => [3.0]

;; Define a Clojure function wrapping an R function.

(def f (r/function (r "function(x) x*10")))

;; Apply it to Clojure data (implicitly converting that data to R).

(-> 5
    f
    r/r->java->clj)
;; => [50.0]

;; Apply it to R data.

(-> "5*5"
    r
    f
    r/r->java->clj)
;; => [250.0]

;; Fdgg


;; Create a tech.ml.dataset dataset object,
;; pass it to an R function to compute the row means,
;; and convert the return value to Clojure.

(let [row-means (-> "function(data) rowMeans(data)"
                    r
                    r/function)]
  (->> {:x [1 2 3]
        :y [4 5 6]}
       dataset/name-values-seq->dataset
       row-means
       r/r->java->clj))
;; => [2.5 3.5 4.5]

;; Load the R package 'dplyr' (assuming it is installed).

(r "library(dplyr)")

;; Use dplyr to process some Clojure dataset,
;; and convert back to the resulting dataset.

(let [filter-by-x (-> "function(data) filter(data, x>=2)"
                          r
                          r/function)
      add-z-column (-> "function(data) mutate(data, z=x+y)"
                       r
                       r/function)]
  (->> {:x [1 2 3]
        :y [4 5 6]}
       dataset/name-values-seq->dataset
       filter-by-x
       add-z-column
       r/r->java->clj))
;; =>
;; _unnamed [2 3]:
;;
;; |    :x |    :y |    :z |
;; |-------+-------+-------|
;; | 2.000 | 5.000 | 7.000 |
;; | 3.000 | 6.000 | 9.000 |


