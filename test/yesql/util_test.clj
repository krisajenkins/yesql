(ns yesql.util-test
  (:require [expectations :refer :all]
            [clojure.template :refer [do-template]]
            [yesql.util :refer :all]))

;;; Test underscores-to-dashes
(do-template [input output]
  (expect output
          (underscores-to-dashes input))

  "nochange" "nochange"
  "current_time" "current-time"
  "this_is_it" "this-is-it")

;;; Test slurp-from-classpath
(expect #"\bSELECT\b"
        (slurp-from-classpath "yesql/sample_files/current_time.sql"))

(expect java.io.FileNotFoundException
        (slurp-from-classpath "nothing/here"))

;;; Test str-non-nil
(expect ""
        (str-non-nil))

(expect "1"
        (str-non-nil 1))

(expect ":a2cd"
        (str-non-nil :a 2 'c "d"))
