(def var0 nil)


(ns
 clojisr.v1.tutorials.codegen-generated-test
 (:require
  [clojisr.v1.r :as r :refer [r ->code r->clj]]
  [tech.v3.dataset :as dataset]
  [scicloj.kindly.v4.kind :as kind]
  [scicloj.kindly.v4.api :as kindly]
  [clojure.test :refer [deftest is]]))


(def var2 (def md (comp kindly/hide-code kind/md)))


(def
 var3
 (md
  "R code in `clojisr` library can be represented in three main ways:\n\n* as string containing R code or script\n* as RObject\n* as Clojure form\n\nRObject is `clojisr` data structure which keeps reference to R objects. Also can act as a function when referenced object is R function. RObject is returned always when R code is executed.\n\nLet's see what is possible in detail.\n\nFirst, require the necessary namespaces."))


(def var4 (md "Also, let us make sure we are using a clean session."))


(def var5 (r/set-default-session-type! :rserve))


(def var6 (r/discard-all-sessions))


(def var7 (md "## R code as a string"))


(def
 var8
 (md
  "To run any R code as string or Clojure form we use `clojisr.v1.r/r` function"))


(def var9 (r "mean(rnorm(100000,mean=1.0,sd=3.0))"))


(def
 var10
 (r
  "abc <- runif(1000);\n          f <- function(x) {mean(log(x))};\n          f(abc)"))


(def
 var11
 (md
  "As mentioned above, every `r` call creates RObject and R variable which keeps result of the execution."))


(def var12 (def result (r "rnorm(10)")))


(def var13 (class result))


(def var14 (:object-name result))


(def
 var15
 (md "Let's use the var name string to see what it represents."))


(def var16 (r (:object-name result)))


(def var17 (md "Now let us move to discussing the ROBject data type."))


(def var18 (md "## RObject"))


(def
 var19
 (md
  "Every RObject acts as Clojure reference to an R variable. All these variables are held in an R environment called `.MEM`. An RObject can represent anything and can be used for further evaluation, even acting as a function if it corresponds to an R function. Here are some examples:"))


(def var20 (md "An r-object holding some R data:"))


(def var21 (def dataset (r "nhtemp")))


(def var22 (md "An r-object holding an R function:"))


(def var23 (def function (r "mean")))


(def var24 (md "Printing the data:"))


(def var25 dataset)


(def var26 (md "Equivalently:"))


(def var27 (r dataset))


(def
 var28
 (md
  "We use `r->clj` to transfer data from R to Clojure (converting an R object to Clojure data):"))


(def
 var29
 (-> (r->clj dataset) (dataset/select-rows 0) (dataset/mapseq-reader)))


(deftest test30 (is (= var29 [{:$series 49.9, :$time 1912.0}])))


(def
 var31
 (md
  "Creating an R object, applying the function to it, and conveting to Clojure data (in this pipeline, both `function` and `r` return an RObject):"))


(def var32 (-> "c(1,2,3,4,5,6)" r function r->clj))


(deftest test33 (is (= var32 [3.5])))


(def var34 (md "## Clojure forms"))


(def
 var35
 (md
  "Calling R with the code as a string is quite limited. You can't easily inject Clojure data into the code. Also, editor support is very limited for this way of writing. So we enable the use of Clojure forms as a [DSL](https://en.wikipedia.org/wiki/Domain-specific_language) to simplify the construnction of R code."))


(def
 var36
 (md
  "In generating R code from Clojure forms, `clojisr` operates on both the var and the symbol level, and can also digest primitive types and basic data structures. There are some special symbols which help in creating R [formulas](https://www.dummies.com/programming/r-formulas-example/) and defining R functions. We will go through all of these in detail."))


(def
 var37
 (md
  "The `->code` function is responsible for turning Clojure forms into R code."))


(def var38 (->code [1 2 4]))


(deftest test39 (is (= var38 "c(1L,2L,4L)")))


(def
 var40
 (md
  "When the `r` function gets an argument that is not a string, it uses `->code` behind the scenes to turn that argument into code as a string."))


(def var41 (r [1 2 4]))


(def var42 (-> [1 2 4] r r->clj))


(deftest test43 (is (= var42 [1 2 4])))


(def var44 (md "Equivalently:"))


(def var45 (-> [1 2 4] ->code r r->clj))


(deftest test46 (is (= var45 [1 2 4])))


(def var47 (md "### Primitive data types"))


(def var48 (-> 1 r r->clj))


(deftest test49 (is (= var48 [1])))


(def var50 (-> 2.0 r r->clj))


