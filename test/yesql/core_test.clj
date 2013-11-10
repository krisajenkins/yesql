(ns yesql.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as sql]
            [yesql.util :refer [slurp-from-classpath]]
            [yesql.core :refer :all]))

(def derby-db {:subprotocol "derby"
               :subname (gensym "yesql_test_derby_")
               :create true})

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
             "SELECT CURRENT_TIMESTAMP AS time\nFROM SYSIBM.SYSDUMMY1")))
    (testing "Query function - select current time."
      (let [current-time-fn (make-query-function "SELECT CURRENT_TIMESTAMP AS time\nFROM SYSIBM.SYSDUMMY1")
            [{current-time :time}] (current-time-fn derby-db)]
        (is (instance? java.util.Date current-time))))))

(deftest defquery-test
  (defquery named-parameters-query "yesql/named_parameters.sql")
  (let [[{current-time :time}] (named-parameters-query derby-db 1 2 3 4)]
    (is (instance? java.util.Date current-time))))

(deftest defquery-metadata-test
  (defquery current-time-query "yesql/current_time.sql")
  (let [metadata (meta (var current-time-query))]
    (is (= (:doc metadata)
           "Just selects the current time.\nNothing fancy."))
    (is (= (:arglists metadata)
           '([db]))))

  (defquery named-parameters-query "yesql/named_parameters.sql")
  (let [metadata (meta (var named-parameters-query))]
    (is (= (:doc metadata)
           "Here's a query with some named and some anonymous parameters.\n(...and some repeats.)"))
    (is (= (:arglists metadata)
           '([db value1 value2 ? ?])))))
