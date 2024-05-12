(ns clojisr.v1.matrix-test
  (:require 
   [clojure.test :refer [deftest is]]
   [clojisr.v1.r :as r]))

(r/require-r '[base])

(deftest options-are-optional-without
  
  (is (= (range 5)
         (->
          (r.base/matrix (range 5))
          (r/r->clj)
          (get 1)))))


(deftest options-are-optional-wit
  ;; does not crash
  (is (= (range 5)
         (->
          (r.base/matrix (range 5))
          (r/r->clj {:some-option true})
          (get 1)))))

