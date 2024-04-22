;; # Dataset transfer from R to Clojure

(ns clojisr.v1.dataset-test
  (:require [clojisr.v1.r :as r :refer [r r->clj clj->r require-r]]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.kindly.v4.api :as kindly]))

^:kindly/hide-code
(def md (comp kindly/hide-code kind/md))

(r/set-default-session-type! :rserve)
(r/discard-all-sessions)

(require-r '[datasets])

(md "## Data Frame")

(md "Any data.frame, also tribble and data.table are treated the same. If row.names are available they are converted to the additional column `:$row.names`.")

r.datasets/BOD
(r->clj '(attributes BOD))
(r->clj r.datasets/BOD)

r.datasets/CO2
(r->clj '(attributes CO2))
(r->clj r.datasets/CO2)

(md "## Table")

(md "Table is converted to a long form where each dimension has it's own column. If column names are not available, column id is prefixed with `:$col`. Values are stored in the last, `:$value` column.")

r.datasets/UCBAdmissions
(r->clj '(attributes UCBAdmissions))
(r->clj r.datasets/UCBAdmissions)

r.datasets/crimtab
(r->clj '(attributes crimtab))
(r->clj r.datasets/crimtab)

(md "## Matrices, arrays, multidimensional arrays")

(md "First two dimensions creates dataset, all additional dimensions are added as columns")

r.datasets/VADeaths
(r->clj '(attributes VADeaths))
(r->clj r.datasets/VADeaths)

r.datasets/freeny-x
(r->clj '(attributes freeny.x))
(r->clj r.datasets/freeny-x)

r.datasets/iris3
(r->clj '(attributes iris3))
(r->clj r.datasets/iris3)

(def array-5d (r '(array ~(range 60) :dim [2 5 1 3 2])))
array-5d
(r->clj '(attributes ~array-5d))
(r->clj array-5d)

(md "## 1D timeseries")

(md "Timeseries are stored in two columns:

* `:$time` - to store time identifier as double
*`:$series` - to store timeseries")

r.datasets/BJsales
(r->clj '(attributes BJsales))
(r->clj r.datasets/BJsales)

(md "## Multidimensional timeseries")

(r '(window EuStockMarkets :end [1991,155]))
(r->clj '(attributes EuStockMarkets))
(r->clj r.datasets/EuStockMarkets)

(md "## Datetime columns")

(def dt (r "
   day <- c(\"20081101\", \"20081101\", \"20081101\", \"20081101\", \"18081101\", \"20081102\", \"20081102\", \"20081102\", \"20081102\", \"20081103\")
   time <- c(\"01:20:00\", \"06:00:00\", \"12:20:00\", \"17:30:00\", \"21:45:00\", \"01:15:00\", \"06:30:00\", \"12:50:00\", \"20:00:00\", \"01:05:00\")
   dts1 <- paste(day, time)
   dts2 <- as.POSIXct(dts1, format = \"%Y%m%d %H:%M:%S\")
   dts3 <- as.POSIXlt(dts1, format = \"%Y%m%d %H:%M:%S\")
   dts <- data.frame(posixct=dts2, posixlt=dts3)"))

dt
(r->clj '(attributes ~dt))
(r->clj dt)

(md "## Distances")

r.datasets/UScitiesD
(r->clj '(attributes UScitiesD))
(r->clj r.datasets/UScitiesD)

(md "## Other")

(md "### List")

r.datasets/Harman23-cor
(r->clj '(attributes Harman23.cor))
(r->clj r.datasets/Harman23-cor)

(md "### Partially named list")

(def pnl (r '[:!list :a 112 "abc" "cde" :b "qwe"]))

pnl
(r->clj '(attributes ~pnl))
(r->clj pnl)

(md "## Dataset -> R")

(md "Every dataset is converted to `data.frame` object.")

(clj->r (r->clj r.datasets/UScitiesD))
