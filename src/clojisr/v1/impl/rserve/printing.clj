(ns clojisr.v1.impl.rserve.printing
  (:require [clojisr.v1.protocols :as prot]
            [clojure.string :as string])
  (:import (org.rosuda.REngine REXP)))

(defn print-to-string
  [session r-obj]
  (->> (.asStrings ^REXP (->> (:object-name r-obj)
                              (format "capture.output(print(%s))")
                              (prot/eval-r->java session)))       
       (string/join "\n")))
