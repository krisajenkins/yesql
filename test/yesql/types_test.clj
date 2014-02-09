(ns yesql.types-test
  (:require [expectations :refer :all]
            [yesql.types :refer :all]))

(def select-query
  (->Query "select-something" "my docstring" "SELECT * FROM table WHERE x = :x"))

(def update-query
  (->Query "update-something!" "my docstring" "UPDATE table SET x = 0 WHERE x = :x"))

(let [tokens (flatten (emit-def select-query))]
  (expect (some #{`clojure.java.jdbc/query} tokens))
  (expect (not-any? #{`clojure.java.jdbc/execute!} tokens)))

(let [tokens (flatten (emit-def update-query))]
  (expect (some #{`clojure.java.jdbc/execute!} tokens))
  (expect (not-any? #{`clojure.java.jdbc/query} tokens)))
