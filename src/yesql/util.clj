(ns yesql.util
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [clojure.core.typed :as t :refer [ann Seqable Option All IFn U Any tc-ignore]]
            [yesql.annotations])
  (:import [java.io FileNotFoundException]))

(ann underscores-to-dashes
  (IFn [nil -> nil]
       [String -> String]))
(defn underscores-to-dashes
  [string]
  (when string
    (string/replace string "_" "-")))

(ann str-non-nil [Any * -> String])
(defn str-non-nil
  "Exactly like `clojure.core/str`, except it returns an empty string
  with no args (whereas `str` would return `nil`)."
  [& args]
  (apply str "" args))

(ann slurp-from-classpath
  [String -> (Option String)])
(defn slurp-from-classpath
  "Slurps a file from the classpath."
  [path]
  (or (some-> path
              io/resource
              slurp)
      (throw (FileNotFoundException. path))))
