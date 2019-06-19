(ns clojuress.renjin.lang
  (:require [clojuress.renjin.engine :refer [reval runtime-context]]
            [clojuress.renjin.lang :refer :all])
  (:import (org.renjin.sexp Symbol Closure Environment Environment$Builder Null SEXP)
           (org.renjin.eval Context)
           (org.renjin.parser RParser)))

(defn ->symbol
  "->symbol creates a Renjin symbol.
 
       (-> :x
           ->symbol
           type)
       => Symbol
 
       (-> :x
           ^Symbol
           ->symbol
           (.asString))
       => \"x\""
  {:added "0.1"}
  [k]
  (Symbol/get (name k)))


(defn ->env-impl
  "->env-impl creates an environment
        corresponding to given bindings
        specified as a map from names to Renjin objects.
 
       (->> {:x 9
             :y \"A\"}
            (fmap ->renjin)
            ->env-impl
            type)
       => Environment
 
       (->> {:x 9
             :y \"A\"}
            (fmap ->renjin)
            ^Environment
            ->env-impl
            (.getNames)
            vec)
       => [\"x\" \"y\"]
 
       (->> {:x 9
             :y \"A\"}
            (fmap ->renjin)
            ^Environment
            ->env-impl
            (#(find-variable % :y))
            vec)
       => [\"A\"]"
  {:added "0.1"}
  [bindings-map]
  (let [child-env (->> ^Context (runtime-context)
                       (.getEnvironment)
                       (Environment/createChildEnvironment))]
    (doseq [[k v] bindings-map]
      (.setVariable ^Environment$Builder child-env
                    ^Symbol (->symbol k)
                    ^SEXP v))
    (.build child-env)))

(defn find-variable
  "find-variable gets the value of a variable
        of a given name in a given environment.
 
       (->> {:x 9
             :y \"A\"}
            (fmap ->renjin)
            ^Environment
            ->env-impl
            (#(find-variable % :y))
            vec)
       => [\"A\"]"
  {:added "0.1"}
  [^Environment env nam]
  (.findVariable env (->symbol nam)))


(defn parse-source
  "passe-source applies Renjin's parser to a given String of R code.
 
       (-> \"1+2\"
           parse-source
           type)
       => ExpressionVector
 
       (-> \"1+2\"
           ^ExpressionVector
           parse-source
           (.toString))
       => \"expression(+(1.0, 2.0))\""
  {:added "0.1"}
  [^String r-code]
  (RParser/parseSource r-code))

(defn eval-expressions-impl
  "Given an environment,
        and a sequence of strings of R expressions,
        eval-expressions-impl evaluates the expressions in the environment,
        returning the return value of the last one.
 
       (->> {:x 3 :w 10}
            (fmap ->renjin)
            ->env-impl
            (eval-expressions-impl [\"y<-2*x+w\"
                                    \"y+1\"])
            vec)
       => [17.0]
 
       (->> {:x (range 9)}
            (fmap ->renjin)
            ->env-impl
            (eval-expressions-impl [\"summary(x)\"])
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
                  (fmap ->renjin)
                  ->env-impl
                  (eval-expressions-impl [\"kmeans(x, centers=2)\"])
                  ->clj)]
         (map #(centers (dec %))
              cluster))
       => [3.0 3.0 3.0 3.0 3.0
           3.0 3.0 3.0 3.0 3.0
           99.0 99.0 99.0 99.0 99.0]"
  {:added "0.1"}
  [r-codes env]
  (let [context (.beginEvalContext ^Context (runtime-context)
                                   env)]
    (->> r-codes
         (map (fn [s]
                (.evaluate context
                           (parse-source (str s "\n")))))
         last)))

(defn apply-function-impl
  "Given an environment,
        and a Renjin function,
        apply-function-impl applies that function
        to the arguments defined by the environment,
        returning its return value.
 
       (->> {:x [-2 3]}
            (fmap ->renjin)
            ->env-impl
            (apply-function-impl (reval \"function(x) abs(x)\"))
            ->clj)
       => [2 3]"
  {:added "0.1"}
  [^Closure closure env]
  (let [context (.beginEvalContext ^Context (runtime-context)
                                   env)]
    (.doApply closure context)))


(defn NULL->nil
  "NULL->nil converts Renjin's NULL to nil; acts as identity otherwise
 
       (-> \"NULL\"
           reval
           NULL->nil
           nil?)
       => true
 
       (-> \"3\"
           reval
           NULL->nil
           nil?)
       => false"
  {:added "0.1"}
  [obj]
  (if (= Null/INSTANCE obj)
    nil
    obj))

;; Extracting attributes of Renjin objects
;; (similar to Clojure metadata)
(defn ->attr
  "->attr extracts attributes of Renjin objects (similar to metadata in Clojure)
       (= [:data.frame]
          (-> \"data.frame(x=1:3)\"
              reval
              (->attr :class)))"
  {:added "0.1"}
  [^SEXP sexp attr-name]
  (-> sexp
       (.getAttribute (Symbol/get (name attr-name)))
       NULL->nil
       (->> (mapv #(if (string? %)
                     (keyword %)
                     %)))))

(defn ->names
  "->names extracts the names attribute of a Renjin object
       (-> \"list(a=1, b=2)\"
           reval
           ->names)
       => [:a :b]"
  {:added "0.1"}
  [^SEXP sexp]
  (some->> (->attr sexp "names")
           (mapv keyword)))


(defn ->class
  "->class extracts the class attribute of a Renjin object
       (-> \"data.frame(x=1:3)\"
           reval
           ->class)
       => [:data.frame]"
  {:added "0.1"}
  [^SEXP sexp]
  (some->> (->attr sexp "class")
           (mapv keyword)))

