(ns clojuress.core
  (:require [clojuress.session :as session]
            [clojuress.rlang.core :as rlang]
            [clojuress.protocols :as prot]
            [clojure.pprint :as pp])
  (:import clojuress.rlang.core.RObject))

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
;;                   (rlang/eval r-code session)))
;; => (defn
;;     r
;;     [r-code & {:keys [session], :or {session (session/get {})}}]
;;     (rlang/eval r-code session))

(defn-optional-session init []
  (rlang/init session))

(defn-optional-session r [r-code]
  (rlang/eval r-code session))

(defn-optional-session eval-r->java [r-code]
  (prot/eval-r->java session r-code))

(defn-optional-session eval-r->java [r-code]
  (prot/eval-r->java session r-code))

(defn-optional-session class [r-object]
  (rlang/class r-object session))

(defn-optional-session names [r-object]
  (rlang/names r-object session))

(defn-optional-session shape [r-object]
  (rlang/shape r-object session))

(defn-optional-session r->java [r-object]
  (rlang/r->java r-object session))

(defn-optional-session java->r [java-object]
  (rlang/java->r java-object session))

(defn-optional-session java->clj [java-object]
  (prot/java->clj session java-object))

(defn-optional-session clj->java [clj-object]
  (prot/clj->java session clj-object))

(def clj->javajava->r (comp java->r clj->java))

(def r->java->rclj (comp java->clj r->java))

(defn-optional-session apply-function [r-function args named-args]
  (rlang/apply-function
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
                   (function (r (name s))))
          f (fn [& args]
              (apply @d args))]
      (eval (list 'def s f)))))
