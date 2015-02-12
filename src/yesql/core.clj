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

(defmacro require-sql
  "Require-like behavior for yesql, to prevent namespace pollution.
   Parameter is a list of [sql-source-file-name [:as alias] [:refer [var1 var2]]]
   At least one of :as or :refer is required
   Usage: (require-sql [\"sql/foo.sql\" :as foo-sql :refer [some-query-fn])"
  [[sql-file & {:keys [as refer]} :as require-args]]
  (when-not (or as refer)
    (throw (Exception. "Missing an :as or a :refer")))
  (let [current-ns (ns-name *ns*)
        ;; Keep this .sql file's defqueries in a predictable place:
        target-ns (symbol (str "yesquire/" sql-file))]
    `(do
       (ns-unalias *ns* '~as)
       (create-ns '~target-ns)
       (in-ns '~target-ns)
       (clojure.core/require '[yesql.core])
       (yesql.core/defqueries ~sql-file)
       (clojure.core/in-ns '~current-ns)
       ~(when as
          `(clojure.core/alias '~as '~target-ns))
       ~(when refer
          `(clojure.core/refer '~target-ns :only '~refer)))))
