## Check that we are running with the correct working directory.
if (!(file.exists("project.clj") &&
      grepl("clojuress", readLines("project.clj")[[1]]))) {
    stop("It Seems that you are running at the wrong location. Your working directory should be the one of the clojuress project.")
}

## Install packages if necessary
package_installed <- function(package) {
    package %in% row.names(installed.packages())
}
if(!package_installed("rJava")) {
    install.packages("rJava")
}
if(!package_installed("rmarkdown")) {
    install.packages("rmarkdown")
}
if(!package_installed("renjin")) {
    ## Renjin, as an R package, is installed from source:
    ## http://docs.renjin.org/en/latest/package/index.html#installation
    source("http://packages.renjin.org/install.R")
}

## Create clojuress jar
system("lein uberjar")

## Render demo
rmarkdown::render("doc/calling-clojure-from-R.Rmd",
                  output_format = "md_document" ,
                  output_file = "calling-clojure-from-R.md")

## Done
"Done"
