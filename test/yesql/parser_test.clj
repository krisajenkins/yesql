(ns yesql.parser-test
  (:require [clojure.string :refer [join]]
            [clojure.test :refer :all]
            [yesql.parser :refer :all]
            [yesql.types :refer [map->Query]]
            [yesql.util :refer [slurp-from-classpath]])
  (:import clojure.lang.ExceptionInfo))

(deftest classify-sql-line-test
  (are [line type] (= (classify-sql-line line) type)
       "--Test." :comment
       "-- Test." :comment
       "  -- Test." :comment
       "--" :comment
       " -- " :comment
       "--name:" :tag
       " -- name:" :tag
       "SELECT" :query))

(deftest parse-tagged-query-test
  (testing "Simple"
    (is (= (parse-tagged-query-file "yesql/sample_files/combined_file.sql")
           (map map->Query
                [{:name "the-time"
                  :docstring "This is another time query.\nExciting, huh?"
                  :querystring "SELECT CURRENT_TIMESTAMP\nFROM SYSIBM.SYSDUMMY1\n"}
                 {:name "sums"
                  :docstring "Just in case you've forgotten\nI made you a sum."
                  :querystring (join "\n"
                                     ["SELECT"
                                      "    :a + 1 adder,"
                                      "    :b - 1 subtractor"
                                      "FROM SYSIBM.SYSDUMMY1"
                                      ""] )}
                 {:name "edge"
                  :docstring "And here's an edge case.\nComments in the middle of the query."
                  :querystring (join "\n"
                                     ["SELECT"
                                      "    1 + 1 AS two"
                                      "FROM SYSIBM.SYSDUMMY1"] )}]))))

  (testing "Edge Cases"
    (is (= (parse-tagged-query-file "yesql/sample_files/tagged_no_comments.sql")
           (map map->Query
                [{:name "the-time"
                  :docstring ""
                  :querystring "SELECT CURRENT_TIMESTAMP\nFROM SYSIBM.SYSDUMMY1"}])))))
