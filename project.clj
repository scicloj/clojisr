(defproject scicloj/clojisr "1.0.0-BETA10-SNAPSHOT"
  :description "Clojure<->R interop"
  :url "https://github.com/scicloj/clojisr"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [scicloj/notespace "2.0.0-alpha3"]                 
                 [com.rpl/specter "1.1.3"]
                 [techascent/tech.datatype "4.88"]
                 [techascent/tech.resource "4.6"]
                 [techascent/tech.ml.dataset "2.0-beta-4"]
                 [org.rosuda.REngine/REngine "2.1.0"]
                 [org.rosuda.REngine/Rserve "1.8.1"]
                 [hiccup "1.0.5"]
                 [org.clojure/tools.logging "1.0.0"]
                 [clj-commons/pomegranate "1.2.0"]]
  :jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/jul-factory"])
