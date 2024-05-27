(ns clojisr.v1.help-test
  (:require 
   [clojure.string :as str] 
   [clojure.test :refer [is deftest]]
   [clojisr.v1.r  :as r]))

(r/require-r '[randomForest])

(deftest help-docstring
  (r/require-r '[randomForest])
  (Thread/sleep 30000)
  (is (str/starts-with? 
       (:doc (meta (var r.randomForest/randomForest)))
       "Classification and Regression with Random Forest")))
  
(deftest help-function 
  (is (str/starts-with? 
       (r/help "randomForest" "randomForest")
       "Classification and Regression with Random Forest")))

(deftest require-defauls-should-not-throws-exception 
  
  ; should not crash
  (r/require-r '[base])
  (r/require-r '[stats])
  (r/require-r '[utils])
  (r/require-r '[graphics])
  (r/require-r '[datasets])
  
  )
