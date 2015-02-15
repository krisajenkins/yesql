(ns yesql.generate
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]
            [clojure.core.typed :as t :refer [ann HMap tc-ignore Any IFn]]
            [clojure.string :as string :refer [join]]
            [yesql.types :refer [map->Query]])
  (:import [yesql.types Query]))

(defn- parameter-list
  [query]
  (let [parameters (re-seq #"(?:\?|:[^\s,\)]+)" query)]
    (map #(if (= % "?") % (keyword (.substring % 1))) parameters)))

(defn- parameter-replacements
  [args]
  (into {}
    (for [[k v] (dissoc args :?)]
      [k (if (sequential? v) (string/join \, (map (constantly \?) v)) "?")])))

(defn- replace-named-args
  [query args]
  (replace args (parameter-list query)))

(defn- replace-positional-args
  [parameters args]
  (loop [jdbc-args [] parameters parameters args args]
    (if (seq args)
      (if (= (first parameters) "?")
        (recur (conj jdbc-args (first args)) (next parameters) (next args))
        (recur (conj jdbc-args (first parameters)) (next parameters) args))
      (concat jdbc-args parameters))))

(defn- jdbc-args
  [query args]
  (let [named-args (replace-named-args query args)
        expected-count (count (filter #{"?"} named-args))
        actual-count (count (:? args))
        jdbc-args (flatten (if (:? args)
                             (replace-positional-args named-args (:? args))
                             named-args))
        missing-keys (filter keyword? jdbc-args)]
    (assert (= expected-count actual-count)
            (str "Query argument mismatch.\n"
                 "Expected " expected-count " positional parameters. "
                 "Got " actual-count ". "
                 "Supply positional parameters as {:? [....]}."))
    (assert (empty? missing-keys)
            (str "Query argument mismatch.\n"
                 "Missing keys: " (seq missing-keys)))
    jdbc-args))

(defn- jdbc-query
  [query args]
  (let [replacements (parameter-replacements args)]
    (reduce-kv #(string/replace %1 (str %2) %3) query replacements)))

(defn  rewrite-query-for-jdbc
  [query args]
  (concat [(jdbc-query query args)] (jdbc-args query args)))

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

(defn- keyword->symbol
  [kw]
  (symbol (name kw)))

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
        required-args (mapv keyword->symbol (set (parameter-list statement)))
        named-args (when-not (empty? required-args) {:keys required-args})
        display-args (-> (if named-args [named-args] [])
                         (list [(or named-args {}) {:keys ['connection]}])
                         ((fn [args] (if default-conn args (rest args)))))
        query-fn (fn [args opts]
                   (if-let [conn (or (:connection opts) default-conn)]
                     (jdbc-fn conn (rewrite-query-for-jdbc statement args) opts)
                     (throw
                      (AssertionError.
                       (str "No database connection specified to '" name "'.\n"
                            "Options must include a :connection key associated "
                            "with a valid clojure.java.jdbc db-spec.")))))
        generated-fn (if default-conn
                       (if (empty? required-args)
                         (fn
                           ([] (query-fn {} {}))
                           ([args opts] (query-fn args opts)))
                         (fn
                           ([args] (query-fn args {}))
                           ([args opts] (query-fn args opts))))
                       query-fn)]
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
