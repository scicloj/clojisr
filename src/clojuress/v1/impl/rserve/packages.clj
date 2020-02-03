(ns clojuress.v1.impl.rserve.packages
  (:require [clojuress.v1.impl.common
             :refer [strange-name?]]
            [clojuress.v1.protocols :as prot]))

(defn package-symbol->r-symbols [session package-symbol functions-only?]
  (->> package-symbol
       name
       (format (str "as.character(unlist(ls"
                    (if functions-only? "f")
                    ".str('package:%s')))"))
       (prot/eval-r->java session)
       (prot/java->clj session)
       (remove strange-name?)
       (map symbol)))
