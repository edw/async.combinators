# async.combinators

A Clojure asynchronous combinator library.

## Installation

To use this library, add the following to your Leiningen project's
`:dependencies` section:

![Clojars Project](http://clojars.org/edw/async.combinators/latest-version.svg)

## Usage

An example wherein a function that waits two seconds before printing
its argument is decorated with the `spawn` combinator, allowing it to
run asynchronously meanwhile the main thread of control waits for the
function to complete at which point a message is printed:

```clojure
(let [done? (atom false)
      f (fn [x]
          ((stall 2000 prn) x)
          (swap! done? (constantly true)))
      g (-> f spawn)]
  (g 42)
  ((upon done? identity prn) :done))
```

An example that returns true:

```clojure
((nilf (complement identity) identity) nil)
```

An example that tries moderately hard to make nil truthy, patiently waiting half a second between attempts. It does not end well:

```clojure
((retry 3 500 identity) nil)
```



## License

Copyright © 2014 Edwin Watkeys.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
