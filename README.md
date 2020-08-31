# ClojisR

Clojure speaks statistics - a [jisr](https://en.wiktionary.org/wiki/جسر) between Clojure and R

[![Clojars Project](https://img.shields.io/clojars/v/scicloj/clojisr.svg)](https://clojars.org/scicloj/clojisr)

## How to pronounce it?

The beginning of the pronunciation is the same as Clojure, but then it rhymes with 'kisser'. Actually, the last vowel is nonexistent, so you may try to pronounce it with less movement between s and r, like 'yesssr!'.

## Status

- still evolving
- not recommended for production yet
- already used by several people in their data science explorations

## Scope of the project

Libraries for Clojure-R interop are not new - see [this list](doc/existing_libraries.md).

This project suggests yet another way to use R from Clojure.

Currently we target only JVM Clojure, but we are interested in generalizing the work to Clojurescript.

The related problem, of calling Clojure from R, may be addressed too in the future. We are experimenting with that.

## Meta Goals

  * Realize what is essential for Clojure to become a beginner-friendly solution for data science.
  
  * Expose the Clojure ecosystem to a different culture and to more diverse groups of users/programmers.

## Technical Goals

  * A Function-centric API, where the default mode of usage is calling R functions on R objects, from Clojure (Status: supported)

  * "R code as Clojure data", inspired by the EDN-based syntax inroducted in [gg4clj](https://github.com/JonyEpsilon/gg4clj) and [used](https://github.com/sbelak/huri/blob/master/src/huri/plot.clj#L299) in [huri](https://github.com/sbelak/huri) (Status: supported)

  * Interop with minimal copying of data (Status: supported)

  * Compatibility with common data abstractions such as [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) datasets (Status: partially supported) 

  * Convenient wrappers for common use cases, such as visualization (Status: basic support for plots and Rmarkdown)
 
  * Abstraction over different runtimes (GNUR R, Renjin, FastR) (Status: GNU R is supported through Rserve; Renjin has some basic support)

  * Convenient multi-session support (Status: basic support with some known issues)

## Usage requirements

* Linux, MacOS or WSL (Windows Subsystem for Linux)
* JDK 1.8 or later
* Clojure 1.9.0 or later
* [R](https://www.r-project.org)
* The [Rserve](https://cran.r-project.org/web/packages/Rserve/index.html) R package (`install.packages("Rserve",,"http://rforge.net")`)
Tested with Rserve version 1.8.6. Earlier versions are [known](https://stackoverflow.com/questions/50410289/running-r-script-from-java-rconnection-eval-exception/50622263#50622263) to have a bug.
* This library: [![Clojars Project](https://img.shields.io/clojars/v/scicloj/clojisr.svg)](https://clojars.org/scicloj/clojisr)

### MacOS installation

Installing R with Rserve on MacOS can be problematic due to issues related to openssl installation. Please apply following steps (thanks to @ezmiller):

1. Download the lastest R for mac from here: https://cloud.r-project.org/
2. Install openssl: `brew install openssl`.
3. Make sure that the `openssl` library is linked. Try in order:
   * `brew link --force openssl`
   * If that doesn't work, follow directions in `brew info openssl` for setting environment variables. Set the `LIBRARY_PATH` environment variable to the location of the library, e.g. `export LIBRARY_PATH=/usr/local/opt/openssl@1.1/lib`.

### Setting up the logging

* `clojisr` library uses [clojure/tools.logging](https://github.com/clojure/tools.logging) for logging. `tools.logging` doesn't force any logging backend and users have to configure it on their side. To force specific backend you can set it using JVM options, for example in lein `profile.clj` or `project.clj`:

```clj
:jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/jul-factory"]
```

### Docker image

Thanks to Carsten Behring we have a Docker template prepared

https://github.com/behrica/clj-py-r-template

The Dockerfile of the template adds as well python + libpython-clj for completeness.

So it has in a single place all dependencies and they do work together and no further setup is required.

* R: 4.0.2
* RServe: latest from rforge.net
* python: 3.8.5
* java:  openjdk 11
* clojure: 1.10.1
* clj-python/libpython-clj 1.45
* scicloj/clojisr 1.0.0-BETA15
* cider-nrepl 0.25.2

## Checking if it works

This should work for you (assuming you have the [clj tool](https://clojure.org/guides/getting_started)):

```clj
$ clj -Sdeps '{:deps {scicloj/clojisr {:mvn/version "1.0.0-BETA15"}}
               :mvn/repos {"bedatadriven" {:url "https://nexus.bedatadriven.com/content/groups/public/"}}}'
Clojure 1.10.1
user=> (require '[clojisr.v1.r :refer [r]])

user=> (r '(+ 1 2))
[1] 3
```

## Known issues

* clojisr can behave in a strange way when abandoned R (with Rserve) processes are running. Please kill such processes before creating an Rserve session.
* Nextjournal can hang due to problems with logging, please add ` org.slf4j/slf4j-nop {:mvn/version "1.7.30"}` to the deps to disable logger.

## Video presentations

The main ideas were discussed at [Scicloj Web meeting #7](https://www.youtube.com/watch?v=XoVX2Ezi_YM) and [ClojuTRE 2019](https://www.youtube.com/watch?v=A55jO02ZKcg).

Note however that:
- The API has changed since then (code generation mechanism, data visualization support, printing - see the [Tutorials](#Tutorials) below).
- On the meeting, there is some careless use of the term 'zero copy'. Actually, what is usually meant by this term is not supported at the moment.

## Tutorials

* [Intro](https://scicloj.github.io/clojisr/doc/clojisr/v1/tutorial-test/)

* [A tutorial about generating R code from Clojure](https://scicloj.github.io/clojisr/doc/clojisr/v1/codegen-test/)

* [Renjin backend](https://scicloj.github.io/clojisr/doc/clojisr/v1/renjin-test/)

* [Titanic tutorial #0](https://scicloj.github.io/clojisr/doc/clojisr/v1/titanic0-test/)

* [Dataset transfer](https://scicloj.github.io/clojisr/doc/clojisr/v1/dataset-test/)

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

Also we run [a stream](https://clojurians.zulipchat.com/#narrow/stream/224816-clojisr-dev) for developers or people interested in contributing.

## Testing

The code tests are embedded in the [tutorials](#tutorials) and are run when they are rendered (using [notespace](https://github.com/scicloj/notespace)). This way we make sure that tests and documentation are in sync.

## Tools used

Working on this project, we enjoyed the following tools (partial list):

* In early versions, [hara.test](https://cljdoc.org/d/hara/test/3.0.7) was used for automated docstrings by tests. We may come back to using it.

* [clj-kondo](https://github.com/borkdude/clj-kondo) for code quality control

* [notespace](https://github.com/scicloj/notespace) for documentation and tests

## License

Copyright © 2019-2020 Scicloj 

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
