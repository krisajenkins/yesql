(defproject yesql "0.5.0-rc2"
  :description "A Clojure library for using SQL"
  :url "https://github.com/krisajenkins/yesql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.7"]
                 [instaparse "1.4.0"]]
  :scm {:name "git"
        :url "https://github.com/krisajenkins/yesql"}
  :profiles {:dev {:dependencies [[expectations "2.1.1"]
                                  [org.apache.derby/derby "10.11.1.1"]]
                   :plugins [[lein-autoexpect "1.4.0"]
                             [lein-expectations "0.0.8"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7a {:dependencies [[org.clojure/clojure "1.7.0-alpha6"]]}
             :1.7b {:dependencies [[org.clojure/clojure "1.7.0-beta3"]]}}
  :aliases {"test-all" ["with-profile" "+1.5:+1.6:+1.7a:+1.7b" "do"
                        ["clean"]
                        ["expectations"]]
            "test-ancient" ["expectations"]})
