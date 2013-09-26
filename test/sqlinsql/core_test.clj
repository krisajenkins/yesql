(ns sqlinsql.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as sql]
            [sqlinsql.core :refer :all]))

(def derby-db {:subprotocol "derby"
               :subname (gensym "sqlinsql_test_derby_")
               :create true})

(deftest startup-test-db
  (sql/with-connection derby-db
    (sql/with-query-results rows ["SELECT CURRENT_TIMESTAMP FROM SYSIBM.SYSDUMMY1"]
      (is (= (count rows)
             1)))))

(deftest slurp-from-classpath-test
  (is (re-find #"SELECT"
               (slurp-from-classpath "sqlinsql/current_time.sql")))
  (is (thrown? java.io.FileNotFoundException
               (slurp-from-classpath "nothing/here"))))

(deftest classpath-file-basename-test
  (is (= (classpath-file-basename "sqlinsql/current_time.sql")
         ["current_time" "sql"]))
  (is (= (classpath-file-basename "sqlinsql/core_test.clj")
         ["core_test" "clj"])))

(deftest underscores-to-dashes-test
  (is (= (underscores-to-dashes "nochange")
         "nochange"))
  (is (= (underscores-to-dashes "current_time")
         "current-time")))

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
  (let [current-time-file (slurp-from-classpath "sqlinsql/current_time.sql")
        named-parameters-file (slurp-from-classpath "sqlinsql/named_parameters.sql")
        complicated-docstring-file (slurp-from-classpath "sqlinsql/complicated_docstring.sql")]
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
