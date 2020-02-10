(defproject scicloj/clojisr "1.0.0-BETA6"
  :description "Clojure<->R interop"
  :url "https://github.com/scicloj/clojisr"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [scicloj/notespace "1.0.3"]
                 [gg4clj "0.1.0"]
                 [nrepl "0.6.0"]
                 [com.rpl/specter "1.1.3"]
                 [techascent/tech.datatype "4.66"]
                 [techascent/tech.resource "4.5"]
                 [techascent/tech.ml.dataset "1.63"]
                 [org.rosuda.REngine/REngine "2.1.0"]
                 [org.rosuda.REngine/Rserve "1.8.1"]
                 [hiccup "1.0.5"]
                 [cambium/cambium.core         "0.9.3"]
                 [cambium/cambium.codec-simple "0.9.3"]
                 [cambium/cambium.logback.core "0.4.3"]
                 [alembic "0.3.2"]])
