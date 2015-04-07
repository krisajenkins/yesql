(ns yesql.types_test
  (:require [yesql.types :as types]
            [expectations :refer :all]))

(def unparseable-statement "CREATE FUNCTION foo() RETURNS int AS $$ DECLARE x int := 0; BEGIN RETURN (SELECT x+1); END; $$ LANGUAGE plpgsql;")

(require 'clojure.walk)
(clojure.walk/macroexpand-all '(let [fn-arg {:statement unparseable-statement
                              :name "foo-q!"
                              :docstring ""}
                      fn-code (#'types/emit-query-fn fn-arg)]
                  (with-redefs [yesql.types/execute-handler (fn [_ [s]] s)]
                    (eval fn-code) ;; creates #'foo-q!
                    (expect unparseable-statement (foo-q!)))))
