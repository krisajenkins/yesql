(ns yesql.annotations
  (:require [clojure.core.typed :as t :refer [ann Option Seq Seqable IFn U Any Map Keyword All]]
            [instaparse.core])
  (:import [java.net URL]))

(ann ^:no-check clojure.core/slurp (IFn [java.net.URL -> String]))
(ann ^:no-check clojure.core/re-matches [java.util.regex.Pattern String -> (Seq String)])
(ann ^:no-check clojure.java.io/resource [String -> (Option URL)])
(ann ^:no-check clojure.pprint/pprint (IFn [Any -> nil]))

(ann ^:no-check instaparse.core/parser [(U String URL) -> instaparse.core.Parser])
(ann ^:no-check instaparse.core/parses
  [instaparse.core.Parser String & :optional {:start Keyword} -> (Seqable Any)])
(ann ^:no-check instaparse.core/failure? [Any -> Boolean])
(ann ^:no-check instaparse.core/transform
  (All [x]
       [(Map Keyword (IFn [Any * -> x])) (Seqable Any) -> (Seqable x)]))
