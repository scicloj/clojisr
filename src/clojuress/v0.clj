(ns clojuress.v0
  (:require [clojuress.v0.session :as session]
            [clojuress.v0.execution :as execution]
            [clojuress.v0.inspection :as inspection]
            [clojuress.v0.using-sessions :as using-sessions]
            [clojuress.v0.protocols :as prot]
            [clojure.pprint :as pp]
            [clojure.string :as string]
            [clojuress.v0 :as r]
            [clojuress.v0.codegen :as codegen])
  (:import clojuress.v0.robject.RObject))


(defn init [& {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (session/init session)))

(defn r [r-code & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (using-sessions/eval-r r-code session)))

(defn eval-r->java [r-code & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (prot/eval-r->java session r-code)))

(defn eval-r->java [r-code & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (prot/eval-r->java session r-code)))

(defn r->java [r-object]
  (using-sessions/r->java r-object))

(defn java->r [java-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (using-sessions/java->r java-object session)))

(defn java->naive-clj [java-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (prot/java->naive-clj session java-object)))

(defn java->clj [java-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (prot/java->clj session java-object)))

(defn clj->java [clj-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (prot/clj->java session clj-object)))

(def clj->java->r (comp java->r clj->java))
(def clj->r clj->java->r)

(def r->java->clj (comp java->clj r->java))
(def r->clj r->java->clj)

(defn discard-session [session-args]
  (session/discard session-args))

(defn discard-default-session []
  (session/discard-default))

(defn discard-all-sessions []
  (session/discard-all))

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
      (r code
         :session-args (:session-args r-object))
      ;; No code information.
      (ex-info "Cannot refresh an object with no code info." {:r-object r-object}))))

(defn auto-refresing-object [r-object]
  (let [mem (atom r-object)]
    (reify clojure.lang.IDeref
      (deref [_]
        (when (-> @mem fresh-object? not)
          (reset! mem (refreshed-object r-object)))
        @mem))))


(defn apply-function [r-function args & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (execution/apply-function r-function args session)))

(defn function
  [r-function]
  (let [autorefreshing (auto-refresing-object
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
          :session (session/fetch-or-make explicit-session-args)))))))

(defn add-functions-to-this-ns [package-symbol function-symbols]
  (doseq [s function-symbols]
    (let [d (delay (function
                    (r (format "{library(%s); `%s`}"
                               (name package-symbol)
                               (name s)))))
          f (fn [& args]
              (apply @d args))
          clojurized-symbol (-> s
                                name
                                (string/replace #"\." "-")
                                symbol)]
      (eval (list 'def clojurized-symbol f)))))

(defn add-package-to-this-ns
  [package-symbol]
  (->> package-symbol
       name
       (format "library(%s)")
       r)
  (->> package-symbol
       name
       vector
       (apply-function (r "function(package_name) as.character(unlist(lsf.str(paste0('package:', package_name))))"))
       r->java->clj
       (filter (fn [function-name]
                 (re-matches #"[A-Za-z][A-Za-z\\.\\_].*" function-name)))
       (map symbol)
       (add-functions-to-this-ns package-symbol)))

;; Overriding pprint
(defmethod pp/simple-dispatch RObject [obj]
  (let [java-object (r->java obj)]
    (pp/pprint [['R
                 :object-name (:object-name obj)
                 :session-args (-> obj :session :session-args)
                 :r-class (inspection/r-class obj)]
                ['->Java java-object]])))

(defn form->r-code
  [form & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (codegen/form->code form session)))

(defn eval-form
  [form & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (execution/eval-form form session)))

(defn na [& {:keys [session-args]}]
  (r "NA" :session-args session-args))

(defn r-object? [obj]
  (instance? RObject obj))

(defn library [libname]
  (->> libname
       (format "library(%s)")
       r))

