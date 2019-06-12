(ns clojuress.renjin.engine-test
  (:require  [hara.test :refer :all]
             [clojuress.renjin.engine :refer :all]))

^{:refer clojuress.renjin.engine/reval :added "0.1"}
(fact "reval runs R code in Renjin"
      (->> "1+2"
           reval
           vec)
      => [3.0])


^{:refer clojuress.renjin.engine/runtime-context :added "0.1"}
(fact "runtime-context gets the default evaluation context from the Renjin script engine.
  Citing the Renjin source:
  https://github.com/bedatadriven/renjin/blob/master/core/src/main/java/org/renjin/eval/Context.java#L42
  /**
   * Contexts are the internal mechanism used to keep track of where a
   * computation has got to (and from where),
   * so that control-flow constructs can work and reasonable information
   * can be produced on error conditions,
   * (such as via traceback) and otherwise (the sys.xxx functions).
   */")
