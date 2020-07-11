# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [1.0.0-BETA14] - 2020-07-11
- `clojisr.v1.rserve` and `clojisr.v1.renjin` namespaces are deprecated now
- setting default session type moved to `clojisr.v1.r/set-default-session-type!` function
- refactoring of RServe process spawn and connection to mitigate (#62)
- abandoned sessions/R processes cleaning fixes
- `tech.ml/dataset` bumped to 3.01 version

## [1.0.0-BETA13] - 2020-07-09
- regresion with `quote` (#61), changed to use `:!wrap` hint
- datetime conversion to string fixes
- `tech.ml.dataset` bumped to 2.15 version
- (experimental) turned off `pomegranate` dynamic dep loading for `renjin`

## [1.0.0-BETA12] - 2020-07-08

Note: `quote` function doesn't work properly

- refactored data and dataset transition between clojure and R backend as described [here](https://github.com/scicloj/clojisr/wiki/R----Dataset) (#14)
- `tech.ml.dataset` bumped to 2.13 version
- removed `java->naive-clj` and added `java->native-clj`
- `require-r` is moved to `clojisr.v1.r` (direct call to `clojisr.v1.require/require-r` is possible but deprecated) 
- reflection warnings mitigation
- integer transition (#73)
- ##Inf/##-Inf transition (#72)
- wrapping into `()` and `{}` (#61)
- unary operator wraps next form into parentheses (#59)
- missing path is created when plotting (#56)
- operators wrapped into % (like `%chin%`) are treated as binary operators (#55)

## [1.0.0-BETA11] - 2020-05-20
- updates in exception handling
- updated tech.datatype, tech.ml.dataset dependency versions with some small code adaptation ([#64](https://github.com/scicloj/clojisr/issues/64))
- notespace is no longer a dependency
- `rsymbol` introduced
- moved to `lein-tools-deps` and all deps are defined in `deps.edn` now

## [1.0.0-BETA10] - 2020-03-29
- new forms added: `if`, `do`, `for` and `while`
- more binary operators: `%%`, `%/%`, `**` and `%in%`
- sequences beginning with command as backticked string are now callable (eg. '("`^`" 2 4))
- vector starting with `:!code` is callable
- JVM shutdown hook is registered to clean all R processes which are run when closing java process
 `bra`, `brabra`, `bra<-`, `brabra<-` can accept `nil` as `empty-symbol` now.
- `data` function introduced (to load data from package)
- removed `empty-symbol` (use `nil` in brackets) and `na` (use just 'NA)
- picking a random free port as the default Rserve port 
- updating notespace dep version
- updating tech.ml.dataset dep version
-
## [1.0.0-BETA9] - 2020-03-21
- changed logging to `clojure/tools.logging`
- major breaking change: a new set of code-generation rules

## [1.0.0-BETA8] - 2020-02-24
- bugfix for plotting (to draw a chart `print` should be called instead of `plot`)
- tech.ml libraries bumped
- deps updated
- bugfix: recognizing S4 functions ("standardGeneric")
- bugfix: handling session-args carefully on r->clj data conversion
- Clojure working directory is now set on R side
- print-loop bound to session (#42)
- RConnection locking revisited (#40)
- GC rewritten (#25)
- More careful session management (all atoms are now `defonce`, `closed?` depends on processes state)
- fixed rmarkdown bug (wrong usage of gc)

## [1.0.0-BETA7] - 2020-02-16
- require-r:
  - :arglists tag added for required R functions
  - warning when package is not found
- bugfix for problems when dynamically loading renjin jar:
  - using pomegranate directly, rather than alembic (to suppoert Java newer than Java 8)
  - handling classloaders carefully
- a new API for data visualization, with more rendering options

## [1.0.0-BETA6] - 2020-02-08
- bugfix of a problem with using Alembic (making sure https is used)
- change in the setup API of backends: requiring the main namesapce of a backend (renjin/rserve) sets default session-type, if no other default has been set yet; 

## [1.0.0-BETA5] - 2020-02-07
- `require-r` can `:refer :all` now
- REXPList, REXPNull are supported in data conversion
- changed the API for setting up and switching backends ("session-types")
- also fixed #27 by loading the renjin Jar on-the-fly with the proper repository into

## [1.0.0-BETA4] - 2020-02-05
The main changes on the user side are the fact that R functions are Clojure functions, and the initial suppoort for a Renjin backend. Details below:
- performance improvement: using an R environment rather than an R list for session memory
- some refactoring to allow other kinds of R backends, in particular exending the Session protocol to take care of details such as printing and package in spection
- more careful id management of sessions
- checking the object class on creation of R objects
- being careful when printing objects of lost sessions
- R objects now extend the IFn inerface, so that R functions are Clojure functions
- bugfix in regreshing objects across sessions
- simplified the implementation of `require-r` -- functions can now be handled as regular objects
- handling of strange symbols/keywords: refactored a bit, added some filtering
- initial support of a Renjin (pure-JVM R) backend
- handling failures in `require-r`
- changes in logging
  
## [1.0.0-BETA3] - 2020-01-28
- handling unhandled cases in java->naive-clj conversion
- loading data, not only functions, at `require-r`
- `require-r` optimization: all underlying robjects are created once, then bound to namespaces
- more careful handling of data conversion of list names: names with reader special characters are not converted to keywords

## [1.0.0-BETA2] - 2020-01-21
- added '$ as binary operator
- performance bugfix: using native double array and type hint when conferting integer R vectors with missing values to clj
- recognizing reticulate python functions as functions
- bugfix in overriding default session args

## [1.0.0-BETA1] - 2020-01-07
Initial v1 version.

Main developments since earlier versions:
- code generation
- changes in error handling, printing, etc.
- requiring of R packages inspired by the python interop story (though less sophisticated)
- function call syntax a bit different (no detailed function signatures yet)
- documentation and tests using [notespace](https://github.com/scicloj/notespace)

## [0.0.1-SNAPSHOT] - Sep. 2019 - Dec. 2019
Initial draft version
