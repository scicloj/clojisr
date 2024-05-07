(ns clojisr.v1.r-test
  (:require [clojisr.v1.r :as r]

            [clojisr.v1.require :as require-r]
            [clojure.test :refer [is deftest] :as t])) 

(require-r/require-r '[datasets])


(def v [1 2 3])
 

(deftest bras
  (is (= [1]
         (-> (r/bra v 1) r/r->clj)))
  (is (= [1]
         (-> (r/brabra v 1) r/r->clj))))


(deftest binaries 
  (is (= [true false false false true true true true true true 0 2 1 1.0 1]
         
         (map 
          (fn [f]
            (first (r/r->clj (f 1 1))))
          
          [r/r== 
           r/r!= 
           r/r<
           r/r>
           r/r<=
           r/r>=
           r/r&
           r/r&&
           r/r|
           r/r||
           r/r-
           r/r+
           r/r*
           r/rdiv
           r/colon
           ]))))

(deftest unary 
  (is (not
       (first (r/r->clj (r/r! true))))))

(deftest bra-colon
  (is  (= [21.0 22.8 21.4]
          (-> r.datasets/mtcars
              (r/r$  "mpg")
              (r/bra (r/colon 2 4)) 
              (r/r->clj)
              ))))
