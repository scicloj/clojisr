(ns clojisr.v1.r-test
  (:require
   [clojisr.v1.r :as r]
   [clojisr.v1.require :as require-r]
   [clojure.string :as str]
   [clojure.test :refer [deftest is] :as t]
   [tech.v3.dataset :as ds]))

(require-r/require-r '[datasets])
(require-r/require-r '[base])

(def v [1 2 3])


(deftest bras
  (is (= [1]
         (-> (r/bra v 1) r/r->clj)))
  (is (= [1]
         (-> (r/brabra v 1) r/r->clj))))


(deftest binaries
  (is (= [true false false false true true true true true true 0 2 1 1.0 1 1 1 0 false true]

         (mapv
          (fn [f]
            (first (r/r->clj (f 1 1))))

          [r/r==
           r/r!=
           r/r<
           r/r>
           r/r<=
           r/r>=
           r/r&
           r/r&&
           r/r|
           r/r||
           r/r-
           r/r+
           r/r*
           r/rdiv
           r/colon
           r/rcolon
           r/r%div%
           r/r%%
           r/rxor
           r/r%in%]))))


(deftest unary

  (is (= 9.0 (-> (r/r** 3 2) r/r->clj first)))
  (is (not
       (-> (r/r! true) r/r->clj first))))



(deftest bra-colon
  
  (is  (= [21.0]
          (-> r.datasets/mtcars
              (r/r$  "mpg")
              (r/brabra 1)
              (r/r->clj))))
  (is  (= [21.0 22.8 21.4]
          (-> r.datasets/mtcars
              (r/r$  "mpg")
              (r/bra (r/colon 2 4))
              (r/r->clj)))))

(deftest str-md

  (r/println-captured-str r.datasets/mtcars)
  (is (=
       "```\n'data.frame':\t32 obs. of  11 variables:\n $ mpg : num  21 21 22.8 21.4 18.7 18.1 14.3 24.4 22.8 19.2 ...\n $ cyl : num  6 6 4 6 8 6 8 4 4 6 ...\n $ disp: num  160 160 108 258 360 ...\n $ hp  : num  110 110 93 110 175 105 245 62 95 123 ...\n $ drat: num  3.9 3.9 3.85 3.08 3.15 2.76 3.21 3.69 3.92 3.92 ...\n $ wt  : num  2.62 2.88 2.32 3.21 3.44 ...\n $ qsec: num  16.5 17 18.6 19.4 17 ...\n $ vs  : num  0 0 1 1 0 1 0 1 1 1 ...\n $ am  : num  1 1 1 0 0 0 0 0 0 0 ...\n $ gear: num  4 4 4 3 3 3 3 4 4 4 ...\n $ carb: num  4 4 1 1 2 1 4 2 2 4 ...\n```"
       (r/str-md r.datasets/mtcars))))



(deftest brabra<-
  
  (is (= 8
         (->
          (r/brabra<-
           (base/matrix (r/colon 1 12))
           1 8)
          r/r->clj
          (ds/column 1)
          first))))

(deftest bra<-
  (is (= 8
         (->
          (r/bra<-
           (range 5)
           1 8)
          r/r->clj
          first))))



(deftest r-require
  (r/require-r '[graphics :refer [plot hist] :generate-doc-strings? true])

  (is (= "#'user/plot"
         (->
          (ns-interns *ns*)
          (get 'plot)
          str)))
  (is (=  "#'user/hist"
          (->
           (ns-interns *ns*)
           (get 'hist)
           str)))
  (r/require-r '[graphics])
  (is (some?
       (find-ns 'r.graphics)))
  (is (some?
       (->
        (ns-aliases *ns*)
        (get 'graphics))))

  (r/require-r '[stats :refer [lm] :generate-doc-strings? true])
  (is (=  "#'user/lm"
          (->
           (ns-interns *ns*)
           (get 'lm)
           str)))

  (is
   (str/starts-with?

    (->
     (ns-interns *ns*)
     (get 'lm)
     meta
     :doc)
    "Fitting Linear Model")))

