(ns clojisr.v1.help-test
  (:require 
   [clojure.string :as str] 
   [clojure.test :refer [is deftest]]
   [clojisr.v1.r  :as r]))


(deftest help-docstring
  (r/require-r '[stats :docstrings? true])
  (is (str/starts-with? 
       (->
        (ns-publics 'r.stats)
        (get 'lm)
        meta
        :doc)
       
       "Fitting Linear")))

(deftest help-function 
  (is (str/starts-with? 
       (r/help "lm" "stats")
       "Fitting Linear")))

(deftest require-defauls-should-not-throws-exception
  (r/require-r '[base :docstrings? true])
  (r/require-r '[stats])
  (r/require-r '[utils])
  (r/require-r '[graphics])
  (r/require-r '[datasets]))



