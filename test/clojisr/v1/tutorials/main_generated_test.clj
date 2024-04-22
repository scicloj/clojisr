(def var0 nil)


(ns
 clojisr.v1.tutorials.main-generated-test
 (:require
  [clojisr.v1.r
   :as
   r
   :refer
   [r
    eval-r->java
    r->java
    java->r
    java->clj
    java->native-clj
    clj->java
    r->clj
    clj->r
    ->code
    r+
    colon
    require-r]]
  [clojisr.v1.robject :as robject]
  [clojisr.v1.session :as session]
  [tech.v3.dataset :as dataset]
  [scicloj.kindly.v4.kind :as kind]
  [scicloj.kindly.v4.api :as kindly]
  [clojure.test :refer [deftest is]]))


(def var2 (def md (comp kindly/hide-code kind/md)))


(def var3 (md "## Basic examples"))


(def var4 (md "Let us start by some basic usage examples of Clojisr."))


(def
 var5
 (md
  "First, let us make sure that we use the Rserve backend (in case we were using another engine instead earlier), and that there are no R sessions currently running. This is typically not needed if you just started working. Here, we do it just in case."))


(def var6 (r/set-default-session-type! :rserve))


(def var7 (r/discard-all-sessions))


(def
 var8
 (md
  "Now let us run some R code, and keep a Clojure handle to the return value."))


(def var9 (def x (r "1+2")))


(def var10 (md "The thing we created is something called an ROBject."))


(def var11 (class x))


(def var12 (md "If we wish, we can convert an ROBject to Clojure: "))


(def var13 (r->clj x))


(deftest test14 (is (= var13 [3.0])))


(def
 var15
 (md
  "Let us see more examples of creating ROBjects and converting them to Clojure: "))


(def var16 (->> "list(A=1,B=2,'#123strange<text> ()'=3)" r r->clj))


(deftest
 test17
 (is (= var16 {:A [1.0], :B [2.0], "#123strange<text> ()" [3.0]})))


(def
 var18
 (md
  "In the other direction, we can convert Clojure data to R data. Note that `nil` is turned to `NA`."))


(def var19 (-> [1 nil 3] clj->r))


(def
 var20
 (md
  "We can run code on a separate R session (specify session-args which are different than the default ones)."))


(def
 var21
 (-> "1+2" (r :session-args {:session-name "mysession"}) r->clj))


(deftest test22 (is (= var21 [3.0])))


(def var23 (md "## Functions"))


(def var24 (md "An R function is also a Clojure function."))


(def var25 (def f (r "function(x) x*10")))


(def
 var26
 (md
  "Let us apply it to Clojure data (implicitly converting that data to R)."))


(def var27 (-> 5 f r->clj))


(deftest test28 (is (= var27 [50.0])))


(def var29 (md "We can also apply it to R data."))


(def var30 (-> "5*5" r f r->clj))


(deftest test31 (is (= var30 [250.0])))


(def
 var32
 (md
  "Functions can get named arguments.\nHere we pass the `na.rm` argument,\nthat tells R whether to remove missing values\nwhenn computing the mean."))


(def var33 (r->clj ((r "mean") [1 nil 3] :na.rm true)))


(deftest test34 (is (= var33 [2.0])))


(def var35 (md "Another example:"))


(def
 var36
 (let
  [f (r "function(w,x,y=10,z=20) w+x+y+z")]
  (->> [(f 1 2) (f 1 2 :y 100) (f 1 2 :z 100)] (map r->clj))))


(deftest test37 (is (= var36 [[33.0] [123.0] [113.0]])))


(def
 var38
 (md
  "Some functions are already created in Clojisr and given special names for convenience. Here are some examples:"))


(def var39 (md "R addition:"))


(def var40 (->> (r+ 1 2 3) r->clj))


(deftest test41 (is (= var40 [6])))


(def
 var42
 (md "R colon (`:`), for creating a range of integers, like `0:9`:"))


(def var43 (r->clj (colon 0 9)))


(deftest test44 (is (= var43 (range 10))))


