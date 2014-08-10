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
  "Ensure that evaluation of `f takes at least `min` milliseconds."
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
