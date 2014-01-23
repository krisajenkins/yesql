(ns yesql.types
  (:require [clojure.java.jdbc :as jdbc]
            [yesql.named-parameters :refer [split-at-parameters reassemble-query]]
            [yesql.util :refer [distinct-except slurp-from-classpath]]))
(defrecord Query
    [name docstring querystring])

(defprotocol Definable
  (emit-def [query]))

(defn replace-question-mark-with-gensym
  [parameter]
  (if (= parameter '?)
    (gensym "P_")
    parameter))

(extend-type Query
  Definable
  (emit-def [{:keys [name docstring querystring]}]
    (let [dbsym (gensym "DB_")
          split-query (split-at-parameters querystring)
          arglist (filterv symbol? split-query)
          query-arglist (mapv replace-question-mark-with-gensym arglist)
          reassemble-arglist (vec (distinct query-arglist))
          function-arglist (into [dbsym] reassemble-arglist)
          arglist-without-question-marks (distinct-except arglist #{'?})
          display-arglist `([~'db ~@arglist-without-question-marks])]
      `(letfn [(reassemble# ~reassemble-arglist
                 (reassemble-query '~split-query ~query-arglist))]
         (def ~(with-meta (symbol name)
                          {:arglists `(quote ~display-arglist)
                           :doc docstring})
           (with-meta
             (fn ~function-arglist
               (->> (reassemble# ~@reassemble-arglist)
                    (jdbc/query ~dbsym)))
             {::query reassemble#}))))))

(defn ->query-vector
  [query-fn & args]
  (assert (-> query-fn meta ::query) "No SQL creation function attached.")
  (let [f (-> query-fn meta ::query)]
    (apply f args)))
