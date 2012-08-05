(ns leiningen.embongo
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [leiningen.core.main :as main])
  (:import [java.net InetSocketAddress Proxy Proxy$Type ProxySelector]
           [de.flapdoodle.embedmongo MongoDBRuntime MongodExecutable MongodProcess]
           [de.flapdoodle.embedmongo.config MongodConfig MongodProcessOutputConfig RuntimeConfig]
           [de.flapdoodle.embedmongo.distribution GenericVersion]
           [de.flapdoodle.embedmongo.io NamedOutputStreamProcessor IStreamProcessor]
           [de.flapdoodle.embedmongo.runtime Network]))

(defn- add-proxy-selector! [proxy-host proxy-port]
  (let [default-selector (ProxySelector/getDefault)]
    (ProxySelector/setDefault (proxy [ProxySelector] []
                                (select [uri]
                                  (if (= (.getHost uri) "fastdl.mongodb.org")
                                    (list (Proxy. Proxy$Type/HTTP (.InetSocketAddress proxy-host proxy-port)))
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

(def logger-config (MongodProcessOutputConfig.
                    (NamedOutputStreamProcessor. "[mongod output]" file-stream-processor)
                    (NamedOutputStreamProcessor. "[mongod error]" file-stream-processor)
                    file-stream-processor))

(def runtime-config
  (doto (RuntimeConfig.)
    (.setMongodOutputConfig logger-config)))

(defn- start-mongo [version port data-dir]
  (.. MongoDBRuntime
      (getInstance runtime-config)
      (prepare (MongodConfig. version port (Network/localhostIsIPv6) data-dir))
      (start)))

(defn- stop-mongo [mongod]
  (.stop mongod))

(defn- get-config-value [project x default]
  (if (nil? (project x)) default (project x)))

(defn embongo
  "Start an instance of MongoDB, run the given task, then stop MongoDB"
  [project task & args]
  (let [port (get-config-value project :mongo-port 27017)
        version (GenericVersion. (get-config-value project :mongo-version "2.0.6"))
        data-dir (project :mongo-data-dir)
        proxy-host (project :mongo-download-proxy-host)
        proxy-port (get-config-value project :mongo-download-proxy-port 80)]
    (if (not (nil? proxy-host)) (add-proxy-selector! proxy-host proxy-port))
    (let [mongod (start-mongo version port data-dir)]
      (try (main/apply-task task project args)
           (finally (stop-mongo mongod))))))
