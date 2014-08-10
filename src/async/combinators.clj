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
      (let [start (System/currentTimeMillis)]
        (let [result (apply f args)
              embargo (- (+ start min) (System/currentTimeMillis))]
          (when (pos? embargo) (a/<!! (a/timeout embargo)))
          result)))))