(def var45 (md "## R dataframes and tech.ml.dataset datasets"))


(def
 var46
 (md
  "At Clojure, we have a structure that is equivalent to R dataframes: a [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) dataset.\n\nLet us create such a dataset, pass it to an R function to compute the row means, and then convert the return value back to Clojure."))


(def
 var47
 (let
  [row-means (r "function(data) rowMeans(data)")]
  (-> {:x [1 2 3], :y [4 5 6]} dataset/->dataset row-means r->clj)))


(deftest test48 (is (= var47 [2.5 3.5 4.5])))


(def var49 (md "Let us see some more dataset proccessing through R."))


(def
 var50
 (md
  "Loading the R package [dplyr](https://dplyr.tidyverse.org/) (assuming it is installed)."))


(def var51 (r "library(dplyr)"))


(def
 var52
 (md
  "Using dplyr to process some Clojure dataset, and convert back to the resulting dataset."))


(def
 var53
 (let
  [filter-by-x
   (r "function(data) filter(data, x>=2)")
   add-z-column
   (r "function(data) mutate(data, z=x+y)")]
  (->
   {:x [1 2 3], :y [4 5 6]}
   dataset/->dataset
   filter-by-x
   add-z-column
   r->clj)))


(deftest
 test54
 (is
  ((fn
    [d]
    (->
     d
     dataset/mapseq-reader
     (= [{:x 2, :y 5, :z 7} {:x 3, :y 6, :z 9}])))
   var53)))


(def
 var55
 (md
  "[Tibbles](https://tibble.tidyverse.org), which are a more recent R dataframe notion, are also supported, as a special case of data frames."))


(def var56 (r "library(tibble)"))


(def var57 (let [tibble (r "tibble")] (tibble :x [1 2 3] :y [4 5 6])))


(def
 var58
 (let
  [tibble (r "tibble")]
  (-> (tibble :x [1 2 3] :y [4 5 6]) r->clj dataset/mapseq-reader)))


(deftest test59 (is (= var58 [{:x 1, :y 4} {:x 2, :y 5} {:x 3, :y 6}])))


(def var60 (md "## R objects"))


(def
 var61
 (md
  "Clojisr holds handles to R objects, that are stored in memory at the R session, where they are assigned random names."))


(def var62 (def one+two (r "1+2")))


(def var63 (class one+two))


(deftest test64 (is (= var63 clojisr.v1.robject.RObject)))


(def
 var65
 (md
  "The name of an object is the place where it is held at R (inside an R [evnironment](http://adv-r.had.co.nz/Environments.html) called `.MEM`)."))


(def var66 (:object-name one+two))


(def var67 (md "## Generating code"))


(def
 var68
 (md
  "Let us see the mechanism by which clojisr generates R code, and the rules defining it."))


(def
 var69
 (md
  "Since we are playing a bit with the internals here, we will need a reference to the R session:"))


(def var70 (def session (session/fetch-or-make nil)))


(def
 var71
 (md
  "For the following examples, we will use some **dummy** handles to R objects with given names:"))


(def var72 (def x (robject/->RObject "robject_x" session nil nil)))


(def var73 (def y (robject/->RObject "robject_y" session nil nil)))


(def var74 (md ".. and some **real** handles to R objects:"))


(def var75 (def minus-eleven (r "-11")))


(def var76 (def abs (r "abs")))


(def
 var77
 (md
  "The function `->code` generates R code according to a certain set of rules. Here we describe some of these rules briefly. We also wrote a dedicated tutorial about the rule set more thoroughly."))


(def
 var78
 (md "For an ROBject, the generated code is just the ROBject name."))


(def var79 (->code x))


(deftest test80 (is (= var79 "robject_x")))


(def
 var81
 (md
  "For a clojure value, we use some form analysis and generate proper R string or values."))


(def var82 (->code "hello"))


