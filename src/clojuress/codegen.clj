(ns clojuress.codegen
  (:require [clojuress.objects-memory :as mem]
            [clojuress.using-sessions :as using-sessions]
            [clojure.string :as string]
            [clojuress.protocols :as prot]
            [clojuress.util :as util :refer [l]]))

(defn r-object->code [r-object]
  (-> r-object
      :object-name
      mem/object-name->memory-place))

(comment
  (let [s (clojuress.using-sessions/fetch-or-make-and-init {})]
     (using-sessions/init-session-memory s)
     [(-> (clojuress.robject/->RObject "dummy" s "dummycode")
          r-object->code)
     (-> "3"
         (using-sessions/eval-r s)
         r-object->code
         (using-sessions/eval-r s))]))

(defn value->code [value session]
  (-> value
      (->> (prot/clj->java session))
      (using-sessions/java->r session)
      r-object->code))

(comment
  (let [s (clojuress.using-sessions/fetch-or-make-and-init {})]
    (using-sessions/init-session-memory s)
    (->> [(-> "3"
              (using-sessions/eval-r s))
          (org.rosuda.REngine.REXPDouble. (double-array [3]))
          3.0]
         (map (fn [v]
                (let [code (value->code v s)]
                  {:code code
                   :result (-> code
                               (using-sessions/eval-r s))}))))))

(defn r-function->code [r-function]
  (if (symbol? r-function)
    (name r-function)
    (r-object->code r-function)))

