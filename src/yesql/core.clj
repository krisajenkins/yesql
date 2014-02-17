(ns yesql.core
  (:require [yesql.parser :refer [parse-tagged-queries]]
            [yesql.types :refer [emit-def]]
            [yesql.util :refer [slurp-from-classpath]]))

(defmacro defquery
  "Defines a query function, as defined in the given SQL file.
   Any comments in that file will form the docstring."
  [name filename]
  ;;; TODO Now that we have a better parser, this is a somewhat suspicious way of writing this code.
  (let [query (->> filename
                   slurp-from-classpath
                   (format "-- name: %s\n%s" name)
                   parse-tagged-queries
                   first)]
    (emit-def query)))

(defmacro defqueries
  "Defines several query functions, as defined in the given SQL file.
   Each query in the file must begin with a `-- name: <function-name>` marker,
   followed by optional comment lines (which form the docstring), followed by
   the query itself."
  [filename]
  (let [queries (->> filename
                     slurp-from-classpath
                     parse-tagged-queries)]
    `(doall [~@(for [query queries]
                 (emit-def query))])))
