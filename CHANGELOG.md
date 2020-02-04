# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [1.0.0-BETA3] - 2020-01-28
- handling unhandled cases in java->naive-clj conversion
- loading data, not only functions, at require-r
- require-r optimization: all underlying robjects are created once, then bound to namespaces
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
