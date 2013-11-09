(ns yesql.util
 (:require [clojure.java.io :refer [as-file resource]])
 (:import [java.io FileNotFoundException]))

(defn underscores-to-dashes
  [string]
  (clojure.string/replace string "_" "-"))

(defn slurp-from-classpath
  "Slurps a file from the classpath."
  [path]
  (if-let [url (resource path)]
    (slurp url)
    (throw (FileNotFoundException. path))))

(defn classpath-file-basename
  [path]
  (if-let [url (resource path)]
    (->> url
         as-file
         .getName
         (re-find #"(.*)\.(.*)?")
         rest)))
