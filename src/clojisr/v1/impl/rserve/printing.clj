(ns clojisr.v1.impl.rserve.printing
  (:require [clojisr.v1.protocols :as prot]
            [clojure.string :as string])
  (:import (org.rosuda.REngine REXP)))

(defn print-to-string
  [session r-obj]
  (let [^REXP output (->> (:object-name r-obj)
                          (format "capture.output(print(%s))")
                          (prot/eval-r->java session))]
    (->> (.asStrings output)       
         (string/join "\n"))))
