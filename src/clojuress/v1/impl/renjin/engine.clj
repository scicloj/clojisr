(ns clojuress.v1.impl.renjin.engine
  (:import (org.renjin.script RenjinScriptEngine RenjinScriptEngineFactory)))

(defn ->engine [] ^RenjinScriptEngine (.getScriptEngine ^RenjinScriptEngineFactory (RenjinScriptEngineFactory.)))

(defn eval! [^RenjinScriptEngine engine
             ^String source]
  (.eval ^RenjinScriptEngine engine source))

(defn runtime-context [engine]
  (.getRuntimeContext ^RenjinScriptEngine engine))

(defn session [engine]
  (.getSession ^RenjinScriptEngine engine))
