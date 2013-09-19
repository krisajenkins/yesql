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

