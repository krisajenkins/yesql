(ns yesql.core
  (:require [clojure.string :refer [split-lines]]
            [yesql.parser :refer [parse-one-tagged-query
                                  parse-tagged-queries]]
            [yesql.types :refer [emit-def]]
            [yesql.util :refer [slurp-from-classpath]]))

(defmacro defquery
  "Defines a query function, as defined in the given SQL file.
   Any comments in that file will form the docstring."
  [name filename]
  (let [lines (->> filename
                   slurp-from-classpath
                   split-lines
                   (cons (format "-- name: %s" name)))
        query (->> lines
                   parse-one-tagged-query
                   first)]
    (emit-def query)))

(defmacro defqueries
  "Defines several query functions, as defined in the given SQL file.
   Each query in the file must begin with a '-- name: <function-name>' marker,
   followed by optional comment lines (which form the docstring), followed by
   the query itself."
  [filename]
  (let [queries (->> filename
                     slurp-from-classpath
                     split-lines
                     parse-tagged-queries)]
    `(doall [~@(for [query queries]
                 (emit-def query))])))
