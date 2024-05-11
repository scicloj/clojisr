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


(def
 var3
 (kind/hiccup
  [:style "\nimg {max-width: 100%;}\nsvg {max-width: 100%;}\n"]))


(def var4 (md "## Basic examples"))


(def var5 (md "Let us start by some basic usage examples of Clojisr."))


(def
 var6
 (md
  "First, let us make sure that we use the Rserve backend (in case we were using another engine instead earlier), and that there are no R sessions currently running. This is typically not needed if you just started working. Here, we do it just in case."))


(def var7 (r/set-default-session-type! :rserve))


(def var8 (r/discard-all-sessions))


(def
 var9
 (md
  "Now let us run some R code, and keep a Clojure handle to the return value."))


(def var10 (def x (r "1+2")))


(def var11 (md "The thing we created is something called an ROBject."))


(def var12 (class x))


(def var13 (md "If we wish, we can convert an ROBject to Clojure: "))


(def var14 (r->clj x))


(deftest test15 (is (= var14 [3.0])))


(def
 var16
 (md
  "Let us see more examples of creating ROBjects and converting them to Clojure: "))


(def var17 (->> "list(A=1,B=2,'#123strange<text> ()'=3)" r r->clj))


(deftest
 test18
 (is (= var17 {:A [1.0], :B [2.0], "#123strange<text> ()" [3.0]})))


(def
 var19
 (md
  "In the other direction, we can convert Clojure data to R data. Note that `nil` is turned to `NA`."))


(def var20 (-> [1 nil 3] clj->r))


(def
 var21
 (md
  "We can run code on a separate R session (specify session-args which are different than the default ones)."))


(def
 var22
 (-> "1+2" (r :session-args {:session-name "mysession"}) r->clj))


(deftest test23 (is (= var22 [3.0])))


(def var24 (md "## Functions"))


(def var25 (md "An R function is also a Clojure function."))


(def var26 (def f (r "function(x) x*10")))


(def
 var27
 (md
  "Let us apply it to Clojure data (implicitly converting that data to R)."))


(def var28 (-> 5 f r->clj))


(deftest test29 (is (= var28 [50.0])))


(def var30 (md "We can also apply it to R data."))


(def var31 (-> "5*5" r f r->clj))


(deftest test32 (is (= var31 [250.0])))


(def
 var33
 (md
  "Functions can get named arguments.\nHere we pass the `na.rm` argument,\nthat tells R whether to remove missing values\nwhenn computing the mean."))


(def var34 (r->clj ((r "mean") [1 nil 3] :na.rm true)))


(deftest test35 (is (= var34 [2.0])))


(def var36 (md "Another example:"))


(def
 var37
 (let
  [f (r "function(w,x,y=10,z=20) w+x+y+z")]
  (->> [(f 1 2) (f 1 2 :y 100) (f 1 2 :z 100)] (map r->clj))))


(deftest test38 (is (= var37 [[33.0] [123.0] [113.0]])))


(def
 var39
 (md
  "Some functions are already created in Clojisr and given special names for convenience. Here are some examples:"))


(def var40 (md "R addition:"))


(def var41 (->> (r+ 1 2 3) r->clj))


(deftest test42 (is (= var41 [6])))


(def
 var43
 (md "R colon (`:`), for creating a range of integers, like `0:9`:"))


(def var44 (r->clj (colon 0 9)))


(deftest test45 (is (= var44 (range 10))))


(def var46 (md "## R dataframes and tech.ml.dataset datasets"))


(def
 var47
 (md
  "At Clojure, we have a structure that is equivalent to R dataframes: a [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) dataset.\n\nLet us create such a dataset, pass it to an R function to compute the row means, and then convert the return value back to Clojure."))


(def
 var48
 (let
  [row-means (r "function(data) rowMeans(data)")]
  (-> {:x [1 2 3], :y [4 5 6]} dataset/->dataset row-means r->clj)))


(deftest test49 (is (= var48 [2.5 3.5 4.5])))


(def var50 (md "Let us see some more dataset proccessing through R."))


(def
 var51
 (md
  "Loading the R package [dplyr](https://dplyr.tidyverse.org/) (assuming it is installed)."))


(def var52 (r "library(dplyr)"))


(def
 var53
 (md
  "Using dplyr to process some Clojure dataset, and convert back to the resulting dataset."))


(def
 var54
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
 test55
 (is
  ((fn
    [d]
    (->
     d
     dataset/mapseq-reader
     (= [{:x 2, :y 5, :z 7} {:x 3, :y 6, :z 9}])))
   var54)))


(def
 var56
 (md
  "[Tibbles](https://tibble.tidyverse.org), which are a more recent R dataframe notion, are also supported, as a special case of data frames."))


(def var57 (r "library(tibble)"))


