(ns leiningen.embongo
  (:require [clojure.string :as string]
            [leiningen.core.main :as main])
  (:import [java.net InetSocketAddress Proxy Proxy$Type ProxySelector]
           [de.flapdoodle.embedmongo MongoDBRuntime MongodExecutable MongodProcess]
           [de.flapdoodle.embedmongo.config MongodConfig]
           [de.flapdoodle.embedmongo.distribution Version]
           [de.flapdoodle.embedmongo.runtime Network]))

(defn- get-version [version-as-string]
  (try 
    (Version/valueOf (str "V" (-> version-as-string
                                  (string/upper-case)
                                  (string/replace  "." "_"))))
    (catch IllegalArgumentException e
      (throw (RuntimeException.
              (str "Unrecognised MongoDB version '" version-as-string "', try one of the following " (reduce #(str %1 ", " %2) (.getEnumConstants Version))))))))

(defn- add-proxy-selector! [proxy-host proxy-port]
  (let [default-selector (ProxySelector/getDefault)]
    (ProxySelector/setDefault (proxy [ProxySelector] []
                                (select [uri]
                                  (if (= (.getHost uri) "fastdl.mongodb.org")
                                    (list (Proxy. Proxy$Type/HTTP (.InetSocketAddress proxy-host proxy-port)))
                                    (.select default-selector uri)))
                                (connectFailed [uri address ex] ())))))

(defn- start-mongo [version port data-dir]
  (.. MongoDBRuntime
      (getDefaultInstance)
      (prepare (MongodConfig. version port (Network/localhostIsIPv6) data-dir))
      (start)))

(defn- stop-mongo [mongod]
  (.stop mongod))

(defn embongo
  "Start an instance of MongoDB, run the given task, then stop MongoDB"
  [project task & args]
  (let [port (if (nil? (project :mongo-port)) 27017 (project :mongo-port))
        version (if (nil? (project :mongo-version)) Version/V2_1_1 (get-version (project :mongo-version)))
        data-dir (project :mongo-data-dir)
        proxy-host (project :mongo-download-proxy-host)
        proxy-port (if (nil? (project :mongo-download-proxy-port)) 80 (project :mongo-download-proxy-port))]
    (do (if (not (nil? proxy-host)) (add-proxy-selector! proxy-host proxy-port))
        (let [mongod (start-mongo version port data-dir)]
          (do
            (main/apply-task task project args)
            (stop-mongo mongod))))))
