(ns clojuress.renjin.lang-test
  (:require [clojuress.renjin.engine :refer [reval]]
            [clojuress.renjin.lang :refer :all]
            [clojuress.renjin.to-clj :refer [->clj]]
            [clojuress.renjin.to-renjin :refer [->renjin]]
            [clojuress.renjin.util :refer [fmap]]
            [hara.test :refer :all])
  (:import (org.renjin.sexp ExpressionVector Symbol Closure Environment Environment$Builder)
           (org.renjin.eval Context)
           (org.renjin.parser RParser)))


^{:refer clojuress.renjin.lang/->symbol :added "0.1"}
(fact "->symbol creates a Renjin symbol."

      (-> :x
          ->symbol
          type)
      => Symbol

      (-> :x
          ^Symbol
          ->symbol
          (.asString))
      => "x")


^{:refer clojuress.renjin.lang/->env-impl :added "0.1"}
(fact "->env-impl creates an environment
       corresponding to given bindings
       specified as a map from names to Renjin objects."

      (->> {:x 9
            :y "A"}
           (fmap ->renjin)
           ->env-impl
           type)
      => Environment

      (->> {:x 9
            :y "A"}
           (fmap ->renjin)
           ^Environment
           ->env-impl
           (.getNames)
           vec)
      => ["x" "y"]

      (->> {:x 9
            :y "A"}
           (fmap ->renjin)
           ^Environment
           ->env-impl
           (#(find-variable % :y))
           vec)
      => ["A"])


^{:refer clojuress.renjin.lang/find-variable :added "0.1"}
(fact "find-variable gets the value of a variable
       of a given name in a given environment."

      (->> {:x 9
            :y "A"}
           (fmap ->renjin)
           ^Environment
           ->env-impl
           (#(find-variable % :y))
           vec)
      => ["A"])

^{:refer clojuress.renjin.lang/parse-source :added "0.1"}
(fact "passe-source applies Renjin's parser to a given String of R code."

      (-> "1+2"
          parse-source
          type)
      => ExpressionVector

      (-> "1+2"
          ^ExpressionVector
          parse-source
          (.toString))
      => "expression(+(1.0, 2.0))")

^{:refer clojuress.renjin.lang/eval-expressions-impl :added "0.1"}
(fact "Given an environment,
       and a sequence of strings of R expressions,
       eval-expressions-impl evaluates the expressions in the environment,
       returning the return value of the last one."

      (->> {:x 3 :w 10}
           (fmap ->renjin)
           ->env-impl
           (eval-expressions-impl ["y<-2*x+w"
                                   "y+1"])
           vec)
      => [17.0]

      (->> {:x (range 9)}
           (fmap ->renjin)
           ->env-impl
           (eval-expressions-impl ["summary(x)"])
           ->clj)
      => {:Min.               0.0
          (keyword "1st Qu.") 2.0
          :Median             4.0
          :Mean               4.0
          (keyword "3rd Qu.") 6.0
          :Max.               8.0}

      "For example, let us see how to use kmeans
      (and get the expected results on a trivial example)."
      (let [{:keys [cluster centers]}
            (->> {:x [1 1 1 1 1
                      5 5 5 5 5
                      99 99 99 99 99]}
                 (fmap ->renjin)
                 ->env-impl
                 (eval-expressions-impl ["kmeans(x, centers=2)"])
                 ->clj)]
        (map #(centers (dec %))
             cluster))
      => [3.0 3.0 3.0 3.0 3.0
          3.0 3.0 3.0 3.0 3.0
          99.0 99.0 99.0 99.0 99.0])


^{:refer clojuress.renjin.lang/apply-function-impl :added "0.1"}
(fact "Given an environment,
       and a Renjin function,
       apply-function-impl applies that function
       to the arguments defined by the environment,
       returning its return value."

      (->> {:x [-2 3]}
           (fmap ->renjin)
           ->env-impl
           (apply-function-impl (reval "function(x) abs(x)"))
           ->clj)
      => [2 3])



^{:refer clojuress.renjin.lang/NULL->nil :added "0.1"}
(fact "NULL->nil converts Renjin's NULL to nil; acts as identity otherwise"

      (-> "NULL"
          reval
          NULL->nil
          nil?)
      => true

      (-> "3"
          reval
          NULL->nil
          nil?)
      => false)


^{:refer clojuress.renjin.lang/->attr :added "0.1"}
(fact "->attr extracts attributes of Renjin objects (similar to metadata in Clojure)"
      (= [:data.frame]
         (-> "data.frame(x=1:3)"
             reval
             (->attr :class))))



^{:refer clojuress.renjin.lang/->names :added "0.1"}
(fact "->names extracts the names attribute of a Renjin object"
      (-> "list(a=1, b=2)"
          reval
          ->names)
      => [:a :b])

^{:refer clojuress.renjin.lang/->class :added "0.1"}
(fact "->class extracts the class attribute of a Renjin object"
      (-> "data.frame(x=1:3)"
          reval
          ->class)
      => [:data.frame])


