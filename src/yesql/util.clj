(ns yesql.util
  (:refer-clojure :exclude [defrecord])
  (:require [clojure.java.io :as io]
            [instaparse.core :as instaparse]
            [clojure.string :as string]
            [clojure.core.typed :as t :refer [ann Seqable Option All IFn U Any]]
            [yesql.annotations])
  (:import (java.io FileNotFoundException StringWriter)
           (java.net URL)))

(ann ^:no-check instaparse.core/get-failure [Any -> (Option instaparse.gll.Failure)])
(ann ^:no-check instaparse.failure/pprint-failure [instaparse.gll.Failure -> nil])

(ann distinct-except
  (All [x]
       [[(Option x) -> Any] (Option (Seqable x)) -> (Seqable (Option x))]))
(defn distinct-except
  "Same as distinct, but keeps duplicates if they pass exception?"
  [exception? coll]
  (lazy-seq
   (when-let [[head & tail] (seq coll)]
     (cons head
           (distinct-except exception?
                            (if (exception? head)
                              tail
                              (remove #(= head %)
                                      tail)))))))

(ann whitespace?
  (IFn [nil -> false]
       [String -> Boolean]))
(defn whitespace?
  [string]
  (if string
    (boolean
     (re-matches #"^\s*$" string))
    false))

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

(ann process-instaparse-result
  (All [x]
       [(Seqable x) -> (Option x)]))
(defn process-instaparse-result
  [parsed]
  (if-let [failure (instaparse/get-failure parsed)]
    (binding [*out* (StringWriter.)]
      (instaparse.failure/pprint-failure failure)
      (throw (ex-info (.toString *out*)
                      failure)))
    (if (second parsed)
      (throw (ex-info "Ambiguous parse - please report this as a bug at https://github.com/krisajenkins/yesql/issues"
                      {:variations (count parsed)}))
      (first parsed))))
