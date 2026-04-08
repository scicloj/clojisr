(ns clojisr.v1.help-test
  (:require [clojure.string :as str] 
            [clojure.test :as t]
            [clojisr.v1.r :as r]))

(t/deftest help-docstring
  (r/require '[stats :docstrings? true])
  (t/is (str/starts-with? (-> (ns-publics 'r.stats) (get 'lm) meta :doc) "Fitting Linear")))

(t/deftest help-function 
  (t/is (str/starts-with? 
         (r/help "lm" "stats")
         "Fitting Linear")))
