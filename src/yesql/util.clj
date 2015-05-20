(ns yesql.util
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]])
  (:import [java.io FileNotFoundException]))

(defn underscores-to-dashes
  [string]
  (when string
    (string/replace string "_" "-")))

(defn str-non-nil
  "Exactly like `clojure.core/str`, except it returns an empty string
  with no args (whereas `str` would return `nil`)."
  [& args]
  (apply str "" args))

(defn slurp-from-classpath
  "Slurps a file from the classpath."
  [path]
  (or (some-> path
              io/resource
              slurp)
      (throw (FileNotFoundException. path))))

;;; TODO There may well be a built-in for this. If there is, I have not found it.
(defn create-root-var
  "Given a name and a value, intern a var in the current namespace, taking metadata from the value."
  ([name value]
   (create-root-var *ns* name value))

  ([ns name value]
   (intern *ns*
           (with-meta (symbol name)
             (meta value))
           value)))
