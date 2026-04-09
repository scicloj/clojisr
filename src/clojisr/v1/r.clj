(ns clojisr.v1.r
  (:refer-clojure :exclude [require])
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
            [clojisr.v1.help :as help]
            [clojisr.v1.util :refer [maybe-wrap-backtick]]
            [clojisr.v1.require :refer [require-r-package]]
            [clojisr.v1.engines :refer [engines]]
            [clojisr.v1.robject :as robject])
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

(defn clj->java->r [clj-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (java->r (clj2java/clj->java session clj-object))))

(defn clj->r [clj-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (java->r (clj2java/clj->java session clj-object))))

(defn r->java->clj [r-object] (-> r-object r r->java java2clj/java->clj))
(defn r->clj [r-object] (r->java->clj r-object))

(defn r->java->native-clj [r-object] (-> r r-object r->java java2clj/java->native))
(defn r->native-clj [r-object] (r->java->native-clj r-object))

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

(defn require-r 
  "Requires R packages and creates 2 clojure namespaces with functions for each R object per package.
   The 2 namespaces get names of 'package-name' and 'r.package-name'.

   It supports as well :as and :refer as clojure.core/require does.
   The function can as well attach the R help of  R objects as doc string to the created clojure vars,
   so IDEs can show it. 
   
   As this is slow (several seconds for larger packages), it is not enabled by default. It can be enabled
   using ':docstrings?' true in the package spec.
   
   Examples:

   (r/require-r '[base])             
   (r/require-r '[stats] :as statistics)
   (r/require-r '[base :as my-base :docstrings? true])"
  [& packages]
  (run! require-r-package packages))

(defn require
  "Requires R packages and creates 2 clojure namespaces with functions for each R object per package.
   The 2 namespaces get names of 'package-name' and 'r.package-name'.

   It supports as well :as and :refer as clojure.core/require does.
   The function can as well attach the R help of  R objects as doc string to the created clojure vars,
   so IDEs can show it. 
   
   As this is slow (several seconds for larger packages), it is not enabled by default. It can be enabled
   using ':docstrings?' true in the package spec.
   
   Examples:

   (r/require '[base])             
   (r/require '[stats] :as statistics)
   (r/require '[base :as my-base :docstrings? true])"
  [& packages]
  (run! require-r-package packages))

(defn r-object? [obj]  (instance? RObject obj))

(comment (defn na [& {:keys [session-args]}]
           (r "NA" :session-args session-args)))

(comment (def ^{:doc "The empty symbol.
  See https://stackoverflow.com/a/20906150/1723677."}
           empty-symbol
           (r "(quote(f(,)))[[2]]")))

(defn library
  "Load R library in the R runtime."
  [libname]
  (->> libname (format "library(%s)") r))

(defn- maybe-unquote-name
  [nm]
  (if (sequential? nm) (name (second nm)) (name nm)))

(defmacro data
  "Load R dataset and def a global var."
  ([dataset-name] `(data ~dataset-name nil))
  ([dataset-name package]
   (let [n (maybe-unquote-name dataset-name)
         ns (symbol n)
         call (if package
                (format "data(%s,package=\"%s\")" n (maybe-unquote-name package))
                (format "data(%s)" n))]
     `(do
        (r ~call)
        (def ~ns (r ~(str ns)))))))

(defn function?
  "Checks if given r-object is a function."
  [r-object]
  (using-sessions/function? r-object))

(defn object-structure
  "For the R function [str](https://www.rdocumentation.org/packages/utils/versions/3.6.1/topics/str), we capture the standard output and return the corresponding string."
  [x]
  (->> ((r "function(x) capture.output(str(x))") x)
       (r->clj)
       (string/join "\n")))

;;

(defmacro defr
  "Create Clojure and R bindings at the same time"
  [name & r]
  `(do
     (def ~name ~@r)
     ((r "`<-`") '~(symbol name) ~name)
     '~name))

(defn rsymbol
  "Create RObject representing symbol, allows access to private symbols."
  ([string-or-symbol]
   (r (maybe-wrap-backtick string-or-symbol)))
  ([package string-or-symbol]
   (rsymbol package string-or-symbol false))
  ([package string-or-symbol private?]
   (r (str (maybe-wrap-backtick package) (if private? ":::" "::") (maybe-wrap-backtick string-or-symbol)))))

(defn help
  "Gets help for an R object or function"
  ([r-object] (help/help r-object))
  ([function package] (help/get-help function package)))

;; arithmetic operators
(defn r- 
  "R arithmetic operator `-`"
  ([e] ((r "`-`") e))
  ([e1 e2] ((r "`-`") e1 e2)))

(defn rdiv 
  "R arithmetic operator `/`"
  ([e] ((r "`/`") 1.0 e))
  ([e1 e2] ((r "`/`") e1 e2)))

(defn r* 
  "R arithmetic operator `*`, but can be used on an arbitraty number of arguments."
  [e1 e2 & args] 
  (reduce (r "`*`") (conj args e2 e1)))

(defn r+
  "R arithmetic operator `+`, but can be used on an arbitraty number of arguments."
  ([e] ((r "`+`") e))
  ([e & args]
   (reduce (r "`+`") e args)))

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

;; tilde

(defn tilde
  "R ~/formula operator"
  ([e] (r `(tilde ~e)))
  ([e1 e2] (r `(tilde ~e1 ~e2))))

(defn rtilde
  "R ~/formula operator"
  ([e] (r `(tilde ~e)))
  ([e1 e2] (r `(tilde ~e1 ~e2))))

;;

(defn r%*%
  "R matrix product"
  ([e1] e1)
  ([e1 e2] ((r "`%*%`") e1 e2)))

(defn r%o%
  "R outer product"
  ([e1] e1)
  ([e1 e2] ((r "`%o%`") e1 e2)))

(defn r%x%
  "R Kronecker product"
  ([e1] e1)
  ([e1 e2] ((r "`%x%`") e1 e2)))

;; extract/replace operators

(defn r$
  "R extract operator `$`"
  [e1 e2] ((r "`$`") e1 e2))

(defn r%in%
  "R match operator `%in%`"
  [e1 e2] ((r "`%in%`") e1 e2))

(defn r%||%
  "R null coalescing operator `%||%`"
  [e1 e2] ((r "`%||%`") e1 e2))

;;

(defn- prepare-args-for-bra
  ([pars]
   (mapv #(if (nil? %) (r "(quote(f(,)))[[2]]") %) pars))
  ([pars all?]
   (if all?
     (prepare-args-for-bra pars)
     (conj (prepare-args-for-bra (butlast pars)) (last pars)))))

(defn bra 
  "R extract operator `[`"
  [& pars]
  (let [bra (r "`[`")
        fixed (prepare-args-for-bra pars true)]
    (apply bra fixed)))

(defn brabra 
  "R extract operator `[[`"
  [& pars]
  (let [bra (r "`[[`")
        fixed (prepare-args-for-bra pars true)]
    (apply bra fixed)))

(defn bra<- 
  "R replace operator `[<-`"
  [& pars]
  (let [bra (r "`[<-`")
        fixed (prepare-args-for-bra pars false)]
    (apply bra fixed)))

(defn brabra<- 
  "R replace operator `[[<-`"
  [& pars]
  (let [bra (r "`[[<-`")
        fixed (prepare-args-for-bra pars false)]
    (apply bra fixed)))

;; register shutdown hook
;; should be called once
(defonce ^:private _shutdown-hook-registered
  (do (.addShutdownHook (Runtime/getRuntime) (Thread. #(locking session/sessions (discard-all-sessions))))
      true))

;; deprecated stuff

(defn println-r-lines
  "Get a sequence of strings, typically corresponding to lines captured from the standard output of R functions, println them sequentially."
  {:deprecated true}
  [r-lines]
  (doseq [line r-lines]
    (println line)))

(defn r-lines->md
  "Get a sequence of strings, typically corresponding to lines captured from the standard output of R functions, format them as markdown."
  {:deprecated true}
  [r-lines]
  (->> r-lines
       r->clj
       (string/join "\n")
       (format "```\n%s\n```")))

(defn println-captured-str
  {:deprecated "Use `object-structure` and println the result."}
  [x]
  (println (object-structure x)))

(defn str-md
  {:deprecated "Format `object-structure` result with \"```\n%s\n```\")"}
  [x]
  (format "```\n%s\n```" (object-structure x)))

(defn print-help
  "Prints help for an R object or function"
  {:deprecated "Call `println` on `help` result"}
  ([r-object] (println (help r-object)))
  ([function package] (println (help function package))))

;; [ts] I don't see any reason to expose this function here (this function is internal and creates a wrapper around RObject when it represents R function or any callable).
(def ^{:deprecated true} function functions/function)

(defn apply-function
  {:deprecated true}
  [r-function args & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (functions/apply-function r-function args session)))