(deftest test83 (is ((partial re-matches #"\"hello\"$") var82)))


(def var84 (->code [1 2 3]))


(deftest test85 (is (= var84 "c(1L,2L,3L)")))


(def
 var86
 (md
  "For a symbol, we generate the code with the corresponding R symbol."))


(def var87 (->code 'x))


(def
 var88
 (md
  "A sequential structure (list, vector, etc.) can be interpreted as a compound expression, for which code generation is defined accorting to the first list element."))


(def
 var89
 (md
  "For a list beginning with the symbol `'function`, we generate an R function definition."))


(def var90 (->code '(function [x y] x)))


(deftest test91 (is (= var90 "function(x,y) {x}")))


(def var92 (md "For a vector instead of list, we create R vector."))


(def var93 (->code '[function [x y] x]))


(deftest test94 (is (= var93 "c(function,c(x,y),x)")))


(def
 var95
 (md
  "For a list beginning with the symbol `'formula`, we generate an R `~`-formula."))


(def var96 (->code '(formula x y)))


(deftest test97 (is (= var96 "(x~y)")))


(def
 var98
 (md
  "For a list beginning with a symbol known to be a binary operator, we generate nested calls."))


(def var99 (->code '(+ x y z)))


(deftest test100 (is (= var99 "((x+y)+z)")))


(def
 var101
 (md
  "For a list beginning with another symbol, we generate a function call with that symbol as the function name."))


(def var102 (->code '(f x)))


(deftest test103 (is (= var102 "f(x)")))


(def
 var104
 (md
  "For a list beginning with an R object that is a function, we generate a function call with that object as the function. If you create the list using the quote sign (`'`), don't forget to unquote symbols refering to things you defined on the Clojure side."))


(def var105 (->code '(~abs x)))


(deftest test106 (is ((partial re-matches #"\.MEM\$.*\(x\)") var105)))


(def
 var107
 (md
  "All other sequential things (that is, those not beginning with a symbol or R function) are intepreted as data, converted implicitly R data representation."))


(def
 var108
 (->code
  (clojure.core/sequence
   (clojure.core/seq
    (clojure.core/concat
     (clojure.core/list abs)
     (clojure.core/list
      (clojure.core/sequence
       (clojure.core/seq
        (clojure.core/concat
         (clojure.core/list 1)
         (clojure.core/list 2)
         (clojure.core/list 3))))))))))


(deftest
 test109
 (is ((partial re-matches #"\.MEM\$.*\(c\(1L,2L,3L\)\)") var108)))


(def var110 (md "Some more examples, showing how these rules compose:"))


(def var111 (->code '(function [x y] (f y))))


(def var112 (->code '(function [x y] (f ~y))))


(def var113 (->code '(function [x y] (+ x y))))


(def var114 (->code (list 'function '[x y] (list '+ 'x 'y))))


(def var115 (->code '(function [x y] (print x) (f x))))


(def var116 (->code '(function [x y] (~abs x))))


(def var117 (->code '(~abs ~minus-eleven)))


(def var118 (->code '(~abs -11)))


(def
 var119
 (md "Use syntax quote ` in case you want to use local bindings."))


(def
 var120
 (let
  [minus-ten -10]
  (->code
   (clojure.core/sequence
    (clojure.core/seq
     (clojure.core/concat
      (clojure.core/list abs)
      (clojure.core/list minus-ten)))))))


(def var121 (md "## Running generated code"))


(def
 var122
 (md
  "Clojure forms can be run as R code. Behind the scences, they are turned to R code using the `->code` function described above. For example:"))


(def var123 (-> '(~abs ~(range -3 0)) r r->clj))


(deftest test124 (is (= var123 [3 2 1])))


(def var125 (md "Or, equivalently:"))


(def var126 (-> '(~abs ~(range -3 0)) ->code r r->clj))


(deftest test127 (is (= var126 [3 2 1])))


(def
 var128
 (md
  "Let us repeat the basic examples from the beginning of this tutorial,\nthis time generating code rather than writing it as Strings."))


(def var129 (def x (r '(+ 1 2))))


(def var130 (r->clj x))


(deftest test131 (is (= var130 [3])))


(def var132 (def f (r '(function [x] (* x 10)))))


(def var133 (-> 5 f r->clj))


(deftest test134 (is (= var133 [50])))


(def var135 (-> "5*5" r f r->clj))


(deftest test136 (is (= var135 [250.0])))


(def
 var137
 (let
  [row-means (r '(function [data] (rowMeans data)))]
  (-> {:x [1 2 3], :y [4 5 6]} dataset/->dataset row-means r->clj)))


(deftest test138 (is (= var137 [2.5 3.5 4.5])))


(def var139 (r '(library dplyr)))


(def
 var140
 (let
  [filter-by-x
   (r '(function [data] (filter data (>= x 2))))
   add-z-column
   (r '(function [data] (mutate data (= z (+ x y)))))]
  (->>
   {:x [1 2 3], :y [4 5 6]}
   dataset/->dataset
   filter-by-x
   add-z-column
   r->clj)))


(def var141 (md "## Requiring R packages"))


(def
 var142
 (md
  "Sometimes, we want to bring to the Clojure world functions and data from R packages.\nHere, we try to follow the [require-python](https://github.com/cnuernber/libpython-clj/blob/master/test/libpython_clj/require_python_test.clj) syntax\nof [libpython-clj](https://github.com/cnuernber/libpython-clj)\n(though currently in a less sophisticated way.)"))


(def var143 (require-r '[stats :as statz :refer [median]]))


(def var144 (-> [1 2 3] r.stats/median r->clj))


(deftest test145 (is (= var144 [2])))


(def var146 (-> [1 2 3] statz/median r->clj))


(deftest test147 (is (= var146 [2])))


(def var148 (-> [1 2 3] median r->clj))


(deftest test149 (is (= var148 [2])))


(def var150 (require-r '[datasets :as datasetz :refer [euro]]))


(def var151 [r.datasets/euro datasetz/euro euro])


(deftest test152 (is ((partial apply =) var151)))


(def var153 (require-r '[base :refer [$]]))


(def var154 (-> {:a 1, :b 2} ($ 'a) r->clj))


(deftest test155 (is (= var154 [1])))


(def var156 (md "## Data visualization"))


(def
 var157
 (md
  "Functions creating R plots or any plotting objects generated by various R libraries can be wrapped in a way that returns an SVG, BufferedImage or can be saved to a file. All of them accept additional parameters specified in `grDevices` R package."))


(def
 var158
 (md
  "Currently there is a bug that sometimes causes axes and labels to disappear when rendered inside a larger HTML."))


(def var159 (require-r '[graphics :refer [plot hist]]))


(def
 var160
 (require-r '[ggplot2 :refer [ggplot aes geom_point xlab ylab labs]]))


(def
 var161
 (require
  '[clojisr.v1.applications.plotting
    :refer
    [plot->svg plot->file plot->buffered-image]]))


(def
 var162
 (md "First example, simple plotting function as SVG string."))


(def
 var163
 (plot->svg
  (fn
   []
   (->>
    rand
    (repeatedly 30)
    (reductions +)
    (plot :xlab "t" :ylab "y" :type "l")))))


(def
 var164
 (md
  "ggplot2 plots (or any other plot objects like lattice) can be also turned into SVG."))


(def
 var165
 (plot->svg
  (let
   [x (repeatedly 99 rand) y (map + x (repeatedly 99 rand))]
   (->
    {:x x, :y y}
    dataset/->dataset
    (ggplot (aes :x x :y y :color '(+ x y) :size '(/ x y)))
    (r+ (geom_point) (xlab "x") (ylab "y"))))))


(def
 var166
 (md
  "Any plot (function or object) can be saved to file or converted to BufferedImage object."))


(def
 var167
 (r->clj
  (plot->file
   "notebooks/images/histogram.jpg"
   (fn
    []
    (hist
     [1 1 1 1 2 3 4 5]
     :main
     "Histogram"
     :xlab
     "data: [1 1 1 1 2 3 4 5]"))
   :width
   800
   :height
   400
   :quality
   50)))


(def
 var168
 (kind/hiccup [:image {:src "notebooks/images/histogram.jpg"}]))


(def
 var169
 (plot->buffered-image
  (fn [] (hist [1 1 1 1 2 3 4 5]))
  :width
  222
  :height
  149))


(def var170 (md "## Intermediary representation as Java objects."))


(def
 var171
 (md
  "Clojisr relies on the fact of an intemediary representation of java, as Java objects. This is usually hidden from the user, but may be useful sometimes.\nIn the current implementation, this is based on [REngine](https://github.com/s-u/REngine)."))


(def var172 (import (org.rosuda.REngine REXP REXPInteger REXPDouble)))


(def var173 (md "We can convert data between R and Java."))


(def var174 (-> "1:9" r r->java class))


(deftest test175 (is (= var174 REXPInteger)))


(def var176 (-> (REXPInteger. 1) java->r r->clj))


(deftest test177 (is (= var176 [1])))


(def
 var178
 (md
  "We can further convert data from the java representation to Clojure."))


(def var179 (-> "1:9" r r->java java->clj))


(deftest test180 (is (= var179 (range 1 10))))


(def
 var181
 (md
  "On the opposite direction, we can also convert Clojure data into the Java represenattion."))


(def var182 (-> (range 1 10) clj->java class))


(deftest test183 (is (= var182 REXPInteger)))


(def var184 (-> (range 1 10) clj->java java->clj))


(deftest test185 (is (= var184 (range 1 10))))


(def
 var186
 (md
  "There is an alternative way of conversion from Java to Clojure, naively converting the internal Java representation to a Clojure data structure. It can be handy when one wants to have plain access to all the metadata (R attributes), etc. "))


(def var187 (->> "1:9" r r->java java->native-clj))


(def
 var188
 (->>
  "data.frame(x=1:3,y=factor('a','a','b'))"
  r
  r->java
  java->native-clj))


(def
 var189
 (md
  "We can evaluate R code and immediately return the result as a java object, without ever creating a handle to an R object holding the result:"))


(def var190 (-> "1+2" eval-r->java class))


(deftest test191 (is (= var190 REXPDouble)))


(def var192 (-> "1+2" eval-r->java (.asDoubles) vec))


(deftest test193 (is (= var192 [3.0])))


(def var194 (md "## More data conversion examples"))


(def
 var195
 (md
  "Convertion between R and Clojure always passes through Java.\nTo stress this, we write it explicitly in the following examples."))


(def var196 (-> "list(a=1:2,b='hi!')" r r->java java->clj))


(deftest test197 (is (= var196 {:a [1 2], :b ["hi!"]})))


(def var198 (md "Partially named lists are also supported"))


(def var199 (-> "list(a=1:2,'hi!')" r r->java java->clj))


(deftest test200 (is (= var199 {:a [1 2], 1 ["hi!"]})))


(def
 var201
 (->
  "table(c('a','b','a','b','a','b','a','b'), c(1,1,2,2,3,3,1,1))"
  r
  r->java
  java->clj
  dataset/mapseq-reader
  set))


(deftest
 test202
 (is
  (=
   var201
   #{{0 "a", 1 "2", :$value 2}
     {0 "b", 1 "3", :$value 1}
     {0 "a", 1 "1", :$value 2}
     {0 "a", 1 "3", :$value 1}
     {0 "b", 1 "2", :$value 1}
     {0 "b", 1 "1", :$value 1}})))


(def
 var203
 (-> {:a [1 2], :b "hi!"} clj->java java->r r->java java->clj))


(deftest test204 (is (= var203 {:a [1 2], :b ["hi!"]})))


(def
 var205
 (->>
  {:a [1 2], :b "hi!"}
  clj->java
  java->r
  ((r "deparse"))
  r->java
  java->clj))


(def var206 (md "### Basic types convertion clj->r->clj"))


(def var207 (def clj->r->clj (comp r->clj r)))


(def var208 (clj->r->clj nil))


(deftest test209 (is (= var208 nil)))


(def var210 (clj->r->clj [10 11]))


(deftest test211 (is (= var210 [10 11])))


(def var212 (clj->r->clj [10.0 11.0]))


(deftest test213 (is (= var212 [10.0 11.0])))


(def var214 (clj->r->clj (list 10.0 11.0)))


(deftest test215 (is (= var214 [10.0 11.0])))


(def var216 (clj->r->clj {:a 1, :b 2}))


(deftest test217 (is (= var216 {:a [1], :b [2]})))


(def var218 (md "### Various R objects"))


(def var219 nil)


(def var220 (-> "list(a=1L,b=c(10,20),c='hi!')" r r->clj))


(deftest test221 (is (= var220 {:a [1], :b [10.0 20.0], :c ["hi!"]})))


(def var222 nil)


(def var223 (-> "c(10,20,30)" r r->clj))


(deftest test224 (is (= var223 [10.0 20.0 30.0])))


(def var225 nil)


(def var226 (-> "c(10L,20L,30L)" r r->clj))


(deftest test227 (is (= var226 [10 20 30])))


(def var228 nil)


(def var229 (-> 'euro r r->clj first))


(deftest test230 (is (= var229 13.7603)))


(def var231 nil)


(def var232 (-> r.stats/dnorm r.base/formals r->clj keys sort))


(deftest test233 (is (= var232 '(:log :mean :sd :x))))


(def var234 nil)


(def var235 (-> "NULL" r r->clj))


(deftest test236 (is (= var235 nil)))


(def var237 nil)


(def var238 (-> "TRUE" r r->clj))


(deftest test239 (is (= var238 [true])))


(def var240 (md "## Inspecting R functions"))


(def
 var241
 (md
  "The `mean` function is defined to expect arguments `x` and `...`.\nThese arguments have no default values (thus, its formals have empty symbols as values):"))


(def var242 (-> 'mean r.base/formals r->clj))


(deftest test243 (is (= var242 {:x (symbol ""), :... (symbol "")})))


(def
 var244
 (md
  "It is an [S3 generic function](http://adv-r.had.co.nz/S3.html) function, which we can realize by printing it:"))


(def var245 (r 'mean))


(def
 var246
 (md
  "So, we can expect possibly more details when inspecting its default implementation.\nNow, we see some arguments that do have default values."))


(def var247 (-> 'mean.default r.base/formals r->clj))


(deftest
 test248
 (is
  (=
   var247
   {:x (symbol ""), :trim [0.0], :na.rm [false], :... (symbol "")})))


(def var249 (md "## R-function-arglists"))


(def
 var250
 (md
  "As we saw earlier, R functions are Clojure functions. The arglists of functions brought up by `require-r` match the expected arguments. Here are some examples:"))


(def var251 (require-r '[base] '[stats] '[grDevices]))


(def
 var252
 (->>
  [#'r.base/mean
   #'r.base/mean-default
   #'r.stats/arima0
   #'r.grDevices/dev-off
   #'r.base/Sys-info
   #'r.base/summary-default
   #'r.base/sin
   #'r.base/sum]
  (map (fn [f] (-> f meta (update :ns (comp symbol str)))))))


(deftest
 test253
 (is
  (=
   var252
   '({:arglists ([x & {:keys [...]}]), :name mean, :ns r.base}
     {:arglists ([x & {:keys [trim na.rm ...]}]),
      :name mean-default,
      :ns r.base}
     {:arglists
      ([x
        &
        {:keys
         [order
          seasonal
          xreg
          include.mean
          delta
          transform.pars
          fixed
          init
          method
          n.cond
          optim.control]}]),
      :name arima0,
      :ns r.stats}
     {:arglists ([& {:keys [which]}]), :name dev-off, :ns r.grDevices}
     {:arglists ([]), :name Sys-info, :ns r.base}
     {:arglists ([object & {:keys [... digits quantile.type]}]),
      :name summary-default,
      :ns r.base}
     {:arglists ([x]), :name sin, :ns r.base}
     {:arglists ([& {:keys [... na.rm]}]), :name sum, :ns r.base}))))
