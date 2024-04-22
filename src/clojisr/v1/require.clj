(ns clojisr.v1.require
  (:require [clojisr.v1.session :as session]
            [clojisr.v1.using-sessions :as using-sessions]
            [clojisr.v1.eval :as evl]
            [clojisr.v1.protocols :as prot]
            [clojisr.v1.known-classes :as known-classes]
            [clojisr.v1.util :as util :refer [clojurize-r-symbol exception-cause]]
            [clojisr.v1.impl.common :refer [strange-symbol-name?]]
            [clojisr.v1.impl.java-to-clj :refer [java->clj]]
            [clojure.tools.logging.readable :as log]))

(defn package-r-object [package-symbol object-symbol]
  (evl/r (format "{%s::`%s`}"
                 (name package-symbol)
                 (name object-symbol))
         (session/fetch-or-make nil)))

(defn package-symbol->nonstrange-r-symbols [package-symbol]
  (let [session (session/fetch-or-make nil)]
    (->> (prot/package-symbol->r-symbol-names session package-symbol)
         (remove strange-symbol-name?)
         (map symbol))))

(defn all-r-symbols-map [package-symbol]
  (->> package-symbol
       package-symbol->nonstrange-r-symbols
       (map (fn [r-symbol]
              [(clojurize-r-symbol r-symbol)
               (try
                 (package-r-object package-symbol r-symbol)
                 (catch Exception e
                   (log/warn [::require-symbol {:package-symbol package-symbol
                                                :r-symbol r-symbol
                                                :exception (exception-cause e)}])))]))
       (filter second)
       (into {})))

(defn find-or-create-ns [ns-symbol]
  (or (find-ns ns-symbol)
      (create-ns ns-symbol)))

(def ^:private empty-symbol (symbol ""))

(defn- r-object->arglists
  "Fetch function aruments using `formals` and produce `:arglists` meta tag value and conforming clojisr R function call style."
  [{:keys [code class]}]
  (when (known-classes/function-classes class)
    (let [sess (session/fetch-or-make nil)
          args (->> sess
                    (evl/r (format
                            ;; https://stackoverflow.com/questions/25978301/how-to-retrieve-formals-of-a-primitive-function/25978487#25978487
                            "with(list(x=%s), if(is.primitive(x)) formals(args(x)) else formals(x))"
                            code))
                    (using-sessions/r->java)
                    (java->clj))
          {:keys [obl opt]} (reduce-kv (fn [m k v]
                                         (let [selector (if (and (= empty-symbol v) ;; dirty logic
                                                                 (not (seq (:opt m)))
                                                                 (not= :... k))
                                                          :obl
                                                          :opt)]
                                           (update m selector conj (symbol k))))
                                       {:obl [] :opt []}
                                       args)]
      (cond
        (and (seq obl)
             (seq opt)) (list (conj obl '& {:keys opt}))
        (seq obl) (list obl)
        (seq opt) (list ['& {:keys opt}])
        :else '([])))))

(defn r-symbol->clj-symbol [r-symbol r-object]
  (if-let [arglists (r-object->arglists r-object)]
    (vary-meta r-symbol assoc :arglists arglists)
    r-symbol))

(defn add-to-ns [ns-symbol r-symbol r-object]
  (intern ns-symbol
          (r-symbol->clj-symbol r-symbol r-object)
          r-object))

(defn symbols->add-to-ns [ns-symbol r-symbols]
  (doseq [[r-symbol r-object] r-symbols]
    (add-to-ns ns-symbol r-symbol r-object)))

(defn require-r-package [[package-symbol & {:keys [as refer]}]]
  (try
    (let [session (session/fetch-or-make nil)]
      (evl/eval-form `(library ~package-symbol) session))
    (let [r-ns-symbol (->> package-symbol
                           (str "r.")
                           symbol)
          r-symbols (all-r-symbols-map package-symbol)]

      ;; r.package namespace
      (find-or-create-ns r-ns-symbol)
      (symbols->add-to-ns r-ns-symbol r-symbols)

      ;; alias namespaces
      ;; https://clojurians.zulipchat.com/#narrow/stream/224816-clojisr-dev/topic/require-r.20vs.20-require-python
      (alias package-symbol r-ns-symbol)
      (when as (alias as r-ns-symbol))

      ;; inject symbol into current namespace
      (when refer
        (let [this-ns-symbol (-> *ns* str symbol)]
          (symbols->add-to-ns this-ns-symbol
                              (if (= refer :all)
                                r-symbols
                                (select-keys r-symbols refer))))))
    (catch Exception e
      (log/warn [::require-r-package {:package-symbol package-symbol
                                      :cause (exception-cause e)}])
      (throw (ex-info (format "R package '%s' is not available. Please install it first." (name package-symbol))
                      {:package-name package-symbol})))))

(defn require-r [& packages]
  {:deprecated "Please use `require-r` function from `clojisr.v1.r` directly."}
  (run! require-r-package packages))
