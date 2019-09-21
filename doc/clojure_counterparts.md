
## Clojure's counterparts of common R data abstractions

Several Clojure libraries have offered counterparts of common [R data abstractions](r_data_abstractions.md).

Here we'll mention a few of those libraries.

### Incanter
[Incanter](http://www.joelboehland.com/posts/all-your-datasets-r-belong-to-us.html) by D.E. Liebke (later maintained by A. Ott), was the first famous Clojure library for data science. It offers notions of datasets (like R data frames), matrices and categorical variables (like R factors). Data frames are called 'datasets' in Incanter.
Of course, it does more than that, willing to be Clojure's counterpart of R itself.

Rincanter, and its forks, supported Incanter's abstractions as the counterparts of R's dataframes.

Indenpendently of Incanter, some of Rincanter's forks also support a notion of categorical variables as a counterpart of R's factors.

### core.matrix
[core.matrix](https://github.com/mikera/core.matrix) by Mike Anderson is mainly a matrix library. It offers a set of abstractions that has several implementations.
In one of its late versions, it started supporting a notion of a [dataset](https://mikera.github.io/core.matrix/doc/clojure.core.matrix.dataset.html) that generalizes Incanter's notion. This was done in cooperation with Incanter's developers, so that Incanter's dataset implementation could be replaced by core.matrix, and thus be able to run on any core.matrix implementation.

As mention above, Rojure uses the dataset abstraction of core.matrix, and thus supports Incanter, too.

### Spork's Incanter forks and extensions
The Spork project by Joinr is a collection of libraries for data science and operations researck. Among other things, it continues the work of Incanter and offers its own notion of typed columnar table. See some comments [here](https://clojureverse.org/t/online-meeting-clojure-data-science/3503/35).


### Neanderthal and Denisovan
[Neanderthal](https://neanderthal.uncomplicate.org) by Dragan Djuric is one of the popular Clojure matrix libraries, and surely the most active and comprehensive one nowadays. It focuses on high-performance computation.

[Denisovan](https://github.com/cailuno/denisovan) is a partial implementation of core.matrix that uses Neanderthal as its engine.


### The Tech stack

The 'tech' collection of libraries by Chris Nuernberger offers a new set of abstractions relevant for data science work. One of the main ideas behind this stack is to [build bridges rather than islands](https://clojureverse.org/t/online-meeting-clojure-data-science/3503/17) - that is, the goal is not to create a specific toolset, but rather to create a platform that can connect to any other relevant toolset, thus enjoying the growth and development of any relevant ecosystem in the field.

Two relevant libraries of this stack are the following:
  * [tech.datatype](https://github.com/techascent/tech.datatype) library offers a set of abstractions for working with various sequential and array-like structures (including tensors of arbitrary dimension, and in particular, matrices).
  * [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) grew out of some discussion following the above-mentioned work of Spork, and other relevant experiences. It offers a 'dataset' abstraction (data frame, in R's language) of typed-columns tabular structires, that allows for different implementations. It has one specific comprehensive implementation based on the [Tablesaw](https://github.com/jtablesaw/tablesaw) java library. It also offers its own notion of categorical variables (similar to R's factors).


### Other libraries offering a data-frame-like notions

Here are some other Clojure libraries that offer some data-frame-like notions. Some of them are strongly inspired by the Python Pandas library.

* [dataframe](https://github.com/ghl3/dataframe) by George Lewis follows closely the main API functions Pandas.
- [koala](https://github.com/aria42/koala) by Aria Haghighi also implements pandas-like functions, and has some nice IO-support. 
- [wombat](https://github.com/ribelo/wombat) by Ribelo is inspired by Pandas, and offers a transducers-based API and implementation.
- [panthera](https://github.com/alanmarazzi/panthera) by Alan Marazzi actually wraps Pandas through [Libpython-clj](https://github.com/cnuernber/libpython-clj).
- [huri](https://github.com/sbelak/huri) by Simon Belak offers data-frame-like experience by processing simple sequences-of-maps.
- [kixi.stats](https://github.com/MastodonC/kixi.stats) by Henri Garner, Simon Belak and Elise Huard offers a large composable toolset of statistical functionality on sequences-of-maps, based on transducers.

