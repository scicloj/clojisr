(ns clojuress.core
  (:require [clojuress.session :as session]
            [clojuress.r.core :as r]
            [clojuress.protocols :as prot]
            [clojure.pprint :as pp]
            [clojuress.util :refer [with-ns]]
            [clojuress.core :as r])
  (:import clojuress.r.core.RObject))

(defmacro defn-optional-session [f args & body]
  (concat (list 'defn
                f
                (into args
                      '[& {:keys [session]
                           :or   {session (session/get {})}}]))
          body))

;; Just some syntactic sugar to define functions
;; that may optionally get session as an argument,
;; and otherwise use the default session.
;;
;; (macroexpand-1 '(defn-optional-session r [r-code]
;;                   (r/eval r-code session)))
;; => (defn
;;     r
;;     [r-code & {:keys [session], :or {session (session/get {})}}]
;;     (r/eval r-code session))

(defn-optional-session init []
  (r/init session))

(defn-optional-session r [r-code]
  (r/eval r-code session))

(defn-optional-session evalr->java [r-code]
  (prot/evalr->java session r-code))

(defn-optional-session evalr->java [r-code]
  (prot/evalr->java session r-code))

(defn-optional-session class [r-object]
  (r/class r-object session))

(defn-optional-session names [r-object]
  (r/names r-object session))

(defn-optional-session shape [r-object]
  (r/shape r-object session))

(defn-optional-session r->java [r-object]
  (r/r->java r-object session))

(defn-optional-session java->r [java-object]
  (r/java->r java-object session))

(defn-optional-session java->clj [java-object]
  (prot/java->clj session java-object))

(defn-optional-session clj->java [clj-object]
  (prot/clj->java session clj-object))

(def clj->javajava->r (comp java->r clj->java))

(def r->java->rclj (comp java->clj r->java))

(defn-optional-session apply-function [r-function args named-args]
  (r/apply-function
   r-function
   (->> args
        (map clj->javajava->r))
   (->> named-args
        (map (fn [[arg-name arg]]
               [arg-name (clj->javajava->r arg)])) )
   session))

(defn-optional-session function [r-function]
  (fn f
    ([first-arg rest-args named-args]
     (f (cons first-arg rest-args)
        named-args))
    ([args named-args]
     (apply-function
      r-function
      args
      named-args
      :session session))))

;; Pretty printing relies on the default session
;; for conversion r->javajava->clj.

(defmethod pp/simple-dispatch RObject [obj]
  (->> obj
       r->java
       (prot/java->clj (session/get {}))
       pp/pprint))

(defn add-functions-to-this-ns [package-symbol function-symbols]
  (doseq [s function-symbols]
    (let [d (delay (r (format "library(%s)"
                              (name package-symbol)))
                   (r (name s)))]
      (eval (list 'def s (function @d))))))

