(ns clojisr.v1.known-classes)

(def function-classes
  #{["function"]
    ;; s4
    ["standardGeneric"]
    ["nonstandardGenericFunction"]
    ;; Functions created by the reticulate package:
    ["python.builtin.function" "python.builtin.object"]
    ["python.builtin.builtin_function_or_method" "python.builtin.object"]})

