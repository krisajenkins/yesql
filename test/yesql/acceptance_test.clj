(ns yesql.acceptance-test
  (:require [expectations :refer :all]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :refer :all])
  (:import [java.sql SQLException]
           [clojure.lang ExceptionInfo]))

(def derby-db {:subprotocol "derby"
               :subname (gensym "memory:")
               :create true})

;;; Single query.
(defquery current-time "yesql/sample_files/acceptance_test_single.sql")

(expect java.util.Date
        (-> (current-time derby-db)
            first
            :time))

;;; Multiple-query workflow.
(defqueries "yesql/sample_files/acceptance_test_combined.sql")

;; Create
(expect (create-person-table! derby-db))

;; Insert -> Select.
(expect {:1 1M} (insert-person<! derby-db "Alice" 20))
(expect {:1 2M} (insert-person<! derby-db "Bob" 25))
(expect {:1 3M} (insert-person<! derby-db "Charlie" 35))

(expect 3 (count (find-older-than derby-db 10)))
(expect 1 (count (find-older-than derby-db 30)))
(expect 0 (count (find-older-than derby-db 50)))

;;; Select with IN.
(expect 2 (count (find-by-age derby-db [20 35])))

;; Update -> Select.
(expect 1 (update-age! derby-db 38 "Alice"))
(expect 0 (update-age! derby-db 38 "David"))

(expect 3 (count (find-older-than derby-db 10)))
(expect 2 (count (find-older-than derby-db 30)))
(expect 0 (count (find-older-than derby-db 50)))

;; Delete -> Select.
(expect 1 (delete-person! derby-db "Alice"))

(expect 2 (count (find-older-than derby-db 10)))
(expect 1 (count (find-older-than derby-db 30)))
(expect 0 (count (find-older-than derby-db 50)))

;; Failing transaction: Insert with abort.
;; Insert two rows in a transaction. The second throws a deliberate error, meaning no new rows created.
(expect 2 (count (find-older-than derby-db 10)))

(expect SQLException
        (jdbc/with-db-transaction [connection derby-db]
          (insert-person<! connection "Eamonn" 20)
          (insert-person<! connection "Bob" 25)))

(expect 2 (count (find-older-than derby-db 10)))

;; Not marking executes with an exclamation mark
(expect #"You must append a '!' to the name of INSERT/UPDATE/DELETE queries"
        (try
          (update-age derby-db 38 "Alice")
          (catch ExceptionInfo e (.getMessage e))))

;; Drop
(expect (drop-person-table! derby-db))
