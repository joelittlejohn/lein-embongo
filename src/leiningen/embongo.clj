(ns leiningen.embongo
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [leiningen.core.main :as main])
  (:import [de.flapdoodle.embed.mongo Command MongodExecutable MongodProcess MongodStarter MongodStarter]
           [de.flapdoodle.embed.mongo.config RuntimeConfigBuilder MongodConfig AbstractMongoConfig]
           [de.flapdoodle.embed.mongo.distribution Version]
           [de.flapdoodle.embed.process.config.io ProcessOutput]
           [de.flapdoodle.embed.process.distribution GenericVersion]
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

(defn- start-mongo [version port data-dir]
  (.. MongodStarter
      (getInstance runtime-config)
      (prepare (MongodConfig. version port (Network/localhostIsIPv6) data-dir))
      (start)))

(defn- config-value [project k default]
  (get (project :embongo) k default))

(defn embongo
  "Start an instance of MongoDB, run the given task, then stop MongoDB"
  [project & args]
  (let [port (config-value project :port 27017)
        version (GenericVersion. (config-value project :version "2.4.3"))
        data-dir (get-in project [:embongo :data-dir])
        proxy-host (get-in project [:embongo :download-proxy-host])
        proxy-port (config-value project :download-proxy-port 80)]
    (when (not (nil? proxy-host)) (add-proxy-selector! proxy-host proxy-port))
    (let [mongod (start-mongo version port data-dir)]
      (if (seq args)
        (try (main/apply-task (first args) project (rest args))
             (finally (.stop mongod)))
        (while true (Thread/sleep 5000))))))
