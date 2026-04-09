(ns clojisr.v1.r-test
  (:require [clojisr.v1.r :as r]
            [clojure.string :as str]
            [clojure.test :as t]
            [clojure.math :as m]
            [tablecloth.api :as tc]))

(r/require '[base] '[datasets])

(t/deftest defr
  (r/defr zzz [2 3 4 5 -1])
  (t/is (= zzz (-> (r/r "zzz") r/r->clj)))
  (t/is (= zzz (-> (r/r 'zzz) r/r->clj))))

(t/deftest operators
  (t/testing "Primitives"
    (t/is (= [-1] (-> (r/r- 1) r/r->clj)))
    (t/is (= [-2] (-> (r/r- 1 3) r/r->clj)))
    (t/is (= [1] (-> (r/r+ 1) r/r->clj)))
    (t/is (= [11] (-> (r/r+ 1 10) r/r->clj)))
    (t/is (= [11.5] (-> (r/r+ 1/2 11) r/r->clj)))
    (t/is (= [(+ 1 1/2 3.3 -7.1)] (-> (r/r+ 1 1/2 3.3 -7.1) r/r->clj)))
    (t/is (= [2] (-> (r/r* 2 1) r/r->clj)))
    (t/is (= [3.1] (-> (r/r* 1/2 6.2) r/r->clj)))
    (t/is (= [(* 2 1/2 3.5 -7.2)] (-> (r/r* 2 1/2 3.5 -7.2) r/r->clj)))
    (t/is (= [(/ 1.0 2.0)] (-> (r/rdiv 2.0) r/r->clj)))
    (t/is (= [(/ 3.0 4.0)] (-> (r/rdiv 3 4) r/r->clj)))
    (t/is (= [(/ 6.0 3.0)] (-> (r/rdiv 6 3) r/r->clj)))
    (t/is (= [(quot 3 4)] (-> (r/r%div% 3 4) r/r->clj)))
    (t/is (= [(quot 4 3)] (-> (r/r%div% 4 3) r/r->clj)))
    (t/is (= [(mod 3 4)] (-> (r/r%% 3 4) r/r->clj)))
    (t/is (= [(mod 4 3)] (-> (r/r%% 4 3) r/r->clj)))
    (t/is (= [(m/pow 2.4 4.4)] (-> (r/r** 2.4 4.4) r/r->clj))))
  (t/testing "Vectors" ;; no need to test all of them
    (t/is (= [1 2 3] (-> (r/r- [-1 -2 -3]) r/r->clj)))
    (t/is (= [-4.0 -4.0 -4.2] (-> (r/r- [-1 -2 -3] [3 2 1.2]) r/r->clj)))
    (t/is (= [1 2 3] (-> (r/r+ [1 2 3]) r/r->clj)))
    (t/is (= [2.0 0.0 -1.8] (-> (r/r+ [-1 -2 -3] [3 2 1.2]) r/r->clj)))
    (t/is (= [0 -1 -2] (-> (r/r+ [-1 -2 -3] 1) r/r->clj)))
    (t/is (= [0 -1 -2] (-> (r/r+ 1 [-1 -2 -3]) r/r->clj))))
  (t/testing "Colon"
    (t/is (= [1 2 3 4] (-> (r/colon 1 4) r/r->clj)))
    (t/is (= [1 2 3 4] (-> (r/rcolon 1 4) r/r->clj))))
  (t/testing "Formula" ;; currently REXPLanguage is a REXPList
    (t/is (= [(symbol "~") [0] [1]] (-> (r/tilde 0 1) r/r->clj)))
    (t/is (= [(symbol "~") '[* a b] '[+ a b]] (-> (r/tilde '(* a b) '(+ a b)) r/r->clj))))
  (t/testing "%in%"
    (t/is (-> (r/r%in% 1 [1 2 3]) r/r->clj first))
    (t/is (-> (r/r%in% -1 [1 2 3]) r/r->clj first not)))
  (t/testing "Products"
    (t/is (== 26.0 (-> (r/r%*% [1 2 3] [3 4 5]) r/r->clj (get-in [1 0]))))
    (t/is (== 15.0 (-> (r/r%o% [1 2 3] [3 4 5]) r/r->clj (get-in [3 2]))))
    (t/is (= [3.0 4.0 5.0 6.0 8.0 10.0 9.0 12.0 15.0] (-> (r/r%x% [1 2 3] [3 4 5]) r/r->clj (get 1)))))
  #_(t/testing "Null coalescing"
      (t/is (== (or nil 2.2) (-> (r/r%||% nil 2.2) r/r->clj first)))
      (t/is (== (or 2.2 nil) (-> (r/r%||% 2.2 nil) r/r->clj first))))
  (t/testing "Compare"
    (t/are [op res] (= res (-> (op [1 2 3] [3 2 1]) r/r->clj))
      r/r== [false true false]
      r/r!= [true false true]
      r/r< [true false false]
      r/r<= [true true false]
      r/r> [false false true]
      r/r>= [false true true]))
  (t/testing "Logical"
    (t/are [op res] (= res (-> (op [true true false false] [true false true false]) r/r->clj))
      r/r| [true true true false]
      r/r& [true false false false]
      r/rxor [false true true false])
    (t/is (= [true] (-> (r/r|| true false) r/r->clj)))
    (t/is (= [false] (-> (r/r&& true false) r/r->clj)))
    (t/is (= [true false] (-> (r/r! [false true]) r/r->clj)))))

;;

(t/deftest binaries
  (t/are [op res] (= res (-> (op 1 1) r/r->clj first))
    r/r== true
    r/r!= false
    r/r< false
    r/r> false
    r/r<= true
    r/r>= true
    r/r& true 
    r/r&& true
    r/r| true
    r/r|| true
    r/r- 0
    r/r+ 2
    r/r* 1
    r/rdiv 1.0
    r/colon 1
    r/rcolon 1
    r/r%div% 1
    r/r%% 0
    r/rxor false
    r/r%in% true))

;;

(t/deftest object-structure
  (t/is (= "'data.frame':\t32 obs. of  11 variables:\n $ mpg : num  21 21 22.8 21.4 18.7 18.1 14.3 24.4 22.8 19.2 ...\n $ cyl : num  6 6 4 6 8 6 8 4 4 6 ...\n $ disp: num  160 160 108 258 360 ...\n $ hp  : num  110 110 93 110 175 105 245 62 95 123 ...\n $ drat: num  3.9 3.9 3.85 3.08 3.15 2.76 3.21 3.69 3.92 3.92 ...\n $ wt  : num  2.62 2.88 2.32 3.21 3.44 ...\n $ qsec: num  16.5 17 18.6 19.4 17 ...\n $ vs  : num  0 0 1 1 0 1 0 1 1 1 ...\n $ am  : num  1 1 1 0 0 0 0 0 0 0 ...\n $ gear: num  4 4 4 3 3 3 3 4 4 4 ...\n $ carb: num  4 4 1 1 2 1 4 2 2 4 ..."
           (r/object-structure datasets/mtcars))))

;;

(t/deftest indexing
  (t/testing "$"
    (t/is (= (range 1947 1963) (-> (r/r$ datasets/longley 'Year) r/r->clj)))
    (t/is (= [11.1] (-> (r/r$ {:a 22 :b 11.1} 'b) r/r->clj)))
    (t/is (= [11.1] (-> (r/r$ [:!list :a 22 :b 11.1] 'b) r/r->clj))))
  (t/testing "["
    (t/is (= [21.0] (-> datasets/mtcars
                        (r/r$ "mpg")
                        (r/bra 1)
                        (r/r->clj))))
    (t/is (= [21.0 22.8 21.4] (-> datasets/mtcars
                                  (r/r$ "mpg")
                                  (r/bra (r/colon 2 4))
                                  (r/r->clj))))
    (t/is (= [21.0 22.8 21.4] (-> datasets/mtcars
                                  (r/r$ "mpg")
                                  (r/bra (r/colon 2 4))
                                  (r/r->clj)))))
  (t/testing "R examples for indexing (?\"[\")"
    (let [x (r/colon 1 12)
          m (base/matrix (r/colon 1 6) :nrow 2 :dimnames [:!list ["a" "b"] (r/bra 'LETTERS (r/colon 1 3))])
          li (r/r {:pi 'pi :e '(exp 1)})
          ci (base/cbind ["a" "b" "a"] ["A" "C" "B"])
          y (r/r [:!list 1 2 :a 4 5])]
      (t/is (= (range 1 13) (r/r->clj x)))
      (t/is (= [["a" "b"] [1 2] [3 4] [5 6]] (-> (r/r->clj m) tc/columns)))
      (t/is (= [10] (-> (r/bra x 10) r/r->clj)))
      (t/is (= (rest (range 1 13)) (-> (r/bra x -1) r/r->clj)))
      (t/is (= [1 3 5] (-> (r/bra m 1 nil) r/r->clj)))
      (t/is (= [["a" 1 3 5]] (-> (r/bra m 1 nil :drop false) r/r->clj tc/rows)))
      (t/is (= [["a" 1 5] ["b" 2 6]] (-> (r/bra m nil [true false true]) r/r->clj tc/rows)))
      (t/is (= [5 4 1] (-> (r/bra m (base/cbind [1 2 1] (r/colon 3 1))) r/r->clj)))
      (t/is (= [1 6 3] (-> (r/bra m ci) r/r->clj)))
      (t/is (= [["a" 3 5] ["b" 4 6]](-> (r/bra m nil -1) r/r->clj tc/rows)))
      (t/is (= [m/PI] (-> (r/brabra li 1) r/r->clj)))
      (t/is (= {:a [4] 2 [5]} (-> (r/bra y [3 4]) r/r->clj)))
      (t/is (= [4] (-> (r/r$ y "a") r/r->clj)))))
  (t/testing "comparing [ and [["
    (let [nx (r/r [:!list :Abc 123 :pi 'pi])]
      (t/is (= {:Abc [123]} (-> (r/bra nx 1) r/r->clj)))
      (t/is (= {:pi [m/PI]} (-> (r/bra nx "pi") r/r->clj)))
      (t/is (= [123] (-> (r/brabra nx 1) r/r->clj)))
      (t/is (= [m/PI] (-> (r/brabra nx "pi") r/r->clj)))))
  (t/testing "nested lists"
    (let [z (r/r (array-map :a (array-map :b 9 :c "hello") :d (r/colon 1 5)))]
      (t/is (= ["hello"] (-> (r/brabra z [1 2]) r/r->clj)))
      (t/is (= ["hello"] (-> (r/brabra z [1 2 1]) r/r->clj)))
      (t/is (= [9] (-> (r/brabra z ["a" "b"]) r/r->clj)))
      (t/is (= ["new"] (-> (r/brabra<- z ["a" "b"] "new") (r/brabra ["a" "b"]) r/r->clj)))))
  (t/testing "assignment"
    (t/is (= 8 (-> (r/brabra<- (base/matrix (r/colon 1 12)) 1 8)
                   (r/r->clj)
                   (tc/column 1)
                   (first))))
    (t/is (= 8 (-> (r/bra<- (range 5) 1 8)
                   (r/r->clj)
                   (first))))))


(t/deftest r-require
  (t/testing "graphics"
    (r/require '[graphics :refer [plot hist]])
    (t/is (-> (ns-interns *ns*) (get 'plot)))
    (t/is (-> (ns-interns *ns*) (get 'hist)))
    (t/is (some? (find-ns 'r.graphics)))
    (t/is (some? (-> (ns-aliases *ns*) (get 'graphics)))))
  (t/testing "stats"
    (r/require '[stats :refer [lm] :docstrings? true])
    (t/is (-> (ns-interns *ns*) (get 'lm)))
    (t/is (str/starts-with? (-> (ns-interns *ns*)
                                (get 'lm)
                                (meta)
                                (:doc))
                            "Fitting Linear Model"))))

;;

(t/deftest rsymbol
  (t/is (r/function? (r/rsymbol "utils" ".R_LIBS" true)))
  (t/is (not (r/function? (r/rsymbol "datasets" "iris"))))
  (t/is (r/r-object? (r/rsymbol "utils" ".R_LIBS" true)))
  (t/is (r/r-object? (r/rsymbol "datasets" "iris"))))

(t/deftest is-function
  (t/is (r/function? (r/r "function() {1;}")))
  (t/is (r/function? (r/r '(function [a] (+ 1 a)))))
  (t/is (not (r/function? (r/r "1+2"))))
  (t/is (not (r/function? (r/r '(+ 1 2))))))

(t/deftest load-data
  (r/data 'iris 'datasets)
  (t/is (= 5 (tc/column-count (r/r->clj iris)))))

;; ensure you have svglite installed
;; devcontainer fail
(t/deftest load-library
  (r/library 'svglite)
  (t/is (r/function? (r/r 'xmlSVG))))

