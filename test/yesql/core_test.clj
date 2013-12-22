(ns yesql.core-test
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]
            [yesql.core :refer :all]))

(def derby-db {:subprotocol "derby"
               :subname (gensym "memory:")
               :create true})

(defquery current-time-query "yesql/sample_files/current_time.sql")
(defquery named-parameters-query "yesql/sample_files/named_parameters.sql")

(deftest startup-test-db
  (is (= (count (jdbc/query derby-db
                            ["SELECT CURRENT_TIMESTAMP FROM SYSIBM.SYSDUMMY1"]))
         1)))

(deftest defquery-test
  (testing "Simple"
    (let [[{current-time :time}] (current-time-query derby-db)]
      (is (instance? java.util.Date current-time)))

    (let [[{current-time :time}] (named-parameters-query derby-db 1 2 3 4)]
      (is (instance? java.util.Date current-time))))

  (testing "No rows"
    (let [result (named-parameters-query derby-db 1 2 0 0)]
      (is (empty? result)))))

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
  (let [[{time :time}] (jdbc/with-db-transaction [connection derby-db]
                         (current-time-query connection))]
    (is (instance? java.util.Date time))))
