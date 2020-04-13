(defproject scicloj/clojisr "1.0.0-BETA11-SNAPSHOT"
  :description "Clojure<->R interop"
  :url "https://github.com/scicloj/clojisr"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :min-lein-version "2.9.1"
  :min-java-version "1.11"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.logging "1.0.0"]
                 
                 [techascent/tech.datatype "4.88"]
                 [techascent/tech.resource "4.6"]
                 [techascent/tech.ml.dataset "2.0-beta-4"]
                 [org.rosuda.REngine/REngine "2.1.0"]
                 [org.rosuda.REngine/Rserve "1.8.1"]
                 [hiccup "1.0.5"]
                 [clj-commons/pomegranate "1.2.0"]

                 ;; pinkgorilla-vizualisation
                 [org.pinkgorilla/gorilla-renderable "3.0.5"] ; to implement pink-gorilla renderer
                 [com.rpl/specter "1.1.3"] ; clojisr.util, svg width/height injection
                 [org.clojure/data.xml "0.0.8"] ; make sure old version from tagsoup is not used
                 [clj-tagsoup/clj-tagsoup "0.3.0" ; to parse xml from the svg
                  :exclusions [org.clojure/clojure ; very, very old clojure version. 
                               org.clojure/core.specs.alpha ; damn old
                               org.clojure/data.xml ; damn old - "0.0.3"
                               ]]]

  ;:jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/jul-factory"]
  :source-paths ["src"]
  :profiles {:notespace {; runs notespace demos
                         ; important to keep this dependency in here only, as we do not want to
                         ; bundle the notebook (big bundle) into clojisr library   
                         :source-paths ["profiles/notebook/src"]
                         ;:main ^:skip-aot notebook.main
                         :dependencies [[scicloj/notespace "2.0.0-alpha4"]]
                         :repl-options {:welcome (println "Profile: notespace")
                        ;:init-ns notebook.main  ;; Specify the ns to start the REPL in (overrides :main in this case only)
                        ;:init (start) ;; This expression will run when first opening a REPL, in the namespace from :init-ns or :main if specified.
                                        }}
             :gorilla {; run the pink-gorilla notebook (standalone, or in repl)
                       ; important to keep this dependency in here only, as we do not want to
                       ; bundle the notebook (big bundle) into clojisr library 
                       :source-paths ["profiles/notebook/src"]
                       :main notebook.main ; ^:skip-aot 
                       :dependencies [[org.pinkgorilla/gorilla-notebook "0.4.12-SNAPSHOT"]]
                       :repl-options {:welcome (println "Profile: gorilla")
                                      :init-ns notebook.main  ;; Specify the ns to start the REPL in (overrides :main in this case only)
                                      :init (start) ;; This expression will run when first opening a REPL, in the namespace from :init-ns or :main if specified.
                                      }}}
  :plugins [[lein-ancient "0.6.15"]
            [min-java-version "0.1.0"]]
  :aliases {"gorilla" ^{:doc "Runs pink-gorilla notebook"}
            ["with-profile" "+gorilla" "run" "-m" "notebook.main"]})
