(ns clojisr.v1.help
  (:require [clojisr.v1.eval :as evl]
            [clojisr.v1.using-sessions :as using-sessions]
            [clojisr.v1.impl.java-to-clj :as java2clj]
            [clojure.string :as str]
            [clojisr.v1.session :as session]
            
            [clojisr.v1.help :as help]))
(defn- un-back-quote [s]
  (str/replace s  "`" "" ))


(defn _get-help[function package]
 ;(println :obtain-help (format  "%s/%s " (name package) (un-back-quote (name function))))
 (->>
  (evl/r (format  "capture.output(tools:::Rd2txt(utils:::.getHelpFile(as.character(help(%s,%s))), options=list(underline_titles=FALSE)))"
                  (name function) (name package))
         (session/fetch-or-make nil))

  (using-sessions/r->java)
  (java2clj/java->clj)
  (str/join "\n")))

(defonce get-help (memoize _get-help))

(defn help
  
  "Gets help for an R object or function"
  ([r-object]
   (let [symbol (second  (re-find #"\{(.*)\}" (:code r-object)))
         split (str/split symbol #"::")]

     (get-help (second split) (first split) )))

  )



