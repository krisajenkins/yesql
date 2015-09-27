(ns yesql.union
  (:require [clojure.java.jdbc :as jdbc]))

; Union is a macro because it translates the query-method names into their 'raw'
; equivalents without actually calling the queries individually
(defmacro union-sql
  "([queries] [queries options])
  Given a collection of queries and an options map, joins each query together
  into a union and returns the result.

  Valid options are :union-append and :union-type

  :union-type is the keyword used to join the queries, for example 'UNION' or
  'UNION ALL'. The default is 'UNION ALL'

  :union-append is an optional string that may be appended after all of the
  unionified queries. An example might be a 'ORDER BY X' or 'LIMIT N' clause.
  "
  ([queries] `(union-sql ~queries {}))
  ([queries options]
    (assert not-empty queries) ; we need at least one
    (let [sql-and-params-vecs
          (mapv (fn [query-and-args]
                (let [query-sym (first query-and-args)
                      args (rest query-and-args)]
                      `(~(symbol (str query-sym "-raw")) ~@args )))
                      queries)]
    `(union-raw ~sql-and-params-vecs ~options))))

(defn- unionize-sql-strings
  "Takes a collection of sql query strings, surrounds each with () and joins
  them with the supplied union type.
  union-type should be UNION or UNION ALL"
  [sql-coll union-type]
  (->> sql-coll
   (map #(str \( % \)))
   (clojure.string/join (str "\n\n" union-type "\n\n"))))

(defn- dbspec-from-multiple-raw-queries
  "The problem with multiple different queries is that each one has its own
  dbspec. Obviously with unions we can only use one dbspec because we query on
  one db.
  In 99% of cases the dbspec will be the same everywhere. For sanity we check
  here that this is indeed the case, and if so return the common dbspec.
  Otherwise raise an exception."
  [raw-queries]
  (let [dbspecs (map first raw-queries)]
    (assert (= 1 (count (into #{} dbspecs))) "cannot UNION queries with multiple distinct db-specs")
    (first dbspecs)))

(defn union-raw
  "Performs a SQL union using the provided RAW sql-and-params vectors.
   Union is performed in the order in which the arguments appear.
   An optional string may be provided which will be appended after the UNIONs
   SQL. For example you may include a LIMIT clause here.

   NOTE: You can get hold of a raw sql-and-params vector for a query-fn by
   executing the query as normal but with `-raw` appended to it
   e.g. for query function get-bar:
   `(get-bar-raw {:foo 1})`
   "
   [sql-and-params-vecs-raw options]
    (let [dbspec (dbspec-from-multiple-raw-queries sql-and-params-vecs-raw)
          sql-and-params-vecs (map second sql-and-params-vecs-raw)
          sql-coll    (map first sql-and-params-vecs)
          params-coll (mapcat rest  sql-and-params-vecs)]
     (jdbc/query dbspec
      (cons
       (str (unionize-sql-strings
             sql-coll
             (get options :union-type "UNION ALL"))
            (:union-append options))
       params-coll))))
