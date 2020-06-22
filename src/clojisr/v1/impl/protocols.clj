(ns clojisr.v1.impl.protocols
  "Internal protocols for backends")

(defprotocol RProto
  "Backend protocols"
  (attribute [exp attr])
  (attribute-names [exp])
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
