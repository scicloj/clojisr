(ns clojisr.v1.help-test
  (:require 
   [clojure.string :as str] 
   [clojure.test :refer [is deftest]]
   [clojisr.v1.r  :as r]))


(r/require-r '[randomForest])

(deftest help-with-doctrsing
  (reset! clojisr.v1.require/attach-help-as-docstring-to-vars true)
  (r/require-r '[randomForest])
  (is (str/starts-with? 
       (:doc (meta (var r.randomForest/randomForest)))
       "Classification and Regression with Random Forest")))


(deftest help-without-doctrsing
  (reset! clojisr.v1.require/attach-help-as-docstring-to-vars false)
  (r/require-r '[randomForest])
  (is (nil?
       (:doc (meta (var r.randomForest/randomForest))))
      ))

(deftest help-function 
  (is (str/starts-with? 
       (r/help "randomForest" "randomForest")
       "Classification and Regression with Random Forest"
                        )))