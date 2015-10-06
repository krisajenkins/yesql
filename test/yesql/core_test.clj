(ns yesql.core-test
  (:require [clojure.java.jdbc :as jdbc]
            [expectations :refer :all]
            [yesql.core :refer :all]))

(def derby-db {:subprotocol "derby"
               :subname (gensym "memory:")
               :create true})

(defquery current-time-query "yesql/sample_files/current_time.sql")
(defquery mixed-parameters-query "yesql/sample_files/mixed_parameters.sql")

;;; Check we can start up the test DB.
(expect (more-> java.sql.Timestamp (-> first :1))
        (jdbc/query derby-db ["SELECT CURRENT_TIMESTAMP FROM SYSIBM.SYSDUMMY1"]))

;;; Test querying.
(expect (more-> java.util.Date
                (-> first :time))
        (current-time-query derby-db))

(expect (more-> java.util.Date
                (-> first :time))
        (mixed-parameters-query derby-db 1 2 3 4))

(expect empty?
        (mixed-parameters-query derby-db 1 2 0 0))

;;; Test Metadata.
(expect {:doc "Just selects the current time.\nNothing fancy."
         :arglists '([db])}
        (in (meta (var current-time-query))))

(expect {:doc "Here's a query with some named and some anonymous parameters.\n(...and some repeats.)"
         :arglists '([db value1 value2 ? ?])}
        (in (meta (var mixed-parameters-query))))

;; Running a query in a transaction and using the result outside of it should work as expected.
(expect-let [[{time :time}] (jdbc/with-db-transaction [connection derby-db]
                              (current-time-query connection))]
  java.util.Date
  time)

;;; Check defqueries returns the list of defined vars.
(expect-let [return-value (defqueries "yesql/sample_files/combined_file.sql")]
  [(var the-time) (var sums) (var edge)]
  return-value)

;;; SQL's quoting rules.
(defquery quoting "yesql/sample_files/quoting.sql")

(expect "'can't'"
        (:word (first (quoting derby-db))))

;;; Switch into a fresh namespace
(ns yesql.core-test.test-require-sql
  (:require [expectations :refer :all]
            [yesql.core :refer :all]))

(require-sql ["yesql/sample_files/combined_file.sql" :as combined])

(expect var? #'combined/edge)

(require-sql ["yesql/sample_files/combined_file.sql" :refer [the-time]])

(expect var? #'the-time)
