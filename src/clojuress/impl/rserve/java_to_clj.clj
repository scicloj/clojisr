(ns clojuress.impl.rserve.java-to-clj
  (:require [clojure.pprint :as pp]
            [clojure.walk :as walk]
            [clojure.string :as string]
            [com.rpl.specter :as specter])
  (:import (org.rosuda.REngine REXP REXPString REXPSymbol REXPDouble REXPInteger REXPLanguage RList REXPNull)
           (java.util Map List Collection)
           (clojure.lang Named)))

(defn java->specified-type
  [^REXP java-obj typ]
  (case typ
    :ints    (.asIntegers ^REXP java-obj)
    :doubles (.asDoubles ^REXP java-obj)
    :strings (.asStrings ^REXP java-obj)))

(defn java->naive-clj
  [^REXP java-obj]
  (->> {:attr  (->> java-obj
                   (._attr)
                   (.asNativeJavaObject))
        :value (->> java-obj
                   (.asNativeJavaObject))}
       (walk/prewalk (fn [v]
                       (if (instance? Map v)
                         (do
                           (println [:v v
                                     :cv (class v)
                                     :cv1 (->> (into {}) class)])
                           (->> v
                                (into {})
                                (specter/transform [specter/MAP-KEYS]
                                                   keyword)))
                         v)))))

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
  (-java->clj [this]
    (vec (.asDoubles this))))

(extend-type REXPInteger
  Clojable
  (-java->clj [this]
    (->> this
         (.asIntegers)
         (mapv (fn [i]
                 (if (REXPInteger/isNA i)
                   nil
                   i))))))

(extend-type REXPString
  Clojable
  (-java->clj [this]
    (vec (.asStrings this))))


(defmethod pp/simple-dispatch REXP [obj]
  (let [clj-obj (java->clj obj)]
    (pp/pprint
     (into [(symbol (.toDebugString obj))]
           (when (not= clj-obj obj)
             [['->Clj clj-obj]])))))
