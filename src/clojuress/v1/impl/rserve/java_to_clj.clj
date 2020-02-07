(ns clojuress.v1.impl.rserve.java-to-clj
  (:require [clojure.walk :as walk]
            [tech.ml.dataset :as dataset]
            [tech.v2.datatype.protocols :as dtype-prot :refer [->array-copy]]
            [clojure.math.combinatorics :refer [cartesian-product]]
            [com.rpl.specter :as specter]
            [clojuress.v1.impl.common :refer [usually-keyword]])
  (:import (org.rosuda.REngine REXP REXPGenericVector REXPString REXPLogical REXPFactor REXPSymbol REXPDouble REXPInteger REXPLanguage REXPList RList REXPNull)
           (java.util Map List Collection Vector)
           (clojure.lang Named)))

(defn java->specified-type
  [^REXP java-obj typ]
  (case typ
    :ints    (.asIntegers ^REXP java-obj)
    :doubles (.asDoubles ^REXP java-obj)
    :strings (.asStrings ^REXP java-obj)))

(defn java->naive-clj
  [^REXP java-obj]
  (->> {:attr  (some->> java-obj
                        (._attr)
                        (.asNativeJavaObject))
        :value (->> java-obj
                    (.asNativeJavaObject))}
       (walk/prewalk (fn [v]
                       (cond
                         (instance? Map v)    (->> v
                                                   (into {})
                                                   (specter/transform [specter/MAP-KEYS]
                                                                      usually-keyword))
                         (instance? Vector v) (vec v)
                         :else                v)))))

(extend-type REXPDouble
  dtype-prot/PToArray
  (->array-copy [item]
    ;; NA maps to REXPDouble/NA.
    (.asDoubles item)))

(extend-type REXPInteger
  dtype-prot/PToArray
  (->array-copy [item]
    ;; NA has to be handled explicitly.
    (let [n      (.length item)
          values (.asIntegers item)
          na?    (.isNA item)]
      (if (every? false? na?)
        values
        (let [target (double-array (.length item))]
          (dotimes [i n]
            (aset ^doubles target
                  i
                  ^double (if (aget na? i)
                            REXPDouble/NA
                            (double (aget values i)))))
          target)))))

(extend-type REXPString
  dtype-prot/PToArray
  (->array-copy [item]
    ;; NA maps to nil.
    (.asStrings item)))

(extend-type REXPLogical
  dtype-prot/PToArray
  (->array-copy [item]
    ;; NA,TRUE,FALSE have to be handled explicitly.
    (let [n      (.length item)
          na? (.isNA item)
          true? (.isTRUE item)
          target (make-array Boolean (.length item))]
      (dotimes [i n]
        (aset target i
              (or (aget true? i)
                  (if (aget na? i)
                    nil
                    false))))
      target)))


(defn java-factor? [^REXP java-obj]
  (-> java-obj
      (.getAttribute "class")
      ->array-copy
      (->> (some (partial = "factor")))))

(defn java-factor->clj-info [^REXPFactor java-factor]
  (when (not (java-factor? java-factor))
    (throw (ex-info "Expected a factor, got something else." {:class (-> java-factor
                                                                         (.getAttribute "class")
                                                                         ->array-copy
                                                                         vec)})))
  (let [levels  (-> java-factor
                    (.getAttribute "levels")
                    ->array-copy)
        indices (-> java-factor
                    (.asFactor)
                    (.asIntegers))]
    {:levels  levels
     :indices indices}))

(defn java-data-frame? [^REXP java-obj]
  (some-> java-obj
          (.getAttribute "class")
          ->array-copy
          (->> (some (partial = "data.frame")))))

(defn java-data-frame->clj-dataset [^REXP java-df]
  (let [^RList columns-named-list (.asList java-df)]
    (->> columns-named-list
         (map ->array-copy)
         (map vector (.names columns-named-list))
         dataset/name-values-seq->dataset)))


(defprotocol Clojable
  (-java->clj [this]))

(defn java->clj
  [java-obj]
  (some-> java-obj
          -java->clj))


(extend-type Object
  Clojable
  (-java->clj [this] this))

(extend-type REXPDouble
  Clojable
  (-java->clj [java-obj]
    (-> java-obj ->array-copy vec)))

(defn table? [^REXP java-obj]
  (some-> java-obj
          (.getAttribute "class")
          -java->clj
          (= ["table"])))

(defn table->clj [^REXPInteger java-obj]
  (let [dimnames (-> java-obj
                     (.getAttribute "dimnames")
                     (.asList)
                     (->> (map -java->clj)))
        counts   (->array-copy java-obj)]
    (-> dimnames
         (->> reverse
              (apply cartesian-product)
              (map vec))
         (interleave counts)
         (->> (apply array-map)))))

(extend-type REXPInteger
  Clojable
  (-java->clj [java-obj]
    (if (table? java-obj)
      (table->clj java-obj)
      (-> java-obj ->array-copy vec))))

(extend-type REXPString
  Clojable
  (-java->clj [java-obj]
    (-> java-obj ->array-copy vec)))

(extend-type REXPLogical
  Clojable
  (-java->clj [java-obj]
    (-> java-obj ->array-copy vec)))

(extend-type REXPFactor
  Clojable
  (-java->clj [java-factor]
    (let [{:keys [levels indices]}
          (java-factor->clj-info
           java-factor)]
      (mapv (fn [i]
              (when-not
                  (= i Integer/MIN_VALUE) ; which means a missing value
                (->> i
                     dec
                     (aget levels))))
            indices))))

(defn rexp-vector->list-or-map
  [^REXP java-obj]
  (let [^RList rlist (.asList java-obj)]
    (if-not (.isNamed rlist) ;; where list doesn't contain keys, return vector of elements
      (mapv java->clj (.values rlist))
      (let [names (.keys rlist)
            values (-> (comp java->clj (fn [^String k] (.at rlist k)))
                       (map names))]
        (do (if (some (partial = "") names)
              (throw (ex-info "Partially named lists are not supported yet. " {:names names})))
            (let [fixed-names (map usually-keyword names)
                  list-as-map (->> values
                                   (interleave fixed-names)
                                   (apply array-map))]
              (if (java-data-frame? java-obj)
                ;; a data  frame
                (dataset/name-values-seq->dataset list-as-map)
                ;; else -- assume a regular list
                list-as-map)))))))

(extend-type REXPGenericVector
  Clojable
  (-java->clj [java-obj]
    (rexp-vector->list-or-map java-obj)))

(extend-type REXPList
  Clojable
  (-java->clj [java-obj]
    (rexp-vector->list-or-map java-obj)))

(extend-type REXPSymbol
  Clojable
  (-java->clj [java-obj]
    (-> java-obj
        (.asString)
        symbol)))

(extend-type REXPLanguage
  Clojable
  (-java->clj [java-obj]
    (->> java-obj
         (.asList)
         (mapv java->clj))))

(extend-type REXPNull
  Clojable
  (-java->clj [_] nil))
