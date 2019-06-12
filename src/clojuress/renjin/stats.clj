(ns clojuress.renjin.stats
  (:require [clojuress.renjin.engine :refer [reval]]
            [clojuress.renjin.to-renjin :refer [->renjin]]
            [clojuress.renjin.to-clj :refer [->clj]]
            [clojuress.renjin.core :refer [function->fn]]
            [clojure.string :as string])
  (:import (org.renjin.sexp Symbol Closure Environment Environment$Builder)
           (org.renjin.eval Context)
           (org.renjin.parser RParser)))


(defn kmeans [argsmap]
  ((function->fn
    "function(x, centers, iter.max, nstart, algorithm) kmeans(x=x, centers=centers, iter.max=iter.max, nstart=nstart, algorithm='Hartigan-Wong')")
   (merge {:iter.max  10.0
           :nstart    1.0
           :algorithm "Hartigan-Wong"}
          argsmap )))
