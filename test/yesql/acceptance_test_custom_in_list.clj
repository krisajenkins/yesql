(ns yesql.acceptance-test-custom-in-list
  (:require [expectations :refer :all]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :refer :all])
  (:import [java.sql SQLException SQLSyntaxErrorException SQLDataException]))

(def derby-db {:subprotocol "derby"
               :subname (gensym "memory:")
               :create true})

;;; Multiple-query workflow.
(defqueries
  "yesql/sample_files/acceptance_test_combined.sql"
  {:connection derby-db :in-list-parameter-predicate set?})

;; Create
(expect (create-person-table!))

;; Insert -> Select.
(expect {:1 1M} (insert-person<! {:name "Alice"
                                  :age 20}))
(expect {:1 2M} (insert-person<! {:name "Bob"
                                  :age 25}))
(expect {:1 3M} (insert-person<! {:name "Charlie"
                                  :age 35}))

;;; Select with IN.
(expect 2 (count (find-by-age {:age #{20 35}})))

;; Drop
(expect (drop-person-table!))
