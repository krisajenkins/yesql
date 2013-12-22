(ns yesql.util
  (:require [clojure.java.io :refer [as-file resource]])
  (:import [java.io FileNotFoundException]))

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

(defn whitespace?
  [string]
  (boolean
   (re-matches #"^\s*$" string)))

(defn underscores-to-dashes
  [string]
  (clojure.string/replace string "_" "-"))

(defn slurp-from-classpath
  "Slurps a file from the classpath."
  [path]
  (if-let [url (resource path)]
    (slurp url)
    (throw (FileNotFoundException. path))))
