(ns leiningen.embongo
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [leiningen.core.main :as main])
  (:import [de.flapdoodle.embed.mongo Command MongodStarter]
           [de.flapdoodle.embed.mongo.config RuntimeConfigBuilder MongodConfigBuilder Net Storage]
           [de.flapdoodle.embed.mongo.distribution Feature Version Versions]
           [de.flapdoodle.embed.process.config.io ProcessOutput]
           [de.flapdoodle.embed.process.distribution IVersion]
           [de.flapdoodle.embed.process.io IStreamProcessor NamedOutputStreamProcessor]
           [de.flapdoodle.embed.process.runtime Network]
           [java.net InetSocketAddress Proxy Proxy$Type ProxySelector]))

(defn- add-proxy-selector! [proxy-host proxy-port]
  (let [default-selector (ProxySelector/getDefault)]
    (ProxySelector/setDefault (proxy [ProxySelector] []
                                (select [uri]
                                  (if (= (.getHost uri) "fastdl.mongodb.org")
                                    (list (Proxy. Proxy$Type/HTTP (InetSocketAddress. proxy-host proxy-port)))
                                    (.select default-selector uri)))
                                (connectFailed [uri address ex] ())))))

(def logging-lock (Object.))
(def file-stream-processor
  (proxy [IStreamProcessor] []
    (process [block]
      (locking logging-lock
        (with-open [writer (io/writer "embongo.log" :append true)]
          (.write writer block))))
    (onProcessed []
      (.process this "\n"))))

(def logger (ProcessOutput.
             (NamedOutputStreamProcessor. "[mongod output]" file-stream-processor)
             (NamedOutputStreamProcessor. "[mongod error]" file-stream-processor)
             (NamedOutputStreamProcessor. "[mongod commands]" file-stream-processor)))

(def runtime-config
  (-> (RuntimeConfigBuilder.)
      (.defaults Command/MongoD)
      (.processOutput logger)
      (.build)))

(defn- parse-version [v]
  (try
    (Version/valueOf v)
    (catch IllegalArgumentException e
      (Versions/withFeatures (reify IVersion (asInDownloadPath [_] v)) (make-array Feature 0)))))

(defn- start-mongo [version port data-dir]
  (.. MongodStarter
      (getInstance runtime-config)
      (prepare (.. (MongodConfigBuilder.)
                   (version (parse-version version))
                   (net (Net. port (Network/localhostIsIPv6)))
                   (replication (Storage. data-dir nil 0)) (build)))
      (start)))

(defn- config-value [project k & [default]]
  (get (project :embongo) k default))

(defn embongo
  "Start an instance of MongoDB, run the given task, then stop MongoDB"
  [project & args]
  (let [port (config-value project :port 27017)
        version (config-value project :version "2.4.3")
        data-dir (config-value project :data-dir)
        proxy-host (config-value project :download-proxy-host)
        proxy-port (config-value project :download-proxy-port 80)]
    (when (not (nil? proxy-host)) (add-proxy-selector! proxy-host proxy-port))
    (println "lein-embongo: starting mongo" port version (or data-dir ""))
    (let [mongod (start-mongo version port data-dir)]
      (if (seq args)
        (try
          (main/apply-task (first args) project (rest args))
          (finally (.stop mongod)))
        (while true (Thread/sleep 5000))))))
