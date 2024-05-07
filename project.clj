(defproject scicloj/clojisr "1.0.0"
  :description "Clojure <-> R interop"
  :url "https://github.com/scicloj/clojisr"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :plugins [[lein-tools-deps "0.4.5"]]
  :test-paths ["test","notebooks"]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  ;; :repositories {"bedatadriven" {:url "https://nexus.bedatadriven.com/content/groups/public/"}}
  :lein-tools-deps/config {:config-files [:install :user :project]}
  :profiles {
             :test     {:dependencies [[org.scicloj/clay "2-beta8"]]}}
                        
  :jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/jul-factory"])
