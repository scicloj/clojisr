## Calling R from Clojure: existing libraries

All of the following projects are quite useful and have inspired the work here.

They differ in in several aspects: (a) the API and syntactic sugar offered; (b) the kind of R backend used: JRI+REngine / Rserve+REngine / Opencpu / just running R from shell (see [R backends](r_backends.md)); (c) whether some Clojure notions are used as the equivalent of R's "data frames" and "matrices", and if so, what notions: Incanter / core.matrix (see [Clojure's counterparts of R's data abstractions](clojure_counterparts.md)).


* [Rincanter](https://github.com/jolby/rincanter) was [announced](http://www.joelboehland.com/posts/all-your-datasets-r-belong-to-us.html) by Joel Boehland allows in early 2010. It was based on JRI (R from Java using JNI) as the R backend, and for data-abstractions used Incanter, an R-inspired library for Clojure, which was popular back then.
 
* This [Rincanter fork](https://github.com/svarcheg/rincanter) by Vladimir Kadychevski changed the R backend to Rserve, through the REngine layer on the Java side, which supports both Rserve and JRI as backends. This change allowed for more robust and production-friendly R-interop, as R runs on a separate process.

* This [continuing fork](https://github.com/skm-ice/rincanter) by the skm-ice group kept working on the API.

* [Rojure](https://github.com/behrica/rojure), [presented](https://groups.google.com/forum/#!topic/numerical-clojure/fQSJiL8QfB0) by Carsten Behring, changed the data abstraction from Incanter to core.matrix. At those days, core.matrix was becoming a standard data abstraction layer used by several libraries, evetually including Incanter itself. This allowed Rojure to be completely independent of Incanter, but still be used from Incanter.

* [clj-jri](https://github.com/fanannan/clj-jri) by SAWADA Takahiro / Gugen Koubou LLC is another, simple, wrapper of R through JRI. It does not support a data-frame-like structure on the Clojure side.

* [rashinban](https://github.com/tnoda/rashinban) by Takahiro Noda is another simple Clojure library for calling R through Rserve, with a clean and simple API, based on wrapping R functions with Clojure functions. It does not support a data-frame-like notion either.

* [Opencpu-clj](https://github.com/behrica/opencpu-clj) is another, earlier project by Carsten Behring. It uses Opencpu as the R backend. 
  * Some experiment to generalize to Clojuresctipt has been done [here](https://www.reddit.com/r/Clojure/comments/8zn0zk/1_some_experiments_in_calling_r_from/) (currently broken).

* [gg4clj](https://github.com/JonyEpsilon/gg4clj) by Jony Hudson [appeared](https://grokbase.com/t/gg/clojure/14ct8ahd1e/ann-gg4clj-0-1-0-ggplot2-in-clojure-and-gorilla-repl) shortly after Opencpu-clj. Its main purpose is wrapping R's famouse "Grammer of Graphics" ggplot2 library (but it actually introduces some ideas that are applicable for more general use). It does so by running any R computation as a brand new process. One interesting innovation of this libraaary is an EDN-like syntax that translates to R. We actually use gg4clj for code generation inside this project.

* [huri](https://github.com/sbelak/huri) by Simon Belak is a general library for data science, that does a lot more than calling R. One of its components is a collection of functions for data visualization, that build upon the way gg4clj offers, and add simple, composable ways to create ggplot2 plots in Clojure.

* [graalvm-interop](https://github.com/davidpham87/graalvm-rinterop) is an **active** project by David Pham, that allows to interoperate with [FastR](https://github.com/oracle/fastr) at [GraalVM](https://www.graalvm.org).

