(ns clojisr.v1.impl.renjin.printing
  (:require [clojisr.v1.protocols :as prot]))

(set! *warn-on-reflection* true)

(defn print-to-string [session r-obj]
  (->> r-obj
       :object-name
       (format "print(%s)")
       (prot/eval-r->java session)
       with-out-str))
