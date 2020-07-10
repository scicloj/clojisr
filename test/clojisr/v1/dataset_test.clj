(ns clojisr.v1.dataset-test
  (:require [notespace.v2.note :as note
             :refer [note note-void note-md]]))

(note-md "# Dataset transfer from R to Clojure")

(note-void (require '[clojisr.v1.r :as r :refer [r r->clj clj->r require-r]]))

(note-void
 (r/set-default-session-type! :rserve)
 (r/discard-all-sessions))

(note-void (require-r '[datasets]))

(note-md "## Data Frame")

(note-md "Any data.frame, also tribble and data.table are treated the same. If row.names are available they are converted to the additional column `:$row.names`.")

(note r.datasets/BOD)
(note (r->clj '(attributes BOD)))
(note (r->clj r.datasets/BOD))

(note r.datasets/CO2)
(note (r->clj '(attributes CO2)))
(note (r->clj r.datasets/CO2))

(note-md "## Table")

(note-md "Table is converted to a long form where each dimension has it's own column. If column names are not available, column id is prefixed with `:$col`. Values are stored in the last, `:$value` column.")

(note r.datasets/UCBAdmissions)
(note (r->clj '(attributes UCBAdmissions)))
(note (r->clj r.datasets/UCBAdmissions))

(note r.datasets/crimtab)
(note (r->clj '(attributes crimtab)))
(note (r->clj r.datasets/crimtab))

(note-md "## Matrices, arrays, multidimensional arrays")

(note-md "First two dimensions creates dataset, all additional dimensions are added as columns")

(note r.datasets/VADeaths)
(note (r->clj '(attributes VADeaths)))
(note (r->clj r.datasets/VADeaths))

(note r.datasets/freeny-x)
(note (r->clj '(attributes freeny.x)))
(note (r->clj r.datasets/freeny-x))

(note r.datasets/iris3)
(note (r->clj '(attributes iris3)))
(note (r->clj r.datasets/iris3))

(note-void (def array-5d (r '(array ~(range 60) :dim [2 5 1 3 2]))))
(note array-5d)
(note (r->clj '(attributes ~array-5d)))
(note (r->clj array-5d))

(note-md "## 1D timeseries")

(note-md "Timeseries are stored in two columns:

* `:$time` - to store time identifier as double
*`:$series` - to store timeseries")

(note r.datasets/BJsales)
(note (r->clj '(attributes BJsales)))
(note (r->clj r.datasets/BJsales))

(note-md "## Multidimensional timeseries")

(note (r '(window EuStockMarkets :end [1991,155])))
(note (r->clj '(attributes EuStockMarkets)))
(note (r->clj r.datasets/EuStockMarkets))

(note-md "## Datetime columns")

(note-void (def dt (r "
   day <- c(\"20081101\", \"20081101\", \"20081101\", \"20081101\", \"18081101\", \"20081102\", \"20081102\", \"20081102\", \"20081102\", \"20081103\")
   time <- c(\"01:20:00\", \"06:00:00\", \"12:20:00\", \"17:30:00\", \"21:45:00\", \"01:15:00\", \"06:30:00\", \"12:50:00\", \"20:00:00\", \"01:05:00\")
   dts1 <- paste(day, time)
   dts2 <- as.POSIXct(dts1, format = \"%Y%m%d %H:%M:%S\")
   dts3 <- as.POSIXlt(dts1, format = \"%Y%m%d %H:%M:%S\")
   dts <- data.frame(posixct=dts2, posixlt=dts3)")))

(note dt)
(note (r->clj '(attributes ~dt)))
(note (r->clj dt))

(note-md "## Distances")

(note r.datasets/UScitiesD)
(note (r->clj '(attributes UScitiesD)))
(note (r->clj r.datasets/UScitiesD))

(note-md "## Other")

(note-md "### List")

(note r.datasets/Harman23-cor)
(note (r->clj '(attributes Harman23.cor)))
(note (r->clj r.datasets/Harman23-cor))

(note-md "### Partially named list")

(note-void (def pnl (r '[:!list :a 112 "abc" "cde" :b "qwe"])))

(note pnl)
(note (r->clj '(attributes ~pnl)))
(note (r->clj pnl))

(note-md "## Dataset -> R")

(note-md "Every dataset is converted to `data.frame` object.")

(note (clj->r (r->clj r.datasets/UScitiesD)))

(comment (notespace.v2.note/compute-this-notespace!))
