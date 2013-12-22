(ns yesql.util-test
  (:require [clojure.test :refer :all]
            [yesql.util :refer :all]))

(deftest distinct-except-test
  (let [coll '[a b c a b]]
    (is (= (distinct-except coll #{'a})
           '[a b c a]))
    (is (= (distinct-except coll #{'b})
           '[a b c b]))
    (is (= (distinct-except coll #{'c})
           '[a b c]))))

(deftest underscores-to-dashes-test
  (is (= (underscores-to-dashes "nochange")
         "nochange"))
  (is (= (underscores-to-dashes "current_time")
         "current-time")))

(deftest whitespace?-test
  (is (whitespace? ""))
  (is (whitespace? " 	"))
  (is (not (whitespace? "a")))
  (is (not (whitespace? " q "))))

(deftest slurp-from-classpath-test
  (is (re-find #"SELECT"
               (slurp-from-classpath "yesql/sample_files/current_time.sql")))
  (is (thrown? java.io.FileNotFoundException
               (slurp-from-classpath "nothing/here"))))
