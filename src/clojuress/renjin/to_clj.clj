(ns clojuress.renjin.to-clj
  (:require [clojuress.renjin.engine :refer [engine reval]]
            [clojuress.renjin.util :refer [first-if-one]]
            [clojuress.renjin.lang :as lang]
            [clojure.walk :as walk])
  (:import javax.script.ScriptEngineManager
           org.renjin.script.RenjinScriptEngine
           org.renjin.invoke.reflection.converters.Converters
           org.renjin.eval.Context
           org.renjin.parser.RParser
           (org.renjin.sexp SEXP Symbol Environment Environment$Builder
                            ListVector IntVector DoubleArrayVector Vector Null
                            FunctionCall
                            Logical)))

(defprotocol Clojable
  (-->clj [this]))

(defn ->clj
  "->clj converts Renjin objects
   to corresponding Clojure data structures.
 
  \"Unless defined otherwise, an object is converted to itself.\"
  (-> #inst \"2016-01-01\"
      ->clj)
  => #inst \"2016-01-01\"
 
  (->clj nil)
  => nil
 
  (->clj {:a 3})
  => {:a 3}
 
  (->clj [:a :b])
  => [:a :b]
 
  \"A Renjin data frame is converted to a sequence of maps.\"
  (-> \"data.frame(x=0:3,y=(0:3)^2)\"
      reval
      ->clj)
  => [{:x 0, :y 0.0}
      {:x 1, :y 1.0}
      {:x 2, :y 4.0}
      {:x 3, :y 9.0}]
 
  \"A Renjin matrix frame is converted to a vector of vectors\"
   (-> \"matrix(1:6, 2)\"
       reval
      ->clj)
   => [[1 2 3]
       [4 5 6]]
 
  \"A named Renjin vector or list that is not a data frame
   is converted to a Clojure map,
   whose values are converted recursively.
   Primitive values are always wrapped with a vector.\"
  (-> \"list(a=12, b='abc')\"
      reval
      ->clj)
  => {:a [12.0] :b [\"abc\"]}
 
  (-> \"c(a='abc', b='def')\"
      reval
      ->clj)
  => {:a \"abc\" :b \"def\"}
 
  (-> \"list(a=c(1,2), b='abc')\"
      reval
      ->clj)
  => {:a [1.0 2.0] :b [\"abc\"]}
 
  \"An unnamed Renjin vector or list
   is converted to a Clojure vector,
   whose elements are converted recursively.
   NA values are converted to nil.\"
 
  (-> \"c(1L,NA)\"
      reval
      ->clj)
  => [1 nil]
 
  (-> \"c(1,2)\"
      reval
      ->clj)
  => [1.0 2.0]
 
  (-> \"list(12, 'abc')\"
      reval
      ->clj)
  => [[12.0] [\"abc\"]]
 
  (-> \"c('abc', 'def')\"
      reval
      ->clj)
  => [\"abc\" \"def\"]
 
  (-> \"c(T,F,F)\"
      reval
      ->clj)
  => [true false false]
 
  (-> \"list(c(1,2), 'abc')\"
      reval
      ->clj)
  => [[1.0 2.0] [\"abc\"]]
 
  \"If a Renjin IntVector is not numeric,
   it means that it represents a so called R 'factor'.
   Its values are converted to keywords
   corresponding to its so called 'levels'.\"
 
  (-> \"factor(c('b','b','a'), levels=c('a','b'))\"
      reval
      ->clj)
  => [:b :b :a]
 
  (-> \"structure(factor(c('b','b','a'), levels=c('a','b')), names=c('x','y','z'))\"
      reval
      ->clj)
  => {:x :b
      :y :b
      :z :a}
 
  \"Renjin logical values are converted to Clojure booleans.\"
  (-> \"TRUE\"
      reval
      ->clj)
  => [true]
  (-> \"FALSE\"
      reval
      ->clj)
  => [false]
 
  \"Renjin symbols are converted to Clojure symbols.\"
  (-> \"abc\"
      lang/->symbol
      ->clj)
  => 'abc"
  {:added "0.1"}
  [obj]
  (some-> obj
          -->clj))

(set! *warn-on-reflection* true)


(extend-type Object
  Clojable
  (-->clj [this] this))


;; Renjin represents a dataframe as a ListVector.
;; Its elements are are the columns,
;; and the "names" attribute holds the column names.
(defn df->maps
  "df->maps converts a Renjin data frame into a sequence of Clojure maps
  (-> \"data.frame(x=0:3,y=(0:3)^2)\"
      reval
      df->maps)
  (=> [{:x 0, :y 0.0}
      {:x 1, :y 1.0}
       {:x 2, :y 4.0}
       {:x 3, :y 9.0}])"
  {:added "0.1"}
  [^ListVector df]
  (let [column-names (map keyword (lang/->attr df :names))]
    (->> df
         (map ->clj)
         (apply map (fn [& row-elements]
                      (zipmap column-names row-elements))))))

(defn renjin-vector->clj
  "renjin-vector->clj is an auxiliary function for the implementation of ->clj.
  It behaves behaves as ->clj would, acting on Renjin vectors or lists,
  but has an additional argument - transf - a transformation to apply to the elements.
  (->> \"c(1,2)\"
       reval
       (renjin-vector->clj inc))
  => [2.0 3.0]
  
  (->> \"c(x=1,y=2)\"
       reval
       (renjin-vector->clj inc))
  => {:x 2.0 :y 3.0}"
  {:added "0.1"}
  [transf v]
  (if (= (lang/->class v) [:data.frame])
    (df->maps v)
    (let [names (lang/->names v)
          dim   (lang/->attr v :dim)]
      (->> v
           (map-indexed (fn [i x]
                          (when (not (.isElementNA ^Vector v ^int i))
                            (-> x
                                ->clj
                                transf))))
           ((if (seq names)
              ;; A named list or vector will be translated to a map.
              (partial zipmap names)
              (if (seq dim)
                ;; A matrix will be translated to a vector of vectors
                (fn [values]
                  (->> values
                       (partition (second dim))
                       (#(do (println %) %))
                       (mapv vec)))
                ;; A regular list or vector will be translated to a vector.
                vec)))))))

(extend-type Vector
  Clojable
  (-->clj [this]
    (renjin-vector->clj identity
                        this)))

(extend-type IntVector
  Clojable
  (-->clj [this]
    (if (.isNumeric this)
      (renjin-vector->clj identity
                          this)
      ;; else - a factor
      (renjin-vector->clj  (comp (lang/->attr this :levels)
                                 dec)
                           this))))

(extend-type Logical
  Clojable
  (-->clj [this]
    ({Logical/TRUE  true
      Logical/FALSE false}
     this)))

(extend-type Symbol
  Clojable
  (-->clj [this]
    (symbol (.toString this))))

(extend-type Null
  Clojable
  (-->clj [this]
    nil))



