(ns clojuress.v1.impl.rserve.clj-to-java
  (:require [clojuress.v1.util :refer [fmap]]
            [tech.ml.dataset :as dataset]
            [tech.ml.protocols.dataset :as ds-prot]
            [tech.v2.datatype.protocols :as dtype-prot]
            [clojuress.v1.robject]
            [clojuress.v1.impl.types :as types])
  (:import (org.rosuda.REngine REXP REXPList REXPGenericVector REXPString REXPSymbol REXPLogical REXPDouble REXPInteger REXPLanguage RList REXPNull)
           (java.util List Collection)
           (clojure.lang Named)
           clojuress.v1.robject.RObject
           java.util.Date))


(declare clj->java)

(defn ->rexp-string
  [xs]
  (REXPString.
   (into-array
    (map (fn [x]
           (cond (nil? x)            nil
                 (instance? Named x) (name x)
                 :else               (str x)))
         xs))))

(defn ->rexp-double
  [xs]
  (->> xs
       (map (fn [x]
              (if (nil? x)
                REXPDouble/NA
                (double x))))
       double-array
       (REXPDouble.)))

(defn ->rexp-integer
  [xs]
  (->> xs
       (map (fn [x]
              (if (nil? x)
                REXPInteger/NA
                (int x))))
       int-array
       (REXPInteger.)))

(defn ->rexp-factor
  [xs]
  (throw (ex-info "Factors are not supported yet." {:xs xs})))

(defn ->rexp-logical
  [xs]
  (->> xs
       (map (fn [x]
              (if (nil? x)
                REXPLogical/NA
                (if x
                  REXPLogical/TRUE
                  REXPLogical/FALSE))))
       byte-array
       (REXPLogical.)))

(defn ->r-time [xs]
  (-> xs
       (->> (map (fn [^Date d]
                   (some-> d (.getTime)))))
       ->rexp-double
       (.asDoubles)
       (REXPDouble. (REXPList.
                     (RList. [(->rexp-string [""])
                              (->rexp-string ["POSIXct" "POSIXt"])]
                             ["tzone"
                              "class"])))))

(def primitive-vector-ctors
  {:integer   ->rexp-integer
   :numeric   ->rexp-double
   :character ->rexp-string
   :factor    ->rexp-factor
   :logical   ->rexp-logical
   :time      ->r-time})

(defn ->primitive-vector [sequential]
  (when-let [primitive-type (types/finest-primitive-r-type sequential)]
    ((primitive-vector-ctors primitive-type) sequential)))

(defn ->list [values]
  (->> values
      ;; recursively
      (map clj->java)
      (RList.)
      (REXPGenericVector.)))

(defn ->named-list [amap]
  (-> (RList. (->> amap
                   vals
                   ;; recursively
                   (map clj->java))
              (->> amap
                   keys
                   (map name)))
      (REXPGenericVector.)))

(defn ->data-frame [dataset]
  (->> dataset
       dataset/column-map
       (fmap (comp clj->java ; recursively
                   seq ; TODO: avoid this wasteful conversion
                   dtype-prot/->array-copy))
       ->named-list
       (.asList)
       (REXP/createDataFrame)))


(defn clj->java
  [obj]
  (or (cond
        ;; an r object
        (instance? RObject obj)
        obj
        ;; a java REXP object
        (instance? REXP obj)
        obj
        ;; nil
        (nil? obj)
        (REXPNull.)
        ;; basic types
        (types/primitive-r-type obj)
        (clj->java [obj])
        ;; a sequential or array of elements of inferrable primitive type
        (sequential? obj)
        (->primitive-vector obj))
      ;; we get here if ->primitive-vetor returned nil, which means: no inferrable primitive type
      (cond ;; a dataset
        (satisfies? ds-prot/PColumnarDataset obj)
        (->data-frame obj)
        ;; a named list
        (map? obj)
        (->named-list obj)
        ;; an unnamed list
        (sequential? obj)
        (->list obj))))
