(defproject yesql "0.4.1-SNAPSHOT"
  :description "A Clojure library for using SQL"
  :url "https://github.com/krisajenkins/yesql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [instaparse "1.3.4"]]
  :scm {:name "git"
        :url "https://github.com/krisajenkins/yesql"}
  :profiles {:dev {:dependencies [[org.apache.derby/derby "10.11.1.1"]
                                  [expectations "2.0.9"]]
                   :plugins [[lein-autoexpect "1.2.2"]
                             [lein-expectations  "0.0.7"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-alpha1"]]}}
  :aliases {"test-all" ["with-profile" "+1.5:+1.6:+1.7" "expectations"]
            "test-ancient" ["expectations"]})
