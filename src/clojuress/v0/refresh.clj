(ns clojuress.v0.refresh
  (:require [clojuress.v0.session :as session]
            [clojuress.v0.eval :as evl]))

(defn fresh-object? [r-object]
  (-> r-object
      :session
      session/fresh?))

(defn refreshed-object [r-object]
  (if (fresh-object? r-object)
    ;; The object is refresh -- just return it.
    r-object
    ;; Try to return a refreshed object.
    (if-let [code (:code r-object)]
      ;; The object has code information -- rerun the code with the same session-args.
      (evl/r code
             (session/fetch (:session-args r-object)))
      ;; No code information.
      (ex-info "Cannot refresh an object with no code info."
               {:r-object r-object}))))

(defn auto-refresing-object [r-object]
  (let [mem (atom r-object)]
    (reify clojure.lang.IDeref
      (deref [_]
        (when (-> @mem fresh-object? not)
          (reset! mem (refreshed-object r-object)))
        @mem))))

