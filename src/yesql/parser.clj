(ns yesql.parser
  (:require [clojure.string :refer [join split-lines]]))

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
