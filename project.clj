(defproject scicloj/clojuress "1.0.0-SNAPSHOT"
  :description "Clojure<->R interop"
  :url "https://github.com/scicloj/clojuress"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :repositories [["bedatadriven" {:url "https://nexus.bedatadriven.com/content/groups/public/"}]]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [gg4clj "0.1.0"]
                 [hara/test "3.0.7"]
                 [hara/code "3.0.7"]
                 [hara/tool "3.0.7"]
                 [hara/module.namespace "3.0.7"]
                 [hara/deploy "3.0.7"]
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
                 [scicloj/notespace "1.0.2-SNAPSHOT"]]
  :injections [(require 'hara.tool)])
