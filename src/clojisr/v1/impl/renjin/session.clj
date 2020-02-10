(ns clojisr.v1.impl.renjin.session
  (:require [clojisr.v1.protocols :as prot]
            [clojisr.v1.impl.renjin.java-to-clj :as java-to-clj]
            [clojisr.v1.impl.renjin.clj-to-java :as clj-to-java]
            [clojisr.v1.impl.renjin.engine :as engine]
            [clojisr.v1.impl.renjin.lang :as lang]
            [clojisr.v1.impl.renjin.packages :as packages]
            [clojisr.v1.impl.renjin.printing :as printing]
            [cambium.core :as log])
  (:import (org.renjin.sexp SEXP)
           (org.renjin.script RenjinScriptEngine RenjinScriptEngineFactory)
           clojisr.v1.protocols.Session))



(defrecord RenjinSession [id session-args engine closed]
  Session
  (close [session]
    (reset! closed true))
  (closed? [session]
    @closed)
  (id [session]
    id)
  (session-args [session]
    session-args)
  (desc [session]
    session-args)
  (eval-r->java [session code]
    (.eval ^RenjinScriptEngine engine ^String code))
  (java->r-set [session varname java-obj]
    (lang/->r-set engine varname java-obj))
  (java->specified-type [session java-obj typ]
    (java-to-clj/java->specified-type java-obj typ))
  (java->naive-clj [session java-obj]
    (java-to-clj/java->naive-clj engine java-obj))
  (java->clj [session java-obj]
    (java-to-clj/java->clj java-obj))
  (clj->java [session clj-obj]
    (clj-to-java/clj->java engine clj-obj))
  (print-to-string [session r-obj]
    (printing/print-to-string session r-obj))
  (package-symbol->r-symbol-names [session package-symbol]
    (packages/package-symbol->r-symbol-names
     session package-symbol)))

(defn make [id session-args]
  (->RenjinSession id
                   session-args
                   (engine/->engine)
                   (atom false)))
