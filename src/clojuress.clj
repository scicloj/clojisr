(ns clojuress
  (:require [clojuress.session :as session]
            [clojuress.rlang :as rlang]
            [clojuress.protocols :as prot]
            [clojure.pprint :as pp])
  (:import clojuress.rlang.RObject))


(defmacro defn-implicit-session [f args & body]
  (concat (list 'defn
                f
                (into args '[& {:keys [session-args]}]))
          (list (concat (list 'let
                              '[session (session/fetch session-args)])
                        body))))

;; Just some syntactic sugar to define functions
;; that may optionally get session as an argument,
;; and otherwise use the default session.
;;
;; (macroexpand-1 '(defn-implicit-session r [r-code]
;;                   (rlang/eval-r r-code session)))
;; =>
;; (defn
;;   r
;;   [r-code & {:keys [session-args]}]
;;   (println [:using session-args])
;;   (let [session (session/fetch session-args)] (rlang/eval-r r-code session)))

(defn-implicit-session init-session []
  (rlang/init-session session))

(defn-implicit-session r [r-code]
  (rlang/eval-r r-code session))

(defn-implicit-session eval-r->java [r-code]
  (prot/eval-r->java session r-code))

(defn-implicit-session eval-r->java [r-code]
  (prot/eval-r->java session r-code))

(defn r-class [r-object]
  (rlang/r-class r-object))

(defn names [r-object]
  (rlang/names r-object))

(defn shape [r-object]
  (rlang/shape r-object))

(defn r->java [r-object]
  (rlang/r->java r-object))

(defn-implicit-session java->r [java-object]
  (rlang/java->r java-object session))

(defn-implicit-session java->naive-clj [java-object]
  (prot/java->naive-clj session java-object))

(defn-implicit-session java->clj [java-object]
  (prot/java->clj session java-object))

(defn-implicit-session clj->java [clj-object]
  (prot/clj->java session clj-object))

(def clj->java->r (comp java->r clj->java))

(def r->java->clj (comp java->clj r->java))

(defn-implicit-session apply-function [r-function args]
  (rlang/apply-function r-function
                        args
                        session))

(defn function [r-function]
  (fn f
    ([& args]
     (let [explicit-session-args
           (when (some-> args butlast last (= :session-args))
             (last args))]
       (apply-function
        r-function
        (if explicit-session-args
          (-> args butlast butlast)
          args)
        :session (session/fetch explicit-session-args))))))

(defn add-functions-to-this-ns [package-symbol function-symbols]
  (doseq [s function-symbols]
    (let [d (delay (r (format "library(%s)"
                              (name package-symbol)))
                   (function (r (name s))))
          f (fn [& args]
              (apply @d args))]
      (eval (list 'def s f)))))


(defmethod pp/simple-dispatch RObject [obj]
  (let [java-object (r->java obj)]
    (pp/pprint [['R
                 :object-name (:object-name obj)
                 :session-args (-> obj :session :session-args)
                 :r-class (r-class obj)]
                ['->Java java-object]])))


