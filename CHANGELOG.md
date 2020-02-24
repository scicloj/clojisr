# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [1.0.0-BETA8-SNAPSHOT]
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
