(ns clojuress.scratch
  (:require [clojuress.protocols :refer [eval->jvm]]
            [clojuress.session :as session]
            [clojuress.core :as r :refer [r]]
            [clojuress.rserve.jvm :as jvm]
            #_[clojuress.functions :as f]
            #_[clojuress.ggplot :as g]
            [clojuress.native.core :as native]
            [clojuress.protocols :as prot]
            [clojuress.packages.base :as base]
            [clojuress.packages.stats :as stats])
  (:import (org.rosuda.REngine REXP REXPSymbol REXPDouble REXPInteger)
           (org.rosuda.REngine.Rserve RConnection)))



(comment

  (r/init)

  ((r/function (r "mean"))
   [[1 2 3]] {})

  (-> 1000
      ((r/function (r "rnorm")) [] {:mean 5}))

  (base/mean
   [[1 2 3]] {})

  (stats/median
   [[1 2 3]] {})


  (def result1
    (-> "(0:10000000)^2"
        r
        time))

  (->> result1
       r/->jvm
       (.asDoubles)
       (take 9)
       (= (map #(double (* % %)) (range 9)))
       time)

  (->> result1
       r/->jvm
       r/jvm->
       r/->jvm
       (.asDoubles)
       (take 9)
       (= (map #(double (* % %)) (range 9)))
       time)

  (def result1
    (-> "data.frame(a=1:10000000, b=rnorm(10000000))"
        r
        time))

  (-> result1
      ((juxt r/class
             r/names
             r/shape))
      (= [["data.frame"]
          ["a" "b"]
          [10000000 2]]))

  (require '[clojuress.functions :as f])

  (->> (r "-5:5")
       f/abs)


  (->> (range -5 5)
       f/abs)

  (->> (r "-5:5")
       ((r/function (r "abs"))))

  (->> 10000000
       f/rnorm
       ((r/function (r "function(x) x*x")))
       f/mean)


  (r/apply-function (r "abs")
                    [(r "-5:5")]
                    (session/get {}))

  (->> [(r "-5:5")]
       (native/apply-function (r "abs")))

  (r "library(ggplot2)")



  (->> (r "1000")
       f/rnorm
       ((r/function (r "(function(x) print(qplot(x)))"))))

  (r "print(qplot(rnorm(1000)))")

  (r "dev.off()")


)





  (comment

    (r/init)

    (.parseAndEval
     (:r-connection (session/get {}))
     "l<-list()")

    (-> (.parseAndEval
         (:r-connection (session/get {}))
         "l")
        (.asList))

    (.parseAndEval
     (:r-connection (session/get {}))
     "l$a<-3")

    (-> (.parseAndEval
         (:r-connection (session/get {}))
         "l$a")
        (.asDouble))


    (def result1
      (time
       (r/eval "(1:10000000)^2")))

    (time
     (def jvm-result1
       (r/->jvm result1)))

    (-> result1
        r/->jvm
        r/jvm->
        r/->jvm
        (.asDoubles)
        (->> (take 9)))


    (setvar (session/get {})
            "x" (jvm/doubles [1 2 3]))


    (-> (getvar (session/get {})
                "x")
        (.asDoubles))

    (setvar (session/get {})
            "l" (eval->jvm (session/get {}) "list(a=1)"))

    (setvar (session/get {})
            "l$b" (jvm/doubles [4 5 6]))

    (-> (getvar (session/get {})
                "l$a")
        (.asDoubles))

    (-> (getvar (session/get {})
                "l$b")
        (.asDoubles))

    (r/init)



    (let [conn (-> {}
                   session/get
                   :r-connection)]
      (.parseAndEval conn "{.memory$abcd <- 1 ; 'ok'}")
      (-> (.parseAndEval conn ".memory$abcd")
          (.asDoubles)))


    (let [conn (-> {}
                   session/get
                   :r-connection)]
      (.eval conn
             (REXP/asCall "<-"
                          [(REXPSymbol. "abcd")
                           (REXPDouble. (double-array [1 2 3]))])
             nil
             false)
      #_(-> (.parseAndEval conn ".memory$abcd")
            (.asDoubles)))


    (-> (REXP/asCall "<-"
                     (REXPSymbol. "abcd")
                     (REXPDouble. (double-array [1 2 3]))))


    (let [conn (-> {}
                   session/get
                   :r-connection)]
      (.eval
       conn
       (jvm/call "<-"
                 {}
                 [(jvm/call "$"
                            {}
                            [(jvm/symbol ".memory")
                             (jvm/symbol "abcd")])
                  (jvm/doubles [1 2 3 4 5])])
       nil
       true))



    (let [conn (-> {}
                   session/get
                   :r-connection)]
      (-> (.eval
           conn
           ".memory$abcd")
          (.asDoubles)))



    (let [conn (-> {}
                   session/get
                   :r-connection)]
      (.eval
       conn
       (REXP/asCall
        "{"
        (into-array REXP
                    [(REXP/asCall "<-"
                                  (REXPSymbol. "X")
                                  (REXPDouble. (double-array [1 2 3])))
                     (REXP/asCall "::"
                                  (REXPSymbol. "base")
                                  (REXPSymbol. "length")
                                  (REXPSymbol. "X"))]))
       nil
       false))













    )
