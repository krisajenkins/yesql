(ns yesql.util-test
  (:require [clojure.test :refer :all]
            [yesql.util :refer :all]))

(deftest underscores-to-dashes-test
  (is (= (underscores-to-dashes "nochange")
         "nochange"))
  (is (= (underscores-to-dashes "current_time")
         "current-time")))

(deftest slurp-from-classpath-test
  (is (re-find #"SELECT"
               (slurp-from-classpath "yesql/current_time.sql")))
  (is (thrown? java.io.FileNotFoundException
               (slurp-from-classpath "nothing/here"))))

(deftest classpath-file-basename-test
  (is (= (classpath-file-basename "yesql/current_time.sql")
         ["current_time" "sql"]))
  (is (= (classpath-file-basename "yesql/core_test.clj")
         ["core_test" "clj"])))
