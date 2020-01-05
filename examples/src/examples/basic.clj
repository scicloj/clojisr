(ns examples.basic
  (:require [clojuress.v0.r :as r :refer [r]]
            [clojuress.v0.require :refer [require-r]]
            [tech.ml.dataset :as dataset]
            [notespace.v0.note :as note
             :refer [note note-void note-md note-as-md note-hiccup note-as-hiccup]]))

(note-md "# R interop -- basic examples")

(note-md "Run some R code, and keep a Clojure handle to the return value.")

(note-void
 (def x (r "1+2")))

(note-md "Convert the R object to Java, then to Clojure:")

(note
 (r/r->clj x))

(note-md "Run some code on a separate session (specified Rserve port, rather than the default one).")

(note
 (-> "1+2"
     (r :session-args {:port 4444})
     r/r->java->clj))

(note-md "Define a Clojure function wrapping an R function.")

(note-void
 (def f (r/function (r "function(x) x*10"))))

(note-md "Apply it to Clojure data (implicitly converting that data to R).")

(note
 (-> 5
     f
     r/r->java->clj))

(note-md "Apply it to R data.")

(-> "5*5"
    r
    f
    r/r->java->clj)
;; => [250.0]


(note-md "Create a tech.ml.dataset dataset object,
pass it to an R function to compute the row means,
and convert the return value to Clojure.")

(note
 (let [row-means (-> "function(data) rowMeans(data)"
                     r
                     r/function)]
   (->> {:x [1 2 3]
         :y [4 5 6]}
        dataset/name-values-seq->dataset
        row-means
        r/r->java->clj)))

(note-md "Load the R package 'dplyr' (assuming it is installed).")

(note-void
 (r "library(dplyr)"))

(note-md "Use dplyr to process some Clojure dataset, and convert back to the resulting dataset.")

(note
 (let [filter-by-x  (-> "function(data) filter(data, x>=2)"
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
        r/r->java->clj)))

(note/render-this-ns!)