(def var58 (let [tibble (r "tibble")] (tibble :x [1 2 3] :y [4 5 6])))


(def
 var59
 (let
  [tibble (r "tibble")]
  (-> (tibble :x [1 2 3] :y [4 5 6]) r->clj dataset/mapseq-reader)))


(deftest test60 (is (= var59 [{:x 1, :y 4} {:x 2, :y 5} {:x 3, :y 6}])))


(def var61 (md "## R objects"))


(def
 var62
 (md
  "Clojisr holds handles to R objects, that are stored in memory at the R session, where they are assigned random names."))


(def var63 (def one+two (r "1+2")))


(def var64 (class one+two))


(deftest test65 (is (= var64 clojisr.v1.robject.RObject)))


(def
 var66
 (md
  "The name of an object is the place where it is held at R (inside an R [evnironment](http://adv-r.had.co.nz/Environments.html) called `.MEM`)."))


(def var67 (:object-name one+two))


(def var68 (md "## Generating code"))


(def
 var69
 (md
  "Let us see the mechanism by which clojisr generates R code, and the rules defining it."))


(def
 var70
 (md
  "Since we are playing a bit with the internals here, we will need a reference to the R session:"))


(def var71 (def session (session/fetch-or-make nil)))


(def
 var72
 (md
  "For the following examples, we will use some **dummy** handles to R objects with given names:"))


(def var73 (def x (robject/->RObject "robject_x" session nil nil)))


(def var74 (def y (robject/->RObject "robject_y" session nil nil)))


(def var75 (md ".. and some **real** handles to R objects:"))


(def var76 (def minus-eleven (r "-11")))


(def var77 (def abs (r "abs")))


(def
 var78
 (md
  "The function `->code` generates R code according to a certain set of rules. Here we describe some of these rules briefly. We also wrote a dedicated tutorial about the rule set more thoroughly."))


(def
 var79
 (md "For an ROBject, the generated code is just the ROBject name."))


(def var80 (->code x))


(deftest test81 (is (= var80 "robject_x")))


(def
 var82
 (md
  "For a clojure value, we use some form analysis and generate proper R string or values."))


(def var83 (->code "hello"))


