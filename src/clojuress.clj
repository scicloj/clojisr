(ns clojuress
  (:require [clojuress.session :as session]
            [clojuress.rlang :as rlang]
            [clojuress.protocols :as prot]
            [clojure.pprint :as pp])
  (:import clojuress.robject.RObject))


(defmacro defn-implicit-session
  "This is just some syntactic sugar to define functions
  that may optionally get session as an argument,
  and otherwise use the default session. 
  
  (defn-implicit-session r [r-code]
    (rlang/eval-r r-code session))
  =expands-to=>
  (defn r [r-code & {:keys [session-args]}]
   
  (let [session (session/fetch session-args)]
     (rlang/eval-r r-code session)))"
  {:added "0.1"} [f args & body]
  (concat (list 'defn
                f
                (into args '[& {:keys [session-args]}]))
          (list (concat (list 'let
                              '[session (session/fetch session-args)]
                              )
                        body))))

(defn init-session [& {:keys [session-args]}]
  (let [session (session/fetch session-args)]
    (rlang/init-session session)))

(defn r [r-code & {:keys [session-args]}]
  (let [session (session/fetch session-args)]
    (rlang/eval-r r-code session)))

(defn eval-r->java [r-code & {:keys [session-args]}]
  (let [session (session/fetch session-args)]
    (prot/eval-r->java session r-code)))

(defn eval-r->java [r-code & {:keys [session-args]}]
  (let [session (session/fetch session-args)]
    (prot/eval-r->java session r-code)))

(defn r-class
  "r-class gets the class of an R object.
  
       (-> \"1+2\"
           r
           r-class)
       => [\"numeric\"]"
  {:added "0.1"} [r-object]
  (rlang/r-class r-object))

(defn names
  "names gets the names attribute of an R object.
  
       (-> \"data.frame(x=1:3,y='hi')\"
           r
           names)
       => [\"x\" \"y\"]"
  {:added "0.1"} [r-object]
  (rlang/names r-object))

(defn shape
  "shape gets the dim (dimension) attribute of an R object.
  
       (-> \"matrix(1:6,2,3)\"
           r
           shape)
       => [2 3]"
  {:added "0.1"} [r-object]
  (rlang/shape r-object))

(defn r->java
  "r->java converts an R object to a java object.
  The precise definition depends on the session implementation.
  For Rserve sessions, this will be something that inherits from
  org.rosuda.REngine.REXP.
  
       (->> \"1:9\"
            r
            r->java
            class)
       => org.rosuda.REngine.REXPInteger"
  {:added "0.1"} [r-object]
  (rlang/r->java r-object))

(defn java->r [java-object & {:keys [session-args]}]
  (let [session (session/fetch session-args)]
    (rlang/java->r java-object session)))

(defn java->naive-clj [java-object & {:keys [session-args]}]
  (let [session (session/fetch session-args)]
    (prot/java->naive-clj session java-object)))

(defn java->clj [java-object & {:keys [session-args]}]
  (let [session (session/fetch session-args)]
    (prot/java->clj session java-object)))

(defn clj->java [clj-object & {:keys [session-args]}]
  (let [session (session/fetch session-args)]
    (prot/clj->java session clj-object)))

(def clj->java->r (comp java->r clj->java))

(def r->java->clj (comp java->clj r->java))

(defn apply-function [r-function args & {:keys [session-args]}]
  (let [session (session/fetch session-args)]
    (rlang/apply-function r-function args session)))

(defn function
  "function creates a Clojure function
  that wraps a given R function
  and acts on R objects.
  
  The function expects a data structure
  containing the arguments,
  with possibly named arguments.
  
  
       (let [f (->> \"function(w,x,y=10,z=20) w+x+y+z\"
                     r
                     function)]
         (->> [(f 1 2)
              (f 1 2 [:= :y 100])
              (f 1 2 [:= :z 100])]
              (map r->java->clj)))
       => [[33.0] [123.0] [113.0]]"
  {:added "0.1"} [r-function]
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

(defn add-functions-to-this-ns
  "add-functions-to-this-ns adds to the current namespace
  a symbol bound to a clojure function wrapping a given r function."
  {:added "0.1"} [package-symbol function-symbols]
  (doseq [s function-symbols]
    (let [d (delay (r (format "library(%s)"
                              (name package-symbol)))
                   (function (r (name s))))
          f (fn [& args]
              (apply @d args))]
      (eval (list 'def s f)))))

;; Overriding pprint
(defmethod pp/simple-dispatch RObject [obj]
  (let [java-object (r->java obj)]
    (pp/pprint [['R
                 :object-name (:object-name obj)
                 :session-args (-> obj :session :session-args)
                 :r-class (r-class obj)]
                ['->Java java-object]])))

