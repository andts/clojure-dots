(defproject dots "0.2.0-SNAPSHOT"
  :description "Dots server in clojure"
  :url "https://github.com/andts/clojure-dots"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [korma "0.3.0-RC5"]
                 [ring/ring-core "1.1.8"]
                 [ring/ring-json "0.2.0"]
                 [compojure "1.1.5"]
                 [http-kit "2.1.13"]
                 [clj-wamp "1.0.0-dyn-topic"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.clojure/tools.cli "0.2.2"]
                 [org.clojure/data.json "0.2.2"]
                 [org.clojure/core.match "0.2.0-beta3"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]
  :main dots.web.main)
