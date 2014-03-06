(ns yesql.util
  (:refer-clojure :exclude [defrecord])
  (:require [clojure.java.io :as io]
            [instaparse.core :as instaparse]
            [clojure.string :as string]
            [clojure.core.typed :refer [ann cf tc-ignore Seq Option All IFn U Nothing]]
            [yesql.annotations])
  (:import (java.io FileNotFoundException StringWriter)
           (java.net URL)))

(ann distinct-except
  (All [x]
       [(Option (Seq x)) (Option (Set x)) -> (Option (Seq x))]))
(defn distinct-except
  "Same as distinct, but keeps duplicates from the exceptions set."
  [coll exceptions]
  {:pre [(coll? coll)
         (set? exceptions)]}
  (let [step (fn step [xs seen]
               (lazy-seq
                ((fn [[f :as xs] seen]
                   (when-let [s (seq xs)]
                     (if (and (contains? seen f)
                              (not (exceptions f)))
                       (recur (rest s) seen)
                       (cons f (step (rest s) (conj seen f))))))
                 xs seen)))]
    (step coll #{})))

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

(ann str-non-nil [String * -> String])
(defn str-non-nil
  "Exactly like `clojure.core/str`, except it returns an empty string
   with no args (whereas `str` would return `nil`)."
  [& args]
  (apply str "" args))

(ann slurp-from-classpath
  [String -> (U String Nothing)])
(tc-ignore
 (defn slurp-from-classpath
   "Slurps a file from the classpath."
   [path]
   (if-let [url (io/resource path)]
     (slurp url)
     (throw (FileNotFoundException. path)))))

(tc-ignore
 (defn process-instaparse-result
   [parsed]
   (cond
    (instaparse/failure? parsed) (let [failure (instaparse/get-failure parsed)]
                                   (binding [*out* (StringWriter.)]
                                     (instaparse.failure/pprint-failure failure)
                                     (throw (ex-info (.toString *out*)
                                                     failure))))
    (< 1 (count parsed)) (throw (ex-info "Ambiguous parse - please report this as a bug at https://github.com/krisajenkins/yesql/issues"
                                         {:variations (count parsed)}))
    :else (first parsed))))
