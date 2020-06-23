(ns clojisr.v1.impl.rserve.rexp
  "SEXP enhancements"
  (:require [clojisr.v1.impl.protocols :as prot]
            [clojisr.v1.impl.common :refer [->seq-with-missing ->column java->column
                                            valid-list-names]]
            [clojisr.v1.impl.types :as types])
  (:import (org.rosuda.REngine REXP REXPDouble REXPInteger REXPLogical REXPString REXPFactor REXPSymbol REXPNull
                               REXPUnknown REXPGenericVector REXPList
                               RFactor RList)))

;;;;;;;;;;;;;;;;;;;;
;; REXP -> Clojure
;;;;;;;;;;;;;;;;;;;;

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn rexp? [exp] (instance? REXP exp))

(extend-type Object
  prot/RProto
  (inherits? [_ _] false)
  prot/Clojable
  (->clj [obj] obj)
  (->native [obj] obj))

(extend-type REXP
  prot/RProto
  (attribute [exp attr] (some-> (.getAttribute ^REXP exp ^String attr) prot/->clj))
  (set-attributes! [exp m] (do
                             (.putAll (.asList (._attr ^REXP exp)) ^java.util.Map m)
                             exp))
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

(defn- list->map-or-vector
  [^REXP exp converter]
  (let [^RList rlist (.asList exp)]
    (if-not (.isNamed rlist)
      (mapv converter (.values rlist))
      (let [names (valid-list-names (.keys rlist))
            values (map converter (.values rlist))]
        (->> values
             (interleave names)
             (apply array-map))))))

(defn- list->columns
  [^REXP exp]
  (let [^RList rlist (.asList exp)]
    (map java->column (.values rlist) (if-not (.isNamed rlist)
                                        (range)
                                        (valid-list-names (.keys rlist))))))


(extend-type REXPGenericVector
  prot/Clojable
  (->clj [exp] (list->map-or-vector exp prot/->clj))
  (->native [exp] (list->map-or-vector exp prot/->native))
  prot/DatasetProto
  (->column [exp _] (prot/->clj exp)) ;; we do not want to create column here, just make map or vector
  (->columns [exp] (list->columns exp)))

(extend-type REXPList
  prot/Clojable
  (->clj [exp] (list->map-or-vector exp prot/->clj))
  (->native [exp] (list->map-or-vector exp prot/->native))
  prot/DatasetProto
  (->column [exp _] (prot/->clj exp)) ;; same as above
  (->columns [exp] (list->columns exp)))

;;;;;;;;;;;;;;;;;;;;
;; Clojure -> REXP
;;;;;;;;;;;;;;;;;;;;

(defn ->rexp-symbol [x] (REXPSymbol. x))
(defn ->rexp-nil [] (REXPNull.))

;; need to add empty attributes
(defn ->rexp-strings [xs] (REXPString. (types/->strings xs) (REXPList. nil)))
(defn ->rexp-doubles [xs] (REXPDouble. (types/->doubles xs REXPDouble/NA) (REXPList. nil)))
(defn ->rexp-integers [xs] (REXPInteger. (types/->integers xs REXPInteger/NA) (REXPList. nil)))
(defn ->rexp-factor
  ([xs] (REXPFactor. (RFactor. (types/->strings xs))))
  ([ids levels] (REXPFactor. (types/->integers ids REXPInteger/NA)
                             (types/->strings levels))))

(defn ->rexp-logical
  [xs]
  (REXPLogical. (->> xs
                     (map (fn [x]
                            (if (nil? x)
                              REXPLogical/NA
                              (if x
                                REXPLogical/TRUE
                                REXPLogical/FALSE))))
                     (byte-array))
                (REXPList. nil)))

(defn ->rexp-list [^java.util.Collection values]
  (-> (RList. values)
      (REXPGenericVector. (REXPList. nil))))

(defn ->rexp-named-list [^java.util.Collection ks
                         ^java.util.Collection vs]
  (-> (RList. vs ks)
      (REXPGenericVector.))) ;; named list creates attr already 
