(ns clojuress.v1.impl.rserve.printing
  (:require [clojuress.v1.protocols :as prot]
            [clojure.string :as string]
            [clojuress.v1.objects-memory :as mem])
  (:import (org.rosuda.REngine REXP)))

(defn print-to-string [session r-obj]
  (->> r-obj
       :object-name
       mem/object-name->memory-place
       (format "capture.output(print(%s))")
       (prot/eval-r->java session)
       ((fn [^REXP result]
          (.asStrings result)))
       (string/join "\n")))
