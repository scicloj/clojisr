(ns generateme
  (:require [clojuress.v1.r :as r :refer [r r->clj clj->r clj->java java->clj java->r r->java]]
            [clojuress.v1.util :as u]
            [clojuress.v1.require :refer [require-r]]
            [tech.ml.dataset :as d]))

#_(r/discard-all-sessions)

(require-r '[lattice :as lat]
           '[latticeExtra :as late]
           '[stats :as stats]
           '[mlmRev]
           '[MEMSS]
           '[grDevices :as dev]
           '[graphics :as g]
           '[base :refer [$ nrow ncol head data summary] :as base]
           '[utils])

(require-r '[datasets :refer :all])

;; Some common enhacements

(defmacro formula [& frm] `(r ~(apply str frm)))

(defmacro def-r [name & r]
  `(do
     (def ~name ~@r)
     (base/<- '~(symbol name) ~name)))

;; FORMALS
(def ^:private empty-symbol (symbol ""))

(defn formals
  [{:keys [code class]}]
  (when (= class ["function"])
    (let [args (-> (format "formals(%s)" code)
                   r
                   r->clj)
          {:keys [obl opt]} (reduce (fn [m [k v]]
                                      (let [selector (if (and (= empty-symbol v)
                                                              (not (seq (:obl m)))) :obl :opt)]
                                        (update m selector conj (symbol k))))
                                    {:obl [] :opt []}
                                    args)]
      (cond
        (and (seq obl)
             (seq opt)) (list (conj obl '& {:keys opt}))
        (seq obl) (list obl)
        (seq opt) (list ['& {:keys opt}])
        :else '([])))))

(formals base/mean)
;; => ([x & {:keys [...]}])
(formals stats/arima0) 
;; => ([x & {:keys [order seasonal xreg include.mean delta transform.pars fixed init method n.cond optim.control]}])
(formals dev/dev-off)
;; => ([& {:keys [which]}])
(formals base/Sys-info)
;; => ([])

(alter-meta! #'stats/arima0 assoc :arglists (formals stats/arima0))

(meta #'stats/arima0)
;; => {:asdf 12, :name arima0, :ns #namespace[stats], :arglists ([x & {:keys [order seasonal xreg include.mean delta transform.pars fixed init method n.cond optim.control]}])}

;;;; LATTICE

;; http://lmdvr.r-forge.r-project.org/figures/figures.html

(dev/x11)

;; Chapter 1

(def chem97 r.mlmRev/Chem97)

(def gcse-formula (formula "~" gcsescore | factor (score)))
;; => #'generateme/gcse-formula

(r->clj (stats/xtabs (r "~ score") :data chem97))
;; => [3688 3627 4619 5739 6668 6681]

;; Figure 1

(-> gcse-formula
    (lat/histogram :data chem97))

;; Figure 2

(-> gcse-formula
    (lat/densityplot :data chem97 :plot.points false :ref true))

;; Figure 3

(-> (formula "~" gcsescore)
    (lat/densityplot :data chem97 :groups 'score :plot.points false
                     :ref true :auto.key {:columns 3}))

;; Figure 4

(def tp1 (-> gcse-formula
             (lat/histogram :data chem97)))

(def tp2 (-> (formula "~" gcsescore)
             (lat/densityplot :data chem97 :groups 'score :plot.points false
                              :auto.key {:space "right" :title "score"})))

(base/class tp2)
;; => [1] "trellis"

(r->clj (base/summary tp1))
;; => {:call [histogram [$ .MEM x3a15bdf5333946c7] [$ .MEM xd0fe9b187d4e47eb]], :packet.sizes [3688.0 3627.0 4619.0 5739.0 6668.0 6681.0], :index.cond [[1 2 3 4 5 6]], :perm.cond [1]}

(do
  (g/plot tp1 :split [1 1 1 2])
  (g/plot tp2 :split [1 2 1 2] :newpage false))

;; Chapter 2

(def oats r.MEMSS/Oats)

;; Figure 1

(def tp1-oats (-> (formula yield "~" nitro | Variety + Block)
                  (lat/xyplot :data oats :type "o")))

(base/print tp1-oats)

(base/dim tp1-oats)
;; => [1] 3 6

(r->clj (base/dimnames tp1-oats))
;; => {:Variety ["Golden Rain" "Marvellous" "Victory"], :Block ["I" "II" "III" "IV" "V" "VI"]}

(stats/xtabs (formula "~" Variety + Block) :data oats)
;; =>              Block
;; Variety       I II III IV V VI
;; Golden Rain 4  4   4  4 4  4
;; Marvellous  4  4   4  4 4  4
;; Victory     4  4   4  4 4  4

(r->clj (base/summary tp1-oats))
;; => {:call [xyplot [$ .MEM x5ec2e4baf8f64509] [$ .MEM xfb4983d6df664a61] [$ .MEM x4bd9f56355754bec]], :packet.sizes [4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0], :index.cond [[1 2 3] [1 2 3 4 5 6]], :perm.cond [1 2]}

(r->clj (base/summary (r/bra tp1-oats (r/empty-symbol) 1)))
;; => {:call [xyplot [$ .MEM x5ec2e4baf8f64509] [$ .MEM xfb4983d6df664a61] [$ .MEM x4bd9f56355754bec] new.levs], :packet.sizes [4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0 4.0], :index.cond [[1 2 3] [1]], :perm.cond [1 2]}

;; Figure 2

(base/print (r/bra tp1-oats (r/empty-symbol) 1))

;; Figure 3

(stats/update tp1-oats :aspect "xy")

;; Figure 4

(stats/update tp1-oats :aspect "xy" :layout [0 18])

;; Figure 5

(stats/update tp1-oats :aspect "xy" :layout [0 18] :between {:x [0 0 0.5] :y 0.5})

;; Figure 6

(-> (formula variety "~" yield | site)
    (lat/dotplot lat/barley :layout [1 6] :aspect 0.7 :groups 'year :auto.key {:space "right"}))

;; Figure 7

(def key-variety
  {:space "right"
   :text (r->clj (base/list (base/levels ($ oats 'Variety))))
   :points {:pch (range 1 3) :col "black"}})

(-> (formula yield "~" nitro | Block)
    (lat/xyplot oats :aspect "xy" :type "o" :groups 'Variety
                :key key-variety :lty 1 :pch (range 1 3)
                :col.line "darkgrey" :col.symbol "black"
                :xlab "Nitrogen concentration (cwt/acre)"
                :ylab "Yield (bushels/acre)"
                :main "Yield of three varieties of oats"
                :sub "A 3 x 4 split plot experiment with 6 blocks"))

;; Figure 8

(-> (formula Class "~" Freq | Sex + Age)
    (lat/barchart :data (base/as-data-frame Titanic)
                  :groups 'Survived :stack true :layout [4 1]
                  :auto.key {:title "Survived" :columns 2}))


;; Figure 9

(-> (formula Class "~" Freq | Sex + Age)
    (lat/barchart :data (base/as-data-frame Titanic)
                  :groups 'Survived :stack true :layout [4 1]
                  :auto.key {:title "Survived" :columns 2}
                  :scales {:x "free"}))

;; Figure 10

(def bc-titanic (-> (formula Class "~" Freq | Sex + Age)
                    (lat/barchart :data (base/as-data-frame Titanic)
                                  :groups 'Survived :stack true :layout [4 1]
                                  :auto.key {:title "Survived" :columns 2}
                                  :scales {:x "free"})))

(stats/update bc-titanic :panel (r "function(...) {panel.grid(h=0,v=-1);panel.barchart(...);}"))

;; Figure 11

(stats/update bc-titanic :panel (r "function(..., border) {panel.barchart(..., border='transparent');}"))

;; Chapter 3

;; Figure 1

(-> (formula "~ eruptions")
    (lat/densityplot :data faithful))

;; Figure 2

(-> (formula "~ eruptions")
    (lat/densityplot :data faithful :kernel "rect" :bw 0.2 :plot.points "rug" :n 200))

;; Figure 3

(-> (formula "~" log (FSC.H) | Days)
    (lat/densityplot :data late/gvhd10 :plot.points false :ref true :layout [2 4]))

;; Figure 4

(-> (formula "~" log2 (FSC.H) | Days)
    (lat/histogram :data late/gvhd10 :xlab "log Forward Scatter"
                   :type "density" :nint 50 :layout [2 4]))


;; Figure 5

(-> (formula "~" gcsescore | factor (score))
    (lat/qqmath :data chem97 :f.value (stats/ppoints 100)))

;; Figure 6

(-> (formula "~" gcsescore | gender)
    (lat/qqmath chem97 :groups 'score :aspect "xy" :f.value (stats/ppoints 100)
                :auto.key {:space "right"}
                :xlab "Standard Normal Quantiles", 
                :ylab "Average GCSE Score"))

;; Figure 7

(defmacro rsymbol [s] `(symbol ~(format "`%s`" (name s))))

(def chem97-mod (base/transform chem97 :gcsescore.trans [(rsymbol "^") 'gcsescore 2.34]))

(-> (formula "~" gcsescore.trans | gender)
    (lat/qqmath chem97-mod :groups 'score :aspect "xy" :f.value (stats/ppoints 100)
                :auto.key {:space "right" :title "score"}
                :xlab "Standard Normal Quantiles", 
                :ylab "Transformed GCSE Score"))

;; Figure 8

(-> (formula "~" gcsescore | factor (score))
    (late/ecdfplot :data chem97 :groups 'gender :auto.key {:columns 2}
                   :subset '(> gcsescore 0)
                   :xlab "Average GCSE Score"))

;; Figure 9

(-> (formula "~" gcsescore | factor (score))
    (lat/qqmath :data chem97 :groups 'gender :auto.key {:columns 2 :points false :lines true}
                :type "l" :distribution 'qunif
                :prepanel 'prepanel.qqmathline :aspect "xy"
                :subset '(> gcsescore 0)
                :xlab "Standard Normal Quantiles", 
                :ylab "Average GCSE Score"))

;; Figure 10

(-> (formula gender "~" gcsescore | factor (score))
    (lat/qq :data chem97 :f.value (stats/ppoints 100) :aspect 1))

;; Figure 11

(-> (formula factor (score) "~" gcsescore | gender)
    (lat/bwplot :data chem97 :xlab "Average GCSE Score"))

;; Figure 12

(-> (formula gcsescore "^" 2.34 "~" gender | factor(score))
    (lat/bwplot :data chem97 :varwidth true :layout [6 1]
                :xlab "Average GCSE Score"))

;; Figure 13

(-> (formula Days "~" log (FSC.H))
    (lat/bwplot :data late/gvhd10
                :xlab "log(Forward Scatter)"
                :ylab "Days Past Transplant"))


;; Figure 14

(-> (formula Days "~" log (FSC.H))
    (lat/bwplot :data late/gvhd10 :panel 'panel.violin :box.ratio 3
                :xlab "log(Forward Scatter)"
                :ylab "Days Past Transplant"))

;; Figure 15

(-> (formula factor (mag) "~" depth)
    (lat/stripplot quakes))

;; Figure 16

(-> (formula depth "~" factor (mag))
    (lat/stripplot quakes :jitter.data true :alpha 0.6
                   :xlab "Magnitude (Richter)" :ylab "Depth (km)"))

;; Figure 17

(-> (r "sqrt(abs(residuals(lm(yield~variety+year+site)))) ~ site")
    (lat/stripplot :data lat/barley :groups 'year :jitter.data true
                   :auto.key {:points true :lines true :columns 2}
                   :type ["p" "a"] :fun 'median
                   :ylab (r "expression(abs('Residual Barley Yield')^{1 / 2})")))

(dev/dev-off)

#_(loop []
    
    (g/plot tp1 :split [1 1 1 2])
    (g/plot tp2 :split [1 2 1 2] :newpage false)
    (Thread/sleep 500)
    (require-r '[datasets])
    (def euro r.datasets/euro)
    (r->clj r.mlmRev/Chem97)
    (r->clj euro)
    (g/plot (-> (formula Class "~" Freq | Sex + Age)
                (lat/barchart :data (base/as-data-frame Titanic)
                              :groups 'Survived :stack true :layout [4 1]
                              :auto.key {:title "Survived" :columns 2})))
    (Thread/sleep 500)


    
    (recur))


;; (System/gc)

#_(count (set (repeatedly 10000000 u/rand-name)))
