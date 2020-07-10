(ns clojisr.v1.renjin-test
  (:require [notespace.v2.note :as note
             :refer [note note-void note-md note-as-hiccup]]))

(note-md "# Renjin interop")

(note-md "[Renjin](https://www.renjin.org/) is an implementation of the R language for the JVM. It supports the whole of the base R language, as well many R packages. For most packages, Renjin's support is partial and some tests fail. See the details [here](https://packages.renjin.org/).")

(note-md "Clojisr has now basic, experimental, support for Renjin as a backend. This tutorial shows some example usage.")

(note-void :setup)

(note-md "## Setup")

(note-void
 (require '[clojisr.v1.r :as r :refer [r eval-r->java r->java java->r java->clj clj->java r->clj clj->r ->code r+ colon
                                       require-r]]
          '[tech.ml.dataset :as dataset]
          '[clojisr.v1.applications.plotting :refer [plot->svg]]))

(note-md "If we `require`d `clojisr.v1.renjin` first, then the default session-type would be `:renjin`. But since we might be loading this namespace after doing some other things, let us make sure that we are using `:renjin`:")

(note-void
 (r/set-default-session-type! :renjin)
 (r/discard-all-sessions))

(note-void :basic-examples)

(note-md "## Basic examples")

(note
 (require-r '[base]
            '[stats]))

(note
 (r '(+ 1 2)))

(note
 (r.stats/median [1 2 4]))

(note-md "From plain clojure data to an R dataframe:")

(note
 (-> {:x [1 2 3]
      :y [4 5 6]}
     r.base/data-frame))

(note
 (-> {:x [1 2 3]
      :y [4 5 6]}
     r.base/data-frame
     r.base/rowMeans))

(note-md "From a tech.ml.dataset dataset to an R dataframe:")

(note
 (-> {:x [1 2 3]
      :y [4 5 6]}
     dataset/name-values-seq->dataset
     r.base/data-frame))

(note
 (-> {:x [1 2 3]
      :y [4 5 6]}
     dataset/name-values-seq->dataset
     r.base/data-frame
     r.base/rowMeans))

(note-void :linear-regression)
(note-md "## Linear regression")

(note
 (let [xs     (repeatedly 99 rand)
       noises (repeatedly 99 rand)
       ys     (map (fn [x noise]
                     (+ (* x -3)
                        2
                        noise))
                   xs
                   noises)
       df     (r.base/data-frame
               :x xs
               :y ys)
       fit    (r.stats/lm '(formula y x)
                          :data df)]
   (r.base/summary fit)))


(note-void :plotting)
(note-md "## Plotting")

(note-void
 (require-r '[graphics]))

(note-as-hiccup
 (plot->svg
  (fn []
    (->> (repeatedly 999 rand)
         (map (fn [x] (* x x)))
         (r.graphics/hist
          :main "histogram"
          :xlab "x"
          :bins 100)))))

(comment (notespace.v2.note/compute-this-notespace!))
