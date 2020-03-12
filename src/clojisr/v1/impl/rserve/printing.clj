(ns clojisr.v1.impl.rserve.printing
  (:require [clojisr.v1.protocols :as prot]
            [clojure.string :as string]
            [clojisr.v1.objects-memory :as mem])
  (:import (org.rosuda.REngine REXP)))

(defn print-to-string [session r-obj]
  (->> r-obj
       :object-name
       (format "capture.output(print(%s))")
       (prot/eval-r->java session)
       ((fn [^REXP result]
          (.asStrings result)))
       (string/join "\n")))
