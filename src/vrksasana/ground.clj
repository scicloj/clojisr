(ns vrksasana.ground)

(defprotocol PGround
  (seedling->ast [this seedling])
  (ast->code [this ast])
  (assignment-code [this var-name code])
  (forgetting-code [this var-name])
  (tree-name->var-name [this tree-name])
  (default-season-name [this])
  (default-season-attributes [this])
  (new-season [this season-name attributes]))

