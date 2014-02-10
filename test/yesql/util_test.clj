(ns yesql.util-test
  (:require [expectations :refer :all]
            [yesql.util :refer :all]))

;;; Test distinct-except
(let [coll '[a b c a b]]
  (expect '[a b c a]
          (distinct-except coll #{'a}))
  (expect '[a b c b]
          (distinct-except coll #{'b}))
  (expect '[a b c]
          (distinct-except coll #{'c})))

;;; Test underscores-to-dashes
(given [input output] (expect output
                              (underscores-to-dashes input))
  "nochange" "nochange"
  "current_time" "current-time"
  "this_is_it" "this-is-it")

;;; Test whitespace?
(expect (whitespace? ""))
(expect (whitespace? " 	"))
(expect (not (whitespace? "a")))
(expect (not (whitespace? " q ")))

;;; Test slurp-from-classpath
(expect #"\bSELECT\b"
        (slurp-from-classpath "yesql/sample_files/current_time.sql"))

(expect java.io.FileNotFoundException
        (slurp-from-classpath "nothing/here"))


;;; Test str-all
(expect ""
        (str-all))

(expect "1"
        (str-all 1))

(expect ":a2cd"
        (str-all :a 2 'c "d"))
