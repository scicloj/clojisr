;; Code taken from libpython-clj
;;
;; Every tracked object is kept as WeakReference in `ptr-set-var` hashmap.
;; Explicit call to `clear-reference-queue` ensure that underlying disposing function is called.

(ns clojisr.v1.gc
  (:import [java.util.concurrent ConcurrentHashMap ConcurrentLinkedDeque]
           [java.lang.ref ReferenceQueue WeakReference]))

(set! *warn-on-reflection* true)

(defn gcreference
  [item queue dispose-fn]
  (proxy [WeakReference Runnable] [item queue]
    (run []
      (when dispose-fn
        (locking this
          (dispose-fn this))))))

(defonce ^:dynamic *stack-gc-context* nil)
(defn stack-context
  ^ConcurrentLinkedDeque []
  *stack-gc-context*)

(defonce reference-queue-var (ReferenceQueue.))
(defn reference-queue
  ^ReferenceQueue []
  reference-queue-var)

(defonce ptr-set-var (ConcurrentHashMap/newKeySet))
(defn ptr-set
  ^java.util.Set []
  ptr-set-var)

(defn track
  [item dispose-fn]
  (let [ptr-val (gcreference item (reference-queue) (fn [ptr-val]
                                                      (.remove (ptr-set) ptr-val)
                                                      (dispose-fn)))
        ^ConcurrentLinkedDeque stack-context (stack-context)]
    ;;We have to keep track of the pointer.  If we do not the pointer gets gc'd then
    ;;it will not be put on the reference queue when the object itself is gc'd.
    ;;Nice little gotcha there.
    (if stack-context
      (.add stack-context ptr-val)
      ;;Ensure we don't lose track of the weak reference.  If it gets cleaned up
      ;;the gc system will fail.
      (.add (ptr-set) ptr-val))
    item))

(defn clear-reference-queue
  []
  (when-let [next-ref (.poll (reference-queue))]
    (.run ^Runnable next-ref)
    (recur)))

(defn clear-stack-context
  []
  (when-let [next-ref (.pollLast (stack-context))]
    (.run ^Runnable next-ref)
    (recur)))

(defmacro with-stack-context
  [& body]
  `(with-bindings {#'*stack-gc-context* (ConcurrentLinkedDeque.)}
     (try
       ~@body
       (finally
         (clear-stack-context)))))
