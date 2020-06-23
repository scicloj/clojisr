(ns clojisr.v1.impl.protocols
  "Internal protocols for backends")

(defprotocol RProto
  "Backend protocols"
  (attribute [exp attr])
  (attribute-names [exp])
  (set-attributes! [exp m])
  (inherits? [exp clss])
  (na? [exp]))

(defprotocol Clojable
  "Convert R Java object to Clojure datastructure"
  (->clj [exp])
  (->native [exp]))

(defprotocol DatasetProto
  "Convert R Java object to tech.ml.dataset structures (Column)"
  (->column [exp name])
  (->columns [exp]))

(defprotocol FactorProto
  (levels [factor])
  (indexes [factor])
  (strings [factor])
  (counts [factor]))

(defprotocol Engine
  "Clojure to native objects converter"
  (->string-vector [session xs])
  (->numeric-vector [session xs])
  (->integer-vector [session xs])
  (->logical-vector [session xs])
  (->factor [session xs] [session ids levels])
  (->data-frame [session xs])
  (->nil [session])
  (->symbol [session x])
  (->list [session vs]) ;; from any vector
  (->named-list [session ks vs]) ;; from map
  (native? [session x]) ;; is native object? (REXP or SEXP)
  )
