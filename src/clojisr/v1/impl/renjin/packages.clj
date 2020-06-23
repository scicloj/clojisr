(ns clojisr.v1.impl.renjin.packages
  (:require [clojisr.v1.protocols :as prot]
            [clojisr.v1.impl.renjin.engine :as engine])
  (:import (org.renjin.sexp ListVector$NameValuePair)
           (org.renjin.eval Context)
           (org.renjin.primitives.packaging NamespaceRegistry ClasspathPackageLoader ClasspathPackage)))

(defn package-symbol->r-symbol-names [session package-symbol]
  (if (= package-symbol 'base)
    (->> session
         :engine
         ^Context
         engine/runtime-context
         (.getBaseEnvironment)
         (.getNames)
         (prot/java->clj session))
    ;; else
    (let [ctx (-> session
                  :engine
                  engine/runtime-context)]
      (-> ^Context
          ctx
          ^NamespaceRegistry
          (.getNamespaceRegistry)
          ^ClasspathPackageLoader
          (.getPackageLoader)
          (.load (name package-symbol))
          ^ClasspathPackage
          (.get)
          (.loadSymbols ctx)
          (->> (map (fn [^ListVector$NameValuePair nvp]
                      (.getName nvp))))))))


