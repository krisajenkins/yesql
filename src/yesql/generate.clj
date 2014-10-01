(ns yesql.generate
  {:core.typed  {:collect-only true}}
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]
            [clojure.core.typed :as t :refer [tc-ignore]]
            [clojure.string :refer [join]]
            [yesql.util :refer [create-root-var]]
            [yesql.statement-parser :refer [parse-statement]]))

(def in-list-parameter?
  "Check if a type triggers IN-list expansion."
  (some-fn list? vector? seq?))

(defn- args-to-placeholders
  [args]
  (if (in-list-parameter? args)
    (clojure.string/join "," (repeat (count args) "?"))
    "?"))

(defn- analyse-split-statement
  [split-statement]
  {:expected-keys (set (map keyword (remove (partial = '?)
                                            (filter symbol? split-statement))))
   :expected-positional-count (count (filter (partial = '?)
                                             split-statement))})

(defn expected-parameter-list
  [statement]
  (let [split-statement (parse-statement statement)
        {:keys [expected-keys expected-positional-count]} (analyse-split-statement split-statement)]
    (if (zero? expected-positional-count)
      expected-keys
      (conj expected-keys :?))))

(defn rewrite-query-for-jdbc
  [statement initial-args]
  (let [split-statement (parse-statement statement)
        {:keys [expected-keys expected-positional-count]} (analyse-split-statement split-statement)
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
                  split-statement)]
      (concat [final-query] final-parameters))))

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
