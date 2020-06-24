(ns clojisr.v1.impl.renjin.sexp
  (:require [clojisr.v1.impl.protocols :as prot]
            [clojisr.v1.impl.common :refer [->seq-with-missing ->column java->column valid-list-names]]
            [clojisr.v1.impl.types :as types])
  (:import [org.renjin.sexp SEXP Vector Symbol StringVector StringArrayVector IntVector IntArrayVector
            DoubleVector DoubleArrayVector LogicalArrayVector ListVector PairList Null]
           [org.rosuda.REngine RFactor]))

;;;;;;;;;;;;;;;;;;;;
;; REXP -> Clojure
;;;;;;;;;;;;;;;;;;;;

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn sexp? [exp] (instance? SEXP exp))

(extend-type Vector
  prot/NAProto
  (na? [exp] (map (fn [^long id]
                    (.isElementNA ^Vector exp id)) (range (.length ^SEXP exp)))))

(extend-type SEXP
  prot/RProto
  (attribute [exp attr] (some-> (.getAttribute ^SEXP exp (Symbol/get attr)) prot/->clj))
  (attributes [exp] (prot/->clj (.toVector (.getAttributes ^SEXP exp))))
  (attribute-names [exp] (map str (.names (.getAttributes ^SEXP exp))))
  (set-attributes! [exp m] (reduce (fn [^SEXP e [^String k ^SEXP v]]
                                     (.setAttribute e k v)) exp m))
  (inherits? [exp clss] (.inherits ^SEXP exp ^String clss)))

(extend-type Null
  prot/Clojable
  (->clj [_] nil)
  (->native [_] nil))

(extend-type Symbol
  prot/Clojable
  (->clj [exp] (symbol (.asString ^SEXP exp)))
  (->native [exp] (.asString ^SEXP exp)))

(extend-type StringArrayVector
  prot/Clojable
  (->clj [exp] (vec (.toArray ^StringVector exp)))
  (->native [exp] (.toArray ^StringVector exp))
  prot/DatasetProto
  (->column [exp name]
    (->column (.toArray ^StringVector exp) name :string (prot/na? exp))))

(extend-type IntVector
  prot/Clojable
  (->clj [exp] (->seq-with-missing exp (prot/na? exp)))
  (->native [exp] (.toIntArray ^IntVector exp))
  prot/DatasetProto
  (->column [exp name]
    (->column (.toIntArray ^IntVector exp) name :int64 (prot/na? exp))))

(extend-type DoubleVector
  prot/Clojable
  (->clj [exp] (->seq-with-missing exp (prot/na? exp)))
  (->native [exp] (.toDoubleArray ^DoubleVector exp))
  prot/DatasetProto
  (->column [exp name]
    (->column (.toDoubleArray ^DoubleVector exp) name :float64 (prot/na? exp))))

(defn- logical->seq
  [^LogicalArrayVector exp]
  (map (fn [^long id] (.isElementTrue exp id)) (range (.length exp))))

(extend-type LogicalArrayVector
  prot/Clojable
  (->clj [exp] (->seq-with-missing (logical->seq exp) (prot/na? exp)))
  (->native [exp] (into-array Boolean (map (fn [^long id] (.getElementAsObject ^LogicalArrayVector exp id)) (range (.length ^SEXP exp)))))
  prot/DatasetProto
  (->column [exp name]
    (->column (vec (logical->seq exp)) name :boolean (prot/na? exp))))

(defn- list->map-or-vector
  [^ListVector exp converter]
  (if-not (.hasNames exp)
    (mapv converter exp)
    (let [names (valid-list-names (seq (.getNames exp)))
          values (map converter exp)]
      (->> values
           (interleave names)
           (apply array-map)))))

(defn- list->columns
  [^ListVector exp]
  (map java->column exp (if-not (.hasNames exp)
                          (range)
                          (valid-list-names (seq (.getNames exp))))))

(extend-type ListVector
  prot/Clojable
  (->clj [exp] (list->map-or-vector exp prot/->clj))
  (->native [exp] (list->map-or-vector exp prot/->native))
  prot/DatasetProto
  (->column [exp _] (prot/->clj exp))
  (->columns [exp] (list->columns exp)))

(extend-type PairList
  prot/Clojable
  (->clj [exp] (prot/->clj (.toVector ^PairList exp)))
  (->native [exp] (prot/->native (.toVector ^PairList exp)))
  prot/DatasetProto
  (->column [exp n] (prot/->column (.toVector ^PairList exp) n))
  (->columns [exp] (prot/->columns (.toVector ^PairList exp))))

;;;;;;;;;;;;;;;;;;;;
;; Clojure -> SEXP
;;;;;;;;;;;;;;;;;;;;

(defn ->sexp-symbol [x] (Symbol/get x))
(defn ->sexp-nil [] (Null/INSTANCE))

(defn ->sexp-strings [xs] (StringArrayVector. (types/->strings xs)))
(defn ->sexp-doubles [xs] (DoubleArrayVector. (types/->doubles xs DoubleVector/NA)))
(defn ->sexp-integers [xs] (IntArrayVector. (types/->integers xs IntVector/NA)))
(defn ->sexp-factor
([xs] (let [rfactor (RFactor. (types/->strings xs))]
        (->sexp-factor (.asIntegers rfactor)
                       (.levels rfactor))))
([ids levels]
 (-> (->sexp-integers ids)
     (prot/set-attributes! {"class" (->sexp-strings ["factor"])
                            "levels" (->sexp-strings levels)}))))

(defn ->sexp-logical
[xs]
(LogicalArrayVector. (->> xs
                          (map (fn [x]
                                 (if (nil? x)
                                   IntVector/NA
                                   (if x 1 0))))
                          (int-array))))

(defn ->sexp-list
[^java.util.List values]
(ListVector. values))

(defn ->sexp-named-list
[ks vs]
(-> (->sexp-list vs)
    (prot/set-attributes! {"names" (->sexp-strings ks)})))
