(ns clojisr.v1.help-test
  (:require 
   [clojure.string :as str] 
   [clojure.test :refer [is deftest]]
   [clojisr.v1.r  :as r]))


(deftest help-docstring
  (r/require-r '[stats :generate-doc-strings? true])
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

  ; should not crash
  (r/require-r '[base :generate-doc-strings? true])
  (r/require-r '[stats])
  (r/require-r '[utils])
  (r/require-r '[graphics])
  (r/require-r '[datasets]))



