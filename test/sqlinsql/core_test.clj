(ns sqlinsql.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as sql]
            [sqlinsql.core :refer :all]))

(def derby-db {:subprotocol "derby"
               :subname "sqlinsql_test_derby"
               :create true})

(deftest startup-test-db
  (sql/with-connection derby-db
    (sql/with-query-results rows ["SELECT CURRENT_TIMESTAMP FROM SYSIBM.SYSDUMMY1"]
      (is (= 1 (count rows))))))

(deftest slurp-from-classpath-test
  (is (re-find #"SELECT"
               (slurp-from-classpath "sqlinsql/current_time.sql"))))

(deftest extraction
  (let [current-time-file (slurp-from-classpath "sqlinsql/current_time.sql")]
    (testing "Query function - select current time."
      (let [current-time-fn (make-query-function "SELECT CURRENT_TIMESTAMP AS time\nFROM SYSIBM.SYSDUMMY1")
            [{current-time :time}] (current-time-fn derby-db)]
        (is (instance? java.util.Date current-time)))
      )
    )) 
