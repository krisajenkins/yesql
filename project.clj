(defproject yesql "0.3.0"
  :description "A Clojure library for using SQL"
  :url "https://github.com/krisajenkins/yesql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.0"]
                 [org.clojure/algo.monads "0.1.4"]]
  :profiles {:dev {:dependencies [[org.apache.derby/derby "10.10.1.1"]]}}
  :scm {:name "git"
        :url "https://github.com/krisajenkins/yesql"})
