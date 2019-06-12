(ns clojuress.renjin.stats-test
  (:require [clojuress.renjin.core :refer [reval function->fn ->clj ->renjin]]
            [clojuress.renjin.stats :as stats]))

(reval "median")

(reval "function(x) median(x)")

((function->fn "function(x) median(x)")
 {:x [9]})

((function->fn "median")
 {:x [9]})

(stats/median {:x (range 9)
               :na.rm false})
