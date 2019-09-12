(ns clojuress.packages.base
  (:require [clojuress :refer [add-functions-to-this-ns]]))

(add-functions-to-this-ns
 'base
 '[mean cbind rbind names class dim])

