(ns sqlinsql.core
  (:refer-clojure :exclude [replace])
  (:require [sqlinsql.named-parameters :refer :all]
            [clojure.java.io :refer [as-file resource]]
            [clojure.string :refer [join replace]]
            [clojure.java.jdbc :as sql]
            [clojure.java.jdbc.sql :refer [select where]]))

(defn slurp-from-classpath
  "Slurps a file from the classpath."
  [path]
  (if-let [url (resource path)]
    (slurp url)))

(defn classpath-file-basename
  [path]
  (if-let [url (resource path)]
    (->> url
         as-file
         .getName
         (re-find #"(.*)\.(.*)?")
         rest)))

(defn underscores-to-dashes
  [string]
  (replace string "_" "-"))

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

(defn make-query-function
  [query]
  (fn [db & parameters]
    (lazy-seq
     (sql/query db
                (cons query parameters)))))

(defn- replace-question-mark-with-gensym
  [parameter]
  (if (= parameter '?)
    (gensym "P_")
    parameter))

(defmacro defquery
  "Defines a query function, as defined in the given SQL file.
Any comments in that file will form the docstring."
  [name filename]
  (let [file (slurp-from-classpath filename)
        query (extract-query file)
        docstring (extract-docstring file)
        [converted parameters] (convert-named-query query)
        dbsym (gensym "DB_")
        namelist (map replace-question-mark-with-gensym parameters)
        arglist (distinct namelist)]
    `(def ~(with-meta name
             {:arglists `(quote ~(list (vec (cons 'db arglist))))
              :doc docstring})
       (fn ~(vec (cons dbsym arglist))
         (lazy-seq
          (sql/query ~dbsym
                     ~(vec (cons converted namelist))))))))
