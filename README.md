# clojuress

Clojure speaks statistics - a library for Clojure-R interop

[This README is being intensivey edited these days.]

## Scope of the project

There are already stable libraries for Clojure-R interop -- see [this list](doc/existing_libraries.md).

This project suggests yet another way to use R from Clojure. At the moment, everything is alpha, and will keep changing.

Currently we target only JVM Clojure, but we are interested in generalizing the work to Clojurescript.

The related problem, of calling Cojure from R, may be addressed too in the future. We are experimenting with that.

## Why this name?

Clojure Speaks Statistics is a homage to [Emacs Speaks Statistics](https://ess.r-project.org).

## Goals

  * A Function-centric API, where the default mode of usage is calling R functions on R objects, from Clojure (Status: a draft exists, it will change)

  * "R code as Clojure data", using and extending the EDN-based syntax inroducted in [gg4clj](https://github.com/JonyEpsilon/gg4clj) and [used](https://github.com/sbelak/huri/blob/master/src/huri/plot.clj#L299) in [huri](https://github.com/sbelak/huri) (Status: currently experimenting with the original gg4clj syntax)

  * Interop with minimal copying of data (Status: supported)

  * Compatibility with common data abstractions such as [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) datasets (Status: partial support) 

  * Convenient wrappers for common use cases, such as visualization (Status: wrote a basic wrapper to Rmarkdown)
 
  * Abstraction over different runtimes (GNUR R, Renjin, FastR) (Status: only GNU R is supported to the momentnot there yet; planning to look into Renjin soon)

  * Convenient multi-session support (Status: a draft exists, needs some polish)


## Requirements

* Linux or MacOS

* [R](https://www.r-project.org)

* The [Rserve](https://cran.r-project.org/web/packages/Rserve/index.html) R package (`install.packages("Rserve")`)

* This project (currently changing)

[![Clojars Project](https://img.shields.io/clojars/v/scicloj/clojuress.svg)](https://clojars.org/scicloj/clojuress)

## Usage

See:

* [basic example](examples/basic_example.clj)

* [literate programming using rmarkdown](examples/rmarkdown_example.clj)

* [interactive data visualization using shiny](examples/shiny_example.clj)


## API Draft
For now, see the [basic example](examples/basic_example.clj) and the [tests](test/clojuress_test.clj) of the `clojuress` namespace.

The API may still change, hopefully after some good ideas in community discussions.

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

Please share your comments, thoughts, ideas and questions at the [Issues Page](https://github.com/scicloj/clojuress/issues) of this project and at the [r-interop streamm](https://clojurians.zulipchat.com/#narrow/stream/204621-r-interop) of the Clojurians Zulip.

## Tools used

Working on this project, we enjoyed the following tools (partial list):

* [hara.test](https://cljdoc.org/d/hara/test/3.0.7) for automated docstrings by tests -- see [a minimal usage example](https://github.com/scicloj/hara-test-example)

* [clj-kondo](https://github.com/borkdude/clj-kondo) for code quality control


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
