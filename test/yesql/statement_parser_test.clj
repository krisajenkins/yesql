(ns yesql.statement-parser-test
  (:require [expectations :refer :all]
            [clojure.template :refer [do-template]]
            [yesql.statement-parser :refer :all]))


(do-template [query _ split-result]
  (expect (quote split-result)
          (split-at-parameters query))

  ;; Simple tests
  "SELECT 1 FROM dual"                    => ["SELECT 1 FROM dual"]
  "SELECT ? FROM dual"                    => ["SELECT " ? " FROM dual"]

  "SELECT :value FROM dual"               => ["SELECT " value " FROM dual"]
  "SELECT 'test'\nFROM dual"              => ["SELECT 'test'\nFROM dual"]
  "SELECT :value, :other_value FROM dual" => ["SELECT " value ", " other_value " FROM dual"]


  ;; Tokenization rules
  "SELECT :age-5 FROM dual"
  => ["SELECT " age "-5 FROM dual"]

  ;; Mixing named & placeholder parameters
  "SELECT :value, ? FROM dual"
  => ["SELECT " value ", " ? " FROM dual"]

  ;; Escapes
  "SELECT :value, :other_value, ':not_a_value' FROM dual"
  => ["SELECT " value ", " other_value ", ':not_a_value' FROM dual"]

  "SELECT 'not \\' :a_value' FROM dual"
  => ["SELECT 'not \\' :a_value' FROM dual"]

  ;; Casting
  "SELECT :value, :other_value, 5::text FROM dual"
  => ["SELECT " value ", " other_value ", 5::text FROM dual"]

  ;; Complex
  "SELECT :a+2*:b+age::int FROM users WHERE username = ? AND :b > 0"
  => ["SELECT " a "+2*" b "+age::int FROM users WHERE username = " ? " AND " b " > 0"]

  "SELECT :value1 + ? + value2 + ? + :value1\nFROM SYSIBM.SYSDUMMY1"
  => ["SELECT " value1 " + " ? " + value2 + " ? " + " value1 "\nFROM SYSIBM.SYSDUMMY1"])

;;; Testing reassemble-query
(expect ["SELECT age FROM users WHERE country = ?" "gb"]
        (reassemble-query (split-at-parameters "SELECT age FROM users WHERE country = :country")
                          {:country "gb"}))

(expect [ "SELECT age FROM users WHERE (country = ? OR country = ?) AND name = ?" "gb" "us" "tom"]
        (reassemble-query (split-at-parameters "SELECT age FROM users WHERE (country = ? OR country = ?) AND name = :name")
                          {:? ["gb" "us"]
                           :name "tom"}))

;;; Testing reassemble-query IN strings.
(expect ["SELECT age FROM users WHERE country = ? AND name IN (?,?,?)" "gb" "tom" "dick" "harry"]
        (reassemble-query (split-at-parameters "SELECT age FROM users WHERE country = :country AND name IN (:names)")
                          {:country "gb"
                           :names ["tom" "dick" "harry"]}))

(expect AssertionError
        (reassemble-query (split-at-parameters "SELECT age FROM users WHERE country = :country AND name = :name")
                          {:country "gb"}))

(expect AssertionError
        (reassemble-query (split-at-parameters "SELECT age FROM users WHERE country = ? AND name = ?")
                          {}))
