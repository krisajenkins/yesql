(defproject yesql "0.4.1-SNAPSHOT"
  :description "A Clojure library for using SQL"
  :url "https://github.com/krisajenkins/yesql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [instaparse "1.3.3"]]
  :scm {:name "git"
        :url "https://github.com/krisajenkins/yesql"}
  :profiles {:dev {:dependencies [[org.apache.derby/derby "10.10.2.0"]
                                  [expectations "1.4.56"]]
                   :plugins [[lein-autoexpect "1.0"]] }
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}}
  :aliases {"test-all" ["with-profile" "+1.4:+1.5:+1.6" "test"]
            "test-ancient" ["test"]})
