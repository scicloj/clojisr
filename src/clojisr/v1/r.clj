(ns clojisr.v1.r
  (:require [clojisr.v1.protocols :as prot]
            [clojisr.v1.session :as session]
            [clojisr.v1.eval :as evl]
            [clojisr.v1.functions :as functions]
            [clojisr.v1.using-sessions :as using-sessions]
            [clojisr.v1.printing]
            [clojisr.v1.codegen :as codegen]
            [clojisr.v1.impl.java-to-clj :as java2clj]
            [clojisr.v1.impl.clj-to-java :as clj2java]
            [clojure.string :as string]
            [clojisr.v1.util :refer [maybe-wrap-backtick]]
            [clojisr.v1.require :refer [require-r-package]]
            [clojisr.v1.engines :refer [engines]])
  (:import clojisr.v1.robject.RObject))

(defn init [& {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (session/init session)))

(defn ->code [form & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (codegen/form->code form session)))

(defn r [form-or-code & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (evl/r form-or-code session)))

(defn eval-r->java [r-code & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (prot/eval-r->java session r-code)))

(defn r->java [r-object]
  (using-sessions/r->java r-object))

(defn java->r [java-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (using-sessions/java->r java-object session)))

(defn java->native-clj [java-object]
  (java2clj/java->native java-object))

(defn java->clj [java-object] (java2clj/java->clj java-object))

(defn clj->java [clj-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (clj2java/clj->java session clj-object)))

(def clj->java->r (comp java->r clj->java))
(def clj->r clj->java->r)

(defn r->java->clj [r-object] (-> r-object r r->java java2clj/java->clj))
(def r->clj r->java->clj)

(defn r->java->native-clj [r-object] (-> r r-object r->java java2clj/java->native))
(def r->native-clj r->java->native-clj)

(defn session-types
  "Return registered session types"
  []
  (keys @engines))

(defn set-default-session-type!
  "Set default session type."
  [session-type]
  (session/set-default-session-type! session-type))

(defn discard-session [session-args]
  (session/discard session-args))

(defn discard-default-session []
  (session/discard-default))

(defn discard-all-sessions []
  (session/discard-all))

(defn apply-function [r-function args & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (functions/apply-function r-function args session)))

(defn require-r [& packages]
  (run! require-r-package packages))

(def function functions/function)

(defn println-r-lines
  "Get a sequence of strings, typically corresponding to lines captured from the standard output of R functions, println them sequentially."
  [r-lines]
  (doseq [line r-lines]
    (println line)))

(defn r-lines->md
  "Get a sequence of strings, typically corresponding to lines captured from the standard output of R functions, format them as markdown."
  [r-lines]
  (->> r-lines
       r->clj
       (string/join "\n")
       (format "```\n%s\n```")))

(defn r-object? [obj]
  (instance? RObject obj))


(comment (defn na [& {:keys [session-args]}]
           (r "NA" :session-args session-args)))

(comment (def ^{:doc "The empty symbol.
  See https://stackoverflow.com/a/20906150/1723677."}
           empty-symbol
           (r "(quote(f(,)))[[2]]")))

(defn library [libname]
  (->> libname
       (format "library(%s)")
       r))

(defn data
  "Load R dataset and def global var"
  ([dataset-name] (data dataset-name nil))
  ([dataset-name package]
   (let [n (name dataset-name)
         ns (symbol n)
         fmt (if package "data(%s,package=\"%s\")" "data(%s)")]
     (r (format fmt n (name package)))
     (intern *ns* ns (r ns)))))


 (defn- captured-str []
   "For the R function [str](https://www.rdocumentation.org/packages/utils/versions/3.6.1/topics/str), we capture the standard output and return the corresponding string."
   (r "function(x) capture.output(str(x))")  )

 (defn println-captured-str[x] 
   (->
    (apply-function
     (captured-str)
     [x])
    println-r-lines))

 (defn str-md [x]
   (->
    (apply-function
     (captured-str)
     [x])
    r-lines->md))


(defmacro defr
  "Create Clojure and R bindings at the same time"
  [name & r]
  `(do
     (def ~name ~@r)
     ((r "`<-`") '~(symbol name) ~name)
     '~name))

(defn rsymbol
  "Create RObject representing symbol"
  ([string-or-symbol]
   (r (maybe-wrap-backtick string-or-symbol)))
  ([package string-or-symbol]
   (r (str (maybe-wrap-backtick package) "::" (maybe-wrap-backtick string-or-symbol)))))



;; FIXME! Waiting for session management.
(defn- prepare-args-for-bra
  ([pars]
   (mapv #(if (nil? %) (r "(quote(f(,)))[[2]]") %) pars))
  ([pars all?]
   (if all?
     (prepare-args-for-bra pars)
     (conj (prepare-args-for-bra (butlast pars)) (last pars)))))




;; register shutdown hook
;; should be called once
(defonce ^:private shutdown-hook-registered
  (do (.addShutdownHook (Runtime/getRuntime) (Thread. #(locking session/sessions (discard-all-sessions))))
      true))


(defn help
  "Gets help for an R object or function"
  ([r-object]
   (let [symbol (second  (re-find #"\{(.*)\}" (:code r-object)))
         split (string/split symbol #"::")]

     (help (second split) (first split))))

  ([function package]
   (->>
    (r (format  "capture.output(tools:::Rd2txt(utils:::.getHelpFile(as.character(help(%s,%s))), options=list(underline_titles=FALSE)))" (name function) (name package)))
    r->clj
    (string/join "\n"))))


(defn print-help
  "Prints help for an R object or function"
  ([r-object] (println (help r-object)))
  ([function package] (println (help function package))))


;; arithmetic operators
(defn r- 
  "R arithmetic operator `-`"
  [e1 e2] ((r "`-`") e1 e2))

(defn rdiv 
  "R arithmetic operator `/`"
  [e1 e2] ((r "`/`") e1 e2))

(defn r* 
  "R arithmetic operator `*`, but can be used on an arbitraty number of arguments."
  [& args] 
  (reduce (r "`*`") args))

(defn r+
  "R arithmetic operator `+`, but can be used on an arbitraty number of arguments."
  [& args]
  (reduce (r "`+`") args))

(defn r** 
  "R arithmetic operator `^`"
  [e1 e2] 
  ((r "`^`") e1 e2)) 

(defn r%div%
  "R arithmetic operator `%/%`"
  [e1 e2]
  ((r "`%/%`") e1 e2))

(defn r%%
  "R arithmetic operator `%%`"
  [e1 e2]
  ((r "`%%`") e1 e2))

;; relational operators
(defn r== 
  "R relational operator `==`"
  [e1 e2] ( (r "`==`") e1 e2))

(defn r!= 
  "R relational operator `=!`"
  [e1 e2] ((r "`!=`") e1 e2))

(defn r< 
  "R relational operator `<`"
  [e1 e2] ((r "`<`") e1 e2))

(defn r> 
  "R relational operator `>`"
  [e1 e2] ((r "`>`") e1 e2))

(defn r<= 
  "R relational operator `<=`" 
  [e1 e2] ((r "`<=`") e1 e2))

(defn r>= 
  "R relational operator `>=`" 
  [e1 e2] ((r "`>=`") e1 e2))

;; logical operators
(defn r& 
  "R logical operator `&`"
  [e1 e2] ((r "`&`") e1 e2))

(defn r&& 
  "R logical operator `&&`"
  [e1 e2] ((r "`&&`") e1 e2))

(defn r| 
  "R logical operator `|`"
  [e1 e2] ((r "`|`") e1 e2))

(defn r||
  "R logical operator `||`"
  [e1 e2] ((r "`||`") e1 e2))

(defn r!
  "R logical operator `!`"
  [e] ((r "`!`") e))

(defn rxor
  "R logical operator `xor`"
  [e1 e2] ((r "`xor`") e1 e2))


;; colon operators
(defn colon 
  "R colon operator `:`"
  [e1 e2] ((r "`:`") e1 e2))
(defn rcolon 
  "R colon operator `:`"
  [e1 e2] (colon e1 e2))

;; extract/replace operators
(defn r$
  "R extract operator `$`"
  [e1 e2] ((r "`$`") e1 e2))


(defn r%in%
  "R match operator `%in%`"
  [e1 e2] ((r "`%in%`") e1 e2))



(defn bra 
  "R extract operator `[`"
  [& pars]
  (let
   [bra (clojisr.v1.r/r "`[`")
    fixed (prepare-args-for-bra pars true)]
    (clojure.core/apply bra fixed)))

(defn brabra 
  "R extract operator `[[`"
  [& pars]
  (let
   [bra (clojisr.v1.r/r "`[[`")
    fixed (prepare-args-for-bra pars true)]
    (clojure.core/apply bra fixed)))

(defn bra<- 
  "R replace operator `[<-`"
  [& pars]
  (let
   [bra (clojisr.v1.r/r "`[<-`")
    fixed (prepare-args-for-bra pars false)]
    (clojure.core/apply bra fixed)))

(defn brabra<- 
  "R replace operator `[[<-`"
  [& pars]
  (let
   [bra (clojisr.v1.r/r "`[[<-`")
    fixed (prepare-args-for-bra pars false)]
    (clojure.core/apply bra fixed)))

