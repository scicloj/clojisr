(ns clojuress.v1.functions
  (:require [clojuress.v1.using-sessions :as using-sessions]
            [clojuress.v1.session :as session]
            [clojuress.v1.refresh :as refresh]
            [clojuress.v1.codegen :as codegen]
            [tech.resource :as resource]
            [cambium.core :as log])
  (:import clojuress.v1.robject.RObject))


(defn apply-function [r-function
                      args
                      session]
  (-> r-function
      list
      (concat args)
      (codegen/form->code session)
      (using-sessions/eval-code session)))

(def functions-mem (atom {}))

(defn function-impl [r-object]
  (if (-> r-object using-sessions/function? not)
    (fn [& _]
      (throw (ex-info "Not a function." {:r-object r-object})))
    (let [autorefreshing (refresh/auto-refresing-object
                          r-object)]
      (fn ([& args]
           (let [explicit-session-args
                 (when (some-> args butlast last (= :session-args))
                   (last args))]
             (apply-function
              @autorefreshing
              (if explicit-session-args
                (-> args butlast butlast)
                args)
              (session/fetch-or-make explicit-session-args))))))))

(defn function
  [r-object]
  (or (@functions-mem r-object)
      (let [f (function-impl r-object)]
        (swap! functions-mem assoc r-object f)
        (resource/track
         r-object
         #(do (log/info [::releasing (:object-name r-object)])
              (swap! functions-mem dissoc r-object))
         :gc)
        f)))
