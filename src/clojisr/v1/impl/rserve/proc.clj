;; copied from Rincanter https://github.com/skm-ice/rincanter/blob/master/src/rincanter/proc.clj
;; originally from https://gist.github.com/codification/1984857

(ns clojisr.v1.impl.rserve.proc
  (:require [clojure.java.io :refer [reader writer]]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as string]
            [clojisr.v1.util :refer [file-exists?]]
            [cambium.core :as log])
  (:import [java.lang ProcessBuilder]
           [java.io File]))

(defn spawn [& args]
  (log/info [::spawning (string/join " " args)])
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



;; Running Rserve -- copied from Rojure:
;; https://github.com/behrica/rojure

(defn- r-path
  "Find path to R executable"
  []
  {:post [(not= % "")]}
  (apply str
         (-> (sh "which" "R")
             (get :out)
             (butlast)))) ; avoid trailing newline

(defn start-rserve
  "Boot up RServe in another process.
   Returns a map with a java.lang.Process that can be 'destroy'ed"
  [{:keys [port init-r sleep]
    :or {init-r ""
         sleep 0}}]
  (let [rstr-temp (format
                   (if (file-exists? "/etc/Rserv.conf")
                     "library(Rserve); run.Rserve(port=%s, config.file='/etc/Rserv.conf');"
                     "library(Rserve); run.Rserve(port=%s);")
                   port)
        rstr      (if (empty? init-r )
                    rstr-temp
                    (str init-r ";" rstr-temp ))]
    (prn rstr)
    (let [rserve (spawn (r-path)
                        "--no-save" ; don't save workspace when quitting
                        "--slave"
                        "-e" ; evaluate (boot server)
                        rstr)]
      (log/info [::sleeping sleep])
      (Thread/sleep sleep)
      rserve)))

(defn alive? [rserve]
  (.isAlive ^Process (:process rserve)))

(defn close [rserve]
  (let [p ^Process (:process rserve)]
    (when (.isAlive p) (.destroy p))))
