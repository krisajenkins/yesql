(ns yesql.middleware-test)

(def log-query-middleware
  (fn [ query-fn ]
    (fn [args call-options]
      (let [ query-name (get-in call-options [:query :name]) ]
        (println [ :begin query-name ])
        (let [ result (query-fn args call-options) ]
          (println [ :end query-name])
          result)))))

