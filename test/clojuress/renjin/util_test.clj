(ns clojuress.renjin.util-test
  (:require [clojuress.renjin.util :refer :all]
            [hara.test :refer :all]))

^{:refer clojuress.renjin.util/first-if-one :added "0.1"}
(fact "Given a vector with a single element, first-if-one extracts that element; acts as identity otherwise."

      (first-if-one [:abc])
      => :abc

      (first-if-one [:abc :def])
      => [:abc :def]

      (first-if-one :abc)
      => :abc)



^{:refer clojuress.renjin.util/fmap :added "0.1"}
(fact
 "fmap updates the values of a given map
 using a given function"

 (->> {:a 1
       :b 2}
      (fmap inc))
 => {:a 2
     :b 3})
