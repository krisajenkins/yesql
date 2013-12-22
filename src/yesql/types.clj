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
          function-arglist (into [dbsym] (distinct query-arglist))
          arglist-without-question-marks (distinct-except arglist #{'?})
          display-arglist `([~'db ~@arglist-without-question-marks])]
      `(def ~(with-meta (symbol name)
               {:arglists `(quote ~display-arglist)
                :doc docstring})
         (fn ~function-arglist
           (jdbc/query ~dbsym
                       (reassemble-query '~split-query
                                         ~query-arglist)))))))
