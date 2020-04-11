(ns clojisr.v1.functions
  (:require [clojisr.v1.using-sessions :as using-sessions]
            [clojisr.v1.session :as session]
            [clojisr.v1.refresh :as refresh]
            [clojisr.v1.codegen :as codegen]
            [clojisr.v1.gc :as gc]
            [clojure.tools.logging.readable :as log]
          ;  [clojisr.v1.robject :refer [RObject]]
             
            )
  ;(:import clojisr.v1.robject.RObject)
  )


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
        (gc/track
         r-object
         ;; TODO: revisit cleaning strategy, below code will never be called (r-object is kept in `functions-mem` atom forever) [ts]
         #(do (log/debug [::function {:message "Releasing function cache."
                                      :object-name r-object}])
              (swap! functions-mem dissoc r-object)))
        f)))
