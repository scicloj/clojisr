(ns clojuress.renjin.engine
  (:import javax.script.ScriptEngineManager
           org.renjin.script.RenjinScriptEngine))

(def manager (ScriptEngineManager.))

(def engine ^RenjinScriptEngine (.getEngineByName ^ScriptEngineManager manager "Renjin"))

(defn reval
  "reval runs R code in Renjin
       (->> \"1+2\"
            reval
            vec)
       => [3.0]"
  {:added "0.1"}
  [^String source]
  (.eval ^RenjinScriptEngine engine source))

(defn runtime-context
  "runtime-context gets the default evaluation context from the Renjin script engine.
   Citing the Renjin source:
   https://github.com/bedatadriven/renjin/blob/master/core/src/main/java/org/renjin/eval/Context.java#L42
   /**
    * Contexts are the internal mechanism used to keep track of where a
    * computation has got to (and from where),
    * so that control-flow constructs can work and reasonable information
    * can be produced on error conditions,
    * (such as via traceback) and otherwise (the sys.xxx functions).
    */"
  {:added "0.1"} []
  (.getRuntimeContext ^RenjinScriptEngine engine))
