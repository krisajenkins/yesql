(ns yesql.integration-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :refer :all])
  (:import [java.sql SQLException]))

(def derby-db {:subprotocol "derby"
               :subname (gensym "memory:")
               :create true})

(defqueries "yesql/sample_files/integration.sql")

(deftest integration-test
  (testing "Create"
    (is (create-person-table! derby-db)))

  (testing "Insert"
    (is (= {:1 1} (insert-person! derby-db "Alice" 20)))
    (is (= {:1 2} (insert-person! derby-db "Bob" 25)))
    (is (= {:1 3} (insert-person! derby-db "Charlie" 35))))
  (testing "Select - Post-Insert"
    (is (= 3 (count (find-older-than derby-db 10))))
    (is (= 1 (count (find-older-than derby-db 30))))
    (is (= 0 (count (find-older-than derby-db 50)))))

  (testing "Update"
    (is (= 1 (update-age! derby-db 38 "Alice")))
    (is (= 0 (update-age! derby-db 38 "David"))))
  (testing "Select - Post-Update"
    (is (= 3 (count (find-older-than derby-db 10))))
    (is (= 2 (count (find-older-than derby-db 30))))
    (is (= 0 (count (find-older-than derby-db 50)))))

  (testing "Delete"
    (is (= 1 (delete-person! derby-db "Alice"))))
  (testing "Select - Post-Delete"
    (is (= 2 (count (find-older-than derby-db 10))))
    (is (= 1 (count (find-older-than derby-db 30))))
    (is (= 0 (count (find-older-than derby-db 50)))))

  ;; Insert two rows in a transaction, the second throws a deliberate error, meaning no new rows created.
  (testing "Failing transaction: Insert with abort."
    (jdbc/with-db-transaction [connection derby-db]
      (is (insert-person! connection "David" 20))
      (is (thrown? SQLException
                   (insert-person! connection "Bob" 25)))))
  (testing "Select - Post-Transaction"
    (is (= 2 (count (find-older-than derby-db 10)))))

  (testing "Drop"
    (is (drop-person-table! derby-db))))
