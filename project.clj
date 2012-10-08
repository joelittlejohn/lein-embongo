(defproject lein-embongo "0.1.4-SNAPSHOT"
  :description "lein plugin wrapper for the flapdoodle.do embedded MongoDB API"
  :url "https://github.com/joelittlejohn/lein-embongo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[de.flapdoodle.embedmongo/de.flapdoodle.embedmongo "1.17"]]
  :eval-in-leiningen true
  :plugins [[lein-release "1.0.1-SNAPSHOT"]]
  :lein-release {:deploy-via :clojars}

  :embongo {:port 37017
            :version "2.0.4"
            :data-dir "/tmp/xxx"})
