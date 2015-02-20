(defproject lein-embongo "0.2.3-SNAPSHOT"
  :description "lein plugin wrapper for the flapdoodle.do embedded MongoDB API"
  :url "https://github.com/joelittlejohn/lein-embongo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[de.flapdoodle.embed/de.flapdoodle.embed.mongo "1.31"]]
  :eval-in-leiningen true
  :repositories [["releases" {:url "https://clojars.org/repo"
                              :creds :gpg}]]

  :embongo {:port 37017
            :data-dir "/tmp/xxx"
            :version "2.4.1"})
