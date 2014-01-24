(ns yesql.types
  (:require [clojure.java.jdbc :as jdbc]
            [yesql.named-parameters :refer [split-at-parameters reassemble-query]]
            [yesql.util :refer [distinct-except slurp-from-classpath]]))

;; ## Protocol

(defprotocol Definable
  (emit-def [query]))

;; ## Query Emitter

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

   The result will be a hash map with these fields."
  [split-query]
  (let [args (filterv symbol? split-query)
        query-args (mapv replace-question-mark-with-gensym args)]
    (hash-map
      :query-args    query-args
      :function-args (distinct query-args)
      :display-args  (distinct-except args #{'?}))))

(defn- emit-query-assemble-form
  "Emit function to reassemble a query."
  [id {:keys [function-args query-args]} split-query]
  `(defn- ~id
     [~@function-args]
     (reassemble-query '~split-query [~@query-args])))

(defn- fn-symbol
  "Attach metadata (docstring/argument lists) to the given symbol."
  [id docstring querystring display-args]
  (->> {:arglists `(quote ([~@display-args] [~'db ~@display-args]))
        :doc (format "%s%n%n(use the %d-argument version to generate the query vector)"
                     (or docstring "")
                     (count display-args))
        ::source (str querystring)}
       (with-meta id)))

(defn- emit-fn-form
  "Emit function with two arities: one to run a query against a database (first parameter is
   DB configuration), and the other to create the plain query vector."
  [id assemble-id docstring querystring {:keys [display-args function-args]} split-query db-fn]
  `(def ~(fn-symbol id docstring querystring display-args)
     (fn
       ([~@function-args] (~assemble-id ~@function-args))
       ([DB# ~@function-args]
        (->> (~assemble-id ~@function-args)
             (~db-fn DB#))))))

(defn- emit-query-fn
  "Emit function to run a query. If the query name ends in `!` it will call `clojure.java.jdbc/execute!`,
   otherwise `clojure.java.jdbc/query` will be used. The function will be able to produce a plain query vector
   if no database configuration is given as the first parameter."
  [{:keys [name docstring querystring]}]
  (let [split-query (split-at-parameters querystring)
        args (split-query->args split-query)
        id (symbol name)
        assemble-id (gensym (str "assemble-" name))
        db-fn (if (.endsWith ^String name "!")
                `jdbc/execute!
                `jdbc/query)]
    `(do
       ~(emit-query-assemble-form assemble-id args split-query)
       ~(emit-fn-form id assemble-id docstring querystring args split-query db-fn))))

(defrecord Query [name docstring querystring]
  Definable
  (emit-def [this]
    (emit-query-fn this)))
