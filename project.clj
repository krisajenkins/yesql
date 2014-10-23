(defproject yesql "0.5.0-beta3-SNAPSHOT"
  :description "A Clojure library for using SQL"
  :url "https://github.com/krisajenkins/yesql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [instaparse "1.3.4"]
                 [org.clojure/core.typed.rt "0.2.72"]]
  :scm {:name "git"
        :url "https://github.com/krisajenkins/yesql"}
  :profiles {:dev {:dependencies [[expectations "2.0.12"]
                                  [org.clojure/core.typed "0.2.72"]
                                  [org.apache.derby/derby "10.11.1.1"]]
                   :plugins [[lein-autoexpect "1.2.2"]
                             [lein-typed "0.3.5"]
                             [lein-expectations "0.0.7"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-alpha2"]]}}
  :aot [yesql.core]
  :jar-exclusions [#"^clojure.*" #"^instaparse.*"]
  :core.typed {:check [yesql.annotations
                       yesql.util
                       yesql.instaparse-util
                       yesql.queryfile-parser
                       yesql.statement-parser
                       yesql.types]}
  :aliases {"test-all" ["with-profile" "+1.5:+1.6:+1.7" "do"
                        ["clean"]
                        ["typed" "check"]
                        ["expectations"]]
            "test-ancient" ["expectations"]})
