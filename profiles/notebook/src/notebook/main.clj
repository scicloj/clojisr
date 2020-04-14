(ns notebook.main
 ; (:import  [clojisr.v1.robject RObject])
  (:require
   [clojure.core.async :refer [thread]]
   [pinkgorilla.notebook-app.cli :refer [parse-opts]]
   [pinkgorilla.notebook-app.core :refer [run-gorilla-server]]
   [clojisr.v1.robject]
   [clojisr.v1.gorilla.renderer] ; bring renderer to scope
   
   )
  (:gen-class))

(defn run-notebook []
  (let [args2 ["-c" "./profiles/notebook/config.edn"]
        {:keys [options]} (parse-opts args2)]
    (println "Options Are: " options)
    (run-gorilla-server options)
    nil))

(defn start []
  (thread
   (run-notebook)))

(defn -main []
  (println "Running PinkGorilla Notebook")
  (run-notebook))

(comment
  (run-notebook)

  ;comment end
  )