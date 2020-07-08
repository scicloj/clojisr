(ns clojisr.v1.renjin
  (:require [clojisr.v1.session :as session]
            [cemerick.pomegranate :refer [add-dependencies]]))

(set! *warn-on-reflection* true)

(add-dependencies :coordinates '[[org.renjin/renjin-script-engine "RELEASE"]]
                  :repositories {"bedatadriven" {:url "https://nexus.bedatadriven.com/content/groups/public/"}
                                 ;; Making sure https is used with Maven Central.
                                 ;; See https://stackoverflow.com/a/59763928/1723677
                                 "maven" {:url "https://repo.maven.apache.org/maven2" }}
                  ;; See this discussion: https://github.com/nrepl/nrepl/issues/113
                  :classloader (.getParent ^clojure.lang.DynamicClassLoader @Compiler/LOADER))

(require '[clojisr.v1.impl.renjin.session :refer [make]])

(session/add-session-type!
 :renjin make)

(defn set-as-default! []
  (session/set-default-session-type! :renjin))

(defn set-as-default-if-missing! []
  (session/set-default-session-type-if-missing! :renjin))

(set-as-default-if-missing!)
