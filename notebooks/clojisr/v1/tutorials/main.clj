;; # Clojisr tutorial

;; ## Setup

(ns clojisr.v1.tutorials.main
  (:require [clojisr.v1.r :as r :refer [r eval-r->java r->java java->r java->clj java->native-clj clj->java r->clj clj->r ->code r+ colon require-r]]
            [clojisr.v1.robject :as robject]
            [clojisr.v1.session :as session]
            [tech.v3.dataset :as dataset]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.kindly.v4.api :as kindly]))

^:kindly/hide-code
(def md (comp kindly/hide-code kind/md))

(md "## Basic examples")

(md "Let us start by some basic usage examples of Clojisr.")

(md "First, let us make sure that we use the Rserve backend (in case we were using another engine instead earlier), and that there are no R sessions currently running. This is typically not needed if you just started working. Here, we do it just in case.")


(r/set-default-session-type! :rserve)
(r/discard-all-sessions)

(md "Now let us run some R code, and keep a Clojure handle to the return value.")


(def x (r "1+2"))

(md "The thing we created is something called an ROBject.")


(class x)

(md "If we wish, we can convert an ROBject to Clojure: ")


(r->clj x)

(kind/test-last [= [3.0]])

(md "Let us see more examples of creating ROBjects and converting them to Clojure: ")


(->> "list(A=1,B=2,'#123strange<text> ()'=3)"
     r
     r->clj)

(kindly/check = {:A [1.0], :B [2.0], "#123strange<text> ()" [3.0]})

(md "In the other direction, we can convert Clojure data to R data. Note that `nil` is turned to `NA`.")


(-> [1 nil 3]
    clj->r)

(md "We can run code on a separate R session (specify session-args which are different than the default ones).")


(-> "1+2"
    (r :session-args {:session-name "mysession"})
    r->clj)

(kindly/check = [3.0])

(md "## Functions")

(md "An R function is also a Clojure function.")


(def f (r "function(x) x*10"))

(md "Let us apply it to Clojure data (implicitly converting that data to R).")


(-> 5
    f
    r->clj)

(kindly/check = [50.0])

(md "We can also apply it to R data.")


(-> "5*5"
    r
    f
    r->clj)

(kindly/check = [250.0])

