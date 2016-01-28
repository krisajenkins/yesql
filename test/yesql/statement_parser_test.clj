(ns yesql.statement-parser-test
  (:require [expectations :refer :all]
            [clojure.template :refer [do-template]]
            [yesql.types :refer [map->Query]]
            [yesql.statement-parser :refer :all]))

(do-template [statement _ split-result]
  (do (expect (quote split-result)
              (tokenize statement))
      (expect (quote split-result)
              (tokenize (map->Query {:name "test"
                                     :doctstring "A test case."
                                     :statement statement}))))

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

  ;; Newlines are preserved.
  "SELECT :value, :other_value, 5::text\nFROM dual"
  => ["SELECT " value ", " other_value ", 5::text\nFROM dual"]

  ;; Complex
  "SELECT :a+2*:b+age::int FROM users WHERE username = ? AND :b > 0"
  => ["SELECT " a "+2*" b "+age::int FROM users WHERE username = " ? " AND " b " > 0"]

  "SELECT :value1 + ? + value2 + ? + :value1\nFROM SYSIBM.SYSDUMMY1"
  => ["SELECT " value1 " + " ? " + value2 + " ? " + " value1 "\nFROM SYSIBM.SYSDUMMY1"]

  "SELECT ARRAY [:value1] FROM dual"
  => ["SELECT ARRAY [" value1 "] FROM dual"])
