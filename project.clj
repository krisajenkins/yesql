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
  :profiles {:dev {:dependencies [[expectations "2.0.9"]
                                  [org.apache.derby/derby "10.11.1.1"]
                                  [org.clojure/core.typed "0.2.68"]]
                   :plugins [[lein-autoexpect "1.2.2"]
                             [lein-typed "0.3.5"]
                             [lein-expectations "0.0.7"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-alpha1"]]}}
  :core.typed {:check [yesql.annotations
                       yesql.util
                       yesql.instaparse-util
                       yesql.queryfile-parser
                       yesql.statement-parser
                       yesql.types]}
  :aliases {"test-all" ["do"
                        ["typed" "check"]
                        ["with-profile" "+1.5:+1.6:+1.7" "expectations"]]
            "test-ancient" ["expectations"]})
