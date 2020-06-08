(ns clojisr.v1.astgen
  (:require [clojisr.v1.using-sessions :as using-sessions]
            [clojisr.v1.protocols :as prot]
            [clojisr.v1.robject]
            [clojisr.v1.util :refer [bracket-data]])
  (:import [clojure.lang Named]
           [clojisr.v1.robject RObject]))

;; helpers

;; Convert instant to date/time R string
(defonce ^:private dt-format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss"))

;; Add context to a call, used to change formatting behaviour in certain cases
(defmacro ^:private with-ctx
  [c & r]
  `(let [~'ctx (conj (or ~'ctx #{}) ~@c)] ~@r))

;; Leave nil untouched when coercing seq
(defn ^:private map-with-nil
  [f xs]
  (map #(some-> % f) xs))

;; all binary operators as set of strings
(def binary-operators #{"$" "=" "<<-" "<-" "+" "-" "/" "*" "&" "&&" "|" "||" "==" "!=" "<=" ">=" "<" ">" "%in%" "%%" "**"})
(def binary-operators-flat #{"=" "<<-" "<-" "$"})
(def unary-operators #{"+" "-"})
(def wrapped-operators #{"+" "-" "/" "*" "&" "&&" "|" "||" "==" "!=" "<=" ">=" "<" ">"})

(defn ->r-object-ast [r-object]
  "Creat the AST of an R object."
  [:ast/r-object (:object-name r-object)])

(defn form->r->ast
  "Given Clojure data, create the AST of the corresponding R object.

   Used for forms which are big Clojure data structures."
  [value session]
  (-> value
      (->> (prot/clj->java session))
      (using-sessions/java->r session)
      ->r-object-ast))

(declare form->ast)

(defn named-or-anything->string
  "Convert any named or any other object to a string."
  [s]
  (if (instance? Named s)
    (name s)
    (str s)))

(defn ->ast-args
  "Given a seq of arguments, create a sequence of AST nodes for these (partially named) arguments.

  Arguments preceeded by keywords are considered named.

  Used to generate function arguments or when coercing vector to a list."
  ([args session ctx] (->ast-args args session ctx nil))
  ([args session ctx bra?]
   (->> (loop [res []
               [fa & ra :as all] args]
          (if (seq all)
            (if (keyword? fa)
              (recur (conj res [:ast/named-arg
                                (name fa)
                                (form->ast (first ra) session ctx)])
                     (rest ra))
              (recur (conj res (if (and bra? (nil? fa))
                                 [:ast/empty-arg]
                                 (form->ast fa session ctx)))
                     ra))
            res)))))

(defn ->function-call-ast
  "Create an AST of an R function call."
  [fname args session ctx mode]
  [:ast/funcall fname
   (case mode
     :plain args
     :recurse-args (map #(form->ast % session ctx) args)
     :prepare-ast-args (->ast-args args session ctx))])

(defn ->parens-ast
  "Create an AST that wraps its contents parentheses
  (may matter in some code generation situations)."
  [sub-ast]
  [:ast/parens sub-ast])

(defn binary-or-unary-call->ast
  "Create the AST of a binary or unary operator function call."
  [f [f1 & fr] session ctx]
  (let [maybe-wrap (if (:flat ctx)
                     identity
                     ->parens-ast)
        res (if-not f1
              (throw (Exception. "Positive number of arguments is required."))
              (let [f1-ast (form->ast f1 session ctx)]
                (if-not fr
                  (if (unary-operators f)
                    [:ast/unary-funcall f f1-ast]
                    f1-ast)
                  (reduce (fn [a1 a2] (maybe-wrap
                                       [:ast/binary-funcall
                                        f
                                        a1
                                        (form->ast a2 session ctx)]))
                          f1-ast fr))))]
    (if (and (wrapped-operators f)
             (not (ctx :unwrap))
             (ctx :flat))
      (->parens-ast res)
      res)))

(defn formula->ast
  "Create a formula AST.

  A pair is expected, f the two sides of the formula (left and right).
  If you want to skip given side, use `nil`.

  Formulas are treated as a binary call and are formatted without parentheses
  (context = `:flat`)."
  [[lf rf] session ctx]
  (with-ctx [:unwrap :flat]
    [:ast/formula
     (some-> lf (form->ast session ctx))
     (some-> rf (form->ast session ctx))]))

(defn symbol-form->ast
  "Create the AST of a binary, unary or regular function call.

  Used for forms that are seqs where the first argument is a symbol."
  [f args session ctx]
  (if (binary-operators f)
    (if (binary-operators-flat f)
      (with-ctx [:flat] (binary-or-unary-call->ast f args session ctx))
      (binary-or-unary-call->ast f args session ctx))
    (->function-call-ast f args session ctx :prepare-ast-args)))

(defn ->block-ast
  "Create the AST of a block of possibly several expressions."
  [expression-asts]
  [:ast/block expression-asts])

(defn ->function-def-ast
  "Create the AST of an R function definition.

  Arguments can be a partially named list."
  [args body session ctx]
  [:ast/function-def
   (->ast-args args session ctx)
   (->block-ast (map #(form->ast % session ctx) body))])

(defn ->if-else-ast
  "Create the AST of an if or an if-else form."
  [vs session ctx]
  [:ast/if-else
   (map #(form->ast % session ctx) (take 3 vs))])

(defn ->for-loop-ast
  "Create the AST of a for-loop."
  [bindings body session ctx]
  (if (seq bindings)
    (let [[v s & r] bindings]
      [:ast/for-loop
       (name v)
       (form->ast s session ctx)
       (->for-loop-ast r body session ctx)])
    (->block-ast
     (map #(form->ast % session ctx) body))))

(defn ->while-loop-ast
  "Create the AST of a while-loop."
  [pred body session ctx]
  [:ast/while-loop
   (form->ast pred session ctx)
   (->block-ast
    (map #(form->ast % session ctx) body))])

(defn ->colon-ast
  "Create the AST of an R colon (:) expression."
  [[a b] session ctx]
  [:ast/colon
   (form->ast a session ctx)
   (form->ast b session ctx)])

(defn ->bacticks-ast
  "Create the AST of a symbol wrapped with backticks."
  [s]
  [:ast/backtick s])

(defn maybe-wrap-with-backticks
  [string-or-symbol]
  (if (symbol? string-or-symbol)
    (name string-or-symbol)
    (->bacticks-ast (name string-or-symbol))))

(defn ->rsymbol-ast
  "Create the AST of a qualified or regular symbol.

  A regular symbol has one part. A qualified symbol has two"
  ([rsymbol-parts session ctx]
   (let [[a b] (->> rsymbol-parts
                    (map (fn [part]
                           (maybe-wrap-with-backticks
                            (if (or (symbol? part) (string? part))
                                 part
                                 (form->ast part session ctx))))))]
     (if b
       [:ast/qualified-symbol a b]
       [:ast/regular-symbol a]))))

(defn ->bracket-call-ast
  "Create the AST of a bracket-call [...]."
  [[bra all?] args session ctx]
  (let [args (if (and (not all?)
                      (nil? (last args)))
               (conj (vec (butlast args)) 'NULL)
               args)]
    [:ast/funcall
     bra
     (->ast-args args session ctx true)]))

(defn ->unquote-form-ast
  "Eval unquoted form.

  Used when unquote symbol is part of regular quoted form.

  Warning: You can't unquote local bindings! For this case use inside syntax quote."
  [u-form session ctx]
  (-> (apply eval u-form)
      (form->ast session ctx)))

(declare seq-form->ast)

(defn vector->ast
  "Construct the AST corresponding to an R vector (using the `c` function.)

  When the first element is a coersion symbol starting with `:!`, values are coerced to the required type.
  When the number of elements is big enough, the data is converted to an R object AST.

  `nil` is converted to `NA`"
  [[f & r :as v-form] session ctx]
  (with-ctx [:na]
    (case f
      :!string (vector->ast (map-with-nil named-or-anything->string r) session ctx)
      :!boolean (vector->ast (map-with-nil #(if % true false) r) session ctx)
      :!int (vector->ast (map-with-nil unchecked-int r) session ctx)
      :!double (vector->ast (map-with-nil unchecked-double r) session ctx)
      :!named (->function-call-ast 'c r session ctx :prepare-ast-args)
      :!list (->function-call-ast 'list r session ctx :prepare-ast-args)
      :!factor (->function-call-ast
                'factor
                [(vector->ast r session ctx)]
                session ctx :plain)
      :!ct (->function-call-ast
            'as.POSIXct
            [(vector->ast r session ctx)]
            session ctx :plain)
      :!lt (->function-call-ast
            'as.POSIXlt
            [(vector->ast r session ctx)]
            session ctx :plain)
      :!call (seq-form->ast r session ctx)
      (if (< (count v-form) 80)
        (->function-call-ast
         'c v-form session ctx :recurse-args)
        (form->r->ast v-form session)))))

(defn seq-form->ast
  "Process a sequence.

  Possible paths are possible if given first element is:

  * one of the symbols: `function` or `formula` - create fn definition or formula
  * ~ (unquote) - eval rest of the form
  * RObject which is a function - create function call
  * sequence - create function call with processed first element.
  * any symbol - function call
  * one of the functions with special names: [,[[,[<-,[[<-,:
  * any other value - construct vector"
  [[f & r :as seq-form] session ctx]
  (if (symbol? f)
    (let [fs (name f)]
      (cond
        (= "colon" fs) (->colon-ast r session ctx)
        (= "function" fs) (->function-def-ast (first r) (rest r) session ctx)
        (or (= "tilde" fs)
            (= "formula" fs)) (formula->ast r session ctx)
        (= "rsymbol" fs) (->rsymbol-ast r session ctx)
        (= "if" fs) (->if-else-ast r session ctx)
        (= "do" fs) (->block-ast (map #(form->ast % session ctx) r))
        (= "for" fs) (->for-loop-ast (first r) (rest r) session ctx)
        (= "while" fs) (->while-loop-ast (first r) (rest r) session ctx)
        (contains? bracket-data fs) (->bracket-call-ast (bracket-data fs) r session ctx)
        (= 'clojure.core/unquote f) (->unquote-form-ast r session ctx)
        :else (symbol-form->ast fs r session ctx)))
    (let [->fcast #(->function-call-ast % r session ctx :prepare-ast-args)]
      (cond
        (using-sessions/function? f) (->fcast
                                      (:object-name f))
        (string? f)                  (if (and (= (first f) \`)
                                              (= (last f) \`))
                                       (->fcast f)
                                       (vector->ast seq-form session ctx))
        (sequential? f)              (->fcast (seq-form->ast f session ctx))
        :else                        (vector->ast seq-form session ctx)))))

(defn map->ast
  "Convert a map to an AST for generatign a named-list.

  For big maps, the data is converted to an R object AST.

  `nil` is treated as `NA`"
  [form session ctx]
  (if (< (count form) 50)
    (with-ctx [:na]
      [:ast/funcall
       'list
       (map (fn [[k v]]
              [:ast/named-arg
               (form->ast k session ctx)
               (form->ast v session ctx)])
            form)])
    (form->r->ast form session)))

(defn nil->ast
  "Convert `nil` to `NA` or `NULL` (based on context)"
  [ctx]
  (cond
    (ctx :nil) nil
    (ctx :na) "NA"
    :else "NULL"))

(defn form->ast
  "Format every possible form to an AST."
  ([form session] (form->ast form session #{}))
  ([form session ctx]
   (cond
     (vector? form) (vector->ast form session ctx) ;; vector always is converted to datatype
     (sequential? form) (seq-form->ast form session ctx) ;; sequence is usually call
     (instance? RObject form) (->r-object-ast form)
     (map? form) (map->ast form session ctx) ;; map goes to a list
     (string? form) [:ast/string form]
     (integer? form) form ;; int is treated literally
     (rational? form) [:ast/parens form] ;; rational is wrapped in in case of used in calculations
     (number? form) form ;; other numbers are treated literally
     (boolean? form) [:str/boolean form]
     (nil? form) (nil->ast ctx)
     (inst? form) [:ast/datetime form]
     (instance? Named form) (name form)
     :else (form->r->ast form session))))


