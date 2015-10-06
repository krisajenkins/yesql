(ns yesql.core-test
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :refer [upper-case]]
            [expectations :refer :all]
            [yesql.core :refer :all]))

(def derby-db {:subprotocol "derby"
               :subname (gensym "memory:")
               :create true})

;;; Test-environment check. Can we actually access the test DB?
(expect (more-> java.sql.Timestamp (-> first :1))
        (jdbc/query derby-db
                    ["SELECT CURRENT_TIMESTAMP FROM SYSIBM.SYSDUMMY1"]))

(defquery current-time-query
  "yesql/sample_files/current_time.sql"
  {:connection derby-db})

(defquery mixed-parameters-query
  "yesql/sample_files/mixed_parameters.sql"
  {:connection derby-db})

;;; Test querying.
(expect (more-> java.util.Date
                (-> first :time))
        (current-time-query))

(expect (more-> java.util.Date
                (-> first :time))
        (mixed-parameters-query {:value1 1
                                 :value2 2
                                 :? [3 4]}))

(expect empty?
        (mixed-parameters-query {:value1 1
                                 :value2 2
                                 :? [0 0]}))

;;; Processor functions
(expect (more-> java.util.Date :time)
        (current-time-query {} {:result-set-fn first}))

(expect (more-> java.util.Date first)
        (current-time-query {} {:row-fn :time}))

(expect (more-> java.util.Date (-> first :TIME))
        (current-time-query {} {:identifiers upper-case}))

(expect java.util.Date
        (current-time-query {} {:result-set-fn first
                                :identifiers clojure.string/upper-case
                                :row-fn :TIME}))

;;; Test comment rules.
(defquery inline-comments-query
  "yesql/sample_files/inline_comments.sql"
  {:connection derby-db})

(expect (more-> java.util.Date :time
                "Not -- a comment" :string)
        (inline-comments-query {} {:result-set-fn first}))

;;; Test Metadata.
(expect (more-> "Just selects the current time.\nNothing fancy." :doc
                'current-time-query :name
                (list '[] '[{} {:keys [connection]}]) :arglists)
        (meta (var current-time-query)))

(expect (more->  "Here's a query with some named and some anonymous parameters.\n(...and some repeats.)" :doc
                 'mixed-parameters-query :name
                 true (-> :arglists list?)
                 ;; TODO We could improve the clarity of what this is testing.
                 2 (-> :arglists count)

                 1 (-> :arglists first count)
                 #{'? 'value1 'value2} (-> :arglists first first   :keys set)

                 2 (-> :arglists second count)
                 #{'? 'value1 'value2} (-> :arglists second first  :keys set)
                 #{'connection}        (-> :arglists second second :keys set))
        (meta (var mixed-parameters-query)))

;; Running a query in a transaction and using the result outside of it should work as expected.
(expect-let [[{time :time}] (jdbc/with-db-transaction [connection derby-db]
                              (current-time-query {}
                                                  {:connection connection}))]
  java.util.Date
  time)

;;; Check defqueries returns the list of defined vars.
(expect-let [return-value (defqueries "yesql/sample_files/combined_file.sql")]
  (repeat 3 clojure.lang.Var)
  (map type return-value))

;;; SQL's quoting rules.
(defquery quoting "yesql/sample_files/quoting.sql")

(expect "'can't'"
        (:word (first (quoting {}
                               {:connection derby-db}))))

;;; Switch into a fresh namespace
(ns yesql.core-test.test-require-sql
  (:require [expectations :refer :all]
            [yesql.core :refer :all]))

(require-sql ["yesql/sample_files/combined_file.sql" :as combined])

(expect var? #'combined/edge)

(require-sql ["yesql/sample_files/combined_file.sql" :refer [the-time]])

(expect var? #'the-time)
