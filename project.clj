(defproject clubnub "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [cheshire "5.0.2"]
                 [digest "1.4.0"]
                 [clj-http "0.3.2"]]
  :profiles {:dev {:dependencies [[midje "1.5-RC1"]]}})
