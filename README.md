# clojuress

Clojure speaks statistics - a library for Clojure-R interop

# Scope of the project

At the moment, this project suggests an experimental way to use R from Clojure. Everything is alpha, and will keep changing.

There are already stable solutions for Clojure-R interop, that you may wish to look into: [Rincanter](http://www.joelboehland.com/posts/all-your-datasets-r-belong-to-us.html), [its](https://github.com/svarcheg/rincanter) [forks](https://github.com/skm-ice/rincanter), or [Rojure](https://groups.google.com/forum/#!topic/numerical-clojure/fQSJiL8QfB0). 

Currently we target only JVM Clojure, but we are interested in generalizing the work to Clojurescript.

The related problem, of calling Cojure from R, may be addressed too in the future. We are experimenting with that.


# Goals

  * experiment with function-centric API 
    * status: a draft exists, it will change

  * experiment with syntax -- e.g., using and extending the EDN-based syntax inroducted in [gg4clj](https://github.com/JonyEpsilon/gg4clj) and [used](https://github.com/sbelak/huri/blob/master/src/huri/plot.clj#L299) in [huri](https://github.com/sbelak/huri)
    * status: not here yet

  * support interop with minimal copying of data
    * status: supported

  * achieve compatibility with common data abstractions such as [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset)
    * status: partial support

  * create convenient wrappers for common use cases -- e.g., data visualization
    * status: planning to add something soon
 
  * abstract over additional runtimes (Renjin, FastR)
    * status: not there yet; planning to look into Renjin in the near future

  * provide some convenience around multi-session support
    * status: a draft exists, needs some polish

# Usage
[Coming soon]

# Examples
[Coming soon]

# API Draft
For now, see the [tests](test/clojuress_test.clj) of the `clojuress` namespace.

# Background

1. [Lisp for statistical computing](doc/lisp_for_stats.md)

2. [Calling R from Clojure: existing libraries](doc/existing_libraries.md)

3. [R backends](doc/r_backends.md)

4. [Some of R's data abstractions](doc/r_data_absractions.md)

5. [Clojure's counterparts of R's data abstractions](doc/clojure_counterparts.md)

# Choices of the current project

This project is still at an experiment stage. We hope to try different design and implementation choices. Below are the current priorities.

## R backends

In this project, we wish to create an abstration that can use more than one possible backend. Most of the code is backend-agnostic.

The first implementation will use Rserve, inspired by the successful experience of Rincanter forks and Rojure. 

In the near future, we plan to add Renjin support. There are several reasons for this to be potentially useful, eventhough Renjin's coverage of R packages is lacking. One is being pure-JVM. Another is that seems to allow a shorter route to embrace a larger part of the R language on the Clojure side, compared to Rserve. Rserve's API, for example, does not support concepts such as R environments, which are supported on Renjin, since it is a full implementation of R base language in the JVM. Another is potential performance improvements, at least in some cases.

Later, we may possibly try FastR. After some [basic exerperiments](https://github.com/scicloj/fastr-examples), this seems to be a more difficult story. As Clojure is not one of the [Truffle](https://github.com/oracle/graal/tree/master/truffle)-based languages of GraalVM, it is kind of a second-class citizen in terms of interop possibilities -- some data-conversion steps are required to communicate with other languages, and to support decent interaction with R's type system, some work will be required.

## Almost-zero-copy

We wish to be able to use R with the a little amount of data copying necessary. That is, applying an R function on an R object should not involve conversion of the whole data between Clojure (or Java) and R. The cases where conversion is considered acceptable are where we actually want to use both R and Clojure to process the data.

At the same time, we want a function-call-oriented API, that makes passing data from Clojure to R as transparent as possible (`get` and `set` would be implementation details, not the user's way of doing things).

Also, we wish to be able to use R through a separate-process service such as Rserve (for the robustness and versatility benefits that it brings).

To achieve all that, we choose to use the backend (Rserve through REngine, at the moment) in a somewhat atypical fashion.

In typical use of Rserve through REngine, one evaluates an R expresion and automatically gets the return value communicated and converted to a Java object (inheriting from `org.rosuda.REngine.REXP` (probbaly meaning "R s-EXPression), which is REngine's representation of any R object). We do not know that.

Instead, we make sure that our calls to R actually do not return a value (except from 'ok'), unless we want them to. Instead, the return value of the evaluated R expression is saved on the R side, and on the Clojure side we just keep a handle of it.

R functions are just a special case of R objects. However, for any handle of an R function, we can construct its Clojure counterpart, that applies that function to any given R arguments that we have handles of, and returns the handle of the return value.

## Resource management

Sometimes, Clojure handles of R objects are released by the garbage collector (typically, this happen some time after they have been involved in the evaluation of some Clojure expression, whose returning value (or any mutable state) does not hold to them). Of course, releasing them would be pointless if we do not release the corresponding R objects they refer to.

This is one of situations that the [tech.resource](LINK) library takes care of, in a simple and transparent way. This, taking care of this problem was nothing but joy. For more details, see this [blog post](http://techascent.com/blog/generalized-resource-management.html).

## The Java layer in between

## Data abstrations

We plan to use mainly the data abstractions of the tech.datatype and `tech.ml.dataset` mentioned above, in addition to basic Java/Clojure notions such as Map, Sequence, etc.

We experiment with two main ways of using an abstraction layer in interop:
* Conversion. Converting between R objects to corresponding Clojure objects implementing the abstraction (e.g., converting an R data frame to a Clojure object implementing the dataset protocol of `tech.ml.dataset`).
* Protocol implementation. Having the (handles of) R objects directly implement the protocols (e.g., having the (handle of) an R data frame implement the dataset protocol of `tech.ml.dataset`).

Regarding conversion, at the moment we support converting of `tech.ml.dataset` datasets to/from R data frames. Soon we should be able to supprt also conversion of tech.datatype tensors to/from R matrices, and of `tech.ml.dataset` categorical columns to/from R factors.

In principle, all these could also be exposed through protocol implementation as well. This will allow one to use `tech.ml.dataset`'s data manipulation capabilities directly with (handles of) R objects, without copying anything to the Clojure side.

# Future opportunities

## Generalizing to clojurescript

We currently target JVM Clojure and rely on Java libraries (mainly REngine, as the Java abstration over Rserve).

In principle, most of what we do could be generalized to run on Clojurescript (either on the browser, or on Node.js). For a backend, one could use the [Javascript client](https://github.com/cscheid/rserve-js) of Rserve. 

Note, however, that at the moment it does not seem to have an abstraction layer such as that of REngine. Thus, to achieve a detailed solution in terms of the R typesystem in Clojurescriipt, one would have to write from scratch a Clojure/Clojurescript layer representing the basic datatypes of the base R language.

This actually has the potential of achieving more intimate understanding control of the whole situation. In our current version, we expose the intermediate Java layer between Clojure and R as part of the API, as it seems to allow for more control in situations where performance is an issue. Replacing that with a well-thought Clojure layer would simplify the situation on botth the developer's and the user's side.

All that chould be a fun future project.

# Discussion

Please share your comments, thoughts, ideas and questions at the [Issues Page](https://github.com/scicloj/clojuress/issues) of this project and at the [r-interop streamm](https://clojurians.zulipchat.com/#narrow/stream/204621-r-interop) of the Clojurians Zulip.


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
