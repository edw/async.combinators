(ns async.combinators
  (:require [clojure.core.async :as a]))

(defn pinch
  "Limit a function `f`'s concurrency to no more than `max-n`
  simultaneous evaluations."
  [max-n f]
  (let [pool (a/chan max-n)]
    (doseq [_ (range max-n)] (a/>!! pool 42))
    (fn [& args]
      (let [token (a/<!! pool)]
        (try (apply f args)
             (finally (a/>!! pool token)))))))

(defn spawn
  "Evaluate function `f` asynchronously. Returns a channel for
  result."
  [f]
  (fn [& args]
    (a/go (apply f args))))

(defn tally
  "Count evaluations of `f` in atom `a`."
  [a f]
  (fn [& args]
    (try (apply f args)
         (finally (swap! a inc)))))

(defn stall
  "Ensure that evaluation of `f` takes at least `min` milliseconds."
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
  [delay f]
  (fn [& args]
    (a/<!! (a/timeout delay))
    (apply f args)))

(defn retry
  "Combinator to evaluate function `f` up to `max` times until it
  produces a truthy value. If no such value is produced, return return
  nil. Wait `delay` milliseconds between evaluations."
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
  [g f]
  (fn [& args]
    (or (apply f args)
        (apply g args))))
