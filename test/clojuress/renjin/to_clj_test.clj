(ns clojuress.renjin.to-clj-test
  (:require [clojuress.renjin.engine :refer [reval]]
            [clojuress.renjin.lang :as lang]
            [clojuress.renjin.to-clj :refer :all]
            [hara.test :refer :all]))



^{:refer clojuress.renjin.to-clj/->clj :added "0.1"}
(fact
 "->clj converts Renjin objects
  to corresponding Clojure data structures."

 "Unless defined otherwise, an object is converted to itself."
 (-> #inst "2016-01-01"
     ->clj)
 => #inst "2016-01-01"

 (->clj {:a 3})
 => {:a 3}

 (->clj [:a :b])
 => [:a :b]

 "If the result of conversion is a single-element vector,
  then as a last step, it is converted to its single object."
 (->clj [:a])
 => :a

 (-> "1"
     reval
     ->clj)
 => 1.0

 "A Renjin data frame is converted to a sequence of maps."
 (-> "data.frame(x=0:3,y=(0:3)^2)"
     reval
     ->clj)
 => [{:x 0, :y 0.0}
     {:x 1, :y 1.0}
     {:x 2, :y 4.0}
     {:x 3, :y 9.0}]

 "A named Renjin vector or list that is not a data frame
  is converted to a Clojure map,
  whose values are converted recursively."
 (-> "list(a=12, b='abc')"
     reval
     ->clj)
 => {:a 12.0 :b "abc"}

 (-> "c(a='abc', b='def')"
     reval
     ->clj)
 => {:a "abc" :b "def"}

 (-> "list(a=c(1,2), b='abc')"
     reval
     ->clj)
 => {:a [1.0 2.0] :b "abc"}

 "An unnamed Renjin vector or list
  is converted to a Clojure vector,
  whose elements are coverted recursively."
 (-> "c(1,2)"
     reval
     ->clj)
 => [1.0 2.0]

 (-> "list(12, 'abc')"
     reval
     ->clj)
 => [12.0 "abc"]

 (-> "c('abc', 'def')"
     reval
     ->clj)
 => ["abc" "def"]

 (-> "c(T,F,F)"
     reval
     ->clj)
 => [true false false]

 (-> "list(c(1,2), 'abc')"
     reval
     ->clj)
 => [[1.0 2.0] "abc"]

 "A Renjin IntVector that is numeric is handled like any vector.
 Otherwise, it represents a so called R 'factor',
 so its values are converted to keywords
 corresponding to its so called 'levels'."

 (-> "factor(c('b','b','a'), levels=c('a','b'))"
     reval
     ->clj)
 => [:b :b :a]

 (-> "structure(factor(c('b','b','a'), levels=c('a','b')), names=c('x','y','z'))"
     reval
     ->clj)
 => {:x :b
     :y :b
     :z :a}

 "Renjin logical values are converted to Clojure booleans."
 (-> "TRUE"
     reval
     ->clj)
 => true
 (-> "FALSE"
     reval
     ->clj)
 => false

 "Renjin symbols are converted to Clojure symbols."
 (-> "abc"
     lang/->symbol
     ->clj)
 => 'abc)


^{:refer clojuress.renjin.to-clj/df->maps :added "0.1"}
(fact
 "df->maps converts a Renjin data frame into a sequence of Clojure maps"
 (-> "data.frame(x=0:3,y=(0:3)^2)"
     reval
     df->maps)
 (=> [{:x 0, :y 0.0}
      {:x 1, :y 1.0}
      {:x 2, :y 4.0}
      {:x 3, :y 9.0}]))



^{:refer clojuress.renjin.to-clj/renjin-vector->clj :added "0.1"}

(fact
 "renjin-vector->clj is an auxiliary function for the implementation of ->clj.
 It behaves behaves as ->clj would, acting on Renjin vectors or lists,
 but has an additional argument - transf - a transformation to apply to the elements."
 (->> "c(1,2)"
      reval
      (renjin-vector->clj inc))
 => [2.0 3.0]

 (->> "c(x=1,y=2)"
      reval
      (renjin-vector->clj inc))
 => {:x 2.0 :y 3.0})
