(ns yesql.generate-test
  (:require [expectations :refer :all]
            [yesql.generate :refer :all]))

(expect #{}
        (expected-parameter-list "SELECT * FROM user"))

(expect #{:?}
        (expected-parameter-list "SELECT * FROM user WHERE user_id = ?"))

(expect #{:name}
        (expected-parameter-list "SELECT * FROM user WHERE user_id = :name"))


(expect #{:name :country :?}
        (expected-parameter-list "SELECT * FROM user WHERE user_id = :name AND country = :country AND age IN (?,?)"))

;;; Testing reassemble-query
(expect ["SELECT age FROM users WHERE country = ?" "gb"]
        (rewrite-query-for-jdbc "SELECT age FROM users WHERE country = :country"
                                {:country "gb"}))

(expect ["SELECT age FROM users WHERE (country = ? OR country = ?) AND name = ?" "gb" "us" "tom"]
        (rewrite-query-for-jdbc "SELECT age FROM users WHERE (country = ? OR country = ?) AND name = :name"
                                {:? ["gb" "us"]
                                 :name "tom"}))

;;; Testing reassemble-query IN strings.
(expect [true true true false false]
        (map in-list-parameter?
             (list []
                   (list)
                   (lazy-seq (cons 1 [2]))
                   {:a 1}
                   #{1 2 3})))

;;; Vectors trigger IN expansion
(expect ["SELECT age FROM users WHERE country = ? AND name IN (?,?,?)" "gb" "tom" "dick" "harry"]
        (rewrite-query-for-jdbc "SELECT age FROM users WHERE country = :country AND name IN (:names)"
                                {:country "gb"
                                 :names ["tom" "dick" "harry"]}))

;;; Lists trigger IN expansion
(expect ["SELECT age FROM users WHERE country = ? AND name IN (?,?,?)" "gb" "tom" "dick" "harry"]
        (rewrite-query-for-jdbc "SELECT age FROM users WHERE country = :country AND name IN (:names)"
                                {:country "gb"
                                 :names (list "tom" "dick" "harry")}))

;;; Lazy seqs of cons of vectors trigger IN expansion
(expect ["SELECT age FROM users WHERE country = ? AND name IN (?,?,?)" "gb" "tom" "dick" "harry"]
        (rewrite-query-for-jdbc "SELECT age FROM users WHERE country = :country AND name IN (:names)"
                                {:country "gb"
                                 :names (lazy-seq (cons "tom" ["dick" "harry"]))}))

;;; Maps do not trigger IN expansion
(expect [ "INSERT INTO json (source, data) VALUES (?, ?)" "google" {:a 1}]
        (rewrite-query-for-jdbc "INSERT INTO json (source, data) VALUES (:source, :data)"
                                {:source "google"
                                 :data {:a 1}}))

(expect [ "INSERT INTO json (data, source) VALUES (?, ?)" {:a 1} "google"]
        (rewrite-query-for-jdbc "INSERT INTO json (data, source) VALUES (:data, :source)"
                                {:source "google"
                                 :data {:a 1}}))

;;; Empty IN-lists are allowed by Yesql - though most DBs will complain.
(expect ["SELECT age FROM users WHERE country = ? AND name IN ()" "gb"]
        (rewrite-query-for-jdbc "SELECT age FROM users WHERE country = :country AND name IN (:names)"
                                {:country "gb"
                                 :names []}))

;;; Incorrect parameters.
(expect AssertionError
        (rewrite-query-for-jdbc "SELECT age FROM users WHERE country = :country AND name = :name"
                                {:country "gb"}))

(expect AssertionError
        (rewrite-query-for-jdbc "SELECT age FROM users WHERE country = ? AND name = ?"
                                {}))
