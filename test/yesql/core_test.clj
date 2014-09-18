(ns yesql.core-test
  (:require [clojure.java.jdbc :as jdbc]
            [expectations :refer :all]
            [yesql.core :refer :all]))

(def derby-db {:subprotocol "derby"
               :subname (gensym "memory:")
               :create true})

;;; Test-environment check. Can we actually access the test DB?
(expect (more-> java.sql.Timestamp (-> first :1))
        (jdbc/query derby-db
                    ["SELECT CURRENT_TIMESTAMP FROM SYSIBM.SYSDUMMY1"]))

(defquery current-time-query
  "yesql/sample_files/current_time.sql"
  {:connection derby-db})

(defquery mixed-parameters-query
  "yesql/sample_files/mixed_parameters.sql"
  {:connection derby-db})

;;; Test querying.
(expect (more-> java.util.Date
                (-> first :time))
        (current-time-query))

(expect (more-> java.util.Date
                (-> first :time))
        (mixed-parameters-query {:value1 1
                                 :value2 2
                                 :? [3 4]}))

(expect empty?
        (mixed-parameters-query {:value1 1
                                 :value2 2
                                 :? [0 0]}))

;;; Test Metadata.
(expect {:doc "Just selects the current time.\nNothing fancy."
         :arglists (list '[]
                         '[_ {:keys [connection]}])}
        (in (meta (var current-time-query))))

(expect {:doc "Here's a query with some named and some anonymous parameters.\n(...and some repeats.)"
         :arglists (list '[]
                         '[{:keys [value1 value2 ?]}]
                         '[{:keys [value1 value2 ?]} {:keys [connection]}])}
        (in (meta (var mixed-parameters-query))))

;; Running a query in a transaction and using the result outside of it should work as expected.
(expect-let [[{time :time}] (jdbc/with-db-transaction [connection derby-db]
                              (current-time-query {}
                                                  {:connection connection}))]
  java.util.Date
  time)

;;; Check defqueries returns the list of defined vars.
(expect-let [return-value (defqueries "yesql/sample_files/combined_file.sql")]
  (repeat 3 clojure.lang.Var)
  (map type return-value))

;;; SQL's quoting rules.
(defquery quoting "yesql/sample_files/quoting.sql")

(expect "'can't'"
        (:word (first (quoting {}
                               {:connection derby-db}))))
