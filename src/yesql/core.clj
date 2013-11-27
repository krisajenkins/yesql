(ns yesql.core
  (:require [clojure.java.jdbc :as sql]
            [yesql.named-parameters :refer :all]
            [yesql.util :refer :all]
            [yesql.parser :refer [extract-docstring extract-query]]))

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
        arglist (vec (filter symbol? split-query))
        query-arglist (mapv replace-question-mark-with-gensym arglist)
        function-arglist (vec (cons dbsym (distinct query-arglist)))
        display-arglist (vec (cons 'db (distinct-except arglist #{'?})))]
    `(def
       ~(with-meta name
          {:arglists `(quote ~(list display-arglist))
           :doc docstring})
       (fn ~function-arglist
         (sql/query ~dbsym
                    (reassemble-query '~split-query ~query-arglist))))))
