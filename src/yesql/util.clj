(ns yesql.util
  (:require [clojure.java.io :as io]
            [instaparse.core :as instaparse])
  (:import (java.io FileNotFoundException StringWriter)))

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

(defn str-non-nil
  "Exactly like `clojure.core/str`, except it returns an empty string
   with no args (whereas `str` would return `nil`)."
  [& args]
  (apply str "" args))

(defn slurp-from-classpath
  "Slurps a file from the classpath."
  [path]
  (if-let [url (io/resource path)]
    (slurp url)
    (throw (FileNotFoundException. path))))

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
   :else (first parsed)))
