(ns yesql.queryfile-parser-test
  (:require [clojure.string :refer [join]]
            [clojure.template :refer [do-template]]
            [expectations :refer :all]
            [instaparse.core :as instaparse]
            [yesql.queryfile-parser :refer :all]
            [yesql.types :refer [map->Query]]
            [yesql.util :refer [slurp-from-classpath]])
  (:import [clojure.lang ExceptionInfo]))

(do-template [start-key input _ expected-output]
  (expect (if expected-output
            (list expected-output)
            (list))
          (instaparse/transform parser-transforms
                                (instaparse/parses parser input :start start-key)))

  :whitespace " "           => " "
  :whitespace " \t "        => " \t "
  :newline "\n"             => "\n"
  :non-whitespace "abc-DEF" => "abc-DEF"
  :any "Test this"          => "Test this"

  :comment "--\n"                   => [:comment "\n"]
  :comment "-- This is a comment\n" => [:comment "This is a comment\n"]
  :comment " --This is a comment\n" => [:comment "This is a comment\n"]
  :comment "-- name: This is not a comment\n" => nil

  :name "--name:test\n"   => [:name "test"]
  :name "-- name: test\n" => [:name "test"]

  :line "SELECT *\n"                 => "SELECT *\n"
  :line "SELECT * FROM dual\n"       => "SELECT * FROM dual\n"
  :line "SELECT * FROM dual\n"       => "SELECT * FROM dual\n"
  :line "SELECT * -- with comment\n" => "SELECT * -- with comment\n"

  :query (join "\n" ["-- name: a-query"
                     "-- This is"
                     "-- a long comment"
                     "SELECT * -- With embedded comments."
                     "FROM dual"
                     ""])
  => (map->Query {:name "a-query"
                  :docstring "This is\na long comment"
                  :statement "SELECT * -- With embedded comments.\nFROM dual"}))

(expect
 [(map->Query {:name "the-time"
               :docstring "This is another time query.\nExciting, huh?"
               :statement "SELECT CURRENT_TIMESTAMP\nFROM SYSIBM.SYSDUMMY1"})
  (map->Query {:name "sums"
               :docstring "Just in case you've forgotten\nI made you a sum."
               :statement (join "\n" ["SELECT"
                                      "    :a + 1 adder,"
                                      "    :b - 1 subtractor"
                                      "FROM SYSIBM.SYSDUMMY1"])})
  (map->Query {:name "edge"
               :docstring "And here's an edge case.\nComments in the middle of the query."
               :statement (join "\n" ["SELECT"
                                      "    1 + 1 AS two"
                                      "FROM SYSIBM.SYSDUMMY1"])})]
 (parse-tagged-queries (slurp-from-classpath "yesql/sample_files/combined_file.sql")))

;;; Failures.
(expect #"Parse error"
        (try
          (parse-tagged-queries (slurp-from-classpath "yesql/sample_files/tagged_no_name.sql"))
          (catch ExceptionInfo e (.getMessage e))))

(expect #"Parse error"
        (try
          (parse-tagged-queries (slurp-from-classpath "yesql/sample_files/tagged_two_names.sql"))
          (catch ExceptionInfo e (.getMessage e))))

;;; Parsing edge cases.

(expect ["this-has-trailing-whitespace"]
        (map :name
             (parse-tagged-queries (slurp-from-classpath "yesql/sample_files/parser_edge_cases.sql"))))
