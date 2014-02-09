(ns yesql.parser-test
  (:require [clojure.string :refer [join]]
            [expectations :refer :all]
            [yesql.parser :refer :all]
            [yesql.types :refer [map->Query]]))

(given [type line] (expect type
                           (classify-sql-line line))
  :comment "--Test."
  :comment "-- Test."
  :comment "  -- Test."
  :comment "--"
  :comment " -- "
  :tag "--name:"
  :tag " -- name:"
  :query "SELECT")

;;; Test parse-tagged-query-test
(expect [(map->Query {:name "the-time"
                      :docstring "This is another time query.\nExciting, huh?"
                      :querystring "SELECT CURRENT_TIMESTAMP\nFROM SYSIBM.SYSDUMMY1\n"})
         (map->Query {:name "sums"
                      :docstring "Just in case you've forgotten\nI made you a sum."
                      :querystring (join "\n"
                                         ["SELECT"
                                          "    :a + 1 adder,"
                                          "    :b - 1 subtractor"
                                          "FROM SYSIBM.SYSDUMMY1"
                                          ""] )})
         (map->Query {:name "edge"
                      :docstring "And here's an edge case.\nComments in the middle of the query."
                      :querystring (join "\n"
                                         ["SELECT"
                                          "    1 + 1 AS two"
                                          "FROM SYSIBM.SYSDUMMY1"] )})]
        (parse-tagged-query-file "yesql/sample_files/combined_file.sql"))

(expect [(map->Query {:name "the-time"
                      :docstring ""
                      :querystring "SELECT CURRENT_TIMESTAMP\nFROM SYSIBM.SYSDUMMY1"})]
        (parse-tagged-query-file "yesql/sample_files/tagged_no_comments.sql"))
