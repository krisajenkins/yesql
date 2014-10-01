(ns yesql.types
  {:core.typed  {:collect-only true}}
  (:require
    [clojure.core.typed :as t]
    [yesql.util :refer [create-root-var]]
    [yesql.generate :as g]))

(defprotocol FunctionGenerator
  (generate-fn [this options]))

(defprotocol VarGenerator
  (generate-var [this options]))

(t/ann-record Query [name :- String
                     docstring :- String
                     statement :- String])
(defrecord Query
  [name docstring statement]
  FunctionGenerator
  (generate-fn [this options]
    (g/generate-query-fn this options))
  VarGenerator
  (generate-var [this options]
    (create-root-var (:name this)
                     (generate-fn this options))))


