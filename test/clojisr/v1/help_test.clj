(ns clojisr.v1.help-test
  (:require 
   [clojure.string :as str] 
   [clojure.test :refer [is deftest]]
   [clojisr.v1.r  :as r]))


(r/require-r '[randomForest])


(deftest help-with-docstring
(reset! clojisr.v1.require/attach-help-as-docstring-to-vars true)  
  (r/require-r '[randomForest])
  (is (str/starts-with? 
       (:doc (meta (var r.randomForest/randomForest)))
       "Classification and Regression with Random Forest"))
  (reset! clojisr.v1.require/attach-help-as-docstring-to-vars false))


(deftest help-without-doctrsing
  (reset! clojisr.v1.require/attach-help-as-docstring-to-vars false)
  (r/require-r '[randomForest])
  (is (nil?
       (:doc (meta (var r.randomForest/randomForest))))
      ))

(deftest help-function 
  (is (str/starts-with? 
       (r/help "randomForest" "randomForest")
       "Classification and Regression with Random Forest")))

(deftest require-defauls-should-not-throws-exception 
  (reset! clojisr.v1.require/attach-help-as-docstring-to-vars true)
  ; should not crash
  (r/require-r '[base])
  (r/require-r '[stats])
  (r/require-r '[utils])
  (r/require-r '[graphics])
  (r/require-r '[datasets])
  (reset! clojisr.v1.require/attach-help-as-docstring-to-vars false)
  )

