(ns clojuress.v1.impl.renjin.packages
  (:require [clojuress.v1.protocols :as prot]
            [clojuress.v1.impl.renjin.engine :as engine]
            [clojuress.v1.session :as session])
  (:import (org.renjin.sexp DynamicEnvironment ListVector$NameValuePair)
           (org.renjin.eval Context)
           (org.renjin.primitives.packaging NamespaceRegistry ClasspathPackageLoader ClasspathPackage)))

(->> nil
     session/fetch-or-make
     :engine
     ^Context
     engine/runtime-context
     (.getBaseEnvironment)
     ;; (.getNames)
     ;; (prot/java->clj session)
     )

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


