(ns clojuress.v1.impl.renjin.java-to-clj
  (:require [clojure.pprint :as pp]
            [clojure.walk :as walk]
            [com.rpl.specter :as specter]
            [tech.ml.dataset :as dataset]
            [tech.v2.datatype :as dtype]
            [tech.v2.datatype.protocols :as dtype-prot :refer [->array-copy]]
            [clojuress.v1.impl.renjin.lang :as lang])
  (:import (org.renjin.sexp SEXP Vector ListVector IntVector Logical Symbol Null StringArrayVector)
           (java.util Map List Collection)
           (clojure.lang Named)))

(defn java->specified-type
  [^SEXP java-obj typ]
  (case typ
    :strings ((fn [^StringArrayVector v] (.toArray v))
              java-obj)))

(defn java->naive-clj
  [^SEXP java-obj]
  (throw (ex-info "Unsupported function." {})))

(defprotocol Clojable
  (-java->clj [this]))

(defn java->clj
  [java-obj]
  (some-> java-obj
          -java->clj))

(extend-type Object
  Clojable
  (-java->clj [this] this))

;; Renjin represents a dataframe as a ListVector.
;; Its elements are are the columns,
;; and the "names" attribute holds the column names.
(defn df->maps
  [^ListVector df]
  (let [column-names (map keyword (lang/->attr df :names))]
    (->> df
         (map java->clj)
         (apply map (fn [& row-elements]
                      (zipmap column-names row-elements))))))

(defn renjin-vector->clj
  [transf v]
  (if (some #(= % :data.frame) (lang/->class v))
    (df->maps v)
    (let [names (lang/->names v)
          dim   (lang/->attr v :dim)]
      (->> v
           (map-indexed (fn [i x]
                          (when (not (.isElementNA ^Vector v ^int i))
                            (transf x))))
           ((if (seq names)
              ;; A named list or vector will be translated to a map.
              (partial zipmap names)
              (if (seq dim)
                ;; A matrix will be translated to a vector of vectors
                (fn [values]
                  (->> values
                       (partition (second dim))
                       (#(do (println %) %))
                       (mapv vec)))
                ;; A regular list or vector will be translated to a vector.
                vec)))))))

(extend-type Vector
  Clojable
  (-java->clj [this]
    (renjin-vector->clj java->clj
                        this)))

(extend-type IntVector
  Clojable
  (-java->clj [this]
    (if (.isNumeric this)
      (renjin-vector->clj java->clj
                          this)
      ;; else - a factor
      (renjin-vector->clj  (comp java->clj
                                 (lang/->attr this :levels)
                                 dec)
                           this))))

(extend-type Logical
  Clojable
  (-java->clj [this]
    ({Logical/TRUE  true
      Logical/FALSE false}
     this)))

(extend-type Symbol
  Clojable
  (-java->clj [this]
    (symbol (.toString this))))

(extend-type Null
  Clojable
  (-java->clj [this]
    nil))


