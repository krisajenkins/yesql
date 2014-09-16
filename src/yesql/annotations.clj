(ns yesql.annotations
  (:require [clojure.core.typed :as t :refer [ann Option Seq Seqable IFn U Any]])
  (:import [java.net URL]))

(ann ^:no-check clojure.core/slurp (IFn [java.net.URL -> String]))
(ann ^:no-check clojure.core/re-matches [java.util.regex.Pattern String -> (Seq String)])
(ann ^:no-check clojure.java.io/resource [String -> (Option URL)])
