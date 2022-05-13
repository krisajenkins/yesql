(defproject yesql "0.5.3"
  :description "A Clojure library for using SQL"
  :url "https://github.com/krisajenkins/yesql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/java.jdbc "0.7.12" :exclusions [org.clojure/clojure]]
                 [instaparse "1.4.12" :exclusions [org.clojure/clojure]]]
  :pedantic? :abort
  :scm {:name "git"
        :url "https://github.com/krisajenkins/yesql"}
  :profiles {:dev {:dependencies [[expectations "2.1.8" :exclusions [org.clojure/clojure]]
                                  [org.apache.derby/derby "10.15.2.0"]]
                   :plugins [[lein-autoexpect "1.4.0"]
                             [lein-expectations "0.0.8" :exclusions [org.clojure/clojure]]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
             :1.11 {:dependencies [[org.clojure/clojure "1.11.1"]]}}
  :aliases {"test-all" ["with-profile" "+1.7:+1.8:+1.9:+1.10:+1.11" "do"
                        ["clean"]
                        ["expectations"]]
            "test-ancient" ["expectations"]})
