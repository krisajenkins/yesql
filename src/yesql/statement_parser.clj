(ns yesql.statement-parser
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
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

(defn split-at-parameters
  "Turns a raw SQL query into a vector of SQL-substrings interspersed with clojure symbols for the query's parameters.

  For example, `(split-at-parameters \"SELECT * FROM person WHERE :age > age\")`
  becomes: `[\"SELECT * FROM person WHERE \" age \" > age\"]`"
  [query]
  (process-instaparse-result
   (instaparse/transform parser-transforms
                         (instaparse/parses parser query :start :statement))))

(def in-list-parameter?
  "Check if a type triggers IN-list expansion."
  (some-fn list? vector? seq?))

(defn- args-to-placeholders
  [args]
  (if (in-list-parameter? args)
    (clojure.string/join "," (repeat (count args) "?"))
    "?"))

(defn- analyse-split-query
  [split-query]
  {:expected-keys (set (map keyword (remove (partial = '?)
                                            (filter symbol? split-query))))
   :expected-positional-count (count (filter (partial = '?)
                                             split-query))})

(defn expected-parameter-list
  [statement]
  (let [split-query (split-at-parameters statement)
        {:keys [expected-keys expected-positional-count]} (analyse-split-query split-query)]
    (if (zero? expected-positional-count)
      expected-keys
      (conj expected-keys :?))))

(defn rewrite-query-for-jdbc
  [statement initial-args]
  (let [split-query (split-at-parameters statement)
        {:keys [expected-keys expected-positional-count]} (analyse-split-query split-query)
        actual-keys (set (keys (dissoc initial-args :?)))
        actual-positional-count (count (:? initial-args))
        missing-keys (set/difference expected-keys actual-keys)]
    (assert (empty? missing-keys)
            (format "Query argument mismatch.\nExpected keys: %s\nActual keys: %s\nMissing keys: %s"
                    (str (seq expected-keys))
                    (str (seq actual-keys))
                    (str (seq missing-keys))))
    (assert (= expected-positional-count actual-positional-count)
            (format (join "\n"
                          ["Query argument mismatch."
                           "Expected %d positional parameters. Got %d."
                           "Supply positional parameters as {:? [...]}"])
                    expected-positional-count actual-positional-count))
    (let [[final-query final-parameters consumed-args]
          (reduce (fn [[query parameters args] token]
                    (cond
                     (string? token) [(str query token)
                                      parameters
                                      args]
                     (symbol? token) (let [[arg new-args] (if (= '? token)
                                                            [(first (:? args)) (update-in args [:?] rest)]
                                                            [(get args (keyword token)) args])]
                                       [(str query (args-to-placeholders arg))
                                        (if (in-list-parameter? arg)
                                          (concat parameters arg)
                                          (conj parameters arg))
                                        new-args])))
                  ["" [] initial-args]
                  split-query)]
      (concat [final-query] final-parameters))))
