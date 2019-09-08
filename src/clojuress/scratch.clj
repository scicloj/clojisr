(ns clojuress.scratch
  (:require [clojuress.core :as r :refer [r]]
            [clojuress.packages.base :as base]
            [clojuress.packages.stats :as stats])
  (:import (org.rosuda.REngine REXP REXPSymbol REXPDouble REXPInteger)
           (org.rosuda.REngine.Rserve RConnection)))



(comment

  (r/init)

  ((r/function (r "mean"))
   [[1 2 3]] {})

  (-> 1000
      ((r/function (r "rnorm")) [] {:mean 5}))

  (base/mean
   [[1 2 3]] {})

  (stats/median
   [[1 2 3]] {})


  (def result1
    (-> "(0:10000000)^2"
        r
        time))

  (->> result1
       r/r->java
       r/java->clj
       (take 9)
       (= (map #(double (* % %)) (range 9)))
       time)

  (->> result1
       r/r->java
       r/java->r
       r/r->java
       r/java->clj
       (take 9)
       (= (map #(double (* % %)) (range 9)))
       time)

  (def result2
    (-> "data.frame(a=1:10000000, b=rnorm(10000000))"
        r
        time))

  (-> result2
      ((juxt r/class
             r/names
             r/shape))
      (= [["data.frame"]
          ["a" "b"]
          [10000000 2]]))

)




