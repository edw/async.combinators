(ns async.combinators
  (:require [clojure.core.async :as a]))

(defn pinch
  "Combinator to limit a function `f`'s concurrency to no more than
  `max-n` simultaneous evaluations."
  {:added "0.1.0"}
  [max-n f]
  (let [pool (a/chan max-n)]
    (doseq [_ (range max-n)] (a/>!! pool 42))
    (fn [& args]
      (let [token (a/<!! pool)]
        (try (apply f args)
             (finally (a/>!! pool token)))))))

(defn spawn
  "Combinator to evaluate function `f` asynchronously. Returns a
  channel for result."
  {:added "0.1.0"}
  [f]
  (fn [& args]
    (a/go (apply f args))))

(defn tally
  "Combinator to count evaluations of `f` in atom `a`."
  {:added "0.1.0"}
  [a f]
  (fn [& args]
    (try (apply f args)
         (finally (swap! a inc)))))

(defn stall
  "Combinator to ensure that evaluation of `f` takes at least `min`
  milliseconds."
  {:added "0.1.0"}
  [min f]
  (if (zero? min)
    f
    (fn [& args]
      (let [start (System/currentTimeMillis)
            result (apply f args)
            embargo (- (+ start min) (System/currentTimeMillis))]
        (when (pos? embargo) (a/<!! (a/timeout embargo)))
        result))))

(defn upon
  "Combinator to evaluate `f` after reference `r` has satisfied
  predicate `g`."
  {:added "0.2.0"}
  [r g f]
  (let [semaphore (a/chan)]
    (if (g @r)
      (a/close! semaphore)
      (add-watch r (gensym "upon")
                 (fn [_ _ _ new-state]
                   (when (g new-state)
                     (a/close! semaphore)))))
    (fn [& args]
      (a/<!! semaphore)
      (apply f args))))

(defn after
  "Combinator to evaluate function `f` after `delay` milliseconds have
  passed."
  {:added "0.3.0"}
  [delay f]
  (fn [& args]
    (a/<!! (a/timeout delay))
    (apply f args)))

(defn retry
  "Combinator to evaluate function `f` up to `max` times until it
  produces a truthy value. If no such value is produced, return
  nil. Wait `delay` milliseconds between evaluations."
  {:added "0.3.0"}
  ([max f] (retry max 0 f))
  ([max delay f]
     (let [g (if (pos? delay) (after delay f) f)]
       (fn [& args]
         (loop [n 0 result (apply f args)]
           (cond (>= n max) nil
                 result result
                 :else (recur (inc n) (apply g args))))))))

(defn nilf
  "Combinator to evaluate function `g` if function `f` evaluates to a
  falsey value."
  {:added "0.3.0"}
  [g f]
  (fn [& args]
    (or (apply f args)
        (apply g args))))

(defn nile
  "Combinator to force function `f` to evaluate to nil in the event
  that an exception is thrown during evaluation."
  {:added "0.4.0"}
  [f]
  (fn [& args]
    (try (apply f args)
         (catch Exception e nil))))

(defn deposit
  "Combinator to asyncronously put value produced by evaluating function
  `f` to port `p`. Returns result of evaluating `f`."
  {:added "0.4.0"}
  [p f]
  (fn [& args]
    (let [result (apply f args)]
      (a/put! p result)
      result)))

(defn argment
  "Combinator to augment the result of evaluating function `f` with
  the argument(s) supplied, speicifcally by returning an instance of
  clojure.lang.MapEntry containing the arguments as the key and the
  result as the value."
  {:added "0.4.0"}
  [f]
  (fn [& args]
    (clojure.lang.MapEntry. args (apply f args))))

(defn batch
  "Evaluate function `f` for side effects with sequences of values
  taken from port `p`. An evaluation occurs when `n` items are taken
  from the port or at least one item has been taken from the port and
  at least `delay` milliseconds have elapsed since `batch` was
  evaluated or `f` was previously evaluated. Returns port `p`."
  {:added "0.5.0"}
  [p n delay f]
  (a/go-loop [last (System/currentTimeMillis)
              coll []
              [x q] (a/alts! [(a/timeout delay) p])
              batched-count 0]
    (if (and (= q p) (nil? x))
      (do (when (seq coll) (f coll)) (+ batched-count (count coll)))
      (let [coll (if (= p q) (conj coll x) coll) now (System/currentTimeMillis)]
        (if (or (= (count coll) n)
                (and (pos? (count coll)) (> now (+ last delay))))
          (do (f coll)
              (recur now [] (a/alts! [(a/timeout delay) p])
                     (+ batched-count (count coll))))
          (recur last coll (a/alts! [(a/timeout delay) p]) batched-count))))))
