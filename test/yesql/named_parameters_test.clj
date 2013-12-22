(ns yesql.named-parameters-test
  (:require [clojure.test :refer :all]
            [yesql.named-parameters :refer :all]))

(defmacro split-test
  [category-name & args]
  "For convenience, run a series of tests of the form <string => [split pieces]>.
  The => symbol is mandatory sugar."
  (assert (zero? (mod (count args) 3)) "split-test requires three forms per case.")
  `(testing ~category-name
     ~(cons 'do
            (for [[string _ result] (partition 3 args)]
              `(is (= (split-at-parameters ~string)
                      (quote ~result)))))))

(deftest split-at-parameters-test
  (split-test "Simple"
              "SELECT 1 FROM dual"                    => ["SELECT 1 FROM dual"]
              "SELECT ? FROM dual"                    => ["SELECT " ? " FROM dual"]

              "SELECT :value FROM dual"               => ["SELECT " value " FROM dual"]
              "SELECT 'test'\nFROM dual"              => ["SELECT 'test'\nFROM dual"]
              "SELECT :value, :other_value FROM dual" => ["SELECT " value ", " other_value " FROM dual"])

  (split-test "Tokenization rules"
              "SELECT :age-5 FROM dual" => ["SELECT " age "-5 FROM dual"])

  (split-test "Mixing named & placeholder parameters"
              "SELECT :value, ? FROM dual" => ["SELECT " value ", " ? " FROM dual"])

  (split-test "Escapes"
              "SELECT :value, :other_value, ':not_a_value' FROM dual"
              => ["SELECT " value ", " other_value ", ':not_a_value' FROM dual"]

              "SELECT 'not \\' :a_value' FROM dual"
              => ["SELECT 'not \\' :a_value' FROM dual"])

  (split-test "Casting"
              "SELECT :value, :other_value, 5::text FROM dual"
              => ["SELECT " value ", " other_value ", 5::text FROM dual"])

  (split-test "Complex"
              "SELECT :a+2*:b+age::int FROM users WHERE username = ? AND :b > 0"
              => ["SELECT " a "+2*" b "+age::int FROM users WHERE username = " ? " AND " b " > 0"]

              "SELECT :value1 + ? + value2 + ? + :value1\nFROM SYSIBM.SYSDUMMY1"
              => ["SELECT " value1 " + " ? " + value2 + " ? " + " value1 "\nFROM SYSIBM.SYSDUMMY1"]))

(deftest reassemble-query-test
  (testing "Simple"
    (is (= (reassemble-query (split-at-parameters "SELECT age FROM users WHERE country = :country") ["gb"])
          ["SELECT age FROM users WHERE country = ?" "gb"])))

  (testing "List arguments"
    (is (= (reassemble-query (split-at-parameters "SELECT age FROM users WHERE country = :country AND name IN (:names)") ["gb" ["tom" "dick" "harry"]])
           ["SELECT age FROM users WHERE country = ? AND name IN (?,?,?)" "gb" "tom" "dick" "harry"])))

  (testing "Argument errors."
    (is (thrown? AssertionError
                 (reassemble-query (split-at-parameters "SELECT age FROM users WHERE country = :country AND name IN (:names)") ["gb"])))))
