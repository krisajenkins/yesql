(ns yesql.types
  (:require [clojure.core.typed :as t :refer [ann HMap]]))

(t/ann-record Query [name :- String
                     docstring :- String
                     statement :- String])
(defrecord Query
    [name docstring statement])
