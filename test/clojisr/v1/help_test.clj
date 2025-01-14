(ns clojisr.v1.help-test
  (:require 
   [clojure.string :as str] 
   [clojure.test :refer [is deftest]]
   [clojisr.v1.r  :as r]))

(r/require-r '[stats])
(Thread/sleep 5000)


(deftest help-docstring
  (r/require-r '[stats])
  (Thread/sleep 5000)
  (is (str/starts-with? 
       (:doc (meta (var r.stats/lm)))
       "Fitting Linear")))
  
(deftest help-function 
  (is (str/starts-with? 
       (r/help "lm" "stats")
       "Fitting Linear")))

(deftest require-defauls-should-not-throws-exception

  ; should not crash
  (r/require-r '[base])
  (r/require-r '[stats])
  (r/require-r '[utils])
  (r/require-r '[graphics])
  (r/require-r '[datasets]))



