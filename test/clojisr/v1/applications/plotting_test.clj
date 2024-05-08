(ns clojisr.v1.applications.plotting-test
  (:require [clojisr.v1.applications.plotting :as plot]
            [clojisr.v1.r :as r]
            [clojure.string :as str]
            [clojure.test :refer [is deftest]]))

(require '[clojisr.v1.applications.plotting :refer [plot->svg plot->file plot->buffered-image]])
(r/require-r '[graphics :refer [plot hist]])

(deftest plot-svg 
  (let [svg
        (plot->svg
         (fn []
           (->> rand
                (repeatedly 30)
                (reductions +)
                (plot :xlab "t"
                      :ylab "y"
                      :type "l"))))]

    (is ( true?
         (str/includes?
          svg
          "M 3.8125 -7.96875 C 3.207031 -7.96875 2.75 -7.664062 2.4375 -7.0625")))))

