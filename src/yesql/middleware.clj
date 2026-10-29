(ns yesql.middleware)

(defn set-connection
  "Query middleware that supplies the database connection for each
  query. connection is either a connection (anything clojure.java.jdbc
  accepts) or a function of no arguments that returns one; a function
  is called each time a query runs, so it can supply a connection that
  changes over time (a per-request or thread-bound connection, for
  example).

  A :connection passed in a query's call-options takes precedence over
  the one supplied here, so a query can still be run against a
  specific connection, such as an open transaction."
  [connection]
  (fn [query-fn]
    (fn [args call-options]
      (query-fn args (if (:connection call-options)
                       call-options
                       (assoc call-options
                              :connection (if (fn? connection)
                                            (connection)
                                            connection)))))))
