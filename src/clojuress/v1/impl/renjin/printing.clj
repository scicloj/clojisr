(ns clojuress.v1.impl.renjin.printing
  (:require [clojuress.v1.protocols :as prot]
            [clojuress.v1.objects-memory :as mem]))

(defn print-to-string [session r-obj]
  (->> r-obj
       :object-name
       mem/object-name->memory-place
       (format "print(%s)")
       (prot/eval-r->java session)
       with-out-str))
