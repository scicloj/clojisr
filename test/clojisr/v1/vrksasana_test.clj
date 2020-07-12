(ns clojisr.v1.vrksasana-test
  (:require [notespace.v2.note :as note
             :refer [note note-md note-void]]
            [notespace.v2.live-reload]))

(note-md "# Session management: a suggested method using Vrksasana")

(note-md :intro "## Intro")

(note-md "
This is an attempt to achieve two goals:
- introduce a new method of session-management to Clojisr, where multiple sessions can be used safely and objects can be refreshed across sessions
- generalize this method, so that it can be used in other contexts (not just R interop)

To achive this, we temporarily add the namespaces `vrksasana.*` under the Clojisr code base. This set of namespaces presents some general-purpose protocols and functionality, as well as a specific implementation for R. The implementation relies on some parts of the Clojisr internals.


If we decide to go with this approach, then we can change the implementation of the main Clojisr API, to use Vrksasana. Vrksasana will thus come as a layer between Clojisr internals and Clojisr API.

Then, some existing parts of Clojisr will become redundant.

We use the name [Vrksasana](https://en.wikipedia.org/wiki/Vriksasana), beause it is about stability and ease with trees.")

(note-md :basic-demo "## Basic demo")

(note-void (require '[notespace.v2.note :refer [check]]))

(note-md "### Setting the ground")

(note-md "Let us require the main Vrksasana namespace")

(note-void
 (require '[vrksasana.core :as v]))

(note-md ".. and the main namespace of the Vrksasana implementation for R.")

(note-void
 (require '[vrksasana.impl.r.core :as r]))

(note-md "As we will see, the central notion of Vrksasana is a `tree`. A tree represents potential computation, based on its `AST` (Abstract Syntax Tree).

Trees grow in the ground. More precisely, they grow in a `ground`. There can be different grounds, and the properties of a specific ground determine how trees grow.")

(note-md "Here we initialize Vrksasana, setting up the default ground to be R.")

(note-void
 (v/restart r/ground))

(note (class r/ground))

(note-md "### Planting a tree")

(note-md "Now we can do some R computation. To do that, we `plant` a tree. It is being planted in the default ground that we've just set.")

(note-void
 (def tree1 (v/plant '(abs (* 10 ~(range -4 4))))))

(note (class tree1))

(note (keys tree1))

(note tree1)

(note-md "We see that the tree has recieved a random name, and that it holds the AST representing the computation that we asked for.")

(note-md "To plant the tree, we needed a Clojure form. In our example it was `'(abs (* 10 ~(range -4 4)))`. We called that form a `seedling`. The rules for interpreting it depend on the ground. For the R ground, these rules are based on the Clojisr rules for code generation.")

(note-md "### Picking a fruit")

(note-md "To actually make the computation happen, we `pick` a `fruit` off the tree.")

(note-void (def fruit1 (v/pick tree1)))

(note (class fruit1))

(note fruit1)

(note-md "We see that the fruit holds an actual R object, and is printed as that R object.")

(note-md "We can convert it to Clojure data.")

(note (->> fruit1
           v/fruit->data
           (check = [40.0 30.0 20.0 10.0 0.0 10.0 20.0 30.0])))

(note-md "### What does the fruit look like on the inside?")

(note (keys fruit1))

(note-md "The fruit remembers the tree.")

(note (->> fruit1
           :tree
           (check = tree1)))

(note-md "The fruit also knows the resulting value. In the case of the R ground, it is an `RObject` of Clojisr.")

(note (-> fruit1 :value class))

(note-md "### Changing seasons")

(note-md "The fruit also knows on which season it was picked. The `season` is a representation of the computational session where things were computed (in this case, it is a Clojisr session, wrapping an Rserve session).")

(note-void (def season1 (:season fruit1)))

(note (class season1))

(note (keys season1))

(note (-> season1 :session class))

(note-md "We can pick our fruits on different seasons. In the context of the R ground, this means different R sessions.")

(note
 (let [data (range 4)
       tree1   (v/plant `(+ 1 ~data))
       tree2   (v/plant `(+ 2 ~data))
       season1 (v/get-or-make-season r/ground :s1)
       season2 (v/get-or-make-season r/ground :s2)]
   {:fruit1 (v/pick tree1 {:season season1})
    :fruit2 (v/pick tree2 {:season season2})}))

(note-md "### Fruit mix")

(note-md "Trees, fruit and Clojure data can be mixed to create seedlings for new trees.")

(note-void
 (def exotic-fruit
   (let [season1    (v/get-or-make-season r/ground :s1)
         season2    (v/get-or-make-season r/ground :s2)
         tree1      (v/plant '(* 1 10))
         fruit1     (v/pick tree1 {:season season1})
         tree2      (v/plant '(* 2 100))
         small-data [3000]
         big-data   (range 40000 40100)
         tree       (v/plant
                     `(+ ~fruit1 ~tree2 ~small-data (min ~big-data) 500000))]
     (v/pick tree {:season season2}))))

(note
 (->> exotic-fruit
      v/fruit->data
      (check = [543210.0])))

(note
 (set! *print-length* 10)
 (:tree exotic-fruit))

(note-md "We see that a tree's AST holds all dependencies as trees, whenever dependencies come from trees, fruit or big data (on the other hand, small data are included explicitly in the code, following the rules of Clojisr code generation). These dependencies, being trees and not fruits, live across seasons. Thus, we can pick their fruit on any season we wish.")

(note-md "We also see that the AST is a bit inefficient. This should improve soon.")

(note-md :main-notions "## Internals")

(note-md "Coming soon.")

(note-md "### Main notions")

(note-md "Coming soon.")

(note-md "### API")

(note-md "Coming soon.")

(note-md "### Code structure")

(note-md "
- The namespaces `vrksasana.*`, excluding `vrksasana.impl.*`, are the general part, that is suggested to become a separate library.

- `vrksasana.impl.r` is the implementation of an R ground for Vrksasana. This is the part suggested to remain in Clojisr.

- `vrsasana.impl.r.season` is the only namespace relying on other parts of Clojisr. Note how thin it is, and note that it uses only some parts of Clojisr.

- If we decide to go with this approach, then we can replace the implementation of the main Clojisr API (`cloisr.v1.r`) to use `vrksasana.core` and `vrksasana.core.impl.r.core`. In other words, `vrksasana` will come as a layer between the existing implementation and the API.

- Then, some existing parts of Clojisr will become redundant. For example, `clojisr.v1.codegen`, which has been copied and refactored at `vrksasana.impl.r.astgen`, `vrksasana.impl.r.codegen`.")

(note-md "### Implementation")

(note-md "Coming soon.")

(note-md :suggested-plan "## Suggested plan")

(note-md "### Remaining tasks")

(note-md "
- update AST generation to match recent changes in Clojisr code generation
- replacing the relevant parts of Clojisr with vrksasana-based parts (should be easy at this stage)
- discuss desired usage patterns and adapt the API
- garbage collection
- Renjin support (almost nothing to do)
- more efficient AST structures (e.g., avoid unnecessarily nesting binary operators at this stage)")

(note-md "### Future directions")

(note-md "
- separating the general part of vrksasana as a separate library
- caching, serializing fruits across sessions
- generalizing some parts of the R ground AST generation for reuse in other grounds
- other useful grounds (e.g., a Clojure ground supporting [sci](https://github.com/borkdude/sci) and nREPL backends).
- mixing fruit from different grounds in the creation of a tree, computable using data conversions")

(comment
  (notespace.v2.note/compute-this-notespace!))

