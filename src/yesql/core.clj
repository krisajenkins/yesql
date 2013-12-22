(ns yesql.core
  (:require [yesql.named-parameters :refer [split-at-parameters reassemble-query]]
            [yesql.parser :refer [extract-docstring extract-query]]
            [yesql.util :refer [distinct-except slurp-from-classpath]]))

(defn- replace-question-mark-with-gensym
  [parameter]
  (if (= parameter '?)
    (gensym "P_")
    parameter))

(defmacro defquery
  "Defines a query function, as defined in the given SQL file.
   Any comments in that file will form the docstring."
  [name filename]
  (let [file (slurp-from-classpath filename)
        docstring (extract-docstring file)
        dbsym (gensym "DB_")
        query (extract-query file)
        split-query (split-at-parameters query)
        arglist (filterv symbol? split-query)
        query-arglist (mapv replace-question-mark-with-gensym arglist)
        function-arglist (into [dbsym] (distinct query-arglist))
        display-arglist  (list (into ['db] (distinct-except arglist #{'?})))]
    `(def
       ~(with-meta name
          {:arglists `(quote ~display-arglist)
           :doc docstring})
       (fn ~function-arglist
         (sql/query ~dbsym
                    (reassemble-query '~split-query
                                      ~query-arglist))))))
