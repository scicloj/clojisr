## R backends

Here is a partial list of possible ways to use R from the JVM. 

* JNI-based, single-session [JRI](https://www.rforge.net/JRI/)
  * used in the original [Rincanter](https://github.com/jolby/rincanter)

* socket-based, multi-session [Rserve](https://github.com/s-u/Rserve); as mentioned above, more robust thanks to us of R in a separate process (or processes); the cost of perfomance vs JNI seems not too painful in typical use cases
  * used in [forks](https://github.com/svarcheg/rincanter) [of](https://github.com/skm-ice/rincanter) Rincanter, and in the subsequent [Rojure](https://github.com/behrica/rojure)

* [REngine](https://github.com/s-u/REngine) - a common abstration on the JVM side, that supports both JRI and Rserve
  * used in both Rincanter and Rojure

* http-based, multi session, secure, slow-for-large-datasets [Opencpu](https://www.opencpu.org)
  * used in [Opencpu-clj](https://github.com/behrica/opencpu-clj)

* shelling out - running a new R process every time
  * used in [gg4clj](https://github.com/JonyEpsilon/gg4clj) and in [huri](https://github.com/sbelak/huri)

* pure-JVM, multi-session [Renjin](https://www.renjin.org)

* a wrapper of several, such as [Rsession](https://github.com/yannrichet/rsession)

* GraalVM-based [FastR](https://www.rforge.net/Rserve/doc.html)

* possibly, a new JNA-based solution (along the lines of [Libpython-clj](https://github.com/cnuernber/libpython-clj))

