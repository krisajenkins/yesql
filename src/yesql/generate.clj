(ns yesql.generate
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.core.typed :as t :refer [ann HMap tc-ignore Any IFn]]
            [yesql.util :refer [distinct-except create-root-var]]
            [yesql.types :refer [map->Query]]
            [yesql.statement-parser :refer [split-at-parameters reassemble-query]])
  (:import [yesql.types Query]))

;; (ann replace-question-mark-with-gensym
;;   [Symbol -> Symbol])
(tc-ignore
 (defn- replace-question-mark-with-gensym
   [parameter]
   (if (= parameter '?)
     (gensym "P_")
     parameter)))

(tc-ignore
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
     :display-args  (distinct-except #{'?} args)})))

;; Maintainer's note: clojure.java.jdbc.execute! returns a list of
;; rowcounts, because it takes a list of parameter groups. In our
;; case, we only ever use one group, so we'll unpack the
;; single-element list with `first`.
(tc-ignore
 (defn execute-handler
   [db sql-and-params]
   (first (jdbc/execute! db sql-and-params))))

(tc-ignore
 (defn insert-handler
   [db [statement & params]]
   (jdbc/db-do-prepared-return-keys db statement params)))

;; (ann ^:no-check generate-query-fn
;;   [yesql.types.Query -> (IFn [Any * -> Any])])
(defn generate-query-fn
  "Generate a function to run a query.

  - If the query name ends in `!` it will call `clojure.java.jdbc/execute!`,
  - If the query name ends in `<!` it will call `clojure.java.jdbc/insert!`,
  - otherwise `clojure.java.jdbc/query` will be used."
  [{:keys [name docstring statement]}]
  (assert name "Query name is mandatory.")
  (assert statement "Query statement is mandatory.")
  (let [split-query (split-at-parameters statement)
        {:keys [query-args display-args function-args]} (split-query->args split-query)
        jdbc-fn (cond
                 (= [\< \!] (take-last 2 name)) `insert-handler
                 (= \! (last name)) `execute-handler
                 :else `jdbc/query)]
    (eval `(with-meta
             (fn [db# ~@function-args]
               (~jdbc-fn db#
                         (reassemble-query '~split-query
                                           ~query-args)))
             (merge {:arglists (quote ([~'db ~@display-args]))
                     :name ~name
                     ::source ~(str statement)}
                    ~(when docstring
                       {:doc docstring}))))))

(defprotocol FunctionGenerator
  (generate-fn [this]))

(defprotocol VarGenerator
  (generate-var [this]))

(extend-type Query
  FunctionGenerator
  (generate-fn [this]
    (generate-query-fn this))
  VarGenerator
  (generate-var [this]
    (create-root-var (:name this)
                     (generate-fn this))))
