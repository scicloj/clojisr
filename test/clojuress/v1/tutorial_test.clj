(ns clojuress.v1.tutorial-test
  (:require [notespace.v1.note :as note
             :refer [note note-void note-md note-as-md note-hiccup note-as-hiccup]]
            [tech.ml.dataset :as dataset]
            [clojuress.v1.r :as r]))

(note-md "# Clojuress tutorial")

(note-md "## Basic examples")

(note-md "Let us start by some basic usage examples of Clojuress.")

(note-void
 (require '[clojuress.v1.r :as r :refer [r eval-r->java r->java java->r java->clj clj->java r->clj clj->r ->code r+ colon function]]
          '[clojuress.v1.require :refer [require-r]]
          '[clojuress.v1.robject :as robject]
          '[clojuress.v1.session :as session]
          '[tech.ml.dataset :as dataset]
          '[notespace.v1.util :refer [check]]))

(note-md "First, let us make sure there are no R sessions currently running.")

(note-void
 (r/discard-all-sessions))

(note-md "Now let us run some R code, and keep a Clojure handle to the return value.")

(note-void
 (def x (r "1+2")))

(note-md "Convert the R to Clojure:")

(note
 (->> x
      r->clj
      (check = [3.0])))

(note-md "Run some code on a separate session (specified Rserve port, rather than the default one).")

(note
 (-> "1+2"
     (r :session-args {:port 4444})
     r->clj
     (->> (check = [3.0]))))

(note-md "Convert Clojure data to R data. Note that `nil` is turned to `NA`.")

(note
 (-> [1 nil 3]
     clj->r))

(note-md "## Functions")

(note-md "We can define a Clojure function wrapping an R function.")

(note-void
 (def f (function (r "function(x) x*10"))))

(note-md "Let us apply it to Clojure data (implicitly converting that data to R).")

(note
 (->> 5
      f
      r->clj
      (check = [50.0])))

(note-md "We can also apply it to R data.")

(note
 (->> "5*5"
      r
      f
      r->clj
      (check = [250.0])))

(note-md "Functions can get named arguments.
Here we pass the `na.rm` argument,
that tells R whether to remove missing values
whenn computing the mean.")

(note
 (->> ((function (r "mean"))
            [1 nil 3]
            :na.rm true)
           r->clj
           (check = [2.0])))

(note-md "An alternative call syntax:")

(note
 (->> ((function (r "mean"))
            [1 nil 3]
            [:= :na.rm true])
           r->clj
           (check = [2.0])))

(note-md "Anoter example:")

(note
 (let [f (->> "function(w,x,y=10,z=20) w+x+y+z"
              r
              function)]
   (->> [(f 1 2)
         (f 1 2 :y 100)
         (f 1 2 :z 100)]
        (map r->clj)
        (check = [[33.0] [123.0] [113.0]]))))

(note-md "Some functions are already created in Clojuress and given special names for convenience. For example:")

(note
 (->> (r+ 1 2 3)
      r->clj
      (check = [6])))

(note
 (->> (colon 0 9)
      r->clj
      (check = (range 10))))


(note-md "## R dataframes and [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) datasets")

(note-md "Create a tech.ml.dataset dataset object,
pass it to an R function to compute the row means,
and convert the return value to Clojure.")

(note
 (let [row-means (-> "function(data) rowMeans(data)"
                     r
                     function)]
   (->> {:x [1 2 3]
         :y [4 5 6]}
        dataset/name-values-seq->dataset
        row-means
        r->clj
        (check = [2.5 3.5 4.5]))))

(note-md "Load the R package 'dplyr' (assuming it is installed).")

(note-void
 (r "library(dplyr)"))

(note-md "Use dplyr to process some Clojure dataset, and convert back to the resulting dataset.")

(note
 (let [filter-by-x  (-> "function(data) filter(data, x>=2)"
                        r
                        function)
       add-z-column (-> "function(data) mutate(data, z=x+y)"
                        r
                        function)]
   (->> {:x [1 2 3]
         :y [4 5 6]}
        dataset/name-values-seq->dataset
        filter-by-x
        add-z-column
        r->clj
        (check (fn [d]
                 (-> d
                     dataset/->flyweight
                     (= [{:x 2.0 :y 5.0 :z 7.0}
                         {:x 3.0 :y 6.0 :z 9.0}])))))))

(note-md "Tibbles are also supported, as a special case of data frames.")

(note-void
 (r "library(tibble)"))

(note
 (let [tibble (function (r "tibble"))]
   (tibble
    :x [1 2 3]
    :y [4 5 6])))

(note
 (let [tibble (function (r "tibble"))]
   (->> (tibble
         :x [1 2 3]
         :y [4 5 6])
        r->clj
        dataset/->flyweight
        (check = [{:x 1.0 :y 4.0}
                  {:x 2.0 :y 5.0}
                  {:x 3.0 :y 6.0}]))))


(note-md "## R objects")

(note-md "Clojuress holds handles to R objects, that are stored in memory at the R session, where they are assigned random names.")

(note-void
 (def one+two (r "1+2")))

(note
 (->> one+two
      class
      (check = clojuress.v1.robject.RObject)))

(note
 (:object-name one+two))

(note-md "We can figure out the place in R memory corresponding to an object's name.")

(note
 (-> one+two
     :object-name
     clojuress.v1.objects-memory/object-name->memory-place))

(note-md "## Generating code")

(note-md "Let us see the code-generation mechanism of Clojuress, and the rules defining it.")

(note-md "We will need a reference to the R session:")

(note-void
 (def session
   (session/fetch-or-make nil)))

(note-md "For the following examples, we will use some dummy handles to R objects:")

(note-void
 (def x (robject/->RObject "x" session nil))
 (def y (robject/->RObject "y" session nil)))

(note-md ".. and some real handles to R objects:")

(note-void
 (def minus-eleven (r "-11"))
 (def abs (r "abs")))

(note-md "For an r-object, we generate the code with that object's location in the R session memory.")

(note
 (->> x
      ->code
      (check = ".MEM$x")))

(note-md "For a clojure value, we implicitly convert to an R object, generating the corresponding code.")

(note
 (->> "hello"
      ->code
      (check re-matches #"\.MEM\$.*")))

(note-md "For a symbol, we generate the code with the corresponding R symbol.")

(note (->code 'x))

(note-md "A sequential structure (list, vector, etc.) can be interpreted as a compound expression, for which code generation is defined accorting to the first list element.")

(note-md "For a list beginning with the symbol `'function`, we generate an R function definition.")

(note (->> '(function [x y] x)
           ->code
           (check = "function(x, y) {x}")))

(note-md "For a vector instead of list, we heve the same behaviour.")

(note (->> '[function [x y] x]
           ->code
           (check = "function(x, y) {x}")))

(note-md "For a list beginning with the symbol `'tilde`, we generate an R `~`-furmula.")

(note (->> '(tilde x y)
           ->code
           (check = "(x ~ y)")))

(note-md "For a list beginning with a symbol known to be a binary operator, we generate the code with that operator between all arguments.")

(note (->> '(+ x y z)
           ->code
           (check = "(x + y + z)")))

(note-md "For a list beginning with another symbol, we generate a function call with that symbol as the function name.")

(note (->> '(f x)
           ->code
           (check = "f(x)")))

(note-md "For a list beginning with an R object that is a function, we generate a function call with that object as the function.")

(note (->> [abs 'x]
           ->code
           (check re-matches #"\.MEM\$.*\(x\)")))

(note-md "All other sequential things (that is, those not beginning with a symbol or R function) are intepreted as data, converted implicitly to R data.")

(note (->> [abs '(1 2 3)]
           ->code
           (check re-matches #"\.MEM\$.*\(\.MEM\$.*\)")))

(note-md "Some more examples, showing how these rules compose:")

(note (->code '(function [x y] (f y))))

(note (->code '(function [x y] (+ x y))))

(note (->code ['function '[x y] ['+ 'x y]]))

(note (->code '(function [x y] (print x) (f x))))

(note (->code ['function '[x y] [abs 'x]]))

(note (->code [abs minus-eleven]))

(note (->code [abs -11]))

(note-md "## Running generated code")

(note-md "Clojure forms can be run as R code. For example:")

(note (->> [abs (range -3 0)]
           r
           r->clj
           (check = [3 2 1])))

(note-md "Let us repeat the basic examples from the beginning of this tutorial,
this time generating code rather than writing it as Strings.")

(note-void
 (def x (r '(+ 1 2))))

(note
 "checking again... "
 (->> x
      r->clj
      (check = [3])))

(note-void
 (def f (function (r '(function [x] (* x 10))))))

(note
 "checking again... "
 (->> 5
      f
      r->clj
      (check = [50])))

(note
 "checking again... "
 (->> "5*5"
      r
      f
      r->clj
      (check = [250.0])))

(note
 (let [row-means (-> '(function [data] (rowMeans data))
                     r
                     function)]
   (->> {:x [1 2 3]
         :y [4 5 6]}
        dataset/name-values-seq->dataset
        row-means
        r->clj
        (check = [2.5 3.5 4.5]))))

(note-void
 (r '(library dplyr)))

(note
 (let [filter-by-x  (-> '(function [data] (filter data (>= x 2)))
                        r
                        function)
       add-z-column (-> '(function [data] (mutate data (= z (+ x y))))
                        r
                        function)]
   (->> {:x [1 2 3]
         :y [4 5 6]}
        dataset/name-values-seq->dataset
        filter-by-x
        add-z-column
        r->clj)))

(note-md "The strange column name is due to dplyr's mutate behaviour when extra parens are added to the expression.")

(note-md "## Requiring R packages")

(note-md "We have seen earlier, that R functions can be wrapped by Clojure functions.
Sometimes, we want to bring to the Clojure world functions from R packages.
Here, we try to follow the [require-python](https://github.com/cnuernber/libpython-clj/blob/master/test/libpython_clj/require_python_test.clj) syntax
of [libpython-clj](https://github.com/cnuernber/libpython-clj)
(though currently in a less sophisticated way.)")

(note-void
 (require-r '[stats :as statz :refer [median]]))

(note
 (->> [1 2 3]
      r.stats/median
      r->clj
     (check = [2])))

(note
 (->> [1 2 3]
      statz/median
      r->clj
     (check = [2])))

(note
 (->> [1 2 3]
      median
      r->clj
      (check = [2])))

(note-md "## Data visualization")

(note-md "Functions creating R plots can be wrapped in a way that returns an SVG.")

(note-md "Currently there is a bug that sometimes causes axes and labels to disappear when rendered inside a larger HTML.")

(note-void
 (require-r '[graphics :refer [plot]])
 (require-r '[ggplot2 :refer [ggplot aes geom_point xlab ylab labs]])
 (require '[clojuress.v1.applications.plotting :refer [plotting-function->svg
                                                       ggplot->svg]]))
(note-as-hiccup
 (plotting-function->svg
  (fn []
    (->> rand
         (repeatedly 30)
         (reductions +)
         (plot :xlab "t"
               :ylab "y"
               :type "l")))))

(note-md "ggplot2 plots can be also turned into SVG.")

(note-as-hiccup
 (ggplot->svg
  (let [x (repeatedly 99 rand)
        y (map +
               x
               (repeatedly 99 rand))]
    (-> {:x x :y y}
        dataset/name-values-seq->dataset
        (ggplot (aes :x x
                     :y y
                     :color '(+ x y)
                     :size '(/ x y)))
        (r+ (geom_point)
            (xlab "x")
            (ylab "y"))))))


(note-md "## Intermediaty representation as Java objects.")

(note-md "Clojuress relies on the fact of an intemediary representation of java, as Java objects. This is usually hidden from the user, but may be useful sometimes.
In the current implementation, this is based on [REngine](https://github.com/s-u/REngine).")

(note-void
 (import (org.rosuda.REngine REXP REXPInteger REXPDouble)))

(note-md "We can convert data between R and Java.")

(note
 (->> "1:9"
      r
      r->java
      class
      (check = REXPInteger)))

(note
 (->> (REXPInteger. 1)
      java->r
      r->clj
      (check = [1])))

(note-md "We can evaluate R code and immediately return the result as a java object, without ever creating a handle to an R object holding the result:")

(note
 (->> "1+2"
      eval-r->java
      class
      (check = REXPDouble)))

(note
 (->> "1+2"
      eval-r->java
      (.asDoubles)
      vec
      (check = [3.0])))

(note-md "## More data conversion examples")

(note-md "Convertion between R and Clojure always passes through Java.
To stress this, we write it explicitly in the following examples.")

(note
 (->> "list(a=1:2,b='hi!')"
      r
      r->java
      java->clj
      (check = {:a [1 2] :b ["hi!"]})))

(note
 (->> "table(c('a','b','a','b','a','b','a','b'), c(1,1,2,2,3,3,1,1))"
     r
     r->java
     java->clj
     (check = {["1" "a"] 2 ["1" "b"] 2 ["2" "a"] 1 ["2" "b"] 1 ["3" "a"] 1 ["3" "b"] 1})))


(note
 (->> {:a [1 2] :b "hi!"}
      clj->java
      java->r
      r->java
      java->clj
      (check = {:a [1 2] :b ["hi!"]})))

(note
 (->> {:a [1 2] :b "hi!"}
      clj->java
      java->r
      ((r/function (r "deparse")))
      r->java
      java->clj
      (check = ["list(a = 1:2, b = \"hi!\")"])))

(note/render-this-ns!)


