
## Choices of the current project

This project is still at an experiment stage. We hope to try different design and implementation choices. Below are the current priorities.

### R backends

In this project, we wish to create an abstration that can use more than one possible backend. Most of the code is backend-agnostic.

The first implementation will use Rserve, inspired by the successful experience of Rincanter forks and Rojure. 

In the near future, we plan to add Renjin support. There are several reasons for this to be potentially useful, eventhough Renjin's coverage of R packages is lacking. One is being pure-JVM. Another is that seems to allow a shorter route to embrace a larger part of the R language on the Clojure side, compared to Rserve. Rserve's API, for example, does not support concepts such as R environments, which are supported on Renjin, since it is a full implementation of R base language in the JVM. Another is potential performance improvements, at least in some cases.

Later, we may possibly try FastR. After some [basic exerperiments](https://github.com/scicloj/fastr-examples), this seems to be a more difficult story. As Clojure is not one of the [Truffle](https://github.com/oracle/graal/tree/master/truffle)-based languages of GraalVM, it is kind of a second-class citizen in terms of interop possibilities -- some data-conversion steps are required to communicate with other languages, and to support decent interaction with R's type system, some work will be required.

### Almost-zero-copy

We wish to be able to use R with the a little amount of data copying necessary. That is, applying an R function on an R object should not involve conversion of the whole data between Clojure (or Java) and R. The cases where conversion is considered acceptable are where we actually want to use both R and Clojure to process the data.

At the same time, we want a function-call-oriented API, that makes passing data from Clojure to R as transparent as possible (`get` and `set` would be implementation details, not the user's way of doing things).

Also, we wish to be able to use R through a separate-process service such as Rserve (for the robustness and versatility benefits that it brings).

To achieve all that, we choose to use the backend (Rserve through REngine, at the moment) in a somewhat atypical fashion.

In typical use of Rserve through REngine, one evaluates an R expresion and automatically gets the return value communicated and converted to a Java object (inheriting from `org.rosuda.REngine.REXP` (probbaly meaning "R s-EXPression), which is REngine's representation of any R object). We do not know that.

Instead, we make sure that our calls to R actually do not return a value (except from 'ok'), unless we want them to. Instead, the return value of the evaluated R expression is saved on the R side, and on the Clojure side we just keep a handle of it.

R functions are just a special case of R objects. However, for any handle of an R function, we can construct its Clojure counterpart, that applies that function to any given R arguments that we have handles of, and returns the handle of the return value.

### Resource management

Sometimes, Clojure handles of R objects are released by the garbage collector (typically, this happen some time after they have been involved in the evaluation of some Clojure expression, whose returning value (or any mutable state) does not hold to them). Of course, releasing them would be pointless if we do not release the corresponding R objects they refer to.

This is one of situations that the [tech.resource](LINK) library takes care of, in a simple and transparent way. This, taking care of this problem was nothing but joy. For more details, see this [blog post](http://techascent.com/blog/generalized-resource-management.html).

### The Java layer in between

At the moment, all the backends we consider, and in particular Rserve+REngine and Renjin, have a Java layer that represents R types, and our use of the backend passes through that layer. That is, when one evaluates R code, the data and the possible return values are communicated as Java datatypes of that layer.

All that could be considered as an implementation detail. If we wish our API to support different backends in a transparent way, then probably this kinds of details would rather be abstracted away. On the other hand, exposing this implementation detail as part of the API has some benefits of performance. Moreover, an object at the Java layer could poetentially be converted to different Clojure interpretations of that object (e.g., a named list could be converted to a list, as well as to an array-map), and could implement some protocols/interfaces, and thus be used from Clojure in a neat way.

This seems to justify exposing the Java layer at this experimental stage.

Thus, for example, conversion functions `clj->java`, `java->R`, `R->java`, `java->clj`, are part of the API, for now.

### Data abstrations

We plan to use mainly the data abstractions of the tech.datatype and `tech.ml.dataset` mentioned above, in addition to basic Java/Clojure notions such as Map, Sequence, etc.

We experiment with two main ways of using an abstraction layer in interop:
* Conversion. Converting between R objects to corresponding Clojure objects implementing the abstraction (e.g., converting an R data frame to a Clojure object implementing the dataset protocol of `tech.ml.dataset`).
* Protocol implementation. Having the (handles of) R objects directly implement the protocols (e.g., having the (handle of) an R data frame implement the dataset protocol of `tech.ml.dataset`).

Regarding conversion, at the moment we support converting of `tech.ml.dataset` datasets to/from R data frames. Soon we should be able to supprt also conversion of tech.datatype tensors to/from R matrices, and of `tech.ml.dataset` categorical columns to/from R factors.

In principle, all these could also be exposed through protocol implementation as well. This will allow one to use `tech.ml.dataset`'s data manipulation capabilities directly with (handles of) R objects, without copying anything to the Clojure side.

### Sessions

Supporting multiple R sessions is one of our goals.

It has to be simple, easy and quick to spawn new R sessions, discard them, and use them in parallel.

On the other hand, the session concept should be transparent if the user does not care about multiple sessions.

Each session can have its own backend. 

We achive that by defining a `Session` protocol, that each backend implementation would implement (e.g., `RserveSession`). We keep a catalogue of all the live sessions. The API functions allow for specifying the desired session to be used (e.g., "an Rserve session at port 4444"), and if that session does not exists, it is created. If a session is not specified, then everything should work with the default session. The default definition can be overridedn by the user.


