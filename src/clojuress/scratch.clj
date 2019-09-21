(ns clojuress.scratch
  (:require [clojuress.packages.base :as base]
            [clojuress.packages.stats :as stats]
            [clojuress.session :as session]
            [clojuress.util :refer [fmap]]
            [clojuress :as r :refer [r]]
            [com.rpl.specter :as specter]
            [clojure.walk :as walk]
            [clojuress.impl.rserve.java-to-clj :as java-to-clj]
            [clojuress.impl.rserve.java :as java]
            [tech.ml.dataset :as dataset]
            [tech.v2.datatype.protocols :as dtype-prot]
            [tech.ml.protocols.dataset :as ds-prot])
  (:import (org.rosuda.REngine REXP REXPSymbol REXPDouble REXPInteger)
           (org.rosuda.REngine.Rserve RConnection)))

(comment

  (r "library(tidyverse)")

  (let [f (r/function (r "function(df) df %>% mutate(z=x+y)"))
        g (r/function (r "function(df) {print(class(df)); df %>% filter(z>5)}"))]
    (->> {:x [1 2 3]
          :y [4 5 6]}
         dataset/name-values-seq->dataset
         f
         g))


  (->> "data.frame(x=c(1:4),y=paste0('hello',1:4),z=(1:4)^2, stringsAsFactors=FALSE)"
       r)

  (->> "with(list(x=1:9, y=(1:9)+rnorm(9)), lm(y~x))"
       r)


  (-> [1 2 3]
      base/mean)

  (-> [1 2 3]
      stats/median)

  (-> [1 nil 3]
      stats/median)

  (-> 1000
      (stats/rnorm [:= :mean 9]))

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
      ((juxt r/r-class
             r/names
             r/shape))
      (= [["data.frame"]
          ["a" "b"]
          [10000000 2]]))

  (session/clean-all)

  (r "1+2" :session-args {:port 5555})

  (r "1+2"  :session-args {:port 6666})


)


