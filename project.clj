(defproject pipeline2-clj "0.1.0-SNAPSHOT"
  :description "Client library for the DAISY Pipeline2 Web Service API"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.codec "0.1.1"]
                 [org.clojure/data.xml "0.2.0-alpha6"]
                 [org.clojure/data.zip "1.0.0"]
                 [org.clojure/tools.logging "1.1.0"]
                 [clojure.java-time "0.3.2"]
                 [pandect "1.0.1"]
                 [crypto-random "1.2.1"]
                 [clj-http "3.12.1"]
                 [cprop "0.1.17"]
                 [slingshot "0.12.2"]
                 [babashka/fs "0.0.5"]]
  :repl-options {:init-ns pipeline2-clj.core}

  :profiles
  {:dev  {:jvm-opts ["-Dconf=dev-config.edn"]}
   :test {:jvm-opts ["-Dconf=test-config.edn"]}})
