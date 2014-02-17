(ns yesql.named-parameters
  (:require [clojure.java.io :as io]
            [instaparse.core :as instaparse]
            [yesql.util :refer [process-instaparse-result str-non-nil]]))

(def parser
  (instaparse/parser (io/resource "yesql/named_parameters.bnf")))

(def parser-transforms
  {:statement vector
   :substatement str-non-nil
   :string str-non-nil
   :string-special str-non-nil
   :string-delimiter identity
   :string-normal identity
   :parameter identity
   :placeholder-parameter symbol
   :named-parameter symbol})

(defn split-at-parameters
  "Turns a raw SQL query into a vector of SQL-substrings interspersed with clojure symbols for the query's parameters.

For example, `(split-at-parameters \"SELECT * FROM person WHERE :age > age\")`
becomes: `[\"SELECT * FROM person WHERE \" age \" > age\"]`"
  [query]
  (process-instaparse-result
   (instaparse/transform parser-transforms
                         (instaparse/parses parser query :start :statement))))

(defn- args-to-placehoders
  [args]
  (if-not (coll? args)
    "?"
    (clojure.string/join "," (repeat (count args) "?"))))

(defn reassemble-query
  "Given a query that's been split into text-and-symbols, and some arguments, reassemble
it as the pair `[string-with-?-parameters args]`, suitable for supply to `clojure.java.jdbc`."
  [split-query args]
  (assert (= (count (filter symbol? split-query))
             (count args))
          "Query parameter count must match args count.")
  (loop [query-string ""
         final-args []
         [query-head & query-tail] split-query
         [args-head & args-tail :as remaining-args] args]
    (cond
     (nil? query-head) (vec (cons query-string final-args))
     (string? query-head) (recur (str query-string query-head)
                                 final-args
                                 query-tail
                                 remaining-args)
     (symbol? query-head) (recur (str query-string (args-to-placehoders args-head))
                                 (if (coll? args-head)
                                   (apply conj final-args args-head)
                                   (conj final-args args-head))
                                 query-tail
                                 args-tail))))
