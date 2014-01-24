(ns yesql.types-test
  (:require [clojure.test :refer :all]
            [yesql.types :refer :all]))

(def select-query
  (->Query "select-something" "my docstring" "SELECT * FROM table WHERE x = :x"))

(def update-query
  (->Query "update-something!" "my docstring" "UPDATE table SET x = 0 WHERE x = :x"))

(deftest emit-query-test
  (let [form (emit-def select-query)
        tokens (flatten form)]
    (is (some #{`clojure.java.jdbc/query} tokens))
    (is (not-any? #{`clojure.java.jdbc/execute!} tokens))))

(deftest emit-execute-test
  (let [form (emit-def update-query)
        tokens (flatten form)]
    (is (some #{`clojure.java.jdbc/execute!} tokens))
    (is (not-any? #{`clojure.java.jdbc/query} tokens))))
