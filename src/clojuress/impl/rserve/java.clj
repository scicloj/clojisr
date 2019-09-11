(ns clojuress.impl.rserve.java
  (:import (org.rosuda.REngine REXP REXPString REXPSymbol REXPDouble REXPInteger REXPLanguage RList REXPNull)
           (java.util List Collection)
           (clojure.lang Named))
  (:require [clojure.pprint :as pp]
            [clojure.string :as string]))


(defn r-list [^Collection names
              ^Collection contents]
  (println [:names names
            :contents contents])
  (RList. contents names))

(defn rexp-language [^List alist]
  (REXPLanguage. alist))

(defn rexp-symbol [name]
  (REXPSymbol. name))

(defn rexp-double [xs]
  (REXPDouble. (double-array xs)))

(defn rexp-int [xs]
  (REXPInteger. (int-array xs)))

(defn call
  [op args]
  (REXP/asCall op (into-array REXP args)))

(defn assignment
  [varname java-obj]
  (call "<-"
        [(if (re-find #"\$" varname)
           (->> (string/split varname #"\$")
                (map rexp-symbol)
                (call "$"))
           (rexp-symbol varname))
         java-obj]))
