(ns clojisr.v1.r-test
  (:require [clojisr.v1.r :as r]
            [clojure.test :refer [is deftest] :as t])) 


(def v [1 2 3])
 
(deftest binaries
 (is true
   (first
    (r/r->clj
     (r/r== 1 1)))))



(deftest bras
  (is (= [1]
         (-> (r/brabra v 1) r/r->clj))))


(deftest binaries 
  (is (= [true false false false true true true true true true]
         
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
           ]))))

    ;r/r!
    ;r/r$

(first (r/r->clj (r/r! true)))
r/r
r/r+
r/r*
r/r==
