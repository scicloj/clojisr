(ns clojuress.v1.util
  (:require [com.rpl.specter :as specter]
            [clojure.string :as string])
  (:import [java.io File]))

(defn first-if-one
  "Given a vector with a single element, first-if-one extracts that element; acts as identity otherwise.
 
       (first-if-one [:abc])
       => :abc
 
       (first-if-one [:abc :def])
       => [:abc :def]
 
       (first-if-one :abc)
       => :abc"
  {:added "0.1"}
  [form]
  (if (and (vector? form)
           (-> form count (= 1)))
    (first form)
    form))

(defn fmap
  "fmap updates the values of a given map
  using a given function.
 
  (->> {:a 1
        :b 2}
       (fmap inc))
  => {:a 2
      :b 3}"
  {:added "0.1"}
  [f m]
  (specter/transform [specter/MAP-VALS]
                     f
                     m))


(defn starts-with?
  "starts-with? checks if a given object s
  is sequential and its first element
  equals a given object v.
 
  (starts-with? 4 4)
  => false
 
  (starts-with? [4] 4)
  => true
 
  (starts-with? [4] 5)
  => false
 
  (starts-with? [] 5)
  => false
 
  (starts-with? [4 5] 4)
  => true
 
  (starts-with? [4 5] 5)
  => false
 
  (starts-with? '(4) 4)
  => true
 
  (starts-with? '(4) 5)
  => false
 
  (starts-with? '() 5)
  => false
 
  (starts-with? '(4 5) 4)
  => true
 
  (starts-with? '(4 5) 5)
  => false"
  {:added "0.1"} [s v]
  (and (sequential? s)
       (-> s first (= v))))

;; Thanks https://gist.github.com/sunng87/13700d3356d5514d35ad
(defn private-field [^Object obj field-name]
  (let [m (.. obj getClass (getDeclaredField field-name))]
    (. m (setAccessible true))
    (. m (get obj))))

;; Thanks https://blog.michielborkent.nl/2018/01/17/transducing-text/
(defn lines-reducible
  [^java.io.BufferedReader rdr]
  (reify clojure.lang.IReduceInit
    (reduce [this f init]
      (try
        (loop [state init]
          (if (reduced? state)
            @state
            (if-let [line (.readLine rdr)]
              (recur (f state line))
              state)))
        (finally
          (.close rdr))))))

(defn -|>
  "A transformation analogous to the -> threading macro,
  but for vectors."
  [x & forms]
  (loop [x x, forms forms]
    (if forms
      (let [form     (first forms)
            threaded (if (vector? form)
                       (into [(first form) x]
                             (next form))
                       [form x])]
        (recur threaded (next forms)))
      x)))

(comment
  (-|> 4
       :+
       [:* 10])
;; => [:* [:+ 4] 10]
  )


(defn tilde [s]
  (->> s
      name
      (str "~")
      keyword))


(defn file-exists? [path]
  (.exists (File. path)))

(defn rand-name []
  (-> (java.util.UUID/randomUUID)
      (string/replace "-" "")
      (->> (take 16)
           (cons \x)
           (apply str))))

(def l list)

(defn clojurize-r-symbol [s]
  (-> s
      name
      (string/replace #"\." "-")
      symbol))
