(ns clojuress.v0.r
  (:require [clojuress.v0.session :as session]
            [clojuress.v0.eval :as evl]
            [clojuress.v0.functions :as functions]
            [clojuress.v0.using-sessions :as using-sessions]
            [clojuress.v0.protocols :as prot]
            [clojuress.v0.printing]
            [clojuress.v0.codegen :as codegen]
            [clojure.string :as string])
  (:import clojuress.v0.robject.RObject))


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

(defn eval-r->java [r-code & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (prot/eval-r->java session r-code)))

(defn r->java [r-object]
  (using-sessions/r->java r-object))

(defn java->r [java-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (using-sessions/java->r java-object session)))

(defn java->naive-clj [java-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (prot/java->naive-clj session java-object)))

(defn java->clj [java-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (prot/java->clj session java-object)))

(defn clj->java [clj-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (prot/clj->java session clj-object)))

(def clj->java->r (comp java->r clj->java))
(def clj->r clj->java->r)

(def r->java->clj (comp java->clj r->java))
(def r->clj r->java->clj)

(defn discard-session [session-args]
  (session/discard session-args))

(defn discard-default-session []
  (session/discard-default))

(defn discard-all-sessions []
  (session/discard-all))

(defn apply-function [r-function args & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (functions/apply-function r-function args session)))

(def function functions/function)

(defn println-r-lines [r-lines]
  "Get a sequence of strings, typically corresponding to lines captured from the standard output of R functions, println them sequentially."
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


(defn na [& {:keys [session-args]}]
  (r "NA" :session-args session-args))

(defn empty-symbol [& {:keys [session-args]}]
  "The empty symbol.
  See https://stackoverflow.com/a/20906150/1723677."
  (r "(quote(f(,)))[[2]]" :session-args session-args))

(defn library [libname]
  (->> libname
       (format "library(%s)")
       r))

(def r== (function (r "`==`")))
(def r!= (function (r "`!=`")))
(def r< (function (r "`<`")))
(def r> (function (r "`>`")))
(def r<= (function (r "`<=`")))
(def r>= (function (r "`>=`")))
(def r& (function (r "`&`")))
(def r&& (function (r "`&&`")))
(def r| (function (r "`||`")))
(def r|| (function (r "`||`")))


(def captured-str
  "For the R function [str](https://www.rdocumentation.org/packages/utils/versions/3.6.1/topics/str), we capture the standard output and return the corresponding string."
  (function (r "function(x) capture.output(str(x))")))

(def println-captured-str (comp println-r-lines captured-str))

(def str-md (comp r-lines->md captured-str))


(defn r+
  "The plus operator is a binary one, and we want to use it on an arbitraty number of arguments."
  [& args]
  (reduce (function (r "`+`")) args))

;; Some special characters will get a name in letters.
(def bra (function (r "`[`")))
(def bra<- (function (r "`[<-`")))
(def brabra (function (r "`[[`")))
(def brabra<- (function (r "`[[<-`")))
(def colon (function (r "`:`")))


