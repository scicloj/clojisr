## Future opportunities

Here are some possible future developments we are considering.

### Generalizing to clojurescript

We currently target JVM Clojure and rely on Java libraries (mainly REngine, as the Java abstration over Rserve).

In principle, most of what we do could be generalized to run on Clojurescript (either on the browser, or on Node.js). For a backend, one could use the [Javascript client](https://github.com/cscheid/rserve-js) of Rserve. 

Note, however, that at the moment it does not seem to have an abstraction layer such as that of REngine. Thus, to achieve a detailed solution in terms of the R typesystem in Clojurescriipt, one would have to write from scratch a Clojure/Clojurescript layer representing the basic datatypes of the base R language.

This actually has the potential of achieving more intimate understanding control of the whole situation. In our current version, we expose the intermediate Java layer between Clojure and R as part of the API, as it seems to allow for more control in situations where performance is an issue. Replacing that with a well-thought Clojure layer would simplify the situation on botth the developer's and the user's side.

All that chould be a fun future project.

