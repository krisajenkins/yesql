(defproject yesql "0.5.1"
  :description "A Clojure library for using SQL"
  :url "https://github.com/krisajenkins/yesql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [instaparse "1.4.1" :exclusions [org.clojure/clojure]]]
  :pedantic? :abort
  :scm {:name "git"
        :url "https://github.com/krisajenkins/yesql"}
  :profiles {:dev {:dependencies [[expectations "2.1.3" :exclusions [org.clojure/clojure]]
                                  [org.apache.derby/derby "10.11.1.1"]]
                   :plugins [[lein-autoexpect "1.4.0"]
                             [lein-expectations "0.0.8"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0-beta2"]]}}
  :aliases {"test-all" ["with-profile" "+1.5:+1.6:+1.7:+1.8" "do"
                        ["clean"]
                        ["expectations"]]
            "test-ancient" ["expectations"]})
