(ns sqlinsql.named-parameters-test
  (:require [clojure.test :refer :all]
            [sqlinsql.named-parameters :refer :all]))

(deftest split-at-parameters-test
  (testing "Simple"
    (is (= (split-at-parameters "SELECT 1 FROM dual")
           '["SELECT 1 FROM dual"]))
    (is (= (split-at-parameters "SELECT :value FROM dual")
           '["SELECT " value " FROM dual"]))
    (is (= (split-at-parameters "SELECT 'test' FROM dual")
           '["SELECT 'test' FROM dual"]))
    (is (= (split-at-parameters "SELECT :value, :other_value FROM dual")
           '["SELECT " value ", " other_value " FROM dual"])))
  (testing "Escapes"
    (is (= (split-at-parameters "SELECT :value, :other_value, ':not_a_value' FROM dual")
           '["SELECT " value ", " other_value ", ':not_a_value' FROM dual"])))
  (testing "Casting"
    (is (= (split-at-parameters "SELECT :value, :other_value, 5::text FROM dual")
           '["SELECT " value ", " other_value ", 5::text FROM dual"])))
  (testing "Complex"
    (is (= (split-at-parameters "SELECT :a+2*:b+age::int FROM users WHERE username = :username AND :b > 0")
           '["SELECT " a "+2*" b "+age::int FROM users WHERE username = " username " AND " b " > 0"])))
  )
