(ns clojuress.renjin.to-renjin
  (:require [clojuress.renjin.engine :refer [engine reval]]
            [clojuress.renjin.lang :refer [eval-expressions-impl ->env-impl]]
            [clojuress.renjin.util :refer [first-if-one fmap]]
            [clojure.walk :as walk]
            [com.rpl.specter :as specter]
            [clojuress.renjin.lang :as lang]
            [clojuress.renjin.engine :as engine])
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


(defn ->character-vector
  "Given a Seqable of Clojure strings/keywords,
        ->character-vector creates a corresponding Renjin StringArrayVector
        of their corresponding names.
 
       (-> [\"abc\" :def]
           ->character-vector
           type)
       => StringArrayVector
 
       (-> [\"abc\" :def]
           ->character-vector
           ->clj)
       => [\"abc\" \"def\"]"
  {:added "0.1"}
  [xs]
  (StringArrayVector.
   ^Iterable
   (map (fn [x]
          (cond (nil? x)            nil
                (instance? Named x) (name x)
                :else               (str x)))
        xs)))

(defn ->list-vector
  "->list-vector converts a Clojure Seqable
        to a Renjin ListVector, converting elements by ->renjin.
 
       (-> [[1 2] \"abc\"]
           ->list-vector
           (.toString))
       => \"list(c(1L, 2L), abc)\""
  {:added "0.1"}
  [xs]
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
       => {:a [1]
           :b [\"A\"]}"
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

(defn ->numeric-vector
  "Given a seqable of numbers,
        ->numeric-vector creates a corresponding Renjin DoubleArrayVector
        (representing an R vector).
 
       (-> (range 4)
           ->numeric-vector
           type)
       => DoubleArrayVector
 
       (-> (range 4)
           ->numeric-vector
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

(defn ->integer-vector
  "Given a seqable of integers,
        ->integer-vector creates a corresponding Renjin IntArrayVector
        (representing an R vector).
 
       (-> (range 4)
           ->integer-vector
           type)
       => IntArrayVector
 
       (-> (range 4)
           ->integer-vector
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
  {:added "0.1"}
  [xs]
  (eval-expressions-impl ["factor(x)"]
                         (->env-impl {:x (->character-vector xs)})))




(defn primitive-type [obj]
  (cond (nil? obj)              :na
        (integer? obj)          :integer
        (number? obj)           :numeric
        (string? obj)           :character
        (keyword? obj)          :factor
        (instance? Boolean obj) :logical
        :else                   nil))

(def valid-interpretations {:na        [:integer :numeric :character :factor :logical]
                            :integer   [:integer :numeric :character]
                            :numeric   [:numeric :character]
                            :character [:character]
                            :factor    [:factor :character]
                            :logical   [:logical :character]})

(def interpretations-priorities
  (->> valid-interpretations
       (mapcat val)
       frequencies))

(defn finest-primitive-type [sequential]
  (let [n-elements (count sequential)]
    (->> sequential
         (mapcat (fn [elem]
                   (-> elem primitive-type valid-interpretations)))
         frequencies
         (filter (fn [[interpration n]]
                   (= n n-elements)))
         (map key)
         (sort-by interpretations-priorities)
         first)))



(def primitive-vector-ctors
  {:integer   ->integer-vector
   :numeric   ->numeric-vector
   :character ->character-vector
   :factor    ->factor-vector
   :logical   ->logical-vector})

(defn ->primitive-vector [sequential]
  (when-let [primitive-type (finest-primitive-type sequential)]
    ((primitive-vector-ctors primitive-type) sequential)))

(defn row-maps->named-columns-list
  "Given a seqable of maps with the same keys,
        considered rows of a table,
        row-maps->named-columns-list creates a Renjin list of vectors
        corresponding to columns of that table.
 
       (-> [{:x 1 :y 2 :z \"A\"}
            {:x 3 :y 4 :z \"B\"}]
           row-maps->named-columns-list
           type)
       => ListVector
 
       (-> [{:x 1 :y 2.0 :z \"A\"}
            {:x 3 :y 4 :z \"B\"}]
           row-maps->named-columns-list
           ->clj)
       => {:x [1 3],
           :y [2.0 4.0],
           :z [\"A\" \"B\"]}"
  {:added "0.1"}
  [row-maps]
  (and (sequential? row-maps)
       (every? map? row-maps)
       (let [attempt (->> row-maps
                          (mapcat keys)
                          distinct
                          (mapcat (fn [k]
                                    [(name k)
                                     (->> row-maps
                                          (map k)
                                          ->primitive-vector)]))
                          (apply array-map))]
         (if (->> attempt
                  vals
                  (some nil?))
           nil
           (->named-list-vector attempt)))))

(defn row-maps->df
  "Given a seqable of maps with the same keys,
        considered rows of a table,
        row-maps->df creates a Renjin data.frame
        corresponding to these data.
 
       (-> [{:x 1 :y 2 :z \"A\"}
            {:x 3 :y 4 :z \"B\"}]
           row-maps->df
           lang/->class)
       => [:data.frame]
 
       (-> [{:x 1 :y 2.0 :z \"A\"}
            {:x 3 :y 4 :z \"B\"}]
           row-maps->df
           ->clj)
       => [{:x 1 :y 2.0 :z \"A\"}
           {:x 3 :y 4.0 :z \"B\"}]"
  {:added "0.1"}
  [row-maps]
  (when-let [named-columns-list (row-maps->named-columns-list row-maps)]
    (lang/apply-function-impl (engine/reval
                               "function(columns) data.frame(columns, stringsAsFactors =FALSE)")
                              (lang/->env-impl
                               {:columns named-columns-list}))))



(defn row-vectors->matrix
  [row-vectors]
  (and (sequential? row-vectors)
       (every? sequential? row-vectors)
       (->> row-vectors (map count) distinct count (= 1))
       (when-let [one-primitive-vector (->> row-vectors
                                            (apply concat)
                                            ->primitive-vector)]
         (lang/apply-function-impl (engine/reval
                                    "function(v,nr) matrix(v,nr)")
                                   (lang/->env-impl
                                    {:v  one-primitive-vector
                                     :nr (->renjin (count row-vectors))})))))

(defn ->renjin
  "->renjin converts Clojure data structures
        to corresponding Renjin objects.
 
       \"nil is converted to Renjin's NULL\"
       (-> nil
           ->renjin)
       => Null/INSTANCE
 
       \"Primitive data elements are first wrapped by a vector,
   to which then ->renjin is re-applied,
   this converting to the corresponding (singleton) ->renjin vector.\"
       (-> 1
           ->renjin
           (.toString))
       => \"c(1L)\"
 
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
       => [:a]
 
       (-> false
           ->renjin
           (.toString))
       => \"FALSE\"
 
       \"A sequential of primitive elements is converted to a Renjin vector
   of the finest type possible.\"
       (-> [2 1]
           ->renjin
           (.toString))
       => \"c(2L, 1L)\"
 
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
 
       (-> [1 32] ->renjin ->clj)
       => [1 32]
 
       (-> [1 2.4 32] ->renjin ->clj)
       => [1.0 2.4 32.0]
 
       (-> [1 \"A\" 2.4 32] ->renjin ->clj)
       => [\"1\" \"A\" \"2.4\" \"32\"]
 
       (-> [1 \"A\" true 2.4 32] ->renjin ->clj)
       => [\"1\" \"A\" \"true\" \"2.4\" \"32\"]
 
       (-> [\"A\" true false] ->renjin ->clj)
       => [\"A\" \"true\" \"false\"]
 
       (-> [true false] ->renjin ->clj)
       => [true false]
 
       (-> [\"A\" :true :false] ->renjin ->clj)
       => [\"A\" \"true\" \"false\"]
 
       (-> [:true :false] ->renjin ->clj)
       => [:true :false]
 
       \"A sequential of maps containing elements
   is converted to a renjin data frame.\"
       (-> [{:a 1 :b 4}
            {:a 3 :c \"hi!\"}]
           ->renjin
           lang/->class)
       => [:data.frame]
 
       (-> [{:a 1 :b 4}
            {:a 3 :c \"hi!\"}]
           ->renjin
           ->clj)
       => [{:a 1 :b 4 :c nil}
           {:a 3 :b nil :c \"hi!\"}]
 
       \"A sequential of primitive vectors of the same length
   is converted to a matrix.\"
       (-> [[1 2 3]
            [4 5 6]]
           ->renjin
           ->clj)
       => [[1 2 3]
           [4 5 6]]
 
       \"A sequential with some non-primitive elements
   is converted to a Renjin list,
   handling elements recursively.\"
       (-> [[1 2] 3]
           ->renjin
           (.toString))
       => \"list(c(1L, 2L), c(3L))\"
 
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
       =>  {:a [1]
            :b [\"A\"]}
 
       (-> {:a 1
            :b \"A\"}
           ->renjin
           (.toString))
       => \"list(a = c(1L), b = A)\"
 
       (-> {:a [1 2]
            :b \"A\"}
           ->renjin
           (.toString))
       => \"list(a = c(1L, 2L), b = A)\""
  {:added "0.1"}
  [obj]
  (cond
    ;; nil
    (nil? obj)
    Null/INSTANCE
    ;; basic types
    (primitive-type obj)
    (->renjin [obj])
    ;; a sequential structure
    (sequential? obj)
    (or (->primitive-vector obj)
        (row-maps->df obj)
        (row-vectors->matrix obj)
        (->list-vector obj))
    ;; a map
    (map? obj)
    (->named-list-vector obj)
    ;; else
    :else (Converters/fromJava obj)))
