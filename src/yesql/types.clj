(ns yesql.types
  (:require [clojure.java.jdbc :as jdbc]
            [yesql.named-parameters :refer [split-at-parameters reassemble-query]]
            [yesql.util :refer [distinct-except slurp-from-classpath]]))

;; ## Protocol
(defprotocol Definable
  (emit-def [query]))

(defn replace-question-mark-with-gensym
  [parameter]
  (if (= parameter '?)
    (gensym "P_")
    parameter))

(defn- split-query->args
  "Use the split-up query string to create the different kinds of argument lists:

   - `:query-args`: the symbols for the tail of the query vector,
   - `:function-args`: the symbols for the query assembly and execution functions,
   - `:display-args`: the symbols to be attached in the metadata.

   The result will be a map with these fields."
  [split-query]
  (let [args (filterv symbol? split-query)
        query-args (mapv replace-question-mark-with-gensym args)]
    {:query-args    query-args
     :function-args (distinct query-args)
     :display-args  (distinct-except args #{'?})}))

(defn- fn-symbol
  "Attach metadata (docstring/argument lists) to the given symbol."
  [id docstring querystring display-args]
  (with-meta id
    {:arglists `(quote ([~'db ~@display-args]))
     :doc (or docstring "")
     ::source (str querystring)}))

(defn- emit-query-fn
  "Emit function to run a query. If the query name ends in `!` it will call `clojure.java.jdbc/execute!`,
   otherwise `clojure.java.jdbc/query` will be used."
  [{:keys [name docstring querystring]}]
  (let [split-query (split-at-parameters querystring)
        {:keys [query-args display-args function-args]} (split-query->args split-query)
        id (symbol name)
        jdbc-fn (if (= \! (last name))
                  `jdbc/execute!
                  `jdbc/query)]
    `(def ~(fn-symbol id docstring querystring display-args)
       (fn [db# ~@function-args]
         (~jdbc-fn db#
                   (reassemble-query '~split-query [~@query-args]))))))

;; ## Query Emitter
(defrecord Query [name docstring querystring]
  Definable
  (emit-def [this]
    (emit-query-fn this)))
