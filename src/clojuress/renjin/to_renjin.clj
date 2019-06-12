(ns clojuress.renjin.to-renjin
  (:require [clojuress.renjin.engine :refer [engine reval]]
            [clojuress.renjin.lang :refer [eval-expressions-impl ->env-impl]]
            [clojuress.renjin.util :refer [first-if-one fmap]]
            [clojure.walk :as walk]
            [com.rpl.specter :as specter])
  (:import (java.util List)
           (clojure.lang Named)
           org.renjin.invoke.reflection.converters.Converters
           org.renjin.eval.Context
           org.renjin.parser.RParser
           (org.renjin.sexp SEXP Symbol Environment Environment$Builder
                            Vector Null
                            FunctionCall
                            DoubleVector DoubleArrayVector
                            IntVector IntArrayVector
                            LogicalVector LogicalArrayVector
                            StringVector StringArrayVector
                            ListVector ListVector$NamedBuilder
                            AttributeMap)))
(declare ->renjin)


(defn ->string-vector
  "Given a Seqable of Clojure strings/keywords,
        ->string-vector creates a corresponding Renjin StringArrayVector
        of their corresponding names.
 
       (-> [\"abc\" :def]
           ->string-vector
           type)
       => StringArrayVector
 
       (-> [\"abc\" :def]
           ->string-vector
           ->clj)
       => [\"abc\" \"def\"]"
  {:added "0.1"}
  [xs]
  (StringArrayVector.
   ^Iterable
   (map (comp str name) xs)))

(defn ->list-vector
  "->list-vector converts a Clojure Seqable
        to a Renjin ListVector, converting elements by ->renjin.
 
       (-> [[1 2] \"abc\"]
           ->list-vector
           (.toString))
       => \"list(c(1, 2), abc)\""
  {:added "0.1"} [xs]
  (ListVector.
   ^List
   (map ->renjin xs)))

(defn ->named-list-vector
  "Given a Seqable of pairs,
       ->named-list-vector creates a corresponding named Renjin ListVector
       (representing a named R list).
 
       (-> {:a 1
            :b \"A\"}
           ->named-list-vector
           type)
       => ListVector
 
       (->  {:a 1
             :b \"A\"}
           ->named-list-vector
           ->clj)
       => {:a 1.0
           :b \"A\"}"
  {:added "0.1"}
  [pairs]
  (let [builder ^ListVector$NamedBuilder (ListVector/newNamedBuilder)]
    (doseq [[nam value] pairs]
      (.add builder
            ^String (if (instance? Named nam)
                      (name nam)
                      (-> nam ->renjin str))
            ^SEXP (->renjin value)))
    (.build builder)))

(defn ->double-vector
  "Given a seqable of numbers,
        ->double-vector creates a corresponding Renjin DoubleArrayVector
        (representing an R vector).
 
       (-> (range 4)
           ->double-vector
           type)
       => DoubleArrayVector
 
       (-> (range 4)
           ->double-vector
            ->clj)
       => [0.0 1.0 2.0 3.0]"
  {:added "0.1"}
  [xs]
  (->> xs
       (map (fn [x]
              (if (nil? x)
                DoubleVector/NA
                (double x))))
       double-array
       (DoubleArrayVector.)))

(defn ->int-vector
  "Given a seqable of integers,
        ->int-vector creates a corresponding Renjin IntArrayVector
        (representing an R vector).
 
       (-> (range 4)
           ->int-vector
           type)
       => IntArrayVector
 
       (-> (range 4)
           ->int-vector
           ->clj)
       => (range 4)"
  {:added "0.1"}
  [xs]
  (->> xs
       (map (fn [x]
              (if (nil? x)
                IntVector/NA
                (int x))))
       int-array
       (IntArrayVector.)))

(defn ->logical-vector
  "Given a seqable of booleans,
        ->logical-vector creates a corresponding Renjin LogicalArrayVector
        (representing an R vector).
 
       (-> [true false false]
           ->logical-vector
           type)
       => LogicalArrayVector
 
       (-> [true false false]
           ->logical-vector
           ->clj)
       => [true false false]"
  {:added "0.1"}
  [xs]
  (->> xs
       (map (fn [x]
              (if (nil? x)
                LogicalVector/NA
                (if x
                  1
                  0))))
       int-array
       (LogicalArrayVector.)))

(defn ->factor-vector
  "Given a Seqable of Clojure strings/keywords,
        ->factor-vector creates a corresponding Renjin factor
        of their corresponding names.
 
       (-> [\"abc\" :def]
           ->factor-vector
           type)
       => IntArrayVector
 
       (-> [\"abc\" :def]
           ->factor-vector
           lang/->class)
       => [:factor]
 
       (-> [\"abc\" :def]
           ->factor-vector
           ->clj)
       => [:abc :def]"
  {:added "0.1"} [xs]
  (eval-expressions-impl ["factor(x)"]
                         (->env-impl {:x (->string-vector xs)})))

(defn row-maps->named-columns-list
  "Given a seqable of maps with the same keys,
        considered rows of a table,
        row-maps->named-columns-list creates a Renjin list of vectors
        corresponding to rows of that table.
 
       (-> [{:x 1 :y 2 :z \"A\"}
            {:x 3 :y 4 :z \"B\"}]
           row-maps->named-columns-list
           ->clj)
       => {:x [1.0 3.0], :y [2.0 4.0], :z [\"A\" \"B\"]}"
  {:added "0.1"}
  [row-maps]
  (->> row-maps
       (mapcat keys)
       distinct
       (map (fn [k]
              [(name k)
               (map k row-maps)]))
       ->named-list-vector))



(defn ->renjin
  "->renjin converts Clojure data structures
        to corresponding Renjin objects.
 
       \"nil is converted to Renjin's NULL\"
       (-> nil
           ->renjin)
       => Null/INSTANCE
 
       \"Basic data elements are first wrapped by a (singleton) vector,
   and then ->renjin is applied to that vecto .\"
       (-> 1
           ->renjin
           (.toString))
       => \"1.0\"
 
       (-> \"abc\"
           ->renjin
           (.toString))
       => \"abc\"
 
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
       => \"FALSE\"
 
       \"A sequential is converted to a Renjin vector
   of type corresponding to the first element,
   if that element is of one of the types handled by ->renjin.\"
       (-> [2 1]
           ->renjin
           (.toString))
       => \"c(2, 1)\"
 
       (-> [\"abc\" \"def\"]
           ->renjin
           (.toString))
       => \"c(abc, def)\"
 
       (-> [\"abc\" \"def\"]
           ->renjin
           ->clj)
       => [\"abc\" \"def\"]
 
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
       => \"c(FALSE, TRUE)\"
 
       \"A sequential whose first element's type
   is not one of the types handled by ->renjin
   is converted to a Renjin list,
   handling elements recursively.\"
       (-> [[1 2] 3]
           ->renjin
           (.toString))
       => \"list(c(1, 2), 3.0)\"
 
       \"->renjin converts maps to Renjin named ListVectors
   (representing R named lists),
   handling values recursively.\"
       (-> {:a 1
            :b \"A\"}
           ->renjin
           type)
       => ListVector
 
       (-> {:a 1
            :b \"A\"}
           ->renjin
           ->clj)
       =>  {:a 1.0
            :b \"A\"}
 
       (-> {:a 1
            :b \"A\"}
           ->renjin
           (.toString))
       => \"list(a = 1.0, b = A)\"
 
       (-> {:a [1 2]
            :b \"A\"}
           ->renjin
           (.toString))
       => \"list(a = c(1, 2), b = A)\""
  {:added "0.1"}
  [obj]
  (-> (cond
        ;; nil
        (nil? obj)
        Null/INSTANCE
        ;; basic types
        (and (not (instance? clojure.lang.Seqable obj))
             (or (number? obj)
                 (string? obj)
                 (keyword? obj)
                 (instance? Boolean obj)))
        (->renjin [obj])
        ;; a sequential structure
        (sequential? obj)
        (let [elem1 (first obj)]
          (cond
            ;; numbers
            (number? elem1)
            (->double-vector obj)
            ;; strings
            (string? elem1)
            (->string-vector obj)
            ;; keywords
            (keyword? elem1)
            (->factor-vector obj)
            ;; booleans
            (instance? Boolean elem1)
            (->logical-vector obj)
            ;; else
            :else
            (->list-vector (map ->renjin obj))))
        ;; a map
        (map? obj)
        (->named-list-vector obj)
        ;; else
        :else obj)
      (Converters/fromJava)))