(deftest test84 (is ((partial re-matches #"\"hello\"$") var83)))


(def var85 (->code [1 2 3]))


(deftest test86 (is (= var85 "c(1L,2L,3L)")))


(def
 var87
 (md
  "For a symbol, we generate the code with the corresponding R symbol."))


(def var88 (->code 'x))


(def
 var89
 (md
  "A sequential structure (list, vector, etc.) can be interpreted as a compound expression, for which code generation is defined accorting to the first list element."))


(def
 var90
 (md
  "For a list beginning with the symbol `'function`, we generate an R function definition."))


(def var91 (->code '(function [x y] x)))


(deftest test92 (is (= var91 "function(x,y) {x}")))


(def var93 (md "For a vector instead of list, we create R vector."))


(def var94 (->code '[function [x y] x]))


(deftest test95 (is (= var94 "c(function,c(x,y),x)")))


(def
 var96
 (md
  "For a list beginning with the symbol `'formula`, we generate an R `~`-formula."))


(def var97 (->code '(formula x y)))


(deftest test98 (is (= var97 "(x~y)")))


(def
 var99
 (md
  "For a list beginning with a symbol known to be a binary operator, we generate nested calls."))


(def var100 (->code '(+ x y z)))


(deftest test101 (is (= var100 "((x+y)+z)")))


(def
 var102
 (md
  "For a list beginning with another symbol, we generate a function call with that symbol as the function name."))


(def var103 (->code '(f x)))


(deftest test104 (is (= var103 "f(x)")))


(def
 var105
 (md
  "For a list beginning with an R object that is a function, we generate a function call with that object as the function. If you create the list using the quote sign (`'`), don't forget to unquote symbols refering to things you defined on the Clojure side."))


(def var106 (->code '(~abs x)))


(deftest test107 (is ((partial re-matches #"\.MEM\$.*\(x\)") var106)))


(def
 var108
 (md
  "All other sequential things (that is, those not beginning with a symbol or R function) are intepreted as data, converted implicitly R data representation."))


(def
 var109
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
 test110
 (is ((partial re-matches #"\.MEM\$.*\(c\(1L,2L,3L\)\)") var109)))


(def var111 (md "Some more examples, showing how these rules compose:"))


(def var112 (->code '(function [x y] (f y))))


(def var113 (->code '(function [x y] (f ~y))))


(def var114 (->code '(function [x y] (+ x y))))


(def var115 (->code (list 'function '[x y] (list '+ 'x 'y))))


(def var116 (->code '(function [x y] (print x) (f x))))


(def var117 (->code '(function [x y] (~abs x))))


(def var118 (->code '(~abs ~minus-eleven)))


(def var119 (->code '(~abs -11)))


(def
 var120
 (md "Use syntax quote ` in case you want to use local bindings."))


(def
 var121
 (let
  [minus-ten -10]
  (->code
   (clojure.core/sequence
    (clojure.core/seq
     (clojure.core/concat
      (clojure.core/list abs)
      (clojure.core/list minus-ten)))))))


(def var122 (md "## Running generated code"))


(def
 var123
 (md
  "Clojure forms can be run as R code. Behind the scences, they are turned to R code using the `->code` function described above. For example:"))


(def var124 (-> '(~abs ~(range -3 0)) r r->clj))


(deftest test125 (is (= var124 [3 2 1])))


(def var126 (md "Or, equivalently:"))


(def var127 (-> '(~abs ~(range -3 0)) ->code r r->clj))


(deftest test128 (is (= var127 [3 2 1])))


(def
 var129
 (md
  "Let us repeat the basic examples from the beginning of this tutorial,\nthis time generating code rather than writing it as Strings."))


(def var130 (def x (r '(+ 1 2))))


(def var131 (r->clj x))


(deftest test132 (is (= var131 [3])))


(def var133 (def f (r '(function [x] (* x 10)))))


(def var134 (-> 5 f r->clj))


(deftest test135 (is (= var134 [50])))


(def var136 (-> "5*5" r f r->clj))


(deftest test137 (is (= var136 [250.0])))


(def
 var138
 (let
  [row-means (r '(function [data] (rowMeans data)))]
  (-> {:x [1 2 3], :y [4 5 6]} dataset/->dataset row-means r->clj)))


(deftest test139 (is (= var138 [2.5 3.5 4.5])))


(def var140 (r '(library dplyr)))


(def
 var141
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


(def var142 (md "## Requiring R packages"))


(def
 var143
 (md
  "Sometimes, we want to bring to the Clojure world functions and data from R packages.\nHere, we try to follow the [require-python](https://github.com/cnuernber/libpython-clj/blob/master/test/libpython_clj/require_python_test.clj) syntax\nof [libpython-clj](https://github.com/cnuernber/libpython-clj)\n(though currently in a less sophisticated way.)"))


(def var144 (require-r '[stats :as statz :refer [median]]))


(def var145 (-> [1 2 3] r.stats/median r->clj))


(deftest test146 (is (= var145 [2])))


(def var147 (-> [1 2 3] statz/median r->clj))


(deftest test148 (is (= var147 [2])))


(def var149 (-> [1 2 3] median r->clj))


(deftest test150 (is (= var149 [2])))


(def var151 (require-r '[datasets :as datasetz :refer [euro]]))


(def var152 [r.datasets/euro datasetz/euro euro])


(deftest test153 (is ((partial apply =) var152)))


(def var154 (require-r '[base :refer [$]]))


(def var155 (-> {:a 1, :b 2} ($ 'a) r->clj))


(deftest test156 (is (= var155 [1])))


(def var157 (md "## Data visualization"))


(def
 var158
 (md
  "Functions creating R plots or any plotting objects generated by various R libraries can be wrapped in a way that returns an SVG, BufferedImage or can be saved to a file. All of them accept additional parameters specified in `grDevices` R package."))


(def
 var159
 (md
  "Currently there is a bug that sometimes causes axes and labels to disappear when rendered inside a larger HTML."))


(def var160 (require-r '[graphics :refer [plot hist]]))


(def
 var161
 (require-r '[ggplot2 :refer [ggplot aes geom_point xlab ylab labs]]))


(def
 var162
 (require
  '[clojisr.v1.applications.plotting
    :refer
    [plot->svg plot->file plot->buffered-image]]))


(def
 var163
 (md "First example, simple plotting function as SVG string."))


(def
 var164
 (plot->svg
  (fn
   []
   (->>
    rand
    (repeatedly 30)
    (reductions +)
    (plot :xlab "t" :ylab "y" :type "l")))))


(def
 var165
 (md
  "ggplot2 plots (or any other plot objects like lattice) can be also turned into SVG."))


(def
 var166
 (plot->svg
  (let
   [x (repeatedly 99 rand) y (map + x (repeatedly 99 rand))]
   (->
    {:x x, :y y}
    dataset/->dataset
    (ggplot (aes :x x :y y :color '(+ x y) :size '(/ x y)))
    (r+ (geom_point) (xlab "x") (ylab "y"))))))


(def
 var167
 (md
  "Any plot (function or object) can be saved to file or converted to BufferedImage object."))


(def
 var168
 (let
  [path "/tmp/histogram.jpg"]
  (r->clj
   (plot->file
    path
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
    50))
  (-> (clojure.java.shell/sh "ls" path) :out kind/code)))


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
  (mapv (fn [f] (-> f meta (update :ns (comp symbol str)))))))


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
