(ns clojuress.impl.rserve.java-to-clj
  (:import (org.rosuda.REngine REXP REXPString REXPSymbol REXPDouble REXPInteger REXPLanguage RList REXPNull)
           (java.util List Collection)
           (clojure.lang Named)))

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
    (vec (.asIntegers this))))

(extend-type REXPString
  Clojable
  (-java->clj [this]
    (vec (.asStrings this))))

