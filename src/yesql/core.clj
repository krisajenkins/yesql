(ns yesql.core
  (:refer-clojure :exclude [replace])
  (:require [yesql.named-parameters :refer :all]
            [clojure.java.io :refer [as-file resource]]
            [clojure.string :refer [join replace split-lines]]
            [clojure.java.jdbc :as sql]
            [clojure.java.jdbc.sql :refer [select where]])
  (:import [java.io FileNotFoundException]))

(defn slurp-from-classpath
  "Slurps a file from the classpath."
  [path]
  (if-let [url (resource path)]
    (slurp url)
    (throw (FileNotFoundException. path))))

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

(defn sql-comment-line?
  "If string is an SQL comment line, returns the text after the comment marker, otherwise nil."
  [string]
  (->> string
       (re-matches #"^\p{Blank}*--\p{Blank}*(.*)")
       second))

(defn extract-docstring
  "Returns the docstring, if any, within the given sqlfile."
  [sqlfile]
  (->> sqlfile
       split-lines
       (map sql-comment-line?)
       (remove nil?)
       (join "\n")))

(defn extract-query
  "Returns the query for the given sqlfile."
  [sqlfile]
  (->> sqlfile
       split-lines
       (remove sql-comment-line?)
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

;; TODO Tidy
;; TODO :file metadata. Seems to get swallowed by 'def.
(defmacro defquery
  "Defines a query function, as defined in the given SQL file.
Any comments in that file will form the docstring."
  [name filename]
  (let [file (slurp-from-classpath filename)
        docstring (extract-docstring file)
        query (extract-query file)
        split-query (split-at-parameters query)
        arglist (vec (filter symbol? split-query))
        dbsym (gensym "DB_")]
    `(def ~(with-meta name
             {:arglists `(quote ~(list (vec (cons 'db-spec arglist))))
              :doc docstring})
       (fn ~(vec (cons dbsym arglist))
         (lazy-seq
          (sql/query ~dbsym
                     (reassemble-query '~split-query ~arglist)))))))
