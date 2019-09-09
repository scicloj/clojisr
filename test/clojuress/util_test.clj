(ns clojuress.util-test
  (:use hara.test)
  (:require [clojuress.util :refer :all]))

^{:refer clojuress.util/first-if-one :added "0.1"}
(fact "Given a vector with a single element, first-if-one extracts that element; acts as identity otherwise."

      (first-if-one [:abc])
      => :abc

      (first-if-one [:abc :def])
      => [:abc :def]

      (first-if-one :abc)
      => :abc)

^{:refer clojuress.util/fmap :added "0.1"}
(fact
 "fmap updates the values of a given map
 using a given function."

 (->> {:a 1
       :b 2}
      (fmap inc))
 => {:a 2
     :b 3})

^{:refer clojuress.util/starts-with? :added "0.1"}
(fact
 "starts-with? checks if a given object s
 is sequential and its first element
 equals a given object v."

 (starts-with? 4 4)
 => false

 (starts-with? [4] 4)
 => true

 (starts-with? [4] 5)
 => false

 (starts-with? [] 5)
 => false

 (starts-with? [4 5] 4)
 => true

 (starts-with? [4 5] 5)
 => false

 (starts-with? '(4) 4)
 => true

 (starts-with? '(4) 5)
 => false

 (starts-with? '() 5)
 => false

 (starts-with? '(4 5) 4)
 => true

 (starts-with? '(4 5) 5)
 => false)
