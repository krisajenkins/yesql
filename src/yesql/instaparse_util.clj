(ns yesql.instaparse-util
  (:require [instaparse.core :as instaparse]
            [clojure.core.typed :as t :refer [ann Seqable Option All IFn U Any HMap]]
            [yesql.annotations])
  (:import [java.io StringWriter]))

(ann ^:no-check instaparse.core/get-failure [Any -> (Option instaparse.gll.Failure)])
(ann ^:no-check instaparse.failure/pprint-failure [instaparse.gll.Failure -> nil])

(ann process-instaparse-result
  (All [x]
       [(Seqable x) HMap -> (Option x)]))
(defn process-instaparse-result
  [parse-results context]
  (if-let [failure (instaparse/get-failure parse-results)]
    (binding [*out* (StringWriter.)]
      (instaparse.failure/pprint-failure failure)
      (throw (ex-info (.toString *out*)
                      failure)))
    (if (second parse-results)
      (throw (ex-info "Ambiguous parse - please report this as a bug at https://github.com/krisajenkins/yesql/issues"
                      {:variations (count parse-results)}))
      (first parse-results))))
