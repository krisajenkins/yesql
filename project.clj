(defproject yesql "0.4.0"
  :description "A Clojure library for using SQL"
  :url "https://github.com/krisajenkins/yesql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [instaparse "1.2.14"]]
  :scm {:name "git"
        :url "https://github.com/krisajenkins/yesql"}
  :profiles {:dev {:dependencies [[org.apache.derby/derby "10.10.1.1"]
                                  [expectations "1.4.56"]]
                   :plugins [[lein-autoexpect "1.0"]] }
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0-beta1"]]}}
  :aliases {"test-all" ["with-profile" "+1.4:+1.5:+1.6" "test"]
            "test-ancient" ["test"]})
