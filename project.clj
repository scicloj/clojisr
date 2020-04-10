(defproject scicloj/clojisr "1.0.0-BETA11-SNAPSHOT"
  :description "Clojure<->R interop"
  :url "https://github.com/scicloj/clojisr"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

;; managed deendencies only define the version of a dependency,
;; if no dependeny needs them, then they are not included
  :managed-dependencies [; to avoid a :exclusion mess, we define certain versions numbers centrally
                         ; serialization libraries are dependencies o many libraries,
                         [org.clojure/core.memoize "0.8.2"]
                         [org.clojure/data.json "1.0.0"]
                         [org.clojure/data.fressian "1.0.0"]
                         [org.clojure/core.match "1.0.0"]
                         [com.cognitect/transit-clj "1.0.324"]
                         [com.cognitect/transit-cljs "0.8.256"]
                         [com.fasterxml.jackson.core/jackson-core "2.11.0.rc1"]
                         [cheshire "5.10.0"]
                         [com.taoensso/encore "2.119.0"]
                         ; patches to get most uptodate version for certain conflicts:
                         [commons-codec "1.12"] ; selmer and clj-http (via gorilla-explore)
                         [ring/ring-codec "1.1.1"] ; ring and compojure
                         [org.flatland/useful "0.11.6"] ; clojail and ring-middleware-format
                         ; pinkgorilla (enforce to use latest version of all projects)
                         [org.pinkgorilla/gorilla-middleware "0.2.21"]
                         [org.pinkgorilla/gorilla-renderable-ui "0.1.29"]
                         [org.pinkgorilla/gorilla-ui "0.1.27"]
                         [org.pinkgorilla/notebook-encoding "0.0.28"]
                         [org.pinkgorilla/gorilla-explore "0.1.19"]
                         [org.pinkgorilla/kernel-cljs-shadowdeps "0.0.12"]
                         [org.pinkgorilla/kernel-cljs-shadow "0.0.25"]
                         ; shadow-cljs
                         [thheller/shadow-cljs "2.8.94"]]
  
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [techascent/tech.datatype "5.0-beta-2"]
                 [techascent/tech.resource "4.6"]
                 [techascent/tech.ml.dataset "2.0-beta-11"]
                 [org.rosuda.REngine/REngine "2.1.0"]
                 [org.rosuda.REngine/Rserve "1.8.1"]
                 [hiccup "1.0.5"]
                 [org.clojure/tools.logging "1.0.0"]
                 [clj-commons/pomegranate "1.2.0"]
                 [org.pinkgorilla/gorilla-renderable "3.0.5"] ; to implement pink-gorilla renderer
                 ]
  :jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/jul-factory"]

  :profiles {:notebook
             ; run the pink-gorilla notebook (standalone, or in repl)
             ; important to keep this dependency in here only, as we do not want to
             ; bundle the notebook (big bundle) into clojisr library       
             {:source-paths ["profiles/notebook/src"]
              :main ^:skip-aot notebook.main
              :dependencies [[org.pinkgorilla/gorilla-notebook "0.4.12-SNAPSHOT"]]
              :repl-options {:welcome (println "Profile: notebook.")
                             :init-ns notebook.main  ;; Specify the ns to start the REPL in (overrides :main in this case only)
                             :init (start) ;; This expression will run when first opening a REPL, in the namespace from :init-ns or :main if specified.
                             }}}
  :aliases {"notebook" ^{:doc "Runs pink-gorilla notebook"}
            ["with-profile" "+notebook" "run" "-m" "notebook.main"] }
  
  
  )
