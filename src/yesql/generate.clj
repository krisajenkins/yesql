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
  (let [raw-args (filterv symbol? split-query)
        query-args (mapv replace-question-mark-with-gensym raw-args)]
    {:query-args    query-args
     :function-args (distinct query-args)
     :display-args  (distinct-except #{'?} raw-args)})))

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
  [{:keys [name docstring statement]
    :as query}
   query-options]
  (assert name      "Query name is mandatory.")
  (assert statement "Query statement is mandatory.")
  (let [split-query (split-at-parameters statement)
        {:keys [query-args display-args function-args]} (split-query->args split-query)
        jdbc-fn (cond
                 (= (take-last 2 name) [\< \!]) insert-handler
                 (= (last name) \!) execute-handler
                 :else jdbc/query)
        real-fn (fn [args call-options]
                  (let [connection (:connection (merge query-options
                                                       call-options))]
                    (assert connection
                            (format "No database connection supplied to function '%s',\nCheck the docs, and supply {:connection ...} as an option to the function call, or globally to the defquery declaration."
                                    name))
                    (jdbc-fn (:connection (merge query-options
                                                 call-options))
                             (reassemble-query split-query args)))) ]
    (with-meta
;;; TODO The next step is to get the query args generated.
      #_(cond
         (and (:connection query-options)
              (empty? expected-args))
         anonymous-version

         (:connection query-options)
         single-arg-version

         :else double-arg-version)
      (fn foo
        ([]
           (foo {}))
        ([args]
           (foo args {}))
        ([args call-options]
           (real-fn args call-options)))
      (merge {:name name
              :arglists (list [display-args]
                              [display-args {:connection '...}])
              ::source (str statement)}
             (when docstring
               {:doc docstring})))))

(generate-query-fn (yesql.types/map->Query {:name "fetch"
                                            :statement "SELECT * FROM users WHERE user_id = ?"})
                   {:dofault-db {:subprotocol "derby"
                                 :subname (gensym "memory:")
                                 :create true}})

(defprotocol FunctionGenerator
  (generate-fn [this options]))

(defprotocol VarGenerator
  (generate-var [this options]))

(extend-type Query
  FunctionGenerator
  (generate-fn [this options]
    (generate-query-fn this options))
  VarGenerator
  (generate-var [this options]
    (create-root-var (:name this)
                     (generate-fn this options))))
