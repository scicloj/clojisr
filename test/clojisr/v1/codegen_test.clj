(ns clojisr.v1.codegen-test
  (:require [notespace.v2.note :as note
             :refer [note note-void note-md]]
            [tech.ml.dataset :as dataset]
            [notespace.v2.live-reload]
            [clojisr.v1.r :as r]))

(note-md "# R code generation from the Clojure forms")

(note-md
 "R code in `clojisr` library can be represented in three main ways:

* as string containing R code or script
* as RObject
* as Clojure form

RObject is `clojisr` data structure which keeps reference to R objects. Also can act as a function when referenced object is R function. RObject is returned always when R code is executed.

Let's see what is possible in detail. 

First, require the necessary namespaces.")

(note-md "Also, let us make sure we are using a clean session.")

(note-void (require '[clojisr.v1.rserve :as rserve]
                    ;; '[clojisr.v1.renjin :as renjin]
                    '[clojisr.v1.r :as r :refer [r ->code r->clj]]
                    '[notespace.v2.note :refer [check]]))

(note-void
 ;; (renjin/set-as-default!)
 (rserve/set-as-default!)
 (r/discard-all-sessions))

(note-md :r-code-as-a-string "## R code as a string")

(note-md "To run any R code as string or Clojure form we use `clojisr.v1.r/r` function")

(note (r "mean(rnorm(100000,mean=1.0,sd=3.0))"))
(note (r "abc <- runif(1000);
          f <- function(x) {mean(log(x))};
          f(abc)"))

(note-md "As mentioned above, every `r` call creates RObject and R variable which keeps result of the execution.")

(note (def result (r "rnorm(10)")))

(note (class result))
(note (:object-name result))

(note-md "Let's use the var name string to see what it represents.")

(note (r (:object-name result)))

(note-md "Now let us move to discussing the ROBject data type.")

(note-md :r-object "## RObject")

(note-md "Every RObject acts as Clojure reference to an R variable. All these variables are held in an R environment called `.MEM`. An RObject can represent anything and can be used for further evaluation, even acting as a function if it corresponds to an R function. Here are some examples:")

(note-md "An r-object holding some R data:")
(note (def dataset (r "nhtemp")))

(note-md "An r-object holding an R function:")
(note (def function (r "mean")))

(note-md "Printing the data:")
(note dataset)

(note-md "Equivalently:")
(note (r dataset))

(note-md "We use `r->clj` to transfer data from R to Clojure (converting an R object to Clojure data):")

(note (-> (r->clj dataset)
          (dataset/select-rows 0)
          (dataset/mapseq-reader)
          (->> (check = [{:$series 49.9 :$time 1912.0}]))))

(note-md "Creating an R object, applying the function to it, and conveting to Clojure data (in this pipeline, both `function` and `r` return an RObject):")

(note (->> "c(1,2,3,4,5,6)"
           r
           function
           r->clj
           (check = [3.5])))

(note-md :clojure-forms "## Clojure forms")

(note-md "Calling R with the code as a string is quite limited. You can't easily inject Clojure data into the code. Also, editor support is very limited for this way of writing. So we enable the use of Clojure forms as a [DSL](https://en.wikipedia.org/wiki/Domain-specific_language) to simplify the construnction of R code.")

(note-md "In generating R code from Clojure forms, `clojisr` operates on both the var and the symbol level, and can also digest primitive types and basic data structures. There are some special symbols which help in creating R [formulas](https://www.dummies.com/programming/r-formulas-example/) and defining R functions. We will go through all of these in detail.")

(note-md "The `->code` function is responsible for turning Clojure forms into R code.")

(note
 (->> [1 2 4]
     ->code
     (check = "c(1,2,4)")))

(note-md "When the `r` function gets an argument that is not a string, it uses `->code` behind the scenes to turn that argument into code as a string.")

(note
 (r [1 2 4]))

(note
 (->> [1 2 4]
      r
      r->clj
      (check = [1.0 2.0 4.0])))

(note-md "Equivalently:")

(note
 (->> [1 2 4]
      ->code
      r
      r->clj
      (check = [1.0 2.0 4.0])))

(note-md "### Primitive data types")

(note (->> (r 1) r->clj (check = [1.0])))
(note (->> (r 2.0) r->clj (check = [2.0])))
(note (->> (r 3/4) r->clj (check = [0.75])))
(note (->> (r true) r->clj (check = [true])))
(note (->> (r false) r->clj (check = [false])))

(note-md "`nil` is converted to NULL or NA (in vectors or maps)")

(note (->> (r nil) r->clj (check = nil)))
(note (->> (->code nil) (check = "NULL")))

(note-md "When you pass a string to `r`, it is treated as code. So we have to escape double quotes if we actually mean to represent an R string (or an R character object, as it is called in R). However, when string is used inside a more complex form, it is escaped automatically.")

(note (->> (->code "\"this is a string\"")
           (check = "\"\"this is a string\"\"")))
(note (->> (r "\"this is a string\"") r->clj (check = ["this is a string"])))
(note (->> (->code '(paste "this is a string"))
           (check = "paste(\"this is a string\")")))
(note (->> (r '(paste "this is a string")) r->clj (check = ["this is a string"])))

(note-md "Any `Named` Clojure object that is not a String (like a keyword or a symbol) is converted to a R symbol.")

(note (->> (->code :keyword) (check = "keyword")))
(note (->> (->code 'symb) (check = "symb")))

(note-md "An RObject is converted to a R variable.")

(note (->code (r "1+2")))

(note-md "Date/time is converted to a string.")

(note (->> #inst "2031-02-03T11:22:33"
           ->code
           (check re-matches #"'2031-02-03 1.:22:33'")))

(note (r #inst "2031-02-03T11:22:33"))

(note (->> #inst "2031-02-03T11:22:33"
           r
           r->clj
           (check (fn [v]
                    (and (vector? v)
                         (-> v count (= 1))
                         (->> v first (re-matches #"2031-02-03 1.:22:33")))))))

(note-md "### Vectors")

(note-md "A Clojure vector is converted to an R vector created using the `c` function. That means that nested vectors are flattened. All the values inside are translated to R recursively.")

(note (->> (->code [1 2 3]) (check = "c(1,2,3)")))
(note (->> (r [[1] [2 [3]]]) r->clj (check = [1.0 2.0 3.0])))

(note-md "Some Clojure sequences are interpreted as function calls, if it makes sense for their first element. However, sequences beginning with numbers or strings are treated as vectors.")

(note (r (range 11)))
(note (r (map str (range 11))))

(note-md "#### Tagged vectors")

(note-md "When the first element of a vector or a sequence is a keyword starting with `:!`, some special conversion takes place.

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

(note (->> (r [:!string 1 nil 3]) r->clj (check = ["1" nil "3"])))
(note (r [:!named 1 2 :abc 3]))
(note (r [:!list :a 1 :b [:!list 1 2 :c ["a" "b"]]]))
(note (->> (r [:!ct #inst "2011-11-01T22:33:11"]) r->clj))
(note (->> (r [:!lt #inst "2011-11-01T22:33:11"]) r->clj))

(note-md "When a vector is big enough, it is transfered not directly as code, but as the name of a newly created R variable holding the corresponding vector data, converted via the Java conversion layer.")

(note (->code (range 10000)))
(note (->> (r (conj (range 10000) :!string)) r->clj first (check = "0")))

(note-md "Treat vector as callable.")

(note (->> (r [:!call 'mean [1 2 3 4]]) r->clj (check = [2.5])))

(note-md "### Maps")

(note-md "A Clojue Map is transformed to an R named list. As with vectors, all data elements inside are processed recursively.")

(note (r {:a 1 :b nil}))
(note (->> (r {:a 1 :b nil :c [2 3 4]}) r->clj (check = {:a [1.0]
                                                         :b [nil]
                                                         :c [2.0 3.0 4.0]})))

(note-md "Bigger maps are transfered to R variables via the Java conversion layer.")

(note (->code (zipmap (map #(str "key" %) (range 100))
                      (range 1000 1100))))
(note (->> (r (zipmap (map #(str "key" %) (range 100))
                      (range 1000 1100)))
           r->clj
           :key23
           (check = [1023])))

(note-md "### Calls, operators and special symbols")

(note-md "Now we come to the most important part, using sequences to represent function calls. One way to do that is using a list, where the first element is a symbol corresponding to the name of an R function, or an RObject corresponding to an R function. To create a function call we use the same structure as in clojure. The two examples below are are equivalent.")

(note-md "Recall that symbols are converted to R variable names on the R side.")

(note (r "mean(c(1,2,3))"))
(note (r '(mean [1 2 3])))
(note (->> (->code '(mean [1 2 3])) (check = "mean(c(1,2,3))")))

(note-md "Here is another example.")

(note (r '(<- x (mean [1 2 3]))))
(note (->> (r 'x) r->clj (check = [2.0])))

(note-md "Here is another example.")

(note-md "Recall that RObjects are converted to the names of the corresponding R objects.")

(note (-> (list (r 'median) [1 2 4])
          ->code))

(note (->> (list (r 'median) [1 2 4])
           r
           r->clj
           (check = [2.0])))

(note-md "You can call using special names (surrounded by backquote) as strings")

(note (->> (r '("`^`" 10 2)) r->clj (check = [100.0])))

(note-md "There are some special symbols which get a special meaning on,:

| symbol | meaning |
| - | - |
| `function` | R function definition |
| `do` | join all forms using \";\" |
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

(note-md "Sometimes symbols are represented as string with spaces inside, also can be prepend with package name. Tick `'` in clojure is not enough for that, for that purpose you can use `rsymbol`.")

(note (->> (r/->code '(rsymbol name)) (check = "name")))
(note (->> (r/->code '(rsymbol "name with spaces")) (check = "`name with spaces`")))
(note (->> (r/->code '(rsymbol package name)) (check = "package::name")))
(note (->> (r/->code '(rsymbol "package with spaces" name)) (check = "`package with spaces`::name")))

(note (->> ((r/rsymbol 'base 'mean) [1 2 3 4]) r->clj (check = [2.5])))
(note (->> ((r/rsymbol "[") 'iris 1) r->clj dataset/->flyweight first :Sepal.Length (check = 5.1)))
(note (->> ((r/rsymbol 'base "[") 'iris 1) r->clj dataset/->flyweight first :Sepal.Length (check = 5.1)))

(note-md "All `bra...` functions accept `nil` or `empty-symbol` to mark empty selector.")

(note-void (def m (r '(matrix (colon 1 6)
                              :nrow 2
                              :dimnames [:!list ["a" "b"] (bra LETTERS (colon 1 3))]))))
(note m)

(note (->> (r '(bra ~m nil 1)) r->clj (check = [1 2])))
(note (->> (r '(bra ~m 1 nil)) r->clj (check = [1 3 5])))
(note (->> (r '(bra ~m 1 nil :drop false)) r->clj dataset/value-reader (check = [["a" 1 3 5]])))
(note (->> (r '(bra<- ~m 1 nil [11 22 33])) r->clj dataset/value-reader (check = [["a" 11.0 22.0 33.0]
                                                                                  ["b" 2.0 4.0 6.0]])))
(note (->> (r '(bra<- ~m nil 1 [22 33])) r->clj dataset/value-reader (check = [["a" 22.0 3.0 5.0]
                                                                               ["b" 33.0 4.0 6.0]])))
(note (->> (r/bra m nil 1) r->clj (check = [1 2])))
(note (->> (r/bra m 1 nil) r->clj (check = [1 3 5])))
(note (->> (r/bra m 1 nil :drop false) r->clj dataset/value-reader (check = [["a" 1 3 5]])))
(note (->> (r/bra<- m 1 nil [11 22 33]) r->clj dataset/value-reader (check = [["a" 11.0 22.0 33.0]
                                                                              ["b" 2.0 4.0 6.0]])))
(note (->> (r/bra<- m nil 1 [22 33]) r->clj dataset/value-reader (check = [["a" 22.0 3.0 5.0]
                                                                           ["b" 33.0 4.0 6.0]])))

(note-void (def l (r [:!list "a" "b" "c"])))
(note l)

(note (->> (r '(brabra ~l 2)) r->clj (check = ["b"])))
(note (->> (r '(brabra<- ~l 2 nil)) r->clj (check = [["a"] ["c"]])))
(note (->> (r '(brabra<- ~l 5 "fifth")) r->clj (check = [["a"] ["b"] ["c"] nil ["fifth"]])))

(note (->> (r/brabra l 2) r->clj (check = ["b"])))
(note (->> (r/brabra<- l 2 nil) r->clj (check = [["a"] ["c"]])))
(note (->> (r/brabra<- l 5 "fifth") r->clj (check = [["a"] ["b"] ["c"] nil ["fifth"]])))


(note-md "You can use `if` with optional `else` form. Use `do` to create block of operations")

(note (->> (r '(if true 11 22)) r->clj (check = [11.0])))
(note (->> (r '(if false 11 22)) r->clj (check = [22.0])))
(note (->> (r '(if true 11)) r->clj (check = [11.0])))
(note (->> (r '(if false 11)) r->clj (check = nil)))
(note (->> (r '(if true (do (<- x [1 2 3 4])
                            (mean x)))) r->clj (check = [2.5])))

(note-md "Loops")

(note (->> (r '(do
                 (<- v 3)
                 (<- coll [v])
                 (while (> v 0)
                   (<- v (- v 1))
                   (<- coll [coll v]))
                 coll))
           r->clj
           (check = [3.0 2.0 1.0 0.0])))

(note-void
 (def for-form '(do
                  (<- coll [])
                  (for [a [1 2]
                        b [3 4]]
                    (<- coll [coll (* a b)]))
                  coll)))
(note (->code for-form))
(note (->> (r for-form) r->clj (check = [3.0 4.0 6.0 8.0])))

(note-md "#### Function definitions")

(note-md "To define a function, use the `function` symbol with a following vector of argument names, and then the body. Arguments are treated as a partially named list.")

(note (r '(<- stat (function [x :median false ...]
                             (ifelse median
                                     (median x ...)
                                     (mean x ...))))))

(note (->> (r '(stat [100 33 22 44 55])) r->clj (check = [50.8])))
(note (->> (r '(stat [100 33 22 44 55] :median true)) r->clj (check = [44.0])))
(note (->> (r '(stat [100 33 22 44 55 nil])) r->clj first (check nil?)))
(note (->> (r '(stat [100 33 22 44 55 nil] :na.rm true)) r->clj (check = [50.8])))

(note-md "#### Formulas")

(note-md "To create an R [formula](https://www.datacamp.com/community/tutorials/r-formula-tutorial), use `tilde` or `formula` with two arguments, for the left and right sides (to skip one, just use `nil`).")

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

(note-md "Sometimes we want to use objects created outside our form (defined earlier or in `let`). For this case you can use the `unqote` (`~`) symbol. There are two options:

* when using quoting `'`, unqote evaluates the uquoted form using `eval`. `eval` has some constrains, the most important is that local bindings (`let` bindings) can't be use.
* when using syntax quoting (backquote `), unqote acts as in clojure macros -- all unquoted forms are evaluated instantly.")

(note
 (def v (r '(+ 1 2 3 4)))
 (r '(* 22.0 ~v)))

(note
 (let [local-v (r '(+ 1 2 3 4))
       local-list [4 5 6]]
   (r `(* 22.0 ~local-v ~@local-list))))

(note-md :calling-R-functions "## Calling R functions")

(note-md "You are not limited to the use code forms. When an RObject correspinds to an R function, it can be used and called as normal Clojure functions.")

(note (def square (r '(function [x] (* x x)))))
(note (->> (square 123) r->clj first (check = 15129.0)))

(comment (notespace.v2.note/compute-this-notespace!))

