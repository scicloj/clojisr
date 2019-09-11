(ns clojuress.packages.stats
  (:require [clojuress :refer [add-functions-to-this-ns]]))

(add-functions-to-this-ns
 'stats
 '[median rnorm])
