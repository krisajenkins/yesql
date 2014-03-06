(ns yesql.parser
  (:require [clojure.java.io :as io]
            [clojure.string :refer [join trim]]
            [clojure.core.typed :as t :refer [ann U]]
            [instaparse.core :as instaparse]
            [yesql.types :refer [map->Query]]
            [yesql.util :refer [process-instaparse-result str-non-nil]]
            [yesql.annotations])
  (:import [java.net URL]))

(ann instaparse/parser [(U String URL) -> instaparse.core.Parser])

(ann parser instaparse.core.Parser)
(def parser
  (some-> "yesql/defqueries.bnf"
          io/resource
          instaparse/parser))

(t/tc-ignore
 (def parser-transforms
   {:whitespace str-non-nil
    :non-whitespace str-non-nil
    :newline str-non-nil
    :any str-non-nil
    :line str-non-nil
    :comment (fn [& args]
               [:comment (apply str-non-nil args)])
    :docstring (fn [& comments]
                 [:docstring (trim (join (map second comments)))])
    :statement (fn [& lines]
                 [:statement (trim (join lines))])
    :query (fn [& args]
             (map->Query (apply merge {} args)))
    :queries list}))


(t/tc-ignore
 (defn parse-tagged-queries
   "Parses a string with Yesql's defqueries syntax into a sequence of `yesql.types.Query` records."
   [text]
   (process-instaparse-result
    (instaparse/transform parser-transforms
                          (instaparse/parses parser
                                             (str text "\n") ;;; TODO This is a workaround for files with no end-of-line marker.
                                             :start :queries)))))
