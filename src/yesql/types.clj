(ns yesql.types
  (:require [clojure.java.jdbc :as jdbc]
            [yesql.named-parameters :refer [split-at-parameters reassemble-query]]
            [yesql.util :refer [distinct-except slurp-from-classpath]]))

;; ## Protocol
(defprotocol Definable
  (emit-def [query]))

(defn- replace-question-mark-with-gensym
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
  [id docstring statement display-args]
  (with-meta id
    {:arglists `(quote ([~'db ~@display-args]))
     :doc (or docstring "")
     ::source (str statement)}))

;; Maintainer's note: clojure.java.jdbc.execute! returns a list of
;; rowcounts, because it takes a list of parameter groups. In our
;; case, we only ever use one group, so we'll unpack the
;; single-element list with `first`.
(defn execute-handler
  [db sql-and-params]
  (first (jdbc/execute! db sql-and-params)))

(defn insert-handler
  [db [statement & params]]
  (jdbc/db-do-prepared-return-keys db statement params))

(defn- emit-query-fn
  "Emit function to run a query.

   - If the query name ends in `-q!`, it will call `clojure.java.jdbc/execute` and not process params,
   - If the query name ends in `!` it will call `clojure.java.jdbc/execute!`,
   - If the query name ends in `<!` it will call `clojure.java.jdbc/insert!`,
   - otherwise `clojure.java.jdbc/query` will be used."
  [{:keys [name docstring statement]}]
  (if (= [\- \q \!] (take-last 3 name))
    `(def ~(fn-symbol (symbol name) docstring statement [])
       (fn [db#] 
         (~`execute-handler db# [~statement])))
    (let [split-query (split-at-parameters statement)
          {:keys [query-args display-args function-args]} (split-query->args split-query)
          jdbc-fn (cond
                    (= [\< \!] (take-last 2 name)) `insert-handler
                    (= \! (last name)) `execute-handler
                    :else `jdbc/query)]
      `(def ~(fn-symbol (symbol name) docstring statement display-args)
         (fn [db# ~@function-args]
           (~jdbc-fn db#
                     (reassemble-query '~split-query
                                       ~query-args)))))))

;; ## Query Emitter
(defrecord Query [name docstring statement]
  Definable
  (emit-def [this]
    (emit-query-fn this)))
