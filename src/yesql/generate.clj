(ns yesql.generate
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.core.typed :as t :refer [ann HMap tc-ignore Any IFn]]
            [clojure.string :refer [join]]
            [yesql.util :refer [create-root-var]]
            [yesql.types :refer [map->Query]]
            [yesql.statement-parser :refer [expected-parameter-list rewrite-query-for-jdbc]])
  (:import [yesql.types Query]))

;; Maintainer's note: clojure.java.jdbc.execute! returns a list of
;; rowcounts, because it takes a list of parameter groups. In our
;; case, we only ever use one group, so we'll unpack the
;; single-element list with `first`.
(tc-ignore
 (defn execute-handler
   [db sql-and-params call-options]
   (first (jdbc/execute! db sql-and-params))))

(tc-ignore
 (defn insert-handler
   [db [statement & params] call-options]
   (jdbc/db-do-prepared-return-keys db statement params)))

(tc-ignore
 (defn query-handler
   [db sql-and-params
    {:keys [row-fn result-set-fn]
     :or {row-fn identity
          result-set-fn doall}
     :as call-options}]
   (jdbc/query db sql-and-params
               :row-fn row-fn
               :result-set-fn result-set-fn)))

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
  (let [jdbc-fn (cond
                 (= (take-last 2 name) [\< \!]) insert-handler
                 (= (last name) \!) execute-handler
                 :else query-handler)
        required-args (expected-parameter-list statement)
        global-connection (:connection query-options)
        real-fn (fn [args call-options]
                  (let [connection (or (:connection call-options)
                                       global-connection)]
                    (assert connection
                            (format (join "\n"
                                          ["No database connection supplied to function '%s',"
                                           "Check the docs, and supply {:connection ...} as an option to the function call, or globally to the defquery declaration."])
                                    name))
                    (jdbc-fn connection
                             (rewrite-query-for-jdbc statement args)
                             call-options)))
        [display-args generated-function] (let [named-args (if-let [as-vec (seq (mapv (comp symbol clojure.core/name)
                                                                                      required-args))]
                                                             {:keys as-vec}
                                                             {})
                                                global-args {:keys ['connection]}]
                                            (if global-connection
                                              (if (empty? required-args)
                                                [(list []
                                                       [named-args global-args])
                                                 (fn foo
                                                   ([] (foo {} {}))
                                                   ([args call-options] (real-fn args call-options)))]
                                                [(list [named-args]
                                                       [named-args global-args])
                                                 (fn foo
                                                   ([args] (foo args {}))
                                                   ([args call-options] (real-fn args call-options)))])
                                              [(list [named-args global-args])
                                               (fn foo
                                                 ([args call-options] (real-fn args call-options)))]))]
    (with-meta generated-function
      (merge {:name name
              :arglists display-args
              ::source (str statement)}
             (when docstring
               {:doc docstring})))))

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