(comment
  [(r-function->code 'sin)
   (let [s (clojuress.using-sessions/fetch-or-make-and-init {})]
     (using-sessions/init-session-memory s)
     (-> "sin"
         (using-sessions/eval-r s)
         r-function->code))])

(declare form->code)

(def binary-operators
  '#{+ - / * & && | || == != <= >= < >})

(defn ->function-def-code [[_ [& arg-symbols] & body] session]
  (format
   "function(%s) {%s}"
   (->> arg-symbols
        (map name)
        (string/join ", "))
   (->> body
        (map #(form->code % session))
        (string/join "; "))))

(comment
  (let [s (clojuress.using-sessions/fetch-or-make-and-init {})
        _ (using-sessions/init-session-memory s)
        sin (-> "sin"
                (using-sessions/eval-r s))]
     [(-> '[function [x y] (+ x y)]
          (->function-def-code s))
      (-> '[function [x y] (sin x)]
          (->function-def-code s))
      (-> ['function ['x 'y] `(~sin ~'x)]
          (->function-def-code s))]))

(defn ->formula-code
  [[_ lhs rhs] session]
  (->> [lhs rhs]
       (map #(form->code % session))
       (apply format "%s ~ %s")))

(comment
  (let [s (clojuress.using-sessions/fetch-or-make-and-init {})
        _ (using-sessions/init-session-memory s)]
    (-> ['tilde 'x 'y]
        (->formula-code s))))

(defn ->binary-funcall-code [[op-symbol & args] session]
  (->> args
       (map #(form->code % session))
       (interleave (repeat (name op-symbol)))
       rest
       (string/join " ")))

(comment
  (let [s (clojuress.using-sessions/fetch-or-make-and-init {})
        _ (using-sessions/init-session-memory s)
        x  (-> "3"
               (using-sessions/eval-r s))]
    [(-> ['+ 'x 'y 'z]
         (->binary-funcall-code s))
     (-> ['+ x 'y 'z]
         (->binary-funcall-code s))]))

(declare args->code)

(defn ->usual-funcall-code [[r-function & args] session]
  (format
   "%s(%s)"
   (r-function->code r-function)
   (args->code args session)))

(comment
  (let [s (clojuress.using-sessions/fetch-or-make-and-init {})
        _ (using-sessions/init-session-memory s)
        x (-> "3"
              (using-sessions/eval-r s))
        sin (-> "sin"
                (using-sessions/eval-r s))]
    [(-> ['sin 'x]
         (->usual-funcall-code s))
     (-> [sin 'x]
         (->usual-funcall-code s))
     (-> ['sin x]
         (->usual-funcall-code s))
     (-> [sin x]
         (->usual-funcall-code s))]))

(defn ->funcall-code
  [[r-function & args] session]
  (if (binary-operators r-function)
    (->binary-funcall-code (cons r-function args) session)
    (->usual-funcall-code (cons r-function args) session)))


(comment
  (let [s   (clojuress.session/fetch-or-make {})
        _   (using-sessions/init-session-memory s)
        x   (-> "3"
                (using-sessions/eval-r s))
        y   (-> "4"
                (using-sessions/eval-r s))
        sin (-> "sin"
                (using-sessions/eval-r s))]
    [(-> ['sin 'x]
         (->funcall-code s))
     (-> [sin 'x]
         (->funcall-code s))
     (-> ['sin x]
         (->funcall-code s))
     (-> [sin x]
         (->funcall-code s))
     (-> ['+ 'x 'y]
         (->funcall-code s))
     (-> ['+ x 'y]
         (->funcall-code s))
     (-> ['+ 'x y]
         (->funcall-code s))
     (-> ['+ x y]
         (->funcall-code s))]))

(defn seq-form->code [form session]
  (cond
    ;; a function declaration
    (-> form first (= 'function))
    (->function-def-code form session)
    ;; a lhs~rhs formula
    (-> form first (= 'tilde))
    (->formula-code form session)
    ;; else -- a function call
    :else
    (->funcall-code form session)))

(comment
  (let [s   (clojuress.session/fetch-or-make {})
        _   (using-sessions/init-session-memory s)
        x   (-> "3"
                (using-sessions/eval-r s))
        y   (-> "4"
                (using-sessions/eval-r s))
        sin (-> "sin"
                (using-sessions/eval-r s))]
    (->> ['[function [x y] (+ x y)]
          '[function [x y] (sin x)]
          ['function ['x 'y] `(~sin ~'x)]
          ['tilde 'x 'y]
          ['sin 'x]
          [sin 'x]
          ['sin x]
          [sin x]
          ['+ 'x 'y]
          ['+ x 'y]
          ['+ 'x y]
          ['+ x y]]
         (map #(seq-form->code % s)))))

(defn form->code [form session]
  (cond (seq? form)    (seq-form->code form session)
        (symbol? form) (name form)
        :else          (value->code form session)))

(comment
  (let [s   (clojuress.session/fetch-or-make {})
        _   (using-sessions/init-session-memory s)
        x   (-> "3"
                (using-sessions/eval-r s))
        y   (-> "4"
                (using-sessions/eval-r s))
        sin (-> "sin"
                (using-sessions/eval-r s))]
    (->> ['(function [x y] (+ x y))
          '(function [x y] (sin x))
          (l 'function ['x 'y] (l sin 'x))
          '(tilde x y)
          (l 'sin 'x)
          (l sin 'x)
          (l 'sin x)
          (l sin x)
          (l '+ 'x 'y)
          (l '+ x 'y)
          (l '+ 'x y)
          (l '+ x y)]
         (map #(form->code % s)))))

(defn arg->arg-name-and-value [arg]
  (if (util/starts-with? arg :=)
    (rest arg)
    [nil arg]))

(comment
  (->> [4
        [:= :a 4]]
       (mapv arg->arg-name-and-value)))

(defn arg-name-and-value->code [[arg-name value] session]
  (str (when arg-name
         (str (name arg-name) " = "))
       (form->code value session)))

(comment
  (let [s   (clojuress.session/fetch-or-make {})
        _   (using-sessions/init-session-memory s)]
    (->> ['(+ x y)
          4
          [:= :a '(+ x y)]
          [:= :a 4]]
         (mapv #(-> %
                    arg->arg-name-and-value
                    (arg-name-and-value->code s))))))

(defn mark-named-args [args]
  (loop [already-marked []
         remaining-args args]
    (if (empty? remaining-args)
      already-marked
      (if (keyword? (first remaining-args))
        (recur (conj already-marked [:= (first remaining-args) (second remaining-args)])
               (rest (rest remaining-args)))
        (recur (conj already-marked (first remaining-args))
               (rest remaining-args))))))

(comment
  (mark-named-args ['(+ x y)
                    [:= :a '(+ x y)]
                    :b '(+ x y)
                   4
                   [:= :c 4]
                   :d 4]))

(defn args->code [args session]
  (->> args
       mark-named-args
       (map (fn [arg]
              (-> arg
                  arg->arg-name-and-value
                  (arg-name-and-value->code session))))
       (string/join ", ")))

(comment
  (let [s   (clojuress.session/fetch-or-make {})
         _   (using-sessions/init-session-memory s)]
    (-> ['(+ x y)
         [:= :a '(+ x y)]
         :b '(+ x y)
         4
         [:= :c 4]
         :d 4]
        (args->code s))))
