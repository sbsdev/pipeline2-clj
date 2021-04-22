(defproject pipeline2-clj "0.1.0-SNAPSHOT"
  :description "Client library for the DAISY Pipeline2 Web Service API"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.codec "0.1.1"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "0.1.2"]
                 [clj-time "0.15.1"]
                 [pandect "0.5.4"]
                 [crypto-random "1.2.0"]
                 [clj-http "3.9.1"]
                 [slingshot "0.9.0"]]
  :repl-options {:init-ns pipeline2-clj.core})
