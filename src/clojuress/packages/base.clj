(ns clojuress.packages.base
  (:require [clojuress.core :refer [add-functions-to-this-ns]]))

(add-functions-to-this-ns
 'base
 '[mean class dim])

