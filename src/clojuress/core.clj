(ns clojuress.core
  (:require [clojuress.session :as session]
            [clojuress.native.core :as native]
            [clojuress.protocols :as prot]
            [clojure.pprint :as pp]
            [clojuress.util :refer [with-ns]]
            [clojuress.core :as r])
  (:import clojuress.native.core.NativeObject))

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
;;                   (native/eval r-code session)))
;; => (defn
;;     r
;;     [r-code & {:keys [session], :or {session (session/get {})}}]
;;     (native/eval r-code session))

(defn-optional-session init []
  (native/init session))

(defn-optional-session r [r-code]
  (native/eval r-code session))

(defn-optional-session eval->jvm [r-code]
  (prot/eval->jvm session r-code))

(defn-optional-session eval->jvm [r-code]
  (prot/eval->jvm session r-code))

(defn-optional-session class [native-object]
  (native/class native-object session))

(defn-optional-session names [native-object]
  (native/names native-object session))

(defn-optional-session shape [native-object]
  (native/shape native-object session))

(defn-optional-session ->jvm [native-object]
  (native/->jvm native-object session))

(defn-optional-session jvm-> [jvm-object]
  (native/jvm-> jvm-object session))

(defn-optional-session ->clj [jvm-object]
  (prot/->clj session jvm-object))

(defn-optional-session clj-> [clj-object]
  (prot/clj-> session clj-object))

(def clj->jvm-> (comp jvm-> clj->))

(def ->jvm->clj (comp ->clj ->jvm))

(defn-optional-session apply-function [native-function args named-args]
  (native/apply-function
   native-function
   (->> args
        (map clj->jvm->))
   (->> named-args
        (map (fn [[arg-name arg]]
               [arg-name (clj->jvm-> arg)])) )
   session))

(defn-optional-session function [native-function]
  (fn f
    ([first-arg rest-args named-args]
     (f (cons first-arg rest-args)
        named-args))
    ([args named-args]
     (apply-function
      native-function
      args
      named-args
      :session session))))

;; Pretty printing relies on the default session
;; for conversion native->jvm->clj.

(defmethod pp/simple-dispatch NativeObject [obj]
  (->> obj
       ->jvm
       (prot/->clj (session/get {}))
       pp/pprint))

(defn add-functions-to-this-ns [package-symbol function-symbols]
  (doseq [s function-symbols]
    (let [d (delay (r (format "library(%s)"
                              (name package-symbol)))
                   (r (name s)))]
      (eval (list 'def s (function @d))))))

