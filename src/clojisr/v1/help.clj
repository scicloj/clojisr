(ns clojisr.v1.help
  (:require [clojisr.v1.eval :as evl]
            [clojisr.v1.using-sessions :as using-sessions]
            [clojisr.v1.impl.java-to-clj :as java2clj]
            [clojure.string :as str]
            ))


(defn help
  "Gets help for an R object or function"
  ([r-object session]
   (let [symbol (second  (re-find #"\{(.*)\}" (:code r-object)))
         split (str/split symbol #"::")]

     (help (second split) (first split) session)))

  ([function package session]
   
   (->>
    (evl/r (format  "capture.output(tools:::Rd2txt(utils:::.getHelpFile(as.character(help(%s,%s))), options=list(underline_titles=FALSE)))"
                    (name function) (name package))
           session)
    
    (using-sessions/r->java)
    (java2clj/java->clj)
    (str/join "\n"))))
