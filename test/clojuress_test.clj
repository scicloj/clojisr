(ns clojuress-test
  (:use hara.test)
  (:require [clojuress :refer :all]
            [clojuress :as r]
            [clojuress.packages.base :as base])
  (:import (org.rosuda.REngine REXP REXPInteger REXPDouble)))

^{:refer clojuress/defn-implicit-session :added "0.1"}
(fact 
 "This is just some syntactic sugar to define functions
that may optionally get session as an argument,
and otherwise use the default session. "

 (defn-implicit-session r [r-code]
   (rlang/eval-r r-code session))
 =expands-to=>
 (defn r [r-code & {:keys [session-args]}]
   (let [session (session/fetch-or-make session-args)]
     (rlang/eval-r r-code session))))

^{:refer clojuress/r-class :added "0.1"}
(fact "r-class gets the class of an R object."

      (-> "1+2"
          r
          r-class)
      => ["numeric"])

^{:refer clojuress/names :added "0.1"}
(fact "names gets the names attribute of an R object."

      (-> "data.frame(x=1:3,y='hi')"
          r
          names)
      => ["x" "y"])

^{:refer clojuress/shape :added "0.1"}
(fact "shape gets the dim (dimension) attribute of an R object."

      (-> "matrix(1:6,2,3)"
          r
          shape)
      => [2 3])

^{:refer clojuress/r->java :added "0.1"}
(fact "r->java converts an R object to a java object.
The precise definition depends on the session implementation.
For Rserve sessions, this will be something that inherits from
org.rosuda.REngine.REXP."

      (->> "1:9"
           r
           r->java
           class)
      => REXPInteger)



^{:refer clojuress/init-session :added "0.1"}
(fact "TODO")

^{:refer clojuress/r :added "0.1"}
(fact "r runs r code and returns a (handle of) the r return value.
Like most of the API functions, a session can be specified explicitly."

      (-> "1+2"
           r
           r->java->clj)
      => [3.0]

      (-> "1+2"
          (r :session-args {:port 4444})
           r->java->clj)
      => [3.0])

^{:refer clojuress/eval-r->java :added "0.1"}
(fact "eval-r->java runs r code and returns the corresponding java object
(precise definition depends on session type)."

      (-> "1+2"
          eval-r->java
          class)
      => REXPDouble

      (-> "1+2"
          eval-r->java
          (.asDoubles)
          vec)
      => [3.0])

^{:refer clojuress/java->r :added "0.1"}
(fact "java->r converts a java object to an r object.
(precise definition depends on session type)."

      (-> (REXPInteger. 1)
          java->r
          r->java->clj)
      => [1])

^{:refer clojuress/java->naive-clj :added "0.1"}
(fact "java->naive-clj converts a java object of the underlying
java r-interop engine (precise definition depending on session)
into clojure, but in a naive way (e.g., not taking care of missing values).")

^{:refer clojuress/java->clj :added "0.1"}
(fact "java->naive-clj converts a java object of the underlying
java r-interop engine (precise definition depending on session)
into clojure."

      (-> "list(a=1:2,b='hi!')"
          r
          r->java
          java->clj)
      => {:a [1 2] :b ["hi!"]})

^{:refer clojuress/clj->java :added "0.1"}
(fact "clj->java converts a clojure data structure
to a java object of the underlying java r-interop engine
(precise definition depending on session)."

      (-> {:a [1 2] :b "hi!"}
          clj->java
          java->r
          r->java->clj)
      => {:a [1 2] :b ["hi!"]}

      (-> {:a [1 2] :b "hi!"}
          clj->java
          java->r
          base/deparse
          r->java->clj)
      => ["list(a = 1:2, b = \"hi!\")"])

^{:refer clojuress/apply-function :added "0.1"}
(fact "apply-function applies an R function to given arguments.

It expects a data structure
containing the arguments,
with possibly named arguments.

If necessary, arguments are converted to R. "

      (let [r-function (-> "function(w,x,y=10,z=20) w+x+y+z"
                           r)]
        (->> [[1 2]
              [1 2 [:= :y 100]]
              [1 2 [:= :z 100]]]
             (map (fn [args]
                    (->> args
                         (apply-function r-function)
                         r->java->clj)))))
      => [[33.0] [123.0] [113.0]])



^{:refer clojuress/function :added "0.1"}
(fact "function creates a Clojure function
that wraps a given R function
and acts on R objects.

Named arguments are supported.

If necessary, arguments are converted to R."

      (let [f (->> "function(w,x,y=10,z=20) w+x+y+z"
                   r
                   function)]
        (->> [(f 1 2)
              (f 1 2 [:= :y 100])
              (f 1 2 [:= :z 100])]
             (map r->java->clj)))
      => [[33.0] [123.0] [113.0]])



^{:refer clojuress/add-functions-to-this-ns :added "0.1"}
(fact "add-functions-to-this-ns adds to the current namespace
a symbol bound to a clojure function wrapping a given r function.")
