(ns clojuress.renjin.to-renjin-test
  (:use hara.test)
  (:require [clojuress.renjin.to-renjin :refer :all]
            [clojuress.renjin.to-clj :refer [->clj]]
            [clojuress.renjin.engine :refer [reval]]
            [clojuress.renjin.lang :as lang]
            [com.rpl.specter :as specter])
  (:import (org.renjin.sexp Null ListVector StringArrayVector DoubleArrayVector IntArrayVector LogicalArrayVector)))

^{:refer clojuress.renjin.to-renjin/->renjin :added "0.1"}
(fact "->renjin converts Clojure data structures
       to corresponding Renjin objects."

      "nil is converted to Renjin's NULL"
      (-> nil
          ->renjin)
      => Null/INSTANCE

      "Basic data elements are first wrapped by a (singleton) vector,
       and then ->renjin is applied to that vecto ."
      (-> 1
          ->renjin
          (.toString))
      => "1.0"

      (-> "abc"
          ->renjin
          (.toString))
      => "abc"

      (-> :a
          ->renjin
          lang/->class)
      => [:factor]

      (-> :a
          ->renjin
          ->clj)
      => :a

      (-> false
          ->renjin
          (.toString))
      => "FALSE"

      "A sequential is converted to a Renjin vector
       of type corresponding to the first element,
       if that element is of one of the types handled by ->renjin."
      (-> [2 1]
          ->renjin
          (.toString))
      => "c(2, 1)"

      (-> ["abc" "def"]
          ->renjin
          (.toString))
      => "c(abc, def)"

      (-> ["abc" "def"]
          ->renjin
          ->clj)
      => ["abc" "def"]

      (-> [:abc :def]
          ->renjin
          lang/->class)
      => [:factor]

      (-> [:abc :def]
          ->renjin
          ->clj)
      => [:abc :def]

      (-> [false true]
          ->renjin
          (.toString))
      => "c(FALSE, TRUE)"

      "A sequential whose first element's type
       is not one of the types handled by ->renjin
       is converted to a Renjin list,
       handling elements recursively."
      (-> [[1 2] 3]
          ->renjin
          (.toString))
      => "list(c(1, 2), 3.0)"

      "->renjin converts maps to Renjin named ListVectors
       (representing R named lists),
       handling values recursively."
      (-> {:a 1
           :b "A"}
          ->renjin
          type)
      => ListVector

      (-> {:a 1
           :b "A"}
          ->renjin
          ->clj)
      =>  {:a 1.0
           :b "A"}

      (-> {:a 1
           :b "A"}
          ->renjin
          (.toString))
      => "list(a = 1.0, b = A)"

      (-> {:a [1 2]
           :b "A"}
          ->renjin
          (.toString))
      => "list(a = c(1, 2), b = A)")

^{:refer clojuress.renjin.to-renjin/->named-list-vector :added "0.1"}
(fact "Given a Seqable of pairs,
      ->named-list-vector creates a corresponding named Renjin ListVector
      (representing a named R list)."

      (-> {:a 1
           :b "A"}
          ->named-list-vector
          type)
      => ListVector

      (->  {:a 1
            :b "A"}
          ->named-list-vector
          ->clj)
      => {:a 1.0
          :b "A"})

^{:refer clojuress.renjin.to-renjin/->double-vector :added "0.1"}
(fact "Given a seqable of numbers,
       ->double-vector creates a corresponding Renjin DoubleArrayVector
       (representing an R vector)."

      (-> (range 4)
          ->double-vector
          type)
      => DoubleArrayVector

      (-> (range 4)
          ->double-vector
           ->clj)
      => [0.0 1.0 2.0 3.0])

^{:refer clojuress.renjin.to-renjin/->int-vector :added "0.1"}
(fact "Given a seqable of integers,
       ->int-vector creates a corresponding Renjin IntArrayVector
       (representing an R vector)."

      (-> (range 4)
          ->int-vector
          type)
      => IntArrayVector

      (-> (range 4)
          ->int-vector
          ->clj)
      => (range 4))

^{:refer clojuress.renjin.to-renjin/->logical-vector :added "0.1"}
(fact "Given a seqable of booleans,
       ->logical-vector creates a corresponding Renjin LogicalArrayVector
       (representing an R vector)."

      (-> [true false false]
          ->logical-vector
          type)
      => LogicalArrayVector

      (-> [true false false]
          ->logical-vector
          ->clj)
      => [true false false])

^{:refer clojuress.renjin.to-renjin/->string-vector :added "0.1"}
(fact "Given a Seqable of Clojure strings/keywords,
       ->string-vector creates a corresponding Renjin StringArrayVector
       of their corresponding names."

      (-> ["abc" :def]
          ->string-vector
          type)
      => StringArrayVector

      (-> ["abc" :def]
          ->string-vector
          ->clj)
      => ["abc" "def"])

^{:refer clojuress.renjin.to-renjin/row-maps->named-columns-list :added "0.1"}
(fact "Given a seqable of maps with the same keys,
       considered rows of a table,
       row-maps->named-columns-list creates a Renjin list of vectors
       corresponding to rows of that table."

      (-> [{:x 1 :y 2 :z "A"}
           {:x 3 :y 4 :z "B"}]
          row-maps->named-columns-list
          ->clj)
      => {:x [1.0 3.0], :y [2.0 4.0], :z ["A" "B"]})


^{:refer clojuress.renjin.to-renjin/->list-vector :added "0.1"}
(fact "->list-vector converts a Clojure Seqable
       to a Renjin ListVector, converting elements by ->renjin."

      (-> [[1 2] "abc"]
          ->list-vector
          (.toString))
      => "list(c(1, 2), abc)")


^{:refer clojuress.renjin.to-renjin/->factor-vector :added "0.1"}
(fact "Given a Seqable of Clojure strings/keywords,
       ->factor-vector creates a corresponding Renjin factor
       of their corresponding names."

      (-> ["abc" :def]
          ->factor-vector
          type)
      => IntArrayVector

      (-> ["abc" :def]
          ->factor-vector
          lang/->class)
      => [:factor]

      (-> ["abc" :def]
          ->factor-vector
          ->clj)
      => [:abc :def])
