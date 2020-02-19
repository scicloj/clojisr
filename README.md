# ClojisR

Clojure speaks statistics - a [jisr](https://en.wiktionary.org/wiki/جسر) between Clojure and R

[![Clojars Project](https://img.shields.io/clojars/v/scicloj/clojisr.svg)](https://clojars.org/scicloj/clojisr)

## How to pronounce it?

The beginning of the pronunciation is the same as Clojure, but then it rhymes with 'kisser'. Actually, the last vowel is nonexistent, so you may try to pronounce it with less movement between s and r, like 'yesssr!'.

## Status

Not tested in production, still evolving.

## Scope of the project

There are already stable libraries for Clojure-R interop - see [this list](doc/existing_libraries.md).

This project suggests yet another way to use R from Clojure.

Currently we target only JVM Clojure, but we are interested in generalizing the work to Clojurescript.

The related problem, of calling Clojure from R, may be addressed too in the future. We are experimenting with that.

## Video presentations

The main ideas were discussed at [Scicloj Web meeting #7](https://www.youtube.com/watch?v=XoVX2Ezi_YM) and [ClojuTRE 2019](https://www.youtube.com/watch?v=A55jO02ZKcg).

Note however that:
- The API has changed since then (mainly data visualization support, clear code generation rules, different printing - see the Intro below).
- On the meeting, there is some careless use of the term 'zero copy'. Actually, what is usually meant by this term is not supported at the moment.

## Meta Goals

  * Realize what is essential for Clojure to become a beginner-friendly solution for data science.
  
  * Expose the Clojure ecosystem to a different culture and to more diverse groups of users/programmers.

## Technical Goals

  * A Function-centric API, where the default mode of usage is calling R functions on R objects, from Clojure (Status: supported)

  * "R code as Clojure data", inspired by but extending the EDN-based syntax inroducted in [gg4clj](https://github.com/JonyEpsilon/gg4clj) and [used](https://github.com/sbelak/huri/blob/master/src/huri/plot.clj#L299) in [huri](https://github.com/sbelak/huri) (Status: supported)

  * Interop with minimal copying of data (Status: supported)

  * Compatibility with common data abstractions such as [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) datasets (Status: partial support) 

  * Convenient wrappers for common use cases, such as visualization (Status: wrote a basic wrapper to Rmarkdown)
 
  * Abstraction over different runtimes (GNUR R, Renjin, FastR) (Status: GNU R is supported through backend; Renjin has some basic support)

  * Convenient multi-session support (Status: a draft exists, needs some polish)

## Usage requirements

* Linux or MacOS

* JDK 1.8 or later

* Clojure 1.9.0 or later

* [R](https://www.r-project.org)

* The [Rserve](https://cran.r-project.org/web/packages/Rserve/index.html) R package (`install.packages("Rserve")`)
Tested with Rserve version 1.8.6. Earlier versions are [known](https://stackoverflow.com/questions/50410289/running-r-script-from-java-rconnection-eval-exception/50622263#50622263) to have a bug. Rserve 1.8.6 is currently not on CRAN, but can be installed with `install.packages("Rserve",,"http://rforge.net")`

* This library

[![Clojars Project](https://img.shields.io/clojars/v/scicloj/clojisr.svg)](https://clojars.org/scicloj/clojisr)

## Checking if it works

This should work for you (assuming you have the [clj tool](https://clojure.org/guides/getting_started)):

```clj
$ clj -Sdeps '{:deps {scicloj/clojisr {:mvn/version "1.0.0-BETA7"}}}}'
Clojure 1.10.1
user=> (require '[clojisr.v1.r :refer [r]])

user=> (r '[+ 1 2])
[1] 3
```

## Tutorials

* [Intro](https://scicloj.github.io/clojisr/resources/public/clojisr/v1/tutorial-test/index.html)

* [Renjin backend](https://scicloj.github.io/clojisr/resources/public/clojisr/v1/renjin-test/index.html)

* [Titanic tutorial #0](https://scicloj.github.io/clojisr/resources/public/clojisr/v1/titanic0-test/index.html)

* More examples -- see the [clojisr-examples repo](https://github.com/scicloj/clojisr-examples#list-of-examples)

## Background

1. [Lisp for statistical computing](doc/lisp_for_stats.md)

2. [Calling R from Clojure: existing libraries](doc/existing_libraries.md)

3. [R backends](doc/r_backends.md)

4. [Some of R's data abstractions](doc/r_data_abstractions.md)

5. [Clojure's counterparts of R's data abstractions](doc/clojure_counterparts.md)

## Choices of the current project

[Here](doc/choices.md) are the current priorities of the project in some central design and implementation questions.


## Future opportunities

[Here](doc/future.md) are some possible future developments we are considering.


## Discussion

Please share your comments, thoughts, ideas and questions at the [Issues Page](https://github.com/scicloj/clojisr/issues) of this project and at the [r-interop stream](https://clojurians.zulipchat.com/#narrow/stream/204621-r-interop) of the Clojurians Zulip.

## Tools used

Working on this project, we enjoyed the following tools (partial list):

* In early versions, [hara.test](https://cljdoc.org/d/hara/test/3.0.7) was used for automated docstrings by tests. We may come back to using it.

* [clj-kondo](https://github.com/borkdude/clj-kondo) for code quality control

* [notespace](https://github.com/scicloj/notespace) for documentation and tests

## License

Copyright © 2019 Scicloj 

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
