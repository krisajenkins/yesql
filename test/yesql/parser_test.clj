(ns yesql.parser-test
  (:require [clojure.test :refer :all]
            [yesql.util :refer [slurp-from-classpath]]
            [yesql.parser :refer :all]))

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
