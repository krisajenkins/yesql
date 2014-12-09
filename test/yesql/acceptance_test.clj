(ns yesql.acceptance-test
  (:require [expectations :refer :all]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :refer :all])
  (:import [java.sql SQLException SQLSyntaxErrorException SQLDataException]))

(def derby-db {:subprotocol "derby"
               :subname (gensym "memory:")
               :create true})

;;; Single query.
(defquery current-time
  "yesql/sample_files/acceptance_test_single.sql")

(expect java.util.Date
        (-> (current-time {} {:connection derby-db})
            first
            :time))

;;; Multiple-query workflow.
(defqueries
  "yesql/sample_files/acceptance_test_combined.sql"
  {:connection derby-db})

;; Create
(expect (create-person-table!))

;; Insert -> Select.
(expect {:1 1M} (insert-person<! {:name "Alice"
                                  :age 20}))
(expect {:1 2M} (insert-person<! {:name "Bob"
                                  :age 25}))
(expect {:1 3M} (insert-person<! {:name "Charlie"
                                  :age 35}))

(expect 3 (count (find-older-than {:age 10})))
(expect 1 (count (find-older-than {:age 30})))
(expect 0 (count (find-older-than {:age 50})))

;;; Select with IN.
(expect 2 (count (find-by-age {:age [20 35]})))

;; Update -> Select.
(expect 1 (update-age! {:age 38
                        :name "Alice"}))
(expect 0 (update-age! {:age 38
                        :name "David"}))

(expect 3 (count (find-older-than {:age 10})))
(expect 2 (count (find-older-than {:age 30})))
(expect 0 (count (find-older-than {:age 50})))

;; Delete -> Select.
(expect 1 (delete-person! {:name "Alice"}))

(expect 2 (count (find-older-than {:age 10})))
(expect 1 (count (find-older-than {:age 30})))
(expect 0 (count (find-older-than {:age 50})))

;; Failing transaction: Insert with abort.
;; Insert two rows in a transaction. The second throws a deliberate error, meaning no new rows created.
(expect 2 (count (find-older-than {:age 10})))

(expect SQLException
        (jdbc/with-db-transaction [connection derby-db]
          (insert-person<! {:name "Eamonn"
                            :age 20}
                           {:connection connection})
          (insert-person<! {:name "Bob"
                            :age 25}
                           {:connection connection} )))

(expect 2
        (count (find-older-than {:age 10})))

;;; Type error.
(expect SQLDataException
        (insert-person<! {:name 5
                          :age "Eamonn"}
                         {:connection derby-db}))

;; Drop
(expect (drop-person-table!))

;; Syntax error handling.
(defquery syntax-error
  "yesql/sample_files/syntax_error.sql"
  {:connection derby-db})

(expect SQLSyntaxErrorException
        (syntax-error))
