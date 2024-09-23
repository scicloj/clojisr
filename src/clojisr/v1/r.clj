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
            [clojisr.v1.util :refer [bracket-data maybe-wrap-backtick]]
            [clojisr.v1.require :refer [require-r-package]]
            [clojisr.v1.engines :refer [engines]]
            [tech.v3.tensor :as dtt])
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

(defn java->clj [java-object & options] (java2clj/java->clj java-object options))

(defn clj->java [clj-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (clj2java/clj->java session clj-object)))

(def clj->java->r (comp java->r clj->java))
(def clj->r clj->java->r)

(defn r->java->clj 
  ([r-object options] 
   (-> r-object 
       (r) 
       (r->java) 
       (java2clj/java->clj options)))
  ( [r-object] (r->java->clj r-object nil)))
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

(def r== (r "`==`"))
(def r!= (r "`!=`"))
(def r< (r "`<`"))
(def r> (r "`>`"))
(def r<= (r "`<=`"))
(def r>= (r "`>=`"))
(def r& (r "`&`"))
(def r&& (r "`&&`"))
(def r| (r "`||`"))
(def r|| (r "`||`"))
(def r! (r "`!`"))
(def r$ (r "`$`"))

(def captured-str
  "For the R function [str](https://www.rdocumentation.org/packages/utils/versions/3.6.1/topics/str), we capture the standard output and return the corresponding string."
  (r "function(x) capture.output(str(x))"))

(def println-captured-str (comp println-r-lines captured-str))

(def str-md (comp r-lines->md captured-str))

(def r** (r "`^`"))
(def rdiv (r "`/`"))
(def r- (r "`-`"))
(defn r* [& args] (reduce (r "`*`") args))
(defn r+
  "The plus operator is a binary one, and we want to use it on an arbitraty number of arguments."
  [& args]
  (reduce (r "`+`") args))

;; Some special characters will get a name in letters.
(def colon (r "`:`"))

;;

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

;; brackets!

;; FIXME! Waiting for session management.
(defn- prepare-args-for-bra
  ([pars]
   (mapv #(if (nil? %) (r "(quote(f(,)))[[2]]") %) pars))
  ([pars all?]
   (if all?
     (prepare-args-for-bra pars)
     (conj (prepare-args-for-bra (butlast pars)) (last pars)))))

(defmacro ^:private make-bras
  []
  `(do ~@(for [[bra-sym-name [bra-str all?]] bracket-data
               :let [bra-sym (symbol bra-sym-name)]]
           `(let [bra# (r ~bra-str)]
              (defn ~bra-sym [& pars#]
                (let [fixed# (prepare-args-for-bra pars# ~all?)]
                  (apply bra# fixed#)))))))

(make-bras)

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

(comment
  (require-r '[datasets])
  
  (require-r '[base])
  
  (r "m <- array(seq(1, 10*5*4*7), dim=c(10, 5, 4,7))")
  

  ( r)
  
  (def m (-> (r "m")
             ( r->clj {:as-tensor true})))

  (r "dim(m)")
  
  )

(require '[tech.v3.tensor :as dtt])

(r "m[10,5,4,7]")
;;=> [1] 1400
(dtt/mget m 9 4 3 6)
;;=> 1400


(r "m[1,1,1,1]")
;;=> [1] 1
(dtt/mget m 0 0 0 0)
;;=> 1

(r "m[3,3,3,3]")
;;=> [1] 523
;;   

;;   
(dtt/mget m 2 2 2 2)
;;=> 353

(r "print(.MEM$xe6ab3051a83d48f3)")

(r "capture.output(print(.MEM$x9cd80e7a0ca4471b))")