(md "Functions can get named arguments.
Here we pass the `na.rm` argument,
that tells R whether to remove missing values
whenn computing the mean.")


(r->clj ((r "mean")
         [1 nil 3]
         :na.rm true))

(kindly/check = [2.0])

(md "Another example:")


(let [f (r "function(w,x,y=10,z=20) w+x+y+z")]
  (->> [(f 1 2)
        (f 1 2 :y 100)
        (f 1 2 :z 100)]
       (map r->clj)))

(kindly/check = [[33.0] [123.0] [113.0]])

(md "Some functions are already created in Clojisr and given special names for convenience. Here are some examples:")

(md "R addition:")


(->> (r+ 1 2 3)
     r->clj)

(kindly/check = [6])

(md "R colon (`:`), for creating a range of integers, like `0:9`:")


(r->clj (colon 0 9))

(kindly/check = (range 10))

(md "## R dataframes and tech.ml.dataset datasets")

(md "At Clojure, we have a structure that is equivalent to R dataframes: a [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) dataset.

Let us create such a dataset, pass it to an R function to compute the row means, and then convert the return value back to Clojure.")


(let [row-means (r "function(data) rowMeans(data)")]
  (-> {:x [1 2 3]
       :y [4 5 6]}
      dataset/->dataset
      row-means
      r->clj))

(kindly/check = [2.5 3.5 4.5])

(md "Let us see some more dataset proccessing through R.")

(md "Loading the R package [dplyr](https://dplyr.tidyverse.org/) (assuming it is installed).")


(r "library(dplyr)")

(md "Using dplyr to process some Clojure dataset, and convert back to the resulting dataset.")


(let [filter-by-x  (r "function(data) filter(data, x>=2)")
      add-z-column (r "function(data) mutate(data, z=x+y)")]
  (-> {:x [1 2 3]
       :y [4 5 6]}
      dataset/->dataset
      filter-by-x
      add-z-column
      r->clj))

(kindly/check (fn [d]
                (-> d
                    dataset/mapseq-reader
                    (= [{:x 2 :y 5 :z 7}
                        {:x 3 :y 6 :z 9}]))))

(md "[Tibbles](https://tibble.tidyverse.org), which are a more recent R dataframe notion, are also supported, as a special case of data frames.")


(r "library(tibble)")


(let [tibble (r "tibble")]
  (tibble
   :x [1 2 3]
   :y [4 5 6]))


(let [tibble (r "tibble")]
  (-> (tibble
       :x [1 2 3]
       :y [4 5 6])
      r->clj
      dataset/mapseq-reader))

(kindly/check = [{:x 1 :y 4}
                 {:x 2 :y 5}
                 {:x 3 :y 6}])

(md "## R objects")

(md "Clojisr holds handles to R objects, that are stored in memory at the R session, where they are assigned random names.")


(def one+two (r "1+2"))


(class one+two)

(kindly/check = clojisr.v1.robject.RObject)

(md "The name of an object is the place where it is held at R (inside an R [evnironment](http://adv-r.had.co.nz/Environments.html) called `.MEM`).")

(:object-name one+two)

(md "## Generating code")

(md "Let us see the mechanism by which clojisr generates R code, and the rules defining it.")

(md "Since we are playing a bit with the internals here, we will need a reference to the R session:")


(def session
  (session/fetch-or-make nil))

(md "For the following examples, we will use some **dummy** handles to R objects with given names:")


(def x (robject/->RObject "robject_x" session nil nil))
(def y (robject/->RObject "robject_y" session nil nil))

(md ".. and some **real** handles to R objects:")


(def minus-eleven (r "-11"))
(def abs (r "abs"))

(md "The function `->code` generates R code according to a certain set of rules. Here we describe some of these rules briefly. We also wrote a dedicated tutorial about the rule set more thoroughly.")

(md "For an ROBject, the generated code is just the ROBject name.")


(->code x)

(kindly/check = "robject_x")

(md "For a clojure value, we use some form analysis and generate proper R string or values.")


(->code "hello")

(kindly/check (partial re-matches #"\"hello\"$"))

(->code [1 2 3])

(kindly/check = "c(1L,2L,3L)")

(md "For a symbol, we generate the code with the corresponding R symbol.")

(->code 'x)

(md "A sequential structure (list, vector, etc.) can be interpreted as a compound expression, for which code generation is defined accorting to the first list element.")

(md "For a list beginning with the symbol `'function`, we generate an R function definition.")

(->code '(function [x y] x))

(kindly/check = "function(x,y) {x}")

(md "For a vector instead of list, we create R vector.")

(->code '[function [x y] x])

(kindly/check = "c(function,c(x,y),x)")

(md "For a list beginning with the symbol `'formula`, we generate an R `~`-formula.")

(->code '(formula x y))

(kindly/check = "(x~y)")

(md "For a list beginning with a symbol known to be a binary operator, we generate nested calls.")

(->code '(+ x y z))

(kindly/check = "((x+y)+z)")

(md "For a list beginning with another symbol, we generate a function call with that symbol as the function name.")

(->code '(f x))

(kindly/check = "f(x)")

(md "For a list beginning with an R object that is a function, we generate a function call with that object as the function. If you create the list using the quote sign (`'`), don't forget to unquote symbols refering to things you defined on the Clojure side.")

(->code '(~abs x))

(kindly/check (partial re-matches #"\.MEM\$.*\(x\)"))

(md "All other sequential things (that is, those not beginning with a symbol or R function) are intepreted as data, converted implicitly R data representation.")

(->code `(~abs (1 2 3)))

(kindly/check (partial re-matches #"\.MEM\$.*\(c\(1L,2L,3L\)\)"))

(md "Some more examples, showing how these rules compose:")

(->code '(function [x y] (f y)))
(->code '(function [x y] (f ~y)))

(->code '(function [x y] (+ x y)))
(->code (list 'function '[x y] (list '+ 'x 'y)))

(->code '(function [x y] (print x) (f x)))

(->code '(function [x y] (~abs x)))

(->code '(~abs ~minus-eleven))

(->code '(~abs -11))

(md "Use syntax quote ` in case you want to use local bindings.")

(let [minus-ten -10]
  (->code `(~abs ~minus-ten)))

(md "## Running generated code")

(md "Clojure forms can be run as R code. Behind the scences, they are turned to R code using the `->code` function described above. For example:")

(-> '(~abs ~(range -3 0))
    r
    r->clj)

(kindly/check = [3 2 1])

(md "Or, equivalently:")

(-> '(~abs ~(range -3 0))
    ->code
    r
    r->clj)

(kindly/check = [3 2 1])

(md "Let us repeat the basic examples from the beginning of this tutorial,
this time generating code rather than writing it as Strings.")


(def x (r '(+ 1 2)))


(r->clj x)

(kindly/check = [3])

(def f (r '(function [x] (* x 10))))


(-> 5
    f
    r->clj)

(kindly/check = [50])


(-> "5*5"
    r
    f
    r->clj)

(kindly/check = [250.0])


(let [row-means (r '(function [data] (rowMeans data)))]
  (-> {:x [1 2 3]
       :y [4 5 6]}
      dataset/->dataset
      row-means
      r->clj))

(kindly/check = [2.5 3.5 4.5])


(r '(library dplyr))


(let [filter-by-x  (r '(function [data] (filter data (>= x 2))))
      add-z-column (r '(function [data] (mutate data (= z (+ x y)))))]
  (->> {:x [1 2 3]
        :y [4 5 6]}
       dataset/->dataset
       filter-by-x
       add-z-column
       r->clj))

(md "## Requiring R packages")

(md "Sometimes, we want to bring to the Clojure world functions and data from R packages.
Here, we try to follow the [require-python](https://github.com/cnuernber/libpython-clj/blob/master/test/libpython_clj/require_python_test.clj) syntax
of [libpython-clj](https://github.com/cnuernber/libpython-clj)
(though currently in a less sophisticated way.)")


(require-r '[stats :as statz :refer [median]])


(-> [1 2 3]
    r.stats/median
    r->clj
    )

(kindly/check = [2])

(-> [1 2 3]
    statz/median
    r->clj)

(kindly/check = [2])

(-> [1 2 3]
    median
    r->clj)

(kindly/check = [2])

(require-r '[datasets :as datasetz :refer [euro]])

[r.datasets/euro
 datasetz/euro
 euro]

(kindly/check (partial apply =))

(require-r '[base :refer [$]])


(-> {:a 1 :b 2}
    ($ 'a)
    r->clj)

(kindly/check = [1])

(md "## Data visualization")

(md "Functions creating R plots or any plotting objects generated by various R libraries can be wrapped in a way that returns an SVG, BufferedImage or can be saved to a file. All of them accept additional parameters specified in `grDevices` R package.")

(md "Currently there is a bug that sometimes causes axes and labels to disappear when rendered inside a larger HTML.")


(require-r '[graphics :refer [plot hist]])
(require-r '[ggplot2 :refer [ggplot aes geom_point xlab ylab labs]])
(require '[clojisr.v1.applications.plotting :refer [plot->svg plot->file plot->buffered-image]])

(md "First example, simple plotting function as SVG string.")

(plot->svg
 (fn []
   (->> rand
        (repeatedly 30)
        (reductions +)
        (plot :xlab "t"
              :ylab "y"
              :type "l"))))

(md "ggplot2 plots (or any other plot objects like lattice) can be also turned into SVG.")

(plot->svg
 (let [x (repeatedly 99 rand)
       y (map +
              x
              (repeatedly 99 rand))]
   (-> {:x x :y y}
       dataset/->dataset
       (ggplot (aes :x x
                    :y y
                    :color '(+ x y)
                    :size '(/ x y)))
       (r+ (geom_point)
           (xlab "x")
           (ylab "y")))))

(md "Any plot (function or object) can be saved to file or converted to BufferedImage object.")

(let [path "/tmp/histogram.jpg"]
  (r->clj (plot->file path
                      (fn [] (hist [1 1 1 1 2 3 4 5]
                                   :main "Histogram"
                                   :xlab "data: [1 1 1 1 2 3 4 5]"))
                      :width 800 :height 400 :quality 50))
  (-> (clojure.java.shell/sh "ls" path)
      :out
      kind/code))

(plot->buffered-image (fn [] (hist [1 1 1 1 2 3 4 5])) :width 222 :height 149)

(md "## Intermediary representation as Java objects.")

(md "Clojisr relies on the fact of an intemediary representation of java, as Java objects. This is usually hidden from the user, but may be useful sometimes.
In the current implementation, this is based on [REngine](https://github.com/s-u/REngine).")


(import (org.rosuda.REngine REXP REXPInteger REXPDouble))

(md "We can convert data between R and Java.")


(-> "1:9"
    r
    r->java
    class)

(kindly/check = REXPInteger)

(-> (REXPInteger. 1)
    java->r
    r->clj)

(kindly/check = [1])

(md "We can further convert data from the java representation to Clojure.")


(-> "1:9"
    r
    r->java
    java->clj)

(kindly/check = (range 1 10))

(md "On the opposite direction, we can also convert Clojure data into the Java represenattion.")


(-> (range 1 10)
    clj->java
    class)

(kindly/check = REXPInteger)

(-> (range 1 10)
    clj->java
    java->clj)

(kindly/check = (range 1 10))

(md "There is an alternative way of conversion from Java to Clojure, naively converting the internal Java representation to a Clojure data structure. It can be handy when one wants to have plain access to all the metadata (R attributes), etc. ")


(->> "1:9"
     r
     r->java
     java->native-clj)


(->> "data.frame(x=1:3,y=factor('a','a','b'))"
     r
     r->java
     java->native-clj)

(md "We can evaluate R code and immediately return the result as a java object, without ever creating a handle to an R object holding the result:")


(-> "1+2"
    eval-r->java
    class)

(kindly/check = REXPDouble)

(-> "1+2"
    eval-r->java
    (.asDoubles)
    vec)

(kindly/check = [3.0])

(md "## More data conversion examples")

(md "Convertion between R and Clojure always passes through Java.
To stress this, we write it explicitly in the following examples.")


(-> "list(a=1:2,b='hi!')"
    r
    r->java
    java->clj)

(kindly/check = {:a [1 2] :b ["hi!"]})

(md "Partially named lists are also supported")


(-> "list(a=1:2,'hi!')"
    r
    r->java
    java->clj)

(kindly/check = {:a [1 2] 1 ["hi!"]})


(-> "table(c('a','b','a','b','a','b','a','b'), c(1,1,2,2,3,3,1,1))"
    r
    r->java
    java->clj
    dataset/mapseq-reader
    set)

(kindly/check = #{{0 "a", 1 "2", :$value 2} {0 "b", 1 "3", :$value 1}
                  {0 "a", 1 "1", :$value 2} {0 "a", 1 "3", :$value 1}
                  {0 "b", 1 "2", :$value 1} {0 "b", 1 "1", :$value 1}})


(-> {:a [1 2] :b "hi!"}
    clj->java
    java->r
    r->java
    java->clj)

(kindly/check = {:a [1 2] :b ["hi!"]})

(->> {:a [1 2] :b "hi!"}
     clj->java
     java->r
     ((r "deparse"))
     r->java
     java->clj)

(md "### Basic types convertion clj->r->clj")

(def clj->r->clj (comp r->clj r))

(clj->r->clj nil)
(kindly/check = nil)

(clj->r->clj [10 11])
(kindly/check = [10 11])

(clj->r->clj [10.0 11.0])
(kindly/check = [10.0 11.0])

(clj->r->clj (list 10.0 11.0))
(kindly/check = [10.0 11.0])

(clj->r->clj {:a 1 :b 2})
(kindly/check = {:a [1] :b [2]})

(md "### Various R objects")

;; Named list
(-> "list(a=1L,b=c(10,20),c='hi!')"
    r
    r->clj)

(kindly/check = {:a [1] :b [10.0 20.0] :c ["hi!"]})

;; Array of doubles
(-> "c(10,20,30)"
    r
    r->clj)

(kindly/check = [10.0 20.0 30.0])

;; Array of longs
(-> "c(10L,20L,30L)"
    r
    r->clj)

(kindly/check = [10 20 30])

;; Timeseries
(-> 'euro
    r
    r->clj
    first)

(kindly/check = 13.7603)

;; Pairlist
(-> r.stats/dnorm
    r.base/formals
    r->clj
    keys
    sort)

(kindly/check = '(:log :mean :sd :x))

;; NULL
(-> "NULL"
    r
    r->clj)

(kindly/check = nil)

;; TRUE/FALSE
(-> "TRUE"
    r
    r->clj)

(kindly/check = [true])

(md "## Inspecting R functions")

(md "The `mean` function is defined to expect arguments `x` and `...`.
These arguments have no default values (thus, its formals have empty symbols as values):")

(-> 'mean
    r.base/formals
    r->clj)

(kindly/check = {:x (symbol "")
                 :... (symbol "")})

(md "It is an [S3 generic function](http://adv-r.had.co.nz/S3.html) function, which we can realize by printing it:")


(r 'mean)

(md "So, we can expect possibly more details when inspecting its default implementation.
Now, we see some arguments that do have default values.")


(-> 'mean.default
    r.base/formals
    r->clj)

(kindly/check = {:x     (symbol "")
                 :trim  [0.0]
                 :na.rm [false]
                 :...   (symbol "")})

(md "## R-function-arglists")

(md "As we saw earlier, R functions are Clojure functions. The arglists of functions brought up by `require-r` match the expected arguments. Here are some examples:")

(require-r
 '[base]
 '[stats]
 '[grDevices])


(->> [#'r.base/mean, #'r.base/mean-default, #'r.stats/arima0,
      #'r.grDevices/dev-off, #'r.base/Sys-info, #'r.base/summary-default
      ;; Primitive functions:
      #'r.base/sin, #'r.base/sum]
     (map (fn [f]
            (-> f
                meta
                (update :ns (comp symbol str))))))


(kindly/check
 =
 '({:arglists ([x & {:keys [...]}]), :name mean, :ns r.base}
   {:arglists ([x & {:keys [trim na.rm ...]}]),
    :name     mean-default,
    :ns       r.base}
   {:arglists
    ([x & {:keys
           [order seasonal xreg include.mean delta
            transform.pars fixed init method n.cond
            optim.control]}]),
    :name arima0,
    :ns   r.stats}
   {:arglists ([& {:keys [which]}]),
    :name dev-off,
    :ns r.grDevices}
   {:arglists ([]),
    :name Sys-info,
    :ns r.base}
   {:arglists ([object & {:keys [... digits quantile.type]}]),
    :name summary-default,
    :ns r.base}
   {:arglists ([x]), :name sin, :ns r.base}
   {:arglists ([& {:keys [... na.rm]}]), :name sum, :ns r.base}))
