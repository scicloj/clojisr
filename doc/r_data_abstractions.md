## Some of R's data abstractions: data frames, matrices, caegorical variables

There are several data abstrations which are central to R use. When thinking about calling R, one has to think whether these abstractions should be part of the story, and in what way.

Here, let us discuss three central notions: data frames, matrices and factors.

### data frames
R's "data frames" are, more or less, tables. They have columns and rows. Columns are fixed-type vectors, that have names. Rows sometimes have names too.
In the Clojure world, this notion is usually called 'dataset'. Internally, data frames are represented as lists of columns.

Many R packages (that is, libraries) rely on this notion in many ways. For example, they typically support handling expression whose symbols are column names of a given data frame, and whose evaluation results in respective computations of the corresponding column vectors.

Certain R packages offer slightly alternative notions, as well as different APIs and implementations. Most notable are the [data tables](https://cran.r-project.org/web/packages/data.table) and [tibbles](https://cran.r-project.org/web/packages/tibble/).

### matrices
Matrices are rectangular arrays of fixed-type. 

R's support of matrices continues the long tradition of [array programming](https://en.wikipedia.org/wiki/Array_programming).

### factors
Factors are vectors whose elements come from a fixed given set of string values. They are used typically in the context of categorical ("nominal") variables in statistics.

Many R packages respect that notion and work well with it.

