(ns clojuress.renjin.to-renjin-test
  (:use hara.test)
  (:require [clojuress.renjin.to-renjin :refer :all]
            [clojuress.renjin.to-clj :refer [->clj]]
            [clojuress.renjin.lang :as lang]
            [com.rpl.specter :as specter]
            [clojuress.renjin.engine :refer [engine reval]])
  (:import (org.renjin.sexp Null ListVector StringArrayVector DoubleArrayVector IntArrayVector LogicalArrayVector)))

^{:refer clojuress.renjin.to-renjin/->renjin :added "0.1"}
(fact "->renjin converts Clojure data structures
       to corresponding Renjin objects."

      "nil is converted to Renjin's NULL"
      (-> nil
          ->renjin)
      => Null/INSTANCE

      "Primitive data elements are first wrapped by a vector,
       to which then ->renjin is re-applied,
       this converting to the corresponding (singleton) ->renjin vector."
      (-> 1
          ->renjin
          (.toString))
      => "c(1L)"

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
      => [:a]

      (-> false
          ->renjin
          (.toString))
      => "FALSE"

      "A sequential of primitive elements is converted to a Renjin vector
       of the finest type possible."
      (-> [2 1]
          ->renjin
          (.toString))
      => "c(2L, 1L)"

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

      (-> [1 32] ->renjin ->clj)
      => [1 32]

      (-> [1 2.4 32] ->renjin ->clj)
      => [1.0 2.4 32.0]

      (-> [1 "A" 2.4 32] ->renjin ->clj)
      => ["1" "A" "2.4" "32"]

      (-> [1 "A" true 2.4 32] ->renjin ->clj)
      => ["1" "A" "true" "2.4" "32"]

      (-> ["A" true false] ->renjin ->clj)
      => ["A" "true" "false"]

      (-> [true false] ->renjin ->clj)
      => [true false]

      (-> ["A" :true :false] ->renjin ->clj)
      => ["A" "true" "false"]

      (-> [:true :false] ->renjin ->clj)
      => [:true :false]

      "A sequential of maps containing elements
      is converted to a renjin data frame."
      (-> [{:a 1 :b 4}
           {:a 3 :c "hi!"}]
          ->renjin
          lang/->class)
      => [:data.frame]

      (-> [{:a 1 :b 4}
           {:a 3 :c "hi!"}]
          ->renjin
          ->clj)
      => [{:a 1 :b 4 :c nil}
          {:a 3 :b nil :c "hi!"}]

      "A sequential of primitive vectors of the same length
      is converted to a matrix."
      (-> [[1 2 3]
           [4 5 6]]
          ->renjin
          ->clj)
      => [[1 2 3]
          [4 5 6]]

      "A sequential with some non-primitive elements
       is converted to a Renjin list,
       handling elements recursively."
      (-> [[1 2] 3]
          ->renjin
          (.toString))
      => "list(c(1L, 2L), c(3L))"

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
      =>  {:a [1]
           :b ["A"]}

      (-> {:a 1
           :b "A"}
          ->renjin
          (.toString))
      => "list(a = c(1L), b = A)"

      (-> {:a [1 2]
           :b "A"}
          ->renjin
          (.toString))
      => "list(a = c(1L, 2L), b = A)")

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
      => {:a [1]
          :b ["A"]})

^{:refer clojuress.renjin.to-renjin/->numeric-vector :added "0.1"}
(fact "Given a seqable of numbers,
       ->numeric-vector creates a corresponding Renjin DoubleArrayVector
       (representing an R vector)."

      (-> (range 4)
          ->numeric-vector
          type)
      => DoubleArrayVector

      (-> (range 4)
          ->numeric-vector
           ->clj)
      => [0.0 1.0 2.0 3.0])

^{:refer clojuress.renjin.to-renjin/->integer-vector :added "0.1"}
(fact "Given a seqable of integers,
       ->integer-vector creates a corresponding Renjin IntArrayVector
       (representing an R vector)."

      (-> (range 4)
          ->integer-vector
          type)
      => IntArrayVector

      (-> (range 4)
          ->integer-vector
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

^{:refer clojuress.renjin.to-renjin/->character-vector :added "0.1"}
(fact "Given a Seqable of Clojure strings/keywords,
       ->character-vector creates a corresponding Renjin StringArrayVector
       of their corresponding names."

      (-> ["abc" :def]
          ->character-vector
          type)
      => StringArrayVector

      (-> ["abc" :def]
          ->character-vector
          ->clj)
      => ["abc" "def"])

^{:refer clojuress.renjin.to-renjin/->list-vector :added "0.1"}
(fact "->list-vector converts a Clojure Seqable
       to a Renjin ListVector, converting elements by ->renjin."

      (-> [[1 2] "abc"]
          ->list-vector
          (.toString))
      => "list(c(1L, 2L), abc)")


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



^{:refer clojuress.renjin.to-renjin/row-maps->named-columns-list :added "0.1"}
(fact "Given a seqable of maps with the same keys,
       considered rows of a table,
       row-maps->named-columns-list creates a Renjin list of vectors
       corresponding to columns of that table."

      (-> [{:x 1 :y 2 :z "A"}
           {:x 3 :y 4 :z "B"}]
          row-maps->named-columns-list
          type)
      => ListVector

      (-> [{:x 1 :y 2.0 :z "A"}
           {:x 3 :y 4 :z "B"}]
          row-maps->named-columns-list
          ->clj)
      => {:x [1 3],
          :y [2.0 4.0],
          :z ["A" "B"]})



^{:refer clojuress.renjin.to-renjin/row-maps->df :added "0.1"}
(fact "Given a seqable of maps with the same keys,
       considered rows of a table,
       row-maps->df creates a Renjin data.frame
       corresponding to these data."

      (-> [{:x 1 :y 2 :z "A"}
           {:x 3 :y 4 :z "B"}]
          row-maps->df
          lang/->class)
      => [:data.frame]

      (-> [{:x 1 :y 2.0 :z "A"}
           {:x 3 :y 4 :z "B"}]
          row-maps->df
          ->clj)
      => [{:x 1 :y 2.0 :z "A"}
          {:x 3 :y 4.0 :z "B"}])
