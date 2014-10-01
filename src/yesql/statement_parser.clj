(ns yesql.statement-parser
  (:require [clojure.java.io :as io]
            [clojure.string :refer [join]]
            [instaparse.core :as instaparse]
            [yesql.util :refer [str-non-nil]]
            [yesql.instaparse-util :refer [process-instaparse-result]]))

(def parser
  (instaparse/parser (io/resource "yesql/statement.bnf")))

(def ^:private parser-transforms
  {:statement vector
   :substatement str-non-nil
   :string str-non-nil
   :string-special str-non-nil
   :string-delimiter identity
   :string-normal identity
   :parameter identity
   :placeholder-parameter symbol
   :named-parameter symbol})

(defn parse-statement
  "Parse a raw SQL statement into a vector of SQL-substrings
  interspersed with clojure symbols for the query's parameters.

  For example, `(parse-statement \"SELECT * FROM person WHERE :age > age\")`
  becomes: `[\"SELECT * FROM person WHERE \" age \" > age\"]`"
  [statement]
  (process-instaparse-result
   (instaparse/transform parser-transforms
                         (instaparse/parses parser statement :start :statement))))
