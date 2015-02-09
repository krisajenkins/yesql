(ns yesql.generate
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]
            [clojure.core.typed :as t :refer [ann HMap tc-ignore Any IFn]]
            [clojure.string :refer [join]]
            [yesql.types :refer [map->Query]]
            [yesql.statement-parser :refer [tokenize]])
  (:import [yesql.types Query]))

(defn- args-to-placeholders
  [args]
  (if (sequential? args)
    (join \, (map (constantly \?) args))
    \?))

(defn- analyse-statement-tokens
  [tokens]
  (let [args (mapv keyword (filter symbol? tokens))]
    {:expected-keys (set (remove #{:?} args))
     :expected-positional-count (count (filterv #{:?} args))}))

(defn expected-parameter-list
  [query]
  (let [tokens (tokenize query)
        {:keys [expected-keys expected-positional-count]} (analyse-statement-tokens tokens)]
    (if (zero? expected-positional-count)
      expected-keys
      (conj expected-keys :?))))

(defn rewrite-query-for-jdbc
  [query initial-args]
  (let [tokens (tokenize query)
        {:keys [expected-keys expected-positional-count]} (analyse-statement-tokens tokens)
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
                                        (if (sequential? arg)
                                          (concat parameters arg)
                                          (conj parameters arg))
                                        new-args])))
                  ["" [] initial-args]
                  tokens)]
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
  [{:keys [name docstring statement]} query-options]
  (assert name      "Query name is mandatory.")
  (assert statement "Query statement is mandatory.")
  (let [jdbc-fn (cond
                 (.endsWith name "<!") insert-handler
                 (.endsWith name "!")  execute-handler
                 :else query-handler)
        default-conn (:connection query-options)
        query-fn (fn [args opts]
                   (if-let [conn (or (:connection opts) default-conn)]
                     (jdbc-fn conn (rewrite-query-for-jdbc statement args) opts)
                     (throw
                      (AssertionError.
                       (str "No database connection specified to '" name "'.\n"
                            "Options must include a :connection key associated "
                            "with a valid clojure.java.jdbc db-spec.")))))
        required-args (expected-parameter-list statement)
        named-args (when-not (empty? required-args)
                     {:keys (mapv (comp symbol clojure.core/name) required-args)})
        display-args (-> (if named-args [named-args] [])
                         (list [(or named-args {}) {:keys ['connection]}])
                         ((fn [args] (if default-conn args (rest args)))))
        generated-fn (if default-conn
                       (if (empty? required-args)
                         (fn wrapper-fn
                           ([] (wrapper-fn {} {}))
                           ([args opts] (query-fn args opts)))
                         (fn wrapper-fn
                           ([args] (wrapper-fn args {}))
                           ([args opts] (query-fn args opts))))
                       (fn [args opts] (query-fn args opts)))]
    (with-meta generated-fn
      (merge {:name name
              :arglists display-args
              ::source (str statement)}
             (when docstring
               {:doc docstring})))))

(defn generate-var [this options]
  (let [value (generate-query-fn this options)
        name (with-meta (symbol (:name this)) (meta value))]
    (intern *ns* name value)))
