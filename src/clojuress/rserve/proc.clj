;; copied from Rincanter https://github.com/skm-ice/rincanter/blob/master/src/rincanter/proc.clj
;; originally from https://gist.github.com/codification/1984857

(ns clojuress.rserve.proc
  (:require [clojure.java.io :refer [reader writer]])
  (:import [java.lang ProcessBuilder]))

(defn spawn [& args]
 (let [process (-> (ProcessBuilder. ^java.util.List args)
                   (.start))]
  {:out (-> process
            (.getInputStream)
            (reader))
   :err (-> process
            (.getErrorStream)
            (reader))
   :in (-> process
           (.getOutputStream)
           (writer))
   :process process}))
