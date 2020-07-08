(ns clojisr.v1.impl.renjin.session
  (:require [clojisr.v1.protocols :as prot]
            [clojisr.v1.impl.renjin.engine :as engine]
            [clojisr.v1.impl.renjin.call :as call]
            [clojisr.v1.impl.renjin.packages :as packages]
            [clojisr.v1.impl.renjin.printing :as printing]
            [clojisr.v1.impl.protocols :as iprot]
            [clojisr.v1.impl.renjin.sexp :as sexp])
  (:import [org.renjin.script RenjinScriptEngine]))

(set! *warn-on-reflection* true)

(defrecord RenjinSession [id session-args engine closed]
  prot/Session
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
    (call/->r-set engine varname java-obj))
  (print-to-string [session r-obj]
    (printing/print-to-string session r-obj))
  (package-symbol->r-symbol-names [session package-symbol]
    (packages/package-symbol->r-symbol-names
     session package-symbol))

  iprot/Engine
  (->nil [_] (sexp/->sexp-nil))
  (->symbol [_ x] (sexp/->sexp-symbol x))
  (->string-vector [_ xs] (sexp/->sexp-strings xs))
  (->numeric-vector [_ xs] (sexp/->sexp-doubles xs))
  (->integer-vector [_ xs] (sexp/->sexp-integers xs))
  (->logical-vector [_ xs] (sexp/->sexp-logical xs))
  (->factor [_ xs] (sexp/->sexp-factor xs))
  (->factor [_ ids levels] (sexp/->sexp-factor ids levels))
  (->list [_ vs] (sexp/->sexp-list vs))
  (->named-list [_ ks vs] (sexp/->sexp-named-list ks vs))
  (native? [_ x] (sexp/sexp? x)))

(defn make [id session-args]
  (->RenjinSession id
                   session-args
                   (engine/->engine)
                   (atom false)))
