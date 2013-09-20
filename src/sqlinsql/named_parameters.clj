(ns sqlinsql.named-parameters)

(require '[clojure.tools.trace :refer :all])
(defn consume-to
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

(def end-of-parameter #{\space \, \" \' \: \& \; \( \) \| \= \+ \- \* \% \/ \\ \< \> \^})

(defn parse-parameter
  [text]
  (if (= (first text) \:)
    (consume-to (rest text) (constantly false) end-of-parameter)
    [nil text]))

(defn parse-quoted-string
  [text quote-char]
  (if (= (first text) quote-char)
    (consume-to (rest text) #{\\} #{quote-char})
    [nil text]))

(defn split-at-parameters
  [query]
  (loop [accumulator []
         chars []
         [head & tail :as remainder] query]
    (cond
     (empty? remainder) (conj accumulator (apply str chars))
     (and (= \: head)
          (= \: (first tail))) (recur accumulator
                                      (conj chars head (first tail))
                                      (rest tail))
          
          (= \: head) (let [[parameter marker next-bit] (parse-parameter remainder)]
                        (recur (conj accumulator
                                     (apply str chars)
                                     (symbol (apply str parameter)))
                               [marker]
                               next-bit)) 
          (= \' head) (let [[string marker next-bit] (parse-quoted-string remainder \')]
                        (recur accumulator
                               (into (conj chars head)
                                     (conj string marker))
                               next-bit)) 
          :else (recur accumulator
                       (conj chars head)
                       tail)))) 
