(ns sqlinsql.core
  (:require [clojure.java.io :refer [resource]]
            [clojure.java.jdbc :as sql]))

(defn slurp-from-classpath
  "Slurps a file from the classpath."
  [path]
  (if-let [file (resource path)]
    (slurp file)))

(defn extract-docstring
  "Returns the docstring, if any, within the given sqlfile."
  [sqlfile]
  (->> sqlfile
       (re-seq #"(?m)^\s*--\s*(.*)")
       (map second)
       (join "\n")))

(defn extract-query
  "Returns the query for the given sqlfile."
  [sqlfile]
  (->> sqlfile
       (re-seq #"(?m)^(?!\s*--\s*)(.*)")
       (map second)
       (join "\n")))

(defn extract-parameters
  "Returns a sequence of the parameter names for the query"
  [query]
  )

(defn make-query-function
  [query]
  (fn [db & parameters]
    (lazy-seq
     (sql/query db
                (cons query parameters)))))

(defmacro defquery
  "Defines a query function, as defined in the given SQL file.
Any comments in that file will form the docstring."
  [name file]
  (let [sqlfile (slurp-from-classpath file)
        query (extract-query sqlfile)
        docstring (extract-docstring sqlfile)
        ]
    `(def ~(with-meta name
             (merge (meta name)
                    {:arglists ''([db & parameters])
                     :doc docstring}))
       (fn [db# & parameters#]
         (lazy-seq
          (sql/query db#
                     (cons ~query parameters#)))))))

(defquery current-time "sqlinsql/current_time.sql")

(def db {:subprotocol "derby"
         :subname "sqlinsql_test_derby"
         :create true})

(:time
 (first
  (current-time db)))
