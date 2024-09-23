(ns clojisr.v1.impl.rserve.call
  (:require [clojure.string :as string]
            [clojisr.v1.util :refer [exception-cause]]
            [clojure.tools.logging.readable :as log])
  (:import (org.rosuda.REngine REXP REXPSymbol REXPLanguage RList
                               REngineException REXPMismatchException)
           (org.rosuda.REngine.Rserve RConnection)))

(defn rexp-symbol
  ^REXP [name]
  (REXPSymbol. name))

;; reimplemented to make call non-reflective
(defn call
  [^String op args]
  (let [^RList l (RList.)]
    (.add l (rexp-symbol op))
    (doseq [^REXP v args]
      (.add l v))
    (REXPLanguage. l)))

(defn assignment
  [varname java-obj]
  (call "<-"
        [(if (re-find #"\$" varname)
           (->> (string/split varname #"\$")
                (map rexp-symbol)
                (call "$"))
           (rexp-symbol varname))
         java-obj]))

(defn try-eval-catching-errors [expression ^RConnection r-connection]
  ;; Using the technique of https://stackoverflow.com/a/40447542/1723677, the way it is used in Rojure.
  (def expression expression)
  (try
    (let [expression-str (-> expression
                             (string/escape char-escape-string)
                             (->> (format "try(eval(parse(text=\"%s\")),silent=TRUE)")))
          ^REXP rexp (locking r-connection
                       (.parseAndEval r-connection expression-str))]
           (def rexp rexp)
      
      (clojure.reflect/reflect rexp)
      (.asIntegers rexp)

      (if (.inherits rexp "try-error")
        (do (log/error [::try-eval-catching-errors {:message (format "Error in R evaluating expression: %s. R exception: %s"
                                                                     expression (.asString rexp))}])
            (throw (Exception. (format "Error in R evaluating expression:\n %s.\nR exception: %s"
                                       expression (.asString rexp)))))
        rexp))
    (catch REngineException ex
      (log/error [::try-eval-catching-errors {:message (format "Caught exception evaluating expression: %s" expression)
                                              :exception (exception-cause ex)}]))
    (catch REXPMismatchException ex
      (log/error [::try-eval-catching-errors {:message (format "Caught exception evaluating expression: %s" expression)
                                              :exception (exception-cause ex)}]))))
