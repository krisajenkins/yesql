(defproject yesql "0.4.2"
  :description "A Clojure library for using SQL"
  :url "https://github.com/krisajenkins/yesql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [instaparse "1.3.6"]]
  :scm {:name "git"
        :url "https://github.com/krisajenkins/yesql"}
  :profiles {:dev {:dependencies [[org.apache.derby/derby "10.11.1.1"]
                                  [expectations "2.1.1"]]
                   :plugins [[lein-autoexpect "1.2.2"]
                             [lein-expectations  "0.0.7"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-RC1"]]}}
  :aliases {"test-all" ["with-profile" "+1.5:+1.6:+1.7" "expectations"]
            "test-ancient" ["expectations"]})
