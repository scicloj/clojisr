(ns clojuress.impl.rserve.java
  (:import (org.rosuda.REngine REXP REXPString REXPSymbol REXPDouble REXPInteger REXPLanguage RList REXPNull)
           (java.util List Collection)
           (clojure.lang Named)))


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
