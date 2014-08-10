# async.combinators

A Clojure asynchronous combinator library.

## Installation

To use this library, add the following to your Leiningen project's
`:dependencies` section:

![Clojars Project](http://clojars.org/edw/async.combinators/latest-version.svg)

## Usage

```clojure
(require '[async.combinators :as ac])

(defn my-func ...)

(def counter (atom 0))

(def f (-> my-func
           (pinch 10)
           (stall 500)
           (tally counter)
           spawn))

(doseq [x my-sequence] (f x))

;; Wait for counter to hit (count my-sequence)

```

## License

Copyright © 2014 Edwin Watkeys.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
