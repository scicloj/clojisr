(ns clojuress.v1.functions
  (:require [clojuress.v1.using-sessions :as using-sessions]
            [clojuress.v1.codegen :as codegen]
            [clojuress.v1.session :as session]
            [clojuress.v1.refresh :as refresh]
            [clojuress.v1.eval :as evl]))

(defn apply-function [r-function
                      args
                      session]
  (-> r-function
      list
      (concat args)
      (codegen/form->code session)
      (using-sessions/eval-code session)))

(defn function
  [r-function]
  (let [autorefreshing (refresh/auto-refresing-object
                        r-function)]
    (fn f
      ([& args]
       (let [explicit-session-args
             (when (some-> args butlast last (= :session-args))
               (last args))]
         (apply-function
          @autorefreshing
          (if explicit-session-args
            (-> args butlast butlast)
            args)
          (session/fetch-or-make explicit-session-args)))))))


