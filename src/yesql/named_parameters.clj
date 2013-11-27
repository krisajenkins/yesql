(ns yesql.named-parameters)

(defn- consume-to
  [text escape? marker?]
  (loop [accumulator []
         [head & remainder :as string] text]
    (cond (not head) [accumulator nil nil]
          (and (escape? head)
               (marker? (first remainder))) (recur (conj accumulator head (first remainder))
                                                   (rest remainder))
               (marker? head) [accumulator head remainder]
               :else (recur (conj accumulator head)
                            remainder))))

(defn split-at-parameters
  [query]
  (loop [chars []
         [head & tail :as remainder] query]
    (case head
      nil [(apply str chars)]
      \' (let [[string marker next-bit] (consume-to tail #{\\} #{\'})]
           (recur (into (conj chars head)
                        (conj string marker))
                  next-bit))
      \? (cons (apply str chars)
               (cons (symbol (str head))
                     (split-at-parameters tail)))
      \: (case (first tail)
           \: (recur (conj chars head (first tail))
                     (rest tail))
           (let [[parameter marker next-bit] (consume-to tail
                                                         (constantly false)
                                                         #{\space \newline \, \" \' \: \& \; \( \) \| \= \+ \- \* \% \/ \\ \< \> \^})]
             (cons (apply str chars)
                   (cons (symbol (apply str parameter))
                         (split-at-parameters (cons marker next-bit))))))
      (recur (conj chars head)
             tail))))

(defn- args-to-placehoders
  [args]
  (if-not (coll? args)
    "?"
    (clojure.string/join "," (repeat (count args) "?"))))

(defn reassemble-query
  [split-query args]
  (assert (= (count (filter symbol? split-query))
             (count args))
          "Query parameter count must match args count.")
  (loop [query-string ""
         final-args []
         [query-head & query-tail] split-query
         [args-head & args-tail :as remaining-args] args]
    (cond
     (nil? query-head) (vec (cons query-string final-args))
     (string? query-head) (recur (str query-string query-head)
                                 final-args
                                 query-tail
                                 remaining-args)
     (symbol? query-head) (recur (str query-string (args-to-placehoders args-head))
                                 (if (coll? args-head)
                                   (apply conj final-args args-head)
                                   (conj final-args args-head))
                                 query-tail
                                 args-tail))))
