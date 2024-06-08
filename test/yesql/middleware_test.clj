(ns yesql.middleware-test)

(defn set-connection-middleware [ connection ]
  (fn [ query-fn ]
    (fn [args call-options]
      ;; Allow call-site specific connections to override whatever is
      ;; injected via the middleware. Important to allow this override
      ;; to let transactions be passed in at a query call site.
      (query-fn args (if (:connection call-options)
                       call-options
                       (assoc call-options :connection connection))))))

(def log-query-middleware
  (fn [ query-fn ]
    (fn [args call-options]
      (let [ query-name (get-in call-options [:query :name]) ]
        (println [ :begin query-name ])
        (let [ result (query-fn args call-options) ]
          (println [ :end query-name])
          result)))))

