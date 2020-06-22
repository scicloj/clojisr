(ns clojisr.v1.impl.rserve.rexp
  "SEXP enhancements"
  (:require [clojisr.v1.impl.protocols :as prot]
            [clojisr.v1.impl.common :refer [->seq-with-missing ->column usually-keyword java->column]])
  (:import (org.rosuda.REngine REXP REXPDouble REXPInteger REXPLogical REXPString REXPFactor REXPSymbol REXPNull
                               REXPUnknown REXPGenericVector REXPList
                               RFactor RList)))

;;

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(extend-type Object
  prot/RProto
  (inherits? [_ _] false)
  prot/Clojable
  (->clj [obj] obj)
  (->native [obj] obj))

(extend-type REXP
  prot/RProto
  (attribute [exp attr] (some-> (.getAttribute ^REXP exp ^String attr) prot/->clj))
  (attribute-names [exp] (seq (.keys (.asList (._attr ^REXP exp)))))
  (inherits? [exp clss] (.inherits ^REXP exp ^String clss))
  (na? [exp] (.isNA ^REXP exp)))

(defmacro emit-rexp-extensions
  ([clss access-fn datatype] `(emit-rexp-extensions ~clss ~access-fn ~access-fn ~datatype))
  ([clss access-fn native-fn datatype]
   (let [exp (with-meta 'exp {:tag clss})]
     `(extend-type ~clss
        prot/Clojable
        (->clj [~exp] (->seq-with-missing (~access-fn ~exp) (.isNA ~exp)))
        (->native [~exp] (~native-fn ~exp))
        prot/DatasetProto
        (->column [~exp name#]
          (->column (~access-fn ~exp) name# ~datatype (.isNA ~exp)))))))

(emit-rexp-extensions REXPDouble .asDoubles :float64)
(emit-rexp-extensions REXPInteger .asIntegers :int64)
(emit-rexp-extensions REXPLogical .isTRUE :boolean)
(emit-rexp-extensions REXPString .asStrings :string)

(defn- map-maybe-keyword
  [^REXPFactor exp]
  (mapv #(or (keyword %) %) (.asStrings exp)))

(defn- ->rfactor
  [^REXPFactor exp]
  (let [^RFactor factor (.asFactor exp)]
    (reify
      clojure.lang.Seqable
      (seq [_] (seq (.asStrings factor)))
      clojure.lang.IFn
      (invoke [_ id] (.levelAtIndex factor id))
      prot/FactorProto
      (levels [_] (seq (.levels factor)))
      (indexes [_] (seq (.asIntegers factor)))
      (strings [_] (seq (.asStrings factor)))
      (counts [_] (zipmap (.levels factor)
                          (map #(.count factor ^String %) (.levels factor)))))))

(emit-rexp-extensions REXPFactor map-maybe-keyword ->rfactor :keyword)

(extend-type REXPSymbol
  prot/Clojable
  (->clj [exp] (symbol (.asNativeJavaObject ^REXPSymbol exp)))
  (->native [exp] (.asNativeJavaObject ^REXPSymbol exp)))

(extend-type REXPNull
  prot/Clojable
  (->clj [_] nil)
  (->native [_] nil))

(extend-type REXPUnknown
  prot/Clojable
  (->clj [_] nil)
  (->native [exp] (.getType ^REXPUnknown exp)))

;; list and generic vector

(defn- ->valid-names
  [^RList rlist]
  (map-indexed (fn [^long id k]
                 (if (empty? k) [[(inc id)]] (usually-keyword k))) (.keys rlist)))

(defn- list->map-or-vector
  [^REXP exp converter]
  (let [^RList rlist (.asList exp)]
    (if-not (.isNamed rlist)
      (mapv converter (.values rlist))
      (let [names (->valid-names rlist)
            values (map converter (.values rlist))]
        (->> values
             (interleave names)
             (apply array-map))))))

(defn- list->columns
  [^REXP exp]
  (let [^RList rlist (.asList exp)]
    (map java->column (.values rlist) (if-not (.isNamed rlist)
                                        (range)
                                        (->valid-names rlist)))))


(extend-type REXPGenericVector
  prot/Clojable
  (->clj [exp] (list->map-or-vector exp prot/->clj))
  (->native [exp] (list->map-or-vector exp prot/->native))
  prot/DatasetProto
  (->columns [exp] (list->columns exp)))

(extend-type REXPList
  prot/Clojable
  (->clj [exp] (list->map-or-vector exp prot/->clj))
  (->native [exp] (list->map-or-vector exp prot/->native))
  prot/DatasetProto
  (->columns [exp] (list->columns exp)))
