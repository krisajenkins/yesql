(ns yesql.queryfile-parser
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str :refer [join trim]]
            [instaparse.core :as instaparse]
            [yesql.types :refer [map->Query]]
            [yesql.util :refer [str-non-nil]]
            [yesql.instaparse-util :refer [process-instaparse-result]]))

(def parser
  (let [url (io/resource "yesql/queryfile.bnf")]
    (assert url)
    (instaparse/parser url)))

(defn- rm-semicolon [s]
  (str/replace s #";$" ""))

(defn- separate [pred s]
  ((juxt filter remove) pred s))

(def parser-transforms
  {:whitespace str-non-nil
   :non-whitespace str-non-nil
   :newline str-non-nil
   :any str-non-nil
   :line str-non-nil
   :rest-of-line (fn [ & args ] (apply str-non-nil args))
   :info (fn [[_ key] & args ]
           [:info (keyword key) (edn/read-string (apply str-non-nil args))])
   :comment (fn [& args]
              [:comment (apply str-non-nil args)])
   :docstring (fn [& comments]
                [:docstring (trim (join (map second comments)))])
   :statement (fn [& lines]
                [:statement (rm-semicolon (trim (join lines)))])
   :query (fn [& args]
            (let [[info-args query-args] (separate #(= (first %) :info) args)
                  infos (reduce (fn [infos [_ k v]]
                                  (assoc infos k v)) {} info-args)]
              (map->Query (assoc (into {} query-args)
                                 :info infos))))
   :queries list})

(defn parse-tagged-queries
  "Parses a string with Yesql's defqueries syntax into a sequence of maps."
  [text]
  (process-instaparse-result
   (instaparse/transform parser-transforms
                         (instaparse/parses parser
                                            (str text "\n") ;;; TODO This is a workaround for files with no end-of-line marker.
                                            :start :queries))
   {}))