(deftest test51 (is (= var50 [2.0])))


(def var52 (-> 3/4 r r->clj))


(deftest test53 (is (= var52 [0.75])))


(def var54 (-> true r r->clj))


(deftest test55 (is (= var54 [true])))


(def var56 (-> false r r->clj))


(deftest test57 (is (= var56 [false])))


(def var58 (md "`nil` is converted to NULL or NA (in vectors or maps)"))


(def var59 (-> nil r r->clj))


(deftest test60 (is (= var59 nil)))


(def var61 (->code nil))


(deftest test62 (is (= var61 "NULL")))


(def var63 (md "Infinities etc."))


(def var64 (-> ##Inf r r->clj))


(deftest test65 (is (= var64 [##Inf])))


(def var66 (->> ##-Inf r r->clj))


(deftest test67 (is (= var66 [##-Inf])))


(def var68 (->> ##NaN r r->clj first))


(deftest
 test69
 (is ((fn* [p1__47451#] (Double/isNaN p1__47451#)) var68)))


(def
 var70
 (md
  "When you pass a string to `r`, it is treated as code. So we have to escape double quotes if we actually mean to represent an R string (or an R character object, as it is called in R). However, when string is used inside a more complex form, it is escaped automatically."))


(def var71 (->code "\"this is a string\""))


(deftest test72 (is (= var71 "\"\"this is a string\"\"")))


(def var73 (-> "\"this is a string\"" r r->clj))


(deftest test74 (is (= var73 ["this is a string"])))


(def var75 (->code '(paste "this is a string")))


(deftest test76 (is (= var75 "paste(\"this is a string\")")))


(def var77 (-> '(paste "this is a string") r r->clj))


(deftest test78 (is (= var77 ["this is a string"])))


(def
 var79
 (md
  "Any `Named` Clojure object that is not a String (like a keyword or a symbol) is converted to a R symbol."))


(def var80 (->code :keyword))


(deftest test81 (is (= var80 "keyword")))


(def var82 (->code 'symb))


(deftest test83 (is (= var82 "symb")))


(def var84 (md "An RObject is converted to a R variable."))


(def var85 (->code (r "1+2")))


(def var86 (md "Date/time is converted to a string."))


(def var87 (->code #inst "2031-02-03T11:22:33.000-00:00"))


(deftest
 test88
 (is ((partial re-matches #"'2031-02-03 1.:22:33'") var87)))


(def var89 (r #inst "2031-02-03T11:22:33.000-00:00"))


(def var90 (-> #inst "2031-02-03T11:22:33.000-00:00" r r->clj))


(deftest
 test91
 (is
  ((fn
    [v]
    (and
     (vector? v)
     (-> v count (= 1))
     (->> v first (re-matches #"2031-02-03 1.:22:33"))))
   var90)))


(def var92 (md "### Vectors"))


(def
 var93
 (md
  "A Clojure vector is converted to an R vector created using the `c` function. That means that nested vectors are flattened. All the values inside are translated to R recursively."))


(def var94 (->code [1 2 3]))


(deftest test95 (is (= var94 "c(1L,2L,3L)")))


(def var96 (-> [[1] [2 [3]]] r r->clj))


(deftest test97 (is (= var96 [1 2 3])))


(def
 var98
 (md
  "Some Clojure sequences are interpreted as function calls, if it makes sense for their first element. However, sequences beginning with numbers or strings are treated as vectors."))


(def var99 (r (range 11)))


(def var100 (r (map str (range 11))))


(def var101 (md "#### Tagged vectors"))


(def
 var102
 (md
  "When the first element of a vector or a sequence is a keyword starting with `:!`, some special conversion takes place.\n\n| keyword | meaning |\n| - | - |\n| `:!string` | vector of strings |\n| `:!boolean` | vector of logicals |\n| `:!int` | vector of integers |\n| `:!double` | vector of doubles |\n| `:!named` | named vector |\n| `:!list` | partially named list |\n| `:!call` | treat the rest of the vector as callable sequence |\n| `:!ct` | vector of POSIXct classes |\n| `:!lt` | vector of POSIXlt classes |\n\n`nil` in a vector is converted to `NA`"))


(def var103 (-> [:!string 1 nil 3] r r->clj))


(deftest test104 (is (= var103 ["1" nil "3"])))


(def var105 (-> [:!boolean 1 true nil false] r r->clj))


(deftest test106 (is (= var105 [true true nil false])))


(def var107 (-> [:!double 1.0 nil 3] r r->clj))


(deftest test108 (is (= var107 [1.0 nil 3.0])))


(def var109 (-> [:!int 1.0 nil 3] r r->clj))


(deftest test110 (is (= var109 [1 nil 3])))


(def var111 (-> [:!named 1 2 :abc 3] r r->clj))


(deftest test112 (is (= var111 [1 2 3])))


(def var113 nil)


(def var114 (-> [:!list :a 1 :b [:!list 1 2 :c ["a" "b"]]] r r->clj))


(deftest
 test115
 (is (= var114 {:a [1], :b {0 [1], 1 [2], :c ["a" "b"]}})))


(def var116 (-> [:!ct #inst "2011-11-01T22:33:11.000-00:00"] r r->clj))


(def var117 (-> [:!lt #inst "2011-11-01T22:33:11.000-00:00"] r r->clj))


(def
 var118
 (md
  "When a vector is big enough, it is transfered not directly as code, but as the name of a newly created R variable holding the corresponding vector data, converted via the Java conversion layer."))


(def var119 (->code (range 10000)))


(def var120 (-> (conj (range 10000) :!string) r r->clj first))


(deftest test121 (is (= var120 "0")))


(def var122 (md "Treat vector as callable."))


(def var123 (-> [:!call 'mean [1 2 3 4]] r r->clj))


(deftest test124 (is (= var123 [2.5])))


(def var125 (md "### Maps"))


(def
 var126
 (md
  "A Clojue Map is transformed to an R named list. As with vectors, all data elements inside are processed recursively."))


(def var127 (r {:a 1, :b nil}))


(def var128 (-> {:a 1, :b nil, :c [2.0 3 4]} r r->clj))


(deftest test129 (is (= var128 {:a [1], :b [nil], :c [2.0 3.0 4.0]})))


(def
 var130
 (md
  "Bigger maps are transfered to R variables via the Java conversion layer."))


(def
 var131
 (->code
  (zipmap
   (map (fn* [p1__47452#] (str "key" p1__47452#)) (range 100))
   (range 1000 1100))))


(def
 var132
 (->
  (r
   (zipmap
    (map (fn* [p1__47453#] (str "key" p1__47453#)) (range 100))
    (range 1000 1100)))
  r->clj
  :key23))


(deftest test133 (is (= var132 [1023])))


(def var134 (md "### Calls, operators and special symbols"))


(def
 var135
 (md
  "Now we come to the most important part, using sequences to represent function calls. One way to do that is using a list, where the first element is a symbol corresponding to the name of an R function, or an RObject corresponding to an R function. To create a function call we use the same structure as in clojure. The two examples below are are equivalent."))


(def
 var136
 (md
  "Recall that symbols are converted to R variable names on the R side."))


(def var137 (r "mean(c(1,2,3))"))


(def var138 (r '(mean [1 2 3])))


(def var139 (->code '(mean [1 2 3])))


(deftest test140 (is (= var139 "mean(c(1L,2L,3L))")))


(def var141 (md "Here is another example."))


(def var142 (r '(<- x (mean [1 2 3]))))


(def var143 (->> 'x r r->clj))


(deftest test144 (is (= var143 [2.0])))


(def var145 (md "Here is another example."))


(def
 var146
 (md
  "Recall that RObjects are converted to the names of the corresponding R objects."))


(def var147 (-> (list (r 'median) [1 2 4]) ->code))


(def var148 (-> (list (r 'median) [1 2 4]) r r->clj))


(deftest test149 (is (= var148 [2])))


(def
 var150
 (md
  "You can call using special names (surrounded by backquote) as strings"))


(def var151 (-> '("`^`" 10 2) r r->clj))


(deftest test152 (is (= var151 [100.0])))


(def
 var153
 (md
  "There are some special symbols which get a special meaning on,:\n\n| symbol | meaning |\n| - | - |\n| `'( )` | Wrap first element of the quoted list into parentheses |\n| `function` | R function definition |\n| `do` | join all forms using \";\"  and wrap into `{}` |\n| `for` | for loop with multiple bindings |\n| `while` | while loop |\n| `if` | if or if-else |\n| `tilde` or `formula` | R formula |\n| `colon` | colon (`:`) |\n| `rsymbol` | qualified and/or backticked symbol wrapper |\n| `bra` | `[` |\n| `brabra` | `[[` |\n| `bra<-` | `[<-` |\n| `brabra<-` | `[[<-` |"))


(def
 var154
 (md
  "Sometimes symbols are represented as string with spaces inside, also can be prepend with package name. Tick `'` in clojure is not enough for that, for that purpose you can use `rsymbol`."))


(def var155 (r/->code '(rsymbol name)))


(deftest test156 (is (= var155 "name")))


(def var157 (r/->code '(rsymbol "name with spaces")))


(deftest test158 (is (= var157 "`name with spaces`")))


(def var159 (r/->code '(rsymbol package name)))


(deftest test160 (is (= var159 "package::name")))


(def var161 (r/->code '(rsymbol "package with spaces" name)))


(deftest test162 (is (= var161 "`package with spaces`::name")))


(def var163 (-> ((r/rsymbol 'base 'mean) [1 2 3 4]) r->clj))


(deftest test164 (is (= var163 [2.5])))


(def
 var165
 (->
  ((r/rsymbol "[") 'iris 1)
  r->clj
  dataset/mapseq-reader
  first
  :Sepal.Length))


(deftest test166 (is (= var165 5.1)))


(def
 var167
 (->
  ((r/rsymbol 'base "[") 'iris 1)
  r->clj
  dataset/mapseq-reader
  first
  :Sepal.Length))


(deftest test168 (is (= var167 5.1)))


(def
 var169
 (md
  "All `bra...` functions accept `nil` or `empty-symbol` to mark empty selector."))


(def
 var170
 (def
  m
  (r
   '(matrix
     (colon 1 6)
     :nrow
     2
     :dimnames
     [:!list ["a" "b"] (bra LETTERS (colon 1 3))]))))


(def var171 m)


(def var172 (-> '(bra ~m nil 1) r r->clj))


(deftest test173 (is (= var172 [1 2])))


(def var174 (-> '(bra ~m 1 nil) r r->clj))


(deftest test175 (is (= var174 [1 3 5])))


(def
 var176
 (-> '(bra ~m 1 nil :drop false) r r->clj dataset/value-reader))


(deftest test177 (is (= var176 [["a" 1 3 5]])))


(def
 var178
 (-> '(bra<- ~m 1 nil [11 22 33]) r r->clj dataset/value-reader))


(deftest test179 (is (= var178 [["a" 11 22 33] ["b" 2 4 6]])))


(def
 var180
 (-> '(bra<- ~m nil 1 [22 33]) r r->clj dataset/value-reader))


(deftest test181 (is (= var180 [["a" 22 3 5] ["b" 33 4 6]])))


(def var182 (-> (r/bra m nil 1) r->clj))


(deftest test183 (is (= var182 [1 2])))


(def var184 (-> (r/bra m 1 nil) r->clj))


(deftest test185 (is (= var184 [1 3 5])))


(def
 var186
 (-> (r/bra m 1 nil :drop false) r->clj dataset/value-reader))


(deftest test187 (is (= var186 [["a" 1 3 5]])))


(def
 var188
 (-> (r/bra<- m 1 nil [11 22 33]) r->clj dataset/value-reader))


(deftest test189 (is (= var188 [["a" 11 22 33] ["b" 2 4 6]])))


(def var190 (-> (r/bra<- m nil 1 [22 33]) r->clj dataset/value-reader))


(deftest test191 (is (= var190 [["a" 22 3 5] ["b" 33 4 6]])))


(def var192 (def l (r [:!list "a" "b" "c"])))


(def var193 l)


(def var194 (-> '(brabra ~l 2) r r->clj))


(deftest test195 (is (= var194 ["b"])))


(def var196 (-> '(brabra<- ~l 2 nil) r r->clj))


(deftest test197 (is (= var196 [["a"] ["c"]])))


(def var198 (-> '(brabra<- ~l 5 "fifth") r r->clj))


(deftest test199 (is (= var198 [["a"] ["b"] ["c"] nil ["fifth"]])))


(def var200 (-> (r/brabra l 2) r->clj))


(deftest test201 (is (= var200 ["b"])))


(def var202 (-> (r/brabra<- l 2 nil) r->clj))


(deftest test203 (is (= var202 [["a"] ["c"]])))


(def var204 (-> (r/brabra<- l 5 "fifth") r->clj))


(deftest test205 (is (= var204 [["a"] ["b"] ["c"] nil ["fifth"]])))


(def
 var206
 (md
  "You can use `if` with optional `else` form. Use `do` to create block of operations"))


(def var207 (-> '(if true 11 22) r r->clj))


(deftest test208 (is (= var207 [11])))


(def var209 (-> '(if false 11 22) r r->clj))


(deftest test210 (is (= var209 [22])))


(def var211 (-> '(if true 11) r r->clj))


(deftest test212 (is (= var211 [11])))


(def var213 (-> '(if false 11) r r->clj))


(deftest test214 (is (= var213 nil)))


(def var215 (-> '(if true (do (<- x [1 2 3 4]) (mean x))) r r->clj))


(deftest test216 (is (= var215 [2.5])))


(def var217 (md "`do` wraps everything into curly braces `{}`"))


(def var218 (->code '(do (<- x 1) (<- x (+ x 1)))))


(deftest test219 (is (= var218 "{x<-1L;x<-(x+1L)}")))


(def var220 (md "Loops"))


(def
 var221
 (->
  '(do
    (<- v 3)
    (<- coll [v])
    (while (> v 0) (<- v (- v 1)) (<- coll [coll v]))
    coll)
  r
  r->clj))


(deftest test222 (is (= var221 [3 2 1 0])))


(def
 var223
 (def
  for-form
  '(do
    (<- coll [])
    (for [a [1 2] b [3 4]] (<- coll [coll (* a b)]))
    coll)))


(def var224 (->code for-form))


(def var225 (-> for-form r r->clj))


(deftest test226 (is (= var225 [3 4 6 8])))


(def var227 (md "Sometimes wrapping into parentheses is needed."))


(def var228 (->code '(:!wrap z)))


(deftest test229 (is (= var228 "(z)")))


(def var230 (->code '[:!list 1.0 2.0 3.0 (:!wrap inside)]))


(deftest test231 (is (= var230 "list(1.0,2.0,3.0,(inside))")))


(def var232 (md "#### Function definitions"))


(def
 var233
 (md
  "To define a function, use the `function` symbol with a following vector of argument names, and then the body. Arguments are treated as a partially named list."))


(def
 var234
 (r
  '(<-
    stat
    (function
     [x :median false ...]
     (ifelse median (median x ...) (mean x ...))))))


(def var235 (-> '(stat [100 33 22 44 55]) r r->clj))


(deftest test236 (is (= var235 [50.8])))


(def var237 (-> '(stat [100 33 22 44 55] :median true) r r->clj))


(deftest test238 (is (= var237 [44])))


(def var239 (-> '(stat [100 33 22 44 55 nil]) r r->clj))


(deftest test240 (is (= var239 [nil])))


(def var241 (-> '(stat [100 33 22 44 55 nil] :na.rm true) r r->clj))


(deftest test242 (is (= var241 [50.8])))


(def var243 (md "#### Formulas"))


(def
 var244
 (md
  "To create an R [formula](https://www.datacamp.com/community/tutorials/r-formula-tutorial), use `tilde` or `formula` with two arguments, for the left and right sides (to skip one, just use `nil`)."))


(def var245 (r '(formula y x)))


(def var246 (r '(formula y (| (+ a b c d) e))))


(def var247 (r '(formula nil (| x y))))


(def var248 (md "#### Operators"))


(def var249 (->code '(+ 1 2 3 4 5)))


(def var250 (->code '(/ 1 2 3 4 5)))


(def var251 (->code '(- [1 2 3])))


(def var252 (->code '(<- a b c 123)))


(def var253 (->code '($ a b c d)))


(def var254 (md "#### Unquoting"))


(def
 var255
 (md
  "Sometimes we want to use objects created outside our form (defined earlier or in `let`). For this case you can use the `unqote` (`~`) symbol. There are two options:\n\n* when using quoting `'`, unqote evaluates the uquoted form using `eval`. `eval` has some constrains, the most important is that local bindings (`let` bindings) can't be use.\n* when using syntax quoting (backquote `), unqote acts as in clojure macros -- all unquoted forms are evaluated instantly."))


(def var256 (def v (r '(+ 1 2 3 4))))


(def var257 (-> '(* 22.0 ~v) r r->clj))


(deftest test258 (is (= var257 [220.0])))


(def
 var259
 (let
  [local-v (r '(+ 1 2 3 4)) local-list [4 5 6]]
  (->
   (clojure.core/sequence
    (clojure.core/seq
     (clojure.core/concat
      (clojure.core/list 'clojure.core/*)
      (clojure.core/list 22.0)
      (clojure.core/list local-v)
      local-list)))
   r
   r->clj)))


(deftest test260 (is (= var259 [26400.0])))


(def var261 (md "## Calling R functions"))


(def
 var262
 (md
  "You are not limited to the use code forms. When an RObject correspinds to an R function, it can be used and called as normal Clojure functions."))


(def var263 (def square (r '(function [x] (* x x)))))


(def var264 (-> 123 square r->clj))


(deftest test265 (is (= var264 [15129])))
