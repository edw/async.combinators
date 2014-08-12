# async.combinators

I am often writing a procedure to implement a function and realize
before long that my elegant poem of a procedure has become something
akin to an assembly manual for discount fiberboard furniture
translated through three languages. The source of this entropy more
often than not is the in-creeping of code to control not _what_ value
I want to produce but _how_ the value should be produced.

For example, you may want to map a million URLs to objects
representing that results of performing a GET request on them. But you
live in the real world, so you don't want to naively initiate a
million asynchronous tasks, because your pipe is only so wide and the
machine(s) you're pounding will tolerate only so much abuse, so you
want to limit the concurrency. Oh, and the Internet, it's not
reliable, so you want to re-try each request a few times before giving
up. And you do want to give up eventually, because not all of those
URLs are going to be valid--or point to data.

And on and on. Pretty soon what was a single line of code has become a
stomach-turning mess. Wouldn't it be nice if someone wrote a
combinator library that allows you to apply various useful behaviors
to a function, letting you concentrate on writing elegant code-haiku
you can send to your parents when they ask, "What exactly do you _do_
for a living?"

I aspire to write such a library for you. And myself, of course.

### Note

I distinguish between the terms _procedure_ and _function_ as if I
were a pedantic Scheme programmer. There's an essay ruminating to be
written on how these two terms may productively be applied to Clojure.

## Installation

To use this library, add the following to your Leiningen project's
`:dependencies` section:

![Clojars Project](http://clojars.org/edw/async.combinators/latest-version.svg)

## Usage

[Full API documentation](http://edw.github.io/async.combinators) is available.

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
