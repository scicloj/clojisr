;; # R code generation from the Clojure forms

(ns clojisr.v1.codegen
  (:require [clojisr.v1.r :as r :refer [r ->code r->clj]]
            [tech.v3.dataset :as dataset]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.kindly.v4.api :as kindly]))

^:kindly/hide-code
(def md (comp kindly/hide-code kind/md))

(md
 "R code in `clojisr` library can be represented in three main ways:

* as string containing R code or script
* as RObject
* as Clojure form

RObject is `clojisr` data structure which keeps reference to R objects. Also can act as a function when referenced object is R function. RObject is returned always when R code is executed.

Let's see what is possible in detail.

First, require the necessary namespaces.")

(md "Also, let us make sure we are using a clean session.")

(r/set-default-session-type! :rserve)
(r/discard-all-sessions)

(md "## R code as a string")

(md "To run any R code as string or Clojure form we use `clojisr.v1.r/r` function")

(r "mean(rnorm(100000,mean=1.0,sd=3.0))")
(r "abc <- runif(1000);
          f <- function(x) {mean(log(x))};
          f(abc)")

(md "As mentioned above, every `r` call creates RObject and R variable which keeps result of the execution.")

(def result (r "rnorm(10)"))

(class result)
(:object-name result)

(md "Let's use the var name string to see what it represents.")

(r (:object-name result))

(md "Now let us move to discussing the ROBject data type.")

(md "## RObject")

(md "Every RObject acts as Clojure reference to an R variable. All these variables are held in an R environment called `.MEM`. An RObject can represent anything and can be used for further evaluation, even acting as a function if it corresponds to an R function. Here are some examples:")

(md "An r-object holding some R data:")
(def dataset (r "nhtemp"))

(md "An r-object holding an R function:")
(def function (r "mean"))

(md "Printing the data:")
dataset

(md "Equivalently:")
(r dataset)

(md "We use `r->clj` to transfer data from R to Clojure (converting an R object to Clojure data):")

(-> (r->clj dataset)
    (dataset/select-rows 0)
    (dataset/mapseq-reader))

(kindly/check = [{:$series 49.9 :$time 1912.0}])

(md "Creating an R object, applying the function to it, and conveting to Clojure data (in this pipeline, both `function` and `r` return an RObject):")

(-> "c(1,2,3,4,5,6)"
    r
    function
    r->clj)

(kindly/check = [3.5])

(md "## Clojure forms")

(md "Calling R with the code as a string is quite limited. You can't easily inject Clojure data into the code. Also, editor support is very limited for this way of writing. So we enable the use of Clojure forms as a [DSL](https://en.wikipedia.org/wiki/Domain-specific_language) to simplify the construnction of R code.")

(md "In generating R code from Clojure forms, `clojisr` operates on both the var and the symbol level, and can also digest primitive types and basic data structures. There are some special symbols which help in creating R [formulas](https://www.dummies.com/programming/r-formulas-example/) and defining R functions. We will go through all of these in detail.")

(md "The `->code` function is responsible for turning Clojure forms into R code.")


(->code [1 2 4])

(kindly/check = "c(1L,2L,4L)")

(md "When the `r` function gets an argument that is not a string, it uses `->code` behind the scenes to turn that argument into code as a string.")


(r [1 2 4])


(-> [1 2 4]
    r
    r->clj)

(kindly/check = [1 2 4])

(md "Equivalently:")


(-> [1 2 4]
    ->code
    r
    r->clj)

(kindly/check = [1 2 4])

(md "### Primitive data types")

(-> 1 r r->clj)
(kindly/check = [1])

(-> 2.0 r r->clj)
(kindly/check = [2.0])

(-> 3/4 r r->clj)
(kindly/check = [0.75])

(-> true r r->clj)
(kindly/check = [true])

(-> false r r->clj)
(kindly/check = [false])

(md "`nil` is converted to NULL or NA (in vectors or maps)")

(-> nil r r->clj)
(kindly/check = nil)

(->code nil)
(kindly/check = "NULL")

(md "Infinities etc.")

(-> ##Inf r r->clj)
(kindly/check = [##Inf])

(->> ##-Inf r r->clj)
(kindly/check = [##-Inf])

(->> ##NaN r r->clj first)
(kindly/check #(Double/isNaN %))

(md "When you pass a string to `r`, it is treated as code. So we have to escape double quotes if we actually mean to represent an R string (or an R character object, as it is called in R). However, when string is used inside a more complex form, it is escaped automatically.")

(->code "\"this is a string\"")
(kindly/check = "\"\"this is a string\"\"")

(-> "\"this is a string\"" r r->clj)
(kindly/check = ["this is a string"])

(->code '(paste "this is a string"))
(kindly/check = "paste(\"this is a string\")")

(-> '(paste "this is a string") r r->clj)
(kindly/check = ["this is a string"])

(md "Any `Named` Clojure object that is not a String (like a keyword or a symbol) is converted to a R symbol.")

(->code :keyword)
(kindly/check = "keyword")

(->code 'symb)
(kindly/check = "symb")

(md "An RObject is converted to a R variable.")

(->code (r "1+2"))

(md "Date/time is converted to a string.")

(->code #inst "2031-02-03T11:22:33")
(kindly/check (partial re-matches #"'2031-02-03 1.:22:33'"))

(r #inst "2031-02-03T11:22:33")

(-> #inst "2031-02-03T11:22:33"
    r
    r->clj)

(kindly/check (fn [v]
                (and (vector? v)
                     (-> v count (= 1))
                     (->> v first (re-matches #"2031-02-03 1.:22:33")))))

(md "### Vectors")

(md "A Clojure vector is converted to an R vector created using the `c` function. That means that nested vectors are flattened. All the values inside are translated to R recursively.")

(->code [1 2 3])
(kindly/check = "c(1L,2L,3L)")

(-> [[1] [2 [3]]] r r->clj)
(kindly/check = [1 2 3])

(md "Some Clojure sequences are interpreted as function calls, if it makes sense for their first element. However, sequences beginning with numbers or strings are treated as vectors.")

(r (range 11))
(r (map str (range 11)))

(md "#### Tagged vectors")

(md "When the first element of a vector or a sequence is a keyword starting with `:!`, some special conversion takes place.

| keyword | meaning |
| - | - |
| `:!string` | vector of strings |
| `:!boolean` | vector of logicals |
| `:!int` | vector of integers |
| `:!double` | vector of doubles |
| `:!named` | named vector |
| `:!list` | partially named list |
| `:!call` | treat the rest of the vector as callable sequence |
| `:!ct` | vector of POSIXct classes |
| `:!lt` | vector of POSIXlt classes |

`nil` in a vector is converted to `NA`")

(-> [:!string 1 nil 3]
    r r->clj)
(kindly/check = ["1" nil "3"])

(-> [:!boolean 1 true nil false]
    r r->clj)
(kindly/check = [true true nil false])

(-> [:!double 1.0 nil 3]
    r r->clj)
(kindly/check = [1.0 nil 3.0])

(-> [:!int 1.0 nil 3]
    r r->clj)
(kindly/check = [1 nil 3])

(-> [:!named 1 2 :abc 3]
    r r->clj)
(kindly/check = [1 2 3]) ;; I think here we should return map maybe?

(-> [:!list :a 1 :b [:!list 1 2 :c ["a" "b"]]]
    r r->clj)
(kindly/check = {:a [1] :b {0 [1] 1 [2] :c ["a" "b"]}})

(-> [:!ct #inst "2011-11-01T22:33:11"]
    r r->clj)

(-> [:!lt #inst "2011-11-01T22:33:11"]
    r r->clj)

(md "When a vector is big enough, it is transfered not directly as code, but as the name of a newly created R variable holding the corresponding vector data, converted via the Java conversion layer.")

(->code (range 10000))

(-> (conj (range 10000) :!string)
    r r->clj first)
(kindly/check = "0")

(md "Treat vector as callable.")

(-> [:!call 'mean [1 2 3 4]]
    r r->clj)
(kindly/check = [2.5])

(md "### Maps")

(md "A Clojue Map is transformed to an R named list. As with vectors, all data elements inside are processed recursively.")

(r {:a 1 :b nil})

(-> {:a 1 :b nil :c [2.0 3 4]}
    r r->clj)
(kindly/check = {:a [1]
                 :b [nil]
                 :c [2.0 3.0 4.0]})

(md "Bigger maps are transfered to R variables via the Java conversion layer.")

(->code (zipmap (map #(str "key" %) (range 100))
                (range 1000 1100)))
(-> (r (zipmap (map #(str "key" %) (range 100))
               (range 1000 1100)))
    r->clj
    :key23)
(kindly/check = [1023])

(md "### Calls, operators and special symbols")

(md "Now we come to the most important part, using sequences to represent function calls. One way to do that is using a list, where the first element is a symbol corresponding to the name of an R function, or an RObject corresponding to an R function. To create a function call we use the same structure as in clojure. The two examples below are are equivalent.")

(md "Recall that symbols are converted to R variable names on the R side.")

(r "mean(c(1,2,3))")
(r '(mean [1 2 3]))
(->code '(mean [1 2 3]))
(kindly/check = "mean(c(1L,2L,3L))")

(md "Here is another example.")

(r '(<- x (mean [1 2 3])))
(->> 'x r r->clj)
(kindly/check = [2.0])

(md "Here is another example.")

(md "Recall that RObjects are converted to the names of the corresponding R objects.")

(-> (list (r 'median) [1 2 4])
    ->code)

(-> (list (r 'median) [1 2 4])
    r
    r->clj)
(kindly/check = [2])

(md "You can call using special names (surrounded by backquote) as strings")

(-> '("`^`" 10 2) r r->clj)
(kindly/check = [100.0])

(md "There are some special symbols which get a special meaning on,:

| symbol | meaning |
| - | - |
| `'( )` | Wrap first element of the quoted list into parentheses |
| `function` | R function definition |
| `do` | join all forms using \";\"  and wrap into `{}` |
| `for` | for loop with multiple bindings |
| `while` | while loop |
| `if` | if or if-else |
| `tilde` or `formula` | R formula |
| `colon` | colon (`:`) |
| `rsymbol` | qualified and/or backticked symbol wrapper |
| `bra` | `[` |
| `brabra` | `[[` |
| `bra<-` | `[<-` |
| `brabra<-` | `[[<-` |")

(md "Sometimes symbols are represented as string with spaces inside, also can be prepend with package name. Tick `'` in clojure is not enough for that, for that purpose you can use `rsymbol`.")

(r/->code '(rsymbol name))
(kindly/check = "name")

(r/->code '(rsymbol "name with spaces"))
(kindly/check = "`name with spaces`")

(r/->code '(rsymbol package name))
(kindly/check = "package::name")

(r/->code '(rsymbol "package with spaces" name))
(kindly/check = "`package with spaces`::name")

(-> ((r/rsymbol 'base 'mean) [1 2 3 4])
    r->clj)
(kindly/check = [2.5])

(-> ((r/rsymbol "[") 'iris 1) r->clj dataset/mapseq-reader first :Sepal.Length)
(kindly/check = 5.1)

(-> ((r/rsymbol 'base "[") 'iris 1) r->clj dataset/mapseq-reader first :Sepal.Length)
(kindly/check = 5.1)

(md "All `bra...` functions accept `nil` or `empty-symbol` to mark empty selector.")

(def m (r '(matrix (colon 1 6)
                   :nrow 2
                   :dimnames [:!list ["a" "b"] (bra LETTERS (colon 1 3))])))
m

(-> '(bra ~m nil 1)
    r r->clj)
(kindly/check = [1 2])

(-> '(bra ~m 1 nil)
    r r->clj)
(kindly/check = [1 3 5])

(-> '(bra ~m 1 nil :drop false)
    r
    r->clj
    dataset/value-reader)
(kindly/check = [["a" 1 3 5]])

(-> '(bra<- ~m 1 nil [11 22 33])
    r
    r->clj
    dataset/value-reader)
(kindly/check = [["a" 11 22 33]
                 ["b" 2 4 6]])

(-> '(bra<- ~m nil 1 [22 33])
    r
    r->clj
    dataset/value-reader)
(kindly/check = [["a" 22 3 5]
                 ["b" 33 4 6]])

(-> (r/bra m nil 1)
    r->clj)
(kindly/check = [1 2])

(-> (r/bra m 1 nil)
    r->clj)
(kindly/check = [1 3 5])

(-> (r/bra m 1 nil :drop false)
    r->clj
    dataset/value-reader)
(kindly/check = [["a" 1 3 5]])

(-> (r/bra<- m 1 nil [11 22 33])
    r->clj
    dataset/value-reader)
(kindly/check = [["a" 11 22 33]
                 ["b" 2 4 6]])

(-> (r/bra<- m nil 1 [22 33])
    r->clj
    dataset/value-reader)
(kindly/check = [["a" 22 3 5]
                 ["b" 33 4 6]])

(def l (r [:!list "a" "b" "c"]))
l

(-> '(brabra ~l 2)
    r r->clj)
(kindly/check = ["b"])

(-> '(brabra<- ~l 2 nil)
    r r->clj)
(kindly/check = [["a"] ["c"]])

(-> '(brabra<- ~l 5 "fifth")
    r r->clj)
(kindly/check = [["a"] ["b"] ["c"] nil ["fifth"]])


(-> (r/brabra l 2)
    r->clj)
(kindly/check = ["b"])

(-> (r/brabra<- l 2 nil)
    r->clj)
(kindly/check = [["a"] ["c"]])

(-> (r/brabra<- l 5 "fifth")
    r->clj)
(kindly/check = [["a"] ["b"] ["c"] nil ["fifth"]])


(md "You can use `if` with optional `else` form. Use `do` to create block of operations")

(-> '(if true 11 22)
    r r->clj)
(kindly/check = [11])

(-> '(if false 11 22)
    r r->clj)
(kindly/check = [22])

(-> '(if true 11)
    r r->clj)
(kindly/check = [11])

(-> '(if false 11)
    r r->clj)
(kindly/check = nil)

(-> '(if true (do (<- x [1 2 3 4])
                  (mean x)))
    r r->clj)
(kindly/check = [2.5])


(md "`do` wraps everything into curly braces `{}`")

(->code '(do (<- x 1)
             (<- x (+ x 1))))
(kindly/check = "{x<-1L;x<-(x+1L)}")

(md "Loops")

(-> '(do
       (<- v 3)
       (<- coll [v])
       (while (> v 0)
         (<- v (- v 1))
         (<- coll [coll v]))
       coll)
    r
    r->clj)
(kindly/check = [3 2 1 0])

(def for-form '(do
                 (<- coll [])
                 (for [a [1 2]
                       b [3 4]]
                   (<- coll [coll (* a b)]))
                 coll))
(->code for-form)
(-> for-form r r->clj)
(kindly/check = [3 4 6 8])

(md "Sometimes wrapping into parentheses is needed.")

(->code '(:!wrap z))
(kindly/check = "(z)")

(->code '[:!list 1.0 2.0 3.0 (:!wrap inside)])
(kindly/check = "list(1.0,2.0,3.0,(inside))")

(md "#### Function definitions")

(md "To define a function, use the `function` symbol with a following vector of argument names, and then the body. Arguments are treated as a partially named list.")

(r '(<- stat (function [x :median false ...]
                       (ifelse median
                               (median x ...)
                               (mean x ...)))))

(-> '(stat [100 33 22 44 55])
    r r->clj)
(kindly/check = [50.8])

(-> '(stat [100 33 22 44 55] :median true)
    r r->clj)
(kindly/check = [44])

(-> '(stat [100 33 22 44 55 nil])
    r r->clj)
(kindly/check = [nil])

(-> '(stat [100 33 22 44 55 nil] :na.rm true)
    r r->clj)
(kindly/check = [50.8])

(md "#### Formulas")

(md "To create an R [formula](https://www.datacamp.com/community/tutorials/r-formula-tutorial), use `tilde` or `formula` with two arguments, for the left and right sides (to skip one, just use `nil`).")

(r '(formula y x))
(r '(formula y (| (+ a b c d) e)))
(r '(formula nil (| x y)))

(md "#### Operators")

(->code '(+ 1 2 3 4 5))
(->code '(/ 1 2 3 4 5))
(->code '(- [1 2 3]))
(->code '(<- a b c 123))
(->code '($ a b c d))

(md "#### Unquoting")

(md "Sometimes we want to use objects created outside our form (defined earlier or in `let`). For this case you can use the `unqote` (`~`) symbol. There are two options:

* when using quoting `'`, unqote evaluates the uquoted form using `eval`. `eval` has some constrains, the most important is that local bindings (`let` bindings) can't be use.
* when using syntax quoting (backquote `), unqote acts as in clojure macros -- all unquoted forms are evaluated instantly.")


(def v (r '(+ 1 2 3 4)))
(-> '(* 22.0 ~v)
    r r->clj)
(kindly/check = [220.0])

(let [local-v (r '(+ 1 2 3 4))
      local-list [4 5 6]]
  (-> `(* 22.0 ~local-v ~@local-list)
      r r->clj))
(kindly/check = [26400.0])

(md "## Calling R functions")

(md "You are not limited to the use code forms. When an RObject correspinds to an R function, it can be used and called as normal Clojure functions.")

(def square (r '(function [x] (* x x))))
(-> 123 square r->clj)
(kindly/check = [15129])
