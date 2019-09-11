(defproject clojuress "0.1.0-SNAPSHOT"
  :description "Clojure<->R interop"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :repositories [["bedatadriven" {:url "https://nexus.bedatadriven.com/content/groups/public/"}]]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.renjin/renjin-script-engine "0.9.2726"]
                 [gg4clj "0.1.0"]
                 [hara/test "3.0.5"]
                 [hara/code "3.0.5"]
                 [hara/tool "3.0.5"]
                 [hara/module.namespace "3.0.5"]
                 [hara/deploy "3.0.5"]
                 [nrepl "0.6.0"]
                 [com.rpl/specter "1.1.2"]
                 [techascent/tech.datatype "4.5"]
                 [techascent/tech.resource "4.5"]
                 [techascent/tech.ml.dataset "1.23"]
                 [org.rosuda.REngine/REngine "2.1.0"]
                 [org.rosuda.REngine/Rserve "1.8.1"]
                 [alembic "0.3.2"]]
  :injections [(require 'hara.tool)]
  ;; :repl-options {:init-ns clojuress.core}
  )
