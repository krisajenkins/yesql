(ns yesql.statement-parser
  (:require [clojure.java.io :as io]
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

(defn- args-to-placeholders
  [args]
  (if-not (coll? args)
    "?"
    (clojure.string/join "," (repeat (count args) "?"))))

(defn reassemble-query
  [split-query initial-args]
  (let [expected-keys (set (map keyword (remove (partial = '?)
                                                (filter symbol? split-query))))
        actual-keys (set (keys (dissoc initial-args :?)))
        expected-positional-count (count (filter (partial = '?)
                                                 split-query))
        actual-positional-count (count (:? initial-args))]
    (assert (= expected-keys actual-keys)
            (format "Query argument mismatch.\nExpected keys: %s\nActual keys: %s\n"
                    (str (seq expected-keys))
                    (str actual-keys)))
    (assert (= expected-positional-count actual-positional-count)
            (format "Query argument mismatch.\nExpected %d positional parameters. Got %d.\nSupply positional parameters as {:? [...]}"
                    expected-positional-count actual-positional-count))
    (let [[final-query final-parameters _]
          (reduce (fn [[query parameters args] token]
                    (cond
                     (string? token) [(str query token)
                                      parameters
                                      args]
                     (symbol? token) (let [[arg new-args] (if (= '? token)
                                                            [(first (:? args)) (update-in args [:?] rest)]
                                                            [(get args (keyword token)) args])]
                                       [(str query (args-to-placeholders arg))
                                        (if (coll? arg)
                                          (concat parameters arg)
                                          (conj parameters arg))
                                        new-args])))
                  ["" [] initial-args]
                  split-query)]
      (vec (cons final-query final-parameters)))))
