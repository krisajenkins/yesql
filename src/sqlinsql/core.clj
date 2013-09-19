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
  ""

  )

(defn extract-query
  "Returns the query for the given sqlfile."
  [sqlfile]
  sqlfile)

(defn extract-parameters
  "Returns a sequence of the parameter names for the query"
  [query]
  )

(defn make-query-function
  [sql]
  (fn [db & parameters]
    (lazy-seq
     (sql/query db
                (into [] (cons sql parameters))))))
