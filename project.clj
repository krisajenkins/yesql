(defproject sqlinsql "0.1.0-SNAPSHOT"
  :description "A Clojure library for using SQL"
  :url "https://github.com/krisajenkins/sqlinsql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.0-alpha5"]]
  :profiles {:dev {:dependencies [[org.apache.derby/derby "10.10.1.1"]]}}
  :scm {:name "git"
        :url "https://github.com/krisajenkins/sqlinsql"})
