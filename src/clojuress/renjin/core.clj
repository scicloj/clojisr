(ns clojuress.renjin.core
  (:require [clojuress.renjin.engine :as engine]
            [clojuress.renjin.to-renjin :as to-renjin]
            [clojuress.renjin.to-clj :as to-clj]
            [clojuress.renjin.lang :as lang]
            [clojuress.renjin.util :as util])
  (:import (org.renjin.sexp Symbol Closure Environment)))

(defn ->env
  "->env creates an environment
       corresponding to given bindings
       specified as a map from names to Clojure data structures
       (converted implicitly to Renjin objects).
 
       (->> {:x 9
             :y \"A\"}
            ->env
            type)
       => Environment
 
       (->> {:x 9
             :y \"A\"}
            ^Environment
            ->env
            (.getNames)
            vec)
       => [\"x\" \"y\"]
 
       (->> {:x 9
             :y \"A\"}
            ^Environment
            ->env
            (#(lang/find-variable % :y))
            vec)
       => [\"A\"]"
  {:added "0.1"}
  [bindings-map]
  (->> bindings-map
       (util/fmap to-renjin/->renjin)
       lang/->env-impl))

(defn eval-expressions
  "Given a map of bindings
        from names to Clojure data structures
        (converted implicitly to Renjin objects),
        and a sequence of strings of R expressions,
        eval-expressions evaluates the expressions in the corresponding environment,
        returning the return value of the last one.
 
       (->> {:x 3 :w 10}
            (eval-expressions [\"y<-2*x+w\"
                                    \"y+1\"])
            vec)
       => [17.0]
 
       (->> {:x (range 9)}
            (eval-expressions [\"summary(x)\"])
            ->clj)
       => {:Min.               0.0
           (keyword \"1st Qu.\") 2.0
           :Median             4.0
           :Mean               4.0
           (keyword \"3rd Qu.\") 6.0
           :Max.               8.0}
 
       \"For example, let us see how to use kmeans
   (and get the expected results on a trivial example).\"
       (let [{:keys [cluster centers]}
             (->> {:x [1 1 1 1 1
                       5 5 5 5 5
                       99 99 99 99 99]}
                  (eval-expressions [\"kmeans(x, centers=2)\"])
                  ->clj)]
         (map #(centers (dec %))
              cluster))
       => [3.0 3.0 3.0 3.0 3.0
           3.0 3.0 3.0 3.0 3.0
           99.0 99.0 99.0 99.0 99.0]"
  {:added "0.1"}
  [r-codes bindings-map]
  (->> bindings-map
       ->env
       (lang/eval-expressions-impl r-codes)))

(defn apply-function
  "Given a map of bindings
        from names to Clojure data structures
        (converted implicitly to Renjin objects)
        and a Renjin function,
        apply-function applies that function
        to the arguments defined by the binding,
        returning its return value.
 
       (->> {:x [-2 3]}
            (apply-function (engine/reval \"function(x) abs(x)\"))
            ->clj)
       => [2 3]"
  {:added "0.1"}
  [^Closure closure bindings-map]
  (->> bindings-map
       ->env
       (lang/apply-function-impl closure)))

(defn function->fn
  "Given some R code that returns a function,
        function->fn creates a Clojure function
        that accepts an argument map,
        interprets it as a the named arguments to the function,
        and applies the function to these arguments.
 
       (let [r-sum (function->fn \"function(x) sum(x)\")]
         (->>  {:x [1 2]}
               r-sum
               ->clj))
       => 3.0"
  {:added "0.1"}
  [function-code]
  (fn [argsmap]
    (apply-function (engine/reval function-code)
                    argsmap)))


(def ->clj to-clj/->clj)

(def ->renjin to-renjin/->renjin)

(def reval engine/reval)

