(defproject dots "0.2.0-SNAPSHOT"
  :description "Dots server in clojure"
  :url "https://github.com/andts/clojure-dots"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [korma "0.3.0-RC5"]
                 [ring/ring-core "1.1.8"]
                 [ring/ring-jetty-adapter "1.1.8"]
                 [ring/ring-json "0.2.0"]
                 [compojure "1.1.5"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]
  :main dots.core.game)
