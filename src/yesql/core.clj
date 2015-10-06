(ns yesql.core
  (:require [yesql.util :refer [slurp-from-classpath]]
            [yesql.generate :refer [generate-var]]
            [yesql.queryfile-parser :refer [parse-tagged-queries]]))

(defn defqueries
  "Defines several query functions, as defined in the given SQL file.
  Each query in the file must begin with a `-- name: <function-name>` marker,
  followed by optional comment lines (which form the docstring), followed by
  the query itself."
  ([filename]
     (defqueries filename {}))
  ([filename options]
     (doall (->> filename
                 slurp-from-classpath
                 parse-tagged-queries
                 (map #(generate-var % options))))))

(defn defquery*
  [name filename options]
  ;;; TODO Now that we have a better parser, this is a somewhat suspicious way of writing this code.
  (doall (->> filename
              slurp-from-classpath
              (format "-- name: %s\n%s" name)
              parse-tagged-queries
              (map #(generate-var % options)))))

;;; defquery is a macro solely because of the unquoted symbol it accepts
;;; as its first argument. It is tempting to deprecate defquery. There
;;; again, it makes things so easy to get started with yesql it might
;;; be worth keeping for that reason alone.
(defmacro defquery
  "Defines a query function, as defined in the given SQL file.
  Any comments in that file will form the docstring."
  ([name filename]
     `(defquery ~name ~filename {}))
  ([name filename options]
     `(defquery* ~(str name) ~filename ~options)))

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
