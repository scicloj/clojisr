(ns clojisr.v1.impl.rserve.packages
  (:require [clojisr.v1.protocols :as prot]))

(defn package-symbol->r-symbol-names [session package-symbol]
  (->> package-symbol
       name
       (format (str "as.character(unlist(ls.str('package:%s')))"))
       (prot/eval-r->java session)
       (prot/java->clj session)))
