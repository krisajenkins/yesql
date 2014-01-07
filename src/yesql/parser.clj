(ns yesql.parser
  (:require [clojure.algo.monads :refer [domonad state-m]]
            [clojure.string :refer [join split-lines trim]]
            [yesql.types :refer [->Query]]
            [yesql.util :refer [slurp-from-classpath]]))

(def yesql-name-tag-regex #"^\p{Blank}*--\p{Blank}*(name):(.*)")
(def sql-comment-regex    #"^\p{Blank}*--(.*)")

(defn- extract-comment
  "If the given line is an sql comment, returns the text of that comment."
  [line]
  (if line
    (if-let [matches (re-matches sql-comment-regex line)]
      (trim (second matches)))))

(defn- extract-yesql-name-tag
  "If the given line is a name tag header, returns the name."
  [line]
  (if line
    (if-let [matches (re-matches yesql-name-tag-regex line)]
      (trim (nth matches 2)))))

(defn classify-sql-line
  [line]
  (cond
   (re-matches yesql-name-tag-regex line) :tag
   (re-matches sql-comment-regex line) :comment
   :else :query))

(defn seek-name-tag
  [[head & tail]]
  (if head
    (if-let [name-tag (extract-yesql-name-tag head)]
      [name-tag tail]
      (recur tail))))

(defn consume-comments
  [lines]
  (loop [accumulator []
         [head & tail :as lines] lines]
    (if (nil? head)
      [accumulator nil]
      (case (classify-sql-line head)
        :comment (recur (conj accumulator (extract-comment head)) tail)
        [accumulator lines]))))

(defn consume-query
  [lines]
  (loop [accumulator []
         [head & tail :as lines] lines]
    (if (nil? head)
      [accumulator nil]
      (case (classify-sql-line head)
        :query (recur (conj accumulator head) tail)
        :comment (recur accumulator tail)
        [accumulator lines]))))

(defn parse-one-tagged-query
  [lines]
  (if lines
    ((domonad state-m
         [name-tag      seek-name-tag
          comment-lines consume-comments
          query-lines   consume-query]

       (cond
        (nil? name-tag)      (throw (ex-info "No name tag found."
                                             {:lines lines}))
        (empty? query-lines) (throw (ex-info (format "No query found under tag '%s'" name-tag)
                                             {:name-tag name-tag
                                              :lines lines}))
        :else (->Query name-tag
                       (join "\n" comment-lines)
                       (join "\n" query-lines))))
     lines)))

(defn parse-tagged-queries
  [lines]
  (if-let [[parsed lines'] (parse-one-tagged-query lines)]
    (cons parsed
          (parse-tagged-queries lines'))))

(defn parse-tagged-query-file
  "Convenience function to turn a filename into a series of Query records."
  [filename]
  (-> filename
      slurp-from-classpath
      split-lines
      parse-tagged-queries))
