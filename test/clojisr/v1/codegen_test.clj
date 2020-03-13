(ns clojisr.v1.codegen-test
  (:require [notespace.v2.note :as note
             :refer [note note-void note-md note-as-md note-hiccup note-as-hiccup check]]
            [clojisr.v1.r :as r]))

(note-md "# R code generation from the Clojure forms")

(note-md
 "R code in `clojisr` library can be represented in three main ways:

* as string containing R code or script
* as RObject
* as Clojure form

RObject is `clojisr` data structure which keeps reference to R objects. Also can act as a function when referenced object is R function. RObject is returned always when R code is executed.

Let's see what is possible in detail. 

First require necessary namespaces.")

(note-void (require '[clojisr.v1.r :as r :refer [r ->code r->clj]]))

(note-md "## String and script")

(note-md "To run any R code as string or Clojure form we use `clojisr.v1.r/r` function")

(note (r "mean(rnorm(100000,mean=1.0,sd=3.0))"))
(note (r "abc <- runif(1000);
          f <- function(x) {mean(log(x))};
          f(abc)"))

(note-md "As mentioned above, every `r` call creates RObject and R variable which keeps result of the execution.")

(note (def result (r "rnorm(10)")))

(note (class result))
(note (:object-name result))

(note-md "Let's use var name string to see what it represents.")

(note (r (:object-name result)))

(note-md "We stop here and let's move to the ROBject itself")

(note-md "## RObject")

(note-md "Every RObject acts as Clojure reference to R variable. This variable is created in separated R environment. RObject can represent anything and can be used for further evaluation and can act as a function.")

(note (def dataset (r "nhtemp")))
(note (def function (r "mean")))

(note (r dataset))

(note-md "Below both `function` and `r` call are RObjects.")

(note (->> (function (r "c(1,2,3,4,5,6)"))
           r->clj
           (check = [3.5])))

(note-md "We use `r->clj` to transfer data from R to Clojure.")

(note (->> (r->clj dataset)
           first
           (check = 49.9)))

(note-md "## Clojure forms")

(note-md "Calling R with string is quite limited. You can't easily inject Clojure objects or values into the string. Also editor support is very limited in such case. So we enable use of Clojure forms as DSL to simplify construnction of R code.")

(note-md "`clojisr` operates on both var and symbol level, also can digest primitive types and basic data structures. There are some special symbols which help creation formulas or define functions. We will go through all of them in detail.")

(note-md "### Primitives")

(note (->> (r 1) r->clj (check = [1.0])))
(note (->> (r 2.0) r->clj (check = [2.0])))
(note (->> (r 3/4) r->clj (check = [0.75])))
(note (->> (r true) r->clj (check = [true])))
(note (->> (r false) r->clj (check = [false])))

(note-md "`nil` is converted to NULL or NA (in vectors or maps)")

(note (->> (r nil) r->clj (check = nil)))
(note (->> (->code nil) (check = "NULL")))

(note-md "When you pass string alone are treated as a code, so we have to escape double quotes if you want to have just string not code executed. However when string is used in more complicated form it's escaped automatically.")

(note (->> (r "\"this is a string\"") r->clj (check = ["this is a string"])))
(note (->> (r '(paste "this is a string")) r->clj (check = ["this is a string"])))

(note-md "Any `Named` object (like keyword or symbol) is converted to a R symbol")

(note (->> (->code :keyword) (check = "keyword")))
(note (->> (->code 'symb) (check = "symb")))

(note-md "RObjects are converted to a R variable")

(note (->code (r "1+2")))

(note-md "Date/time is formatted to a string.")

(note (r #inst "2031-02-03T11:22:33"))

(note-md "### Vectors")

(note-md "Vectors are converted to a R vector created using `c` functions. That means that every nested vectors are flatten. All values are translated to R recursively.")

(note (->> (->code [1 2 3]) (check = "c(1,2,3)")))
(note (->> (r [[1] [2 [3]]]) r->clj (check = [1.0 2.0 3.0])))

(note-md "Any sequence is a function call in general, however sequences containing numbers or strings are treated as vectors.")

(note (->> (r (range 11))))
(note (->> (r (map str (range 11)))))

(note-md "#### Tagged vectors")

(note-md "When first element is a keyword starting with `:!` some special conversion is taken.

| keyword | meaning |
| - | - |
| `:!string` | vector of strings |
| `:!boolean` | vector of logicals |
| `:!int` | vector of integers |
| `:!double` | vector of doubles |
| `:!named` | named vector |
| `:!list` | partially named list |
| `:!ct` | vector of POSIXct classes |
| `:!lt` | vector of POSIXlt classes |

`nil` in a vector is converted to `NA`")

(note (->> (r [:!string 1 nil 3]) r->clj (check = ["1" nil "3"])))
(note (r [:!named 1 2 :abc 3]))
(note (r [:!list :a 1 :b [:!list 1 2 :c ["a" "b"]]]))
(note (->> (r [:!ct #inst "2011-11-01T22:33:11"]) r->clj first long))
(note (->> (r [:!lt #inst "2011-11-01T22:33:11"]) r->clj))

(note-md "When vector is big enough, it is transfered via Java backend first.")

(note (->code (range 10000)))
(note (->> (r (conj (range 10000) :!string)) r->clj first (check = "0")))

(note-md "### Maps")

(note-md "Maps are transformed to a R named lists. As in vectors all data are processed recursively.")

(note (r {:a 1 :b nil}))
(note (->> (r {:a 1 :b nil :c [2 3 4]}) r->clj (check = {:a [1.0]
                                                         :b [nil]
                                                         :c [2.0 3.0 4.0]})))

(note-md "Also bigger maps are transfered via Java first.")

(note (->code (zipmap (map #(str "key" %) (range 100))
                      (range 1000 1100))))
(note (->> (r (zipmap (map #(str "key" %) (range 100))
                      (range 1000 1100)))
           r->clj
           :key23
           (check = [1023])))

(note-md "### Calls, operators and special symbols")

(note-md "Now we came to the most important part, usings sequences to generate function calls. For that we use lists and symbols. To create a function call we use the same structure as in clojure. Below two examples are equivalent.")

(note (r "mean(c(1,2,3))"))
(note (r '(mean [1 2 3])))
(note (->> (->code '(mean [1 2 3])) (check = "mean(c(1,2,3))")))

(note-md "Symbols and RObjects are treated as variables on R side.")

(note (r '(<- x (mean [1 2 3]))))
(note (->> (r 'x) r->clj (check = [2.0])))

(note-md "There are special symbols which have special meaning

| symbol | meaning |
| - | - |
| `function` | R function definition |
| `tilde` or `formula` | R formula |
| `colon` | colon (`:`) |
| `bra` | `[` |
| `brabra` | `[[` |
| `bra<-` | `[<-` |
| `brabra<-` | `[[<-` |")

(note-md "#### Function definition")

(note-md "To define a function use `function` symbol with following vector of arguments and body lines. Arguments are treated as partially named list.")

(note (r '(<- stat (function [x :median false ...]
                             (ifelse median
                                     (median x ...)
                                     (mean x ...))))))

(note (->> (r '(stat [100 33 22 44 55])) r->clj (check = [50.8])))
(note (->> (r '(stat [100 33 22 44 55] :median true)) r->clj (check = [44.0])))
(note (->> (r '(stat [100 33 22 44 55 nil])) r->clj first (check #(Double/isNaN %))))
(note (->> (r '(stat [100 33 22 44 55 nil] :na.rm true)) r->clj (check = [50.8])))

(note-md "#### Formulas")

(note-md "To create formula use `tilde` or `formula` with two arguments, for left and right sides (to skip one just use `nil`).")

(note (r '(formula y x)))
(note (r '(formula y (| (+ a b c d) e))))
(note (r '(formula nil (| x y))))

(note-md "#### Operators")

(note (->code '(+ 1 2 3 4 5)))
(note (->code '(/ 1 2 3 4 5)))
(note (->code '(- [1 2 3])))
(note (->code '(<- a b c 123)))
(note (->code '($ a b c d)))

(note-md "#### Unquoting")

(note-md "Sometimes we want to use objects created outside our form (defined earlier or in `let`). For this case you can use `unqote` (`~`) symbol. There are two options:

* when used quoting `'`, unqote evaluates uquoted form using `eval`. `eval` has some constrains, the most important is that `let` bindings can't be use
* when used syntax quoting (backquote), unqote acts as in macros, all unquoted forms are evaluated instantly.")

(note
 (def v (r '(+ 1 2 3 4)))
 (r '(* 22.0 ~v)))

(note
 (let [local-v (r '(+ 1 2 3 4))
       local-list [4 5 6]]
   (r `(* 22.0 ~local-v ~@local-list))))

(note-md "You are not limited to use code forms, when return value (RObject) is a R function it can be used and called as normal Clojure functions.")

(note (def square (r '(function [x] (* x x)))))
(note (->> (square 123) r->clj first (check = 15129.0)))

(notespace.v2.note/compute-this-notespace!)
