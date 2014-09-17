(ns yesql.util
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [clojure.core.typed :as t :refer [ann Seqable Option All IFn U Any tc-ignore]]
            [yesql.annotations])
  (:import [java.io FileNotFoundException]))

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

(defn pprint-with-meta
  [thing]
  (when (instance? clojure.lang.IMeta thing)
    (print "^")
    (pprint (meta thing))
    (print " "))
  (pprint thing))

;;; TODO There may well be a built-in for this. If there is, I have not found it.
(tc-ignore
 (defn create-root-var
  "Given a name and a value, intern a var in the current namespace, taking metadata from the value."
  ([name value]
     (create-root-var *ns* name value))

  ([ns name value]
     (intern *ns*
             (with-meta (symbol name)
               (meta value))
             value))))
