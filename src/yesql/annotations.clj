(ns yesql.annotations
  (:require [clojure.core.typed :refer [ann Option Seq IFn]])
  (:import [java.net URL]))

(ann ^:no-check clojure.java.io/resource [String -> (Option URL)])
(ann ^:no-check clojure.core/slurp (IFn [(java.net.URL) -> String]))
(ann ^:no-check clojure.core/re-matches [java.util.regex.Pattern String -> (Seq String)])
