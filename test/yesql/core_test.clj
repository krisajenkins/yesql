(ns yesql.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as sql]
            [yesql.util :refer [slurp-from-classpath]]
            [yesql.core :refer :all]))

(def derby-db {:subprotocol "derby"
               :subname (gensym "memory:")
               :create true})

(defquery current-time-query "yesql/current_time.sql")
(defquery named-parameters-query "yesql/named_parameters.sql")

(deftest startup-test-db
  (sql/with-connection derby-db
    (sql/with-query-results rows ["SELECT CURRENT_TIMESTAMP FROM SYSIBM.SYSDUMMY1"]
      (is (= (count rows)
             1)))))

(deftest sql-comment-line?-test
  (are [line match] (= (sql-comment-line? line)
                       match)
       "--Test." "Test."
       "-- Test." "Test."
       "  -- Test." "Test."
       "--" ""
       "-- " ""
       "Test." nil))

(deftest extraction
  (let [current-time-file (slurp-from-classpath "yesql/current_time.sql")
        named-parameters-file (slurp-from-classpath "yesql/named_parameters.sql")
        complicated-docstring-file (slurp-from-classpath "yesql/complicated_docstring.sql")]
    (testing "Docstring extraction."
      (is (= (extract-docstring current-time-file)
             "Just selects the current time.\nNothing fancy."))
      (is (= (extract-docstring complicated-docstring-file) "This is a simple query.\n\nbut...\n\nThe docstring\nis tricksy.\nIsn't it?")))
    (testing "Query extraction."
      (is (= (extract-query current-time-file)
             "SELECT CURRENT_TIMESTAMP AS time\nFROM SYSIBM.SYSDUMMY1"))
      (is (= (extract-query complicated-docstring-file)
             "SELECT CURRENT_TIMESTAMP AS time\nFROM SYSIBM.SYSDUMMY1")))))

(deftest defquery-test
  (let [[{current-time :time}] (current-time-query derby-db)]
    (is (instance? java.util.Date current-time)))

  (let [[{current-time :time}] (named-parameters-query derby-db 1 2 3 4)]
    (is (instance? java.util.Date current-time))))

(deftest defquery-metadata-test
  (let [metadata (meta (var current-time-query))]
    (is (= (:doc metadata)
           "Just selects the current time.\nNothing fancy."))
    (is (= (:arglists metadata)
           '([db]))))

  (let [metadata (meta (var named-parameters-query))]
    (is (= (:doc metadata)
           "Here's a query with some named and some anonymous parameters.\n(...and some repeats.)"))
    (is (= (:arglists metadata)
           '([db value1 value2 ? ?])))))

(deftest transaction-handling-test
  ;; Running a query in a transaction and using the result outside of it should work as expected.
  (let [[{time :time}] (sql/db-transaction [connection derby-db]
                                           (current-time-query connection))]
    (is (instance? java.util.Date time))))
