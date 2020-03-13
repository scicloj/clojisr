(ns clojisr.v1.impl.renjin.clj-to-java
  (:require [clojisr.v1.util :refer [fmap]]
            [tech.ml.dataset :as dataset]
            [tech.ml.protocols.dataset :as ds-prot]
            [tech.v2.datatype.protocols :as dtype-prot]
            [clojisr.v1.robject]
            [clojisr.v1.impl.renjin.lang :as lang]
            [clojisr.v1.impl.renjin.engine :as engine]
            [clojisr.v1.impl.types :as types])
  (:import (java.util List Collection)
           (clojure.lang Named)
           clojisr.v1.robject.RObject
           java.util.Date
           org.renjin.invoke.reflection.converters.Converters
           org.renjin.eval.Context
           org.renjin.parser.RParser
           (org.renjin.sexp SEXP Symbol
                            Vector Null
                            FunctionCall
                            Symbol
                            DoubleVector DoubleArrayVector
                            IntVector IntArrayVector
                            LogicalVector LogicalArrayVector
                            StringVector StringArrayVector
                            ListVector ListVector$NamedBuilder
                            AttributeMap)))

(declare clj->java)


(defn ->character-vector
  [_ xs]
  (StringArrayVector.
   ^Iterable
   (map (fn [x]
          (cond (nil? x)            nil
                (instance? Named x) (name x)
                :else               (str x)))
        xs)))

(defn ->list-vector
  [engine xs]
  (ListVector.
   ^List
   (map (partial clj->java engine) xs)))

(defn ->named-list-vector
  [engine pairs]
  (let [builder ^ListVector$NamedBuilder (ListVector/newNamedBuilder)]
    (doseq [[nam value] pairs]
      (.add builder
            ^String (if (instance? Named nam)
                      (name nam)
                      (-> nam (clj->java engine) str))
            ^SEXP (clj->java engine value)))
    (.build builder)))

(defn ->numeric-vector
  [_ xs]
  (->> xs
       (map (fn [x]
              (if (nil? x)
                DoubleVector/NA
                (double x))))
       double-array
       (DoubleArrayVector.)))

(defn ->integer-vector
  [_ xs]
  (->> xs
       (map (fn [x]
              (if (nil? x)
                IntVector/NA
                (int x))))
       int-array
       (IntArrayVector.)))

(defn ->logical-vector
  [_ xs]
  (->> xs
       (map (fn [x]
              (if (nil? x)
                LogicalVector/NA
                (if x
                  1
                  0))))
       int-array
       (LogicalArrayVector.)))

(defn ->factor-vector
  [engine xs]
  (lang/eval-expressions engine
                         ["factor(x)"]
                         (lang/->env engine
                                     {:x (->character-vector xs)})))

(defn ->r-time [xs]
  (throw (ex-info "Unsupported function." {})))

(def primitive-vector-ctors
  {:integer   ->integer-vector
   :numeric   ->numeric-vector
   :character ->character-vector
   :factor    ->factor-vector
   :logical   ->logical-vector
   :time      ->r-time})

(defn ->primitive-vector [engine sequential]
  (when-let [primitive-type (types/finest-primitive-r-type sequential)]
    ((primitive-vector-ctors primitive-type) engine sequential)))


(defn ->data-frame
  [engine dataset]
  (lang/eval-expressions
   engine
   ["data.frame(columns, stringsAsFactors=FALSE)"]
   (lang/->env engine
               {:columns (->> dataset
                              dataset/column-map
                              (fmap (partial ->primitive-vector engine))
                              (->named-list-vector engine))})))



(defn clj->java
  [engine obj]
  (or (cond
        ;; an r object
        (instance? RObject obj)
        (Symbol/get (:object-name obj))
        ;; a renjin object
        (instance? SEXP obj)
        obj
        ;; nil
        (nil? obj)
        Null/INSTANCE
        ;; basic types
        (types/primitive-r-type obj)
        (clj->java engine [obj])
        ;; a sequential or array of elements of inferrable primitive type
        (sequential? obj)
        (->primitive-vector engine obj))
      ;; we get here if ->primitive-vetor returned nil, which means: no inferrable primitive type
      (cond ;; a dataset
        (satisfies? ds-prot/PColumnarDataset obj)
        (->data-frame engine obj)
        ;; a map
        (map? obj)
        (->named-list-vector engine obj)
        ;; a sequential thing with no inferrable primitive type
        (sequential? obj)
        (->list-vector engine obj))))